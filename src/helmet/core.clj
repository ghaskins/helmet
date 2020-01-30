(ns helmet.core
  (:require [clj-yaml.core :as yaml]
            [ubergraph.core :as uber]
            [ubergraph.alg :refer [topsort]]
            [me.raynes.fs :as fs]
            [me.raynes.conch :as sh]
            [me.raynes.conch.low-level :as sh.ll]
            [clojure.string :as string]))

(defn- chart-yaml
  "Computes the file reference to the Chart.yaml for a given 'path'"
  [path]
  (fs/file path "Chart.yaml"))

(defn is-chart?
  "Simple predicate returning 'true' if the path appears to point to a Chart by detecting if a Chart.yaml is present"
  [path]
  (fs/file? (chart-yaml path)))

(defn- load-chart-yaml
  "Loads and parses the Chart.yaml"
  [path]
  (when (is-chart? path)
    (->> (chart-yaml path)
         (slurp)
         (yaml/parse-string))))

(defn- load-metadata-overrides
  "Loads and parses a metadata.yaml file, which is expected to contain details such as appVersion overrides"
  [path]
  (when (fs/file? path)
    (-> (slurp path)
        (yaml/parse-string :keywords false))))

(defn- file-scheme?
  "Predicate returning 'true' if the path includes a file:// scheme prefix"
  [path]
  (string/includes? path "file://"))

(defn- strip-file-scheme
  "Strips off the 'file://' scheme"
  [path]
  (subs path 7))

(defn- get-deps
  "Recursively builds a model of a Chart, including its direct and transitive dependencies of type 'file://'"
  [path]
  (-> (load-chart-yaml path)
      (assoc :path (fs/normalized path))
      (update :dependencies (fn [deps]
                              (->> deps
                                   (map :repository)
                                   (filter file-scheme?)
                                   (map strip-file-scheme)
                                   (map (partial fs/file path))
                                   (map get-deps)
                                   (remove empty?))))))

(defn- add-node
  "Recursively builds a DAG of Charts, where:

   * the Chart name is the node
   * each node has:
      * path attribute: containing the FQP to the Chart in the filesystem
      * an edge for each dependency
   "
  [g {:keys [name path dependencies] :as node}]
  (as-> g $
        (uber/add-nodes-with-attrs $ [name {:path path}])
        (reduce (fn [acc dep]
                  (-> (add-node acc dep)
                      (uber/add-directed-edges [name (:name dep)]))) $ dependencies)))

(defn- copy-chart
  "Copies a Chart from src to dst, cleaning up residual artifacts such as Chart.lock"
  [src dst]
  (fs/copy-dir src dst)
  (fs/delete (fs/file dst "Chart.lock")))

(defn update-dependency
  "Translates any file:// based dependencies to be relative to the current directory, since all
  of the charts are moved to the output directory for processing"
  [dep]
  (update dep :repository
          (fn [repository]
            (if (file-scheme? repository)
              (->> (strip-file-scheme repository)
                   (fs/base-name)
                   (str "file://../"))
              repository))))

(defn- get-appversion [metadata chart]
  (when-let [appversion (get-in metadata [chart "image" "tag"])]
    (string/replace appversion #"^v" "")))

(defn- update-chart
  "
  Perform any necessary transformations of the Chart.yaml by streaming it in and writing it back out:

   - Conditionally updates the 'appVersion' value.  The table of appVersions to replace is specified on input.
   - Refactors any file:// based dependencies since the Charts have been moved.

  N.B. Updating the version is optional.  If a replacement value is not found, Helmet will leave the value
       as specified in the original Chart.yaml alone.
  "
  [metadata chart path]
  (let [appversion (get-appversion metadata chart)]
    (as-> (load-chart-yaml path) $
          (cond-> $ (some? appversion) (assoc :appVersion appversion))
          (update $ :dependencies #(map update-dependency %))
          (yaml/generate-string $)
          (spit (chart-yaml path) $))))

(defn- command-args [command]
  (remove empty? (string/split command #" ")))

(defn- build-chart
  "Executes 'helm dep build' on the specified chart, using our computed DAG as an attribute reference"
  [{:keys [output command verbose] :as config} versions graph chart]
  (let [[_ {:keys [path]}] (uber/node-with-attrs graph chart)
        dir (fs/normalized (fs/file output chart))]
    (println (str "HELMET: building " chart))
    (when verbose (println (str "HELMET: running \"" command "\" in " dir)))
    (copy-chart path dir)
    (update-chart versions chart dir)
    (let [proc (apply sh.ll/proc (sh/add-proc-args (command-args command) {:dir dir}))]
      (sh.ll/stream-to-out proc :out)
      (sh.ll/stream-to-out proc :err)
      (let [exit-code @(future (sh.ll/exit-code proc))]
        (when-not (zero? exit-code)
          (throw (ex-info "command failed" {:command command :dir dir :exit-code exit-code})))))))

(defn exec
  "Primary entry point for Helmet.  Computes the file:// oriented DAG and then runs 'helm dep build' in reverse
  topological order"
  [{:keys [path metadata] :as config}]
  (let [graph (add-node (uber/digraph) (get-deps path))
        targets (reverse (topsort graph))
        appversions (load-metadata-overrides metadata)]
    (run! (partial build-chart config appversions graph) targets)))

