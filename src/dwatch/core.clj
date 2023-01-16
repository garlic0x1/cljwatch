(ns dwatch.core
  (:require [clojure.java.shell :as shell]
            [clojure.core.async :as a :refer [<!]]
            [clojure.java.io :as io]))

(defonce state (atom {}))
(defonce procs (atom {}))
(def conf (read-string (slurp "dwatch.edn")))

(defn updated-file? [file]
  (let [fname (.getAbsolutePath file) lmod (.lastModified file)]
    (when (or (not (contains? @state (keyword fname)))
              (not= ((keyword fname) @state) lmod))
      (reset! state (conj @state {(keyword fname) lmod}))
      true)))

(defn updated-dir? [path]
  (some true? (doall (map updated-file? (file-seq (io/file path))))))

(defn restart-proc [cmd]
  (when-let [proc ((keyword cmd) @procs)]
    (.destroy proc))
  (swap! procs assoc (keyword cmd) (.exec (Runtime/getRuntime) cmd)))

(defn restart-procs [item]
  (run! restart-proc (:procs item)))

(defn job [item]
  (when (updated-dir? (:dir item))
    (println (map #(shell/sh "bash" "-c" %) (:scripts item)))
    (println (restart-procs item))))

(defn worker [item]
  (a/go (while true (job item) (<! (a/timeout 1000)))))
-
(defn main []
  (doall (map worker conf)))
