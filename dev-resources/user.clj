;; Copyright © 2020 Manetu, Inc.  All rights reserved

(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [helmet.core :as core]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]))

(def test-chart (fs/file (io/resource "foo")))

(defn exec []
  (core/exec {:input test-chart}))
