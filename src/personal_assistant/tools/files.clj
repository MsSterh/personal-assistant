(ns personal-assistant.tools.files
  "Safe filesystem tool functions exposed to the LLM.

  Reads and directory listing accept any path; write/edit/delete/move are
  containment-checked against CWD via `personal-assistant.paths`. Every fn
  returns a plain map suitable for serialization back to the LLM — failures are
  `{:error ...}`, never exceptions."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [personal-assistant.paths :as paths])
  (:import [java.io File]))

(defn read-file
  "Read any readable path. Relative paths resolve against the working directory;
  absolute paths may point anywhere. Returns `{:path .. :content ..}` or
  `{:error ..}`."
  [{:keys [path]}]
  (let [f (paths/resolve-path path)]
    (cond
      (not (.exists f)) {:error (str "File does not exist: " path)}
      (.isDirectory f)  {:error (str "Path is a directory, not a file: " path)}
      :else (try
              {:path path :content (slurp f)}
              (catch Exception e
                {:error (str "Could not read " path ": " (.getMessage e))})))))

(defn write-file
  "Create or overwrite a file inside CWD. Returns `{:path .. :written ..}` or
  `{:error ..}`."
  [{:keys [path content]}]
  (let [{:keys [^File path error]} (paths/validate-write-path path)]
    (if error
      {:error error}
      (try
        (when-let [parent (.getParentFile path)]
          (.mkdirs parent))
        (spit path (or content ""))
        {:path (.getPath path) :written (count (or content ""))}
        (catch Exception e
          {:error (str "Could not write " (.getPath path) ": " (.getMessage e))})))))

(defn append-file
  "Append `content` to the end of a file inside CWD, creating it if absent.
  Returns `{:path .. :appended ..}` or `{:error ..}`."
  [{:keys [path content]}]
  (let [{:keys [^File path error]} (paths/validate-write-path path)]
    (if error
      {:error error}
      (try
        (when-let [parent (.getParentFile path)]
          (.mkdirs parent))
        (spit path (or content "") :append true)
        {:path (.getPath path) :appended (count (or content ""))}
        (catch Exception e
          {:error (str "Could not append to " (.getPath path) ": " (.getMessage e))})))))

(defn edit-file
  "Exact string replacement inside an existing file in CWD. Errors when the
  match is missing or non-unique."
  [{:keys [path old-string new-string]}]
  (let [{:keys [^File path error]} (paths/validate-write-path path)]
    (if error
      {:error error}
      (cond
        (not (.exists path)) {:error (str "File does not exist: " (.getPath path))}
        (str/blank? old-string) {:error "old-string must not be empty"}
        :else
        (let [content (slurp path)
              occurrences (count (re-seq (re-pattern (java.util.regex.Pattern/quote old-string))
                                         content))]
          (cond
            (zero? occurrences) {:error (str "old-string not found in " (.getPath path))}
            (> occurrences 1)   {:error (str "old-string is not unique in " (.getPath path)
                                             " (" occurrences " matches)")}
            :else (try
                    (spit path (str/replace-first content old-string (or new-string "")))
                    {:path (.getPath path) :replaced 1}
                    (catch Exception e
                      {:error (str "Could not edit " (.getPath path) ": " (.getMessage e))}))))))))

(defn delete-file
  "Remove a file inside CWD."
  [{:keys [path]}]
  (let [{:keys [^File path error]} (paths/validate-write-path path)]
    (if error
      {:error error}
      (cond
        (not (.exists path)) {:error (str "File does not exist: " (.getPath path))}
        (.isDirectory path)  {:error (str "Refusing to delete a directory: " (.getPath path))}
        :else (if (.delete path)
                {:path (.getPath path) :deleted true}
                {:error (str "Could not delete " (.getPath path))})))))

(defn move-file
  "Move/rename a file; both source and destination must be inside CWD."
  [{:keys [source dest]}]
  (let [src (paths/validate-write-path source)
        dst (paths/validate-write-path dest)]
    (cond
      (:error src) {:error (:error src)}
      (:error dst) {:error (:error dst)}
      (not (.exists ^File (:path src))) {:error (str "Source does not exist: " source)}
      :else
      (let [^File s (:path src)
            ^File d (:path dst)]
        (try
          (when-let [parent (.getParentFile d)]
            (.mkdirs parent))
          (if (.renameTo s d)
            {:source (.getPath s) :dest (.getPath d) :moved true}
            ;; renameTo can fail across mounts; fall back to copy+delete.
            (do (io/copy s d)
                (.delete s)
                {:source (.getPath s) :dest (.getPath d) :moved true}))
          (catch Exception e
            {:error (str "Could not move " (.getPath s) " to " (.getPath d) ": "
                         (.getMessage e))}))))))

(defn list-dir
  "List entries of a directory (read-anywhere). Returns `{:path .. :entries ..}`
  where each entry is `{:name .. :dir? .. :size ..}`."
  [{:keys [path]}]
  (let [f (paths/resolve-path (or path "."))]
    (cond
      (not (.exists f))   {:error (str "Directory does not exist: " path)}
      (not (.isDirectory f)) {:error (str "Not a directory: " path)}
      :else
      (let [entries (->> (.listFiles f)
                         (sort-by #(.getName ^File %))
                         (mapv (fn [^File e]
                                 {:name (.getName e)
                                  :dir? (.isDirectory e)
                                  :size (.length e)})))]
        {:path (.getPath f) :entries entries}))))
