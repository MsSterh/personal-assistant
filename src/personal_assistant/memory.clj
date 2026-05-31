(ns personal-assistant.memory
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.time LocalDate]))

(defn today-str []
  (str (LocalDate/now)))

(defn load-user-profile
  ([] (load-user-profile "USER.md"))
  ([path]
   (let [f (io/file path)]
     (if (.exists f)
       (slurp f)
       (do (binding [*out* *err*]
             (println "Warning: USER.md not found, skipping user profile."))
           "")))))

(defn load-memory
  ([] (load-memory "MEMORY.md"))
  ([path]
   (let [f (io/file path)]
     (if (.exists f)
       (slurp f)
       (do (binding [*out* *err*]
             (println "Warning: MEMORY.md not found, skipping memory."))
           "")))))

(defn build-memory-context [profile memory]
  (let [parts (cond-> []
                (not (str/blank? profile)) (conj (str "## User Profile\n\n" (str/trim profile)))
                (not (str/blank? memory))  (conj (str "## Memory\n\n" (str/trim memory))))]
    (str/join "\n\n" parts)))

(def ^:private marker-pattern #"<!--\s*remember:\s*(.*?)\s*-->")

(defn extract-new-facts [response]
  (map second (re-seq marker-pattern response)))

(defn strip-markers [response]
  (-> response
      (str/replace marker-pattern "")
      (str/replace #" {2,}" " ")
      str/trim))

(defn append-facts-to-daily-note
  ([facts date-str] (append-facts-to-daily-note facts date-str "memory"))
  ([facts date-str base-dir]
   (let [dir  (io/file base-dir)
         file (io/file dir (str date-str ".md"))]
     (.mkdirs dir)
     (spit file
           (str (str/join "\n" (map #(str "- " %) facts)) "\n")
           :append true))))
