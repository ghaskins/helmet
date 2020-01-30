(ns helmet.main
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [helmet.core :as core]
            [me.raynes.fs :as fs])
  (:gen-class))

(def options
  [["-h" "--help"]
   ["-v" "--version" "Print the version and exit"]
   ["-p" "--path PATH" "The path to the Helm chart to package"
    :default "."
    :validate [core/is-chart? "Must be a path to a Helm chart"]]
   ["-o" "--output PATH" "The path for output files"
    :default "target"]
   [nil "--metadata PATH" "The path to a YAML table with metadata overrides"
    :validate [fs/file? "The metadata yaml must exist"]]
   ["-c" "--command COMMAND"
    :default "helm dep update --skip-refresh"]
   [nil "--verbose"]])

(defn exit [status msg & rest]
  (do
    (apply println msg rest)
    status))

(defn version [] (str "helmet version: v" (System/getProperty "helmet.version")))

(defn prep-usage [msg] (->> msg flatten (string/join \newline)))

(defn usage [options-summary]
  (prep-usage [(version)
               ""
               "Usage: helmet [options]"
               ""
               "Options:"
               options-summary]))

(defn -app
  [& args]
  (let [{{:keys [help path] :as options} :options :keys [arguments errors summary]} (parse-opts args options)]
    (cond

      help
      (exit 0 (usage summary))

      (not= errors nil)
      (exit -1 "Error: " (string/join errors))

      (:version options)
      (exit 0 (version))

      (not (core/is-chart? path))
      (exit -1 (str "\"" path "\" does not appear to be a Helm chart"))

      :else
      (try
        (core/exec options)
        0
        (catch clojure.lang.ExceptionInfo e
          (exit (-> e ex-data :exit-code) ""))))))

(defn -main
  [& args]
  (System/exit (apply -app args)))
