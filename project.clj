(defproject helmet "1.1.0"
  :description "A tool to manage transitive dependencies in Helm"
  :url "https://gitlab.com/manetu/tools/helmet"
  :plugins [[lein-bin "0.3.5"]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clj-commons/clj-yaml "0.7.0"]
                 [ubergraph "0.8.2"]
                 [me.raynes/fs "1.4.6"]
                 [me.raynes/conch "0.8.0"]
                 [org.clojure/tools.cli "0.4.2"]]
  :main ^:skip-aot helmet.main
  :target-path "target/%s"

  :bin {:name "helmet"
        :bin-path "target"
        :bootclasspath false}

  ;; nREPL by default starts in the :main namespace, we want to start in `user`
  ;; because that's where our development helper functions like (refresh) live.
  :repl-options {:init-ns user}

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.3.1"]]
                   :resource-paths ["test-resources"]}
             :uberjar {:aot :all}})
