(ns personal-assistant.context
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- parse-frontmatter [content]
  (when (str/starts-with? content "---\n")
    (let [body (subs content 4)
          end  (str/index-of body "\n---")]
      (when end
        (yaml/parse-string (subs body 0 end))))))

(defn load-soul
  ([] (load-soul (io/resource "SOUL.md")))
  ([source]
   (let [content (slurp source)
         fm      (parse-frontmatter content)]
     {:role    "system"
      :content (if fm (:behavior fm) content)})))
