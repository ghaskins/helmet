(ns helmet.core
  (:require [clj-yaml.core :as yaml]
            [ubergraph.core :as uber]
            [ubergraph.alg :refer [topsort]]
            [me.raynes.fs :as fs]
            [me.raynes.conch :as sh]
            [clojure.string :as string]))

(sh/programs helm)

(defn- desc-path [path]
  (fs/file path "Chart.yaml"))

(defn is-chart? [path]
  (fs/file? (desc-path path)))

(defn- load-desc [path]
  (when (is-chart? path)
    (->> (desc-path path)
         (slurp)
         (yaml/parse-string))))

(defn- load-versions [path]
  (when (fs/file? path)
    (-> (slurp path)
        (yaml/parse-string :keywords false))))

(defn- get-deps [path]
  (-> (load-desc path)
      (assoc :path (fs/normalized path))
      (update :dependencies (fn [deps]
                              (->> deps
                                   (map :repository)
                                   (filter #(string/includes? % "file://"))
                                   (map #(fs/file path (subs % 7)))
                                   (map get-deps)
                                   (remove empty?))))))

(defn- add-node [g {:keys [name path dependencies] :as node}]
  (as-> g $
        (uber/add-nodes-with-attrs $ [name {:path path}])
        (reduce (fn [acc dep]
                  (-> (add-node acc dep)
                      (uber/add-directed-edges [name (:name dep)]))) $ dependencies)))

(defn- copy-chart [src dst]
  (fs/delete-dir dst)
  (fs/copy-dir src dst)
  (fs/delete (fs/file dst "Chart.lock")))

(defn- update-version [versions chart path]
  (let [version (get versions chart)]
    (as-> (load-desc path) $
          (cond-> $ (some? version) (assoc :appVersion version))
          (yaml/generate-string $)
          (spit (desc-path path) $))))

(defn- build-chart [{:keys [output] :as config} versions graph chart]
  (println "building:" chart)
  (let [[_ {:keys [path]}] (uber/node-with-attrs graph chart)
        dst-path (fs/normalized (fs/file output chart))]
    (copy-chart path dst-path)
    (update-version versions chart dst-path)
    (helm "dep" "build" dst-path {:verbose true})))

(defn exec [{:keys [path version-overrides] :as config}]
  (let [graph (add-node (uber/digraph) (get-deps path))
        targets (reverse (topsort graph))
        versions (load-versions version-overrides)]
    (run! (partial build-chart config versions graph) targets)))

