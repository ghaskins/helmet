(ns helmet.core
  (:require [clj-yaml.core :as yaml]
            [ubergraph.core :as uber]
            [ubergraph.alg :refer [topsort]]
            [me.raynes.fs :as fs]
            [me.raynes.conch :as sh])
  (:gen-class))

(sh/programs helm)

(defn load-desc [path]
  (let [source (fs/file path "Chart.yaml")]
    (when (fs/file? source)
      (into {} (yaml/parse-string (slurp source))))))

(defn get-deps [path]
  (-> (load-desc path)
      (assoc :path (fs/normalized path))
      (update :dependencies (fn [deps]
                              (->> deps
                                   (map :repository)
                                   (filter #(clojure.string/includes? % "file://"))
                                   (map #(fs/file path (subs % 7)))
                                   (map get-deps)
                                   (remove empty?))))))

(defn add-node [g {:keys [name path dependencies] :as node}]
  (as-> g $
        (uber/add-nodes-with-attrs $ [name {:path path}])
        (reduce (fn [acc dep]
                  (-> (add-node acc dep)
                      (uber/add-directed-edges [name (:name dep)]))) $ dependencies)))

(defn package [config graph chart]
  (println "building:" chart)
  (let [[_ {:keys [path]}] (uber/node-with-attrs graph chart)]
    (helm "dep" "build" path)))

(defn exec [{:keys [input] :as config}]
  (let [graph (add-node (uber/digraph) (get-deps input))
        targets (reverse (topsort graph))]
    (run! (partial package config graph) targets)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
