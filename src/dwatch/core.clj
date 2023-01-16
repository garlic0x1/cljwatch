(ns dwatch.core
  (:require [clojure.java.shell :as shell]
            [clojure.core.async :as a :refer [<! >!! <! >!]]
            [clojure.java.io :as io]))

(defonce state (atom {}))
(def conf (read-string (slurp "dwatch.edn")))

(defn updated-file? [file]
  (let [fname (.getAbsolutePath file) lmod (.lastModified file)]
    (when (or (not (contains? @state (keyword fname)))
              (not= ((keyword fname) @state) lmod))
      (println fname)
      (reset! state (conj @state {(keyword fname) lmod}))
      true)))

(defn updated-dir? [path]
  (some true? (doall (map updated-file? (file-seq (io/file path))))))

(defn job [item]
  (if (updated-dir? (:dir item))
      (println (map #(shell/sh "bash" "-c" %) (:scripts item)))
      nil))

(defn worker [item]
  (a/go
    (while true
      (job item)
      (<! (a/timeout 1000)))))
-
(defn main []
  (doall (map worker conf)))
