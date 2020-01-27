(defproject helmet "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clj-commons/clj-yaml "0.7.0"]
                 [ubergraph "0.8.2"]
                 [me.raynes/fs "1.4.6"]
                 [me.raynes/conch "0.8.0"]]
  :main ^:skip-aot helmet.core
  :target-path "target/%s"

  ;; nREPL by default starts in the :main namespace, we want to start in `user`
  ;; because that's where our development helper functions like (refresh) live.
  :repl-options {:init-ns user}

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.3.1"]]
                   :resource-paths ["test-resources"]}
             :uberjar {:aot :all}})
