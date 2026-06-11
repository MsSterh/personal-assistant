(ns personal-assistant.paths
  "Path resolution and containment checks for the safe file tools.

  Reads may target any path; mutating operations (write/edit/delete/move) are
  confined to the current working directory. Containment is checked against the
  canonical real path of the target so symlink escapes are caught."
  (:require [clojure.java.io :as io])
  (:import [java.io File]))

(defn cwd-root
  "The canonical absolute File for the process working directory."
  ^File []
  (.getCanonicalFile (io/file (System/getProperty "user.dir"))))

(defn resolve-path
  "Resolve `path` to a canonical absolute File, relative to `root` (default
  `cwd-root`). Resolves `.`, `..`, and symlinks. For a not-yet-existing file the
  parent directory is canonicalized and the final name appended, so writes to a
  new file inside CWD still resolve correctly."
  (^File [path] (resolve-path path (cwd-root)))
  (^File [path ^File root]
   (let [f (io/file path)
         f (if (.isAbsolute f) f (io/file root path))]
     (if (.exists f)
       (.getCanonicalFile f)
       ;; Doesn't exist yet: canonicalize the parent so symlinks/.. in the
       ;; directory portion are resolved, then append the final segment.
       (let [parent (.getParentFile f)
             cparent (if parent (.getCanonicalFile parent) root)]
         (io/file cparent (.getName f)))))))

(defn within-cwd?
  "Does the resolved `file` fall inside `root` (default `cwd-root`)? The root
  itself counts as inside."
  ([^File file] (within-cwd? file (cwd-root)))
  ([^File file ^File root]
   (let [fp (.toPath file)
         rp (.toPath root)]
     (.startsWith fp rp))))

(defn validate-write-path
  "Validate a user-supplied `path` for a mutating operation. Returns
  `{:path <File>}` when the resolved path is inside CWD, otherwise
  `{:error <message>}`."
  ([path] (validate-write-path path (cwd-root)))
  ([path ^File root]
   (let [resolved (resolve-path path root)]
     (if (within-cwd? resolved root)
       {:path resolved}
       {:error (str "Path escapes the working directory: " path
                    " (resolved to " (.getPath resolved) ")")}))))
