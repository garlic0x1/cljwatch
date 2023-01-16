(ns dwatch.core
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io]))

(println (shell/sh "echo" "hi"))
(defonce state (atom {}))
(def conf (slurp "dwatch.edn"))

(defn updated-file? [file]
  (let [fname (.getAbsolutePath file) lmod (.lastModified file)]
    (when (or (not (contains? @state (keyword fname)))
              (not= ((keyword fname) @state) lmod))
      (reset! state (conj @state {(keyword fname) lmod}))
      true)))

(defn updated-dir? [directory]
  (some? (doall (map updated-file? (file-seq directory)))))

(let [directory (io/file ".")]
  (updated-dir? directory))

(defn set-interval [callback ms]
  (future (while true (do (Thread/sleep ms) (callback)))))

(defn job [item]
  (run! #(println (shell/sh "-c" %)) (:scripts item)))
-
(doall (pmap #(set-interval (job %) 1000) conf))
