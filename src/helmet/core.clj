(ns helmet.core
  (:require [clj-yaml.core :as yaml]
            [ubergraph.core :as uber]
            [ubergraph.alg :refer [topsort]]
            [me.raynes.fs :as fs]
            [me.raynes.conch :as sh]
            [clojure.string :as string]))

(sh/programs helm)

(defn is-chart? [path]
  (fs/file? (fs/file path "Chart.yaml")))

(defn load-desc [path]
  (when (is-chart? path)
    (->> (fs/file path "Chart.yaml")
         (slurp)
         (yaml/parse-string)
         (into {}))))

(defn get-deps [path]
  (-> (load-desc path)
      (assoc :path (fs/normalized path))
      (update :dependencies (fn [deps]
                              (->> deps
                                   (map :repository)
                                   (filter #(string/includes? % "file://"))
                                   (map #(fs/file path (subs % 7)))
                                   (map get-deps)
                                   (remove empty?))))))

(defn add-node [g {:keys [name path dependencies] :as node}]
  (as-> g $
        (uber/add-nodes-with-attrs $ [name {:path path}])
        (reduce (fn [acc dep]
                  (-> (add-node acc dep)
                      (uber/add-directed-edges [name (:name dep)]))) $ dependencies)))

(defn build-deps [{:keys [output]} graph chart]
  (println "building:" chart)
  (let [[_ {:keys [path]}] (uber/node-with-attrs graph chart)
        dst-path (fs/normalized (fs/file output chart))]
    (fs/copy-dir path dst-path)
    (fs/delete (fs/file dst-path "Chart.lock"))
    (helm "dep" "build" dst-path {:verbose true})))

(defn exec [{:keys [path] :as config}]
  (let [graph (add-node (uber/digraph) (get-deps path))
        targets (reverse (topsort graph))]
    (run! (partial build-deps config graph) targets)))

