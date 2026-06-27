(ns personal-assistant.tools.registry
  "Maps the file tools to OpenAI-format function schemas and dispatches parsed
  `tool_calls` from the LLM to the matching Clojure fn."
  (:require [cheshire.core :as json]
            [personal-assistant.tools.files :as files]
            [personal-assistant.tools.web :as web]))

;; Each entry: tool name -> {:fn .. :schema ..}. The schema is the OpenAI
;; function-calling definition sent with the chat request; :fn takes a map of
;; keyword args and returns a serializable result map.

(defn- string-prop
  ([desc] {:type "string" :description desc}))

(def tools
  {"read_file"
   {:fn files/read-file
    :schema {:type "function"
             :function {:name "read_file"
                        :description "Read the contents of a file at any local path (not restricted to the working directory)."
                        :parameters {:type "object"
                                     :properties {:path (string-prop "Path to the file to read.")}
                                     :required ["path"]}}}}

   "write_file"
   {:fn files/write-file
    :schema {:type "function"
             :function {:name "write_file"
                        :description "Create or overwrite a file. Restricted to inside the current working directory."
                        :parameters {:type "object"
                                     :properties {:path (string-prop "Path to the file, inside the working directory.")
                                                  :content (string-prop "Full contents to write.")}
                                     :required ["path" "content"]}}}}

   "append_file"
   {:fn files/append-file
    :schema {:type "function"
             :function {:name "append_file"
                        :description "Append content to the end of a file, creating it if it does not exist. Restricted to inside the current working directory."
                        :parameters {:type "object"
                                     :properties {:path (string-prop "Path to the file, inside the working directory.")
                                                  :content (string-prop "Content to append to the end of the file.")}
                                     :required ["path" "content"]}}}}

   "edit_file"
   {:fn files/edit-file
    :schema {:type "function"
             :function {:name "edit_file"
                        :description "Replace an exact, unique substring in an existing file. Restricted to inside the working directory."
                        :parameters {:type "object"
                                     :properties {:path (string-prop "Path to the file to edit.")
                                                  :old-string (string-prop "Exact text to replace; must appear exactly once.")
                                                  :new-string (string-prop "Replacement text.")}
                                     :required ["path" "old-string" "new-string"]}}}}

   "delete_file"
   {:fn files/delete-file
    :schema {:type "function"
             :function {:name "delete_file"
                        :description "Delete a file. Restricted to inside the current working directory."
                        :parameters {:type "object"
                                     :properties {:path (string-prop "Path to the file to delete.")}
                                     :required ["path"]}}}}

   "move_file"
   {:fn files/move-file
    :schema {:type "function"
             :function {:name "move_file"
                        :description "Move or rename a file. Both source and destination must be inside the working directory."
                        :parameters {:type "object"
                                     :properties {:source (string-prop "Current path of the file.")
                                                  :dest (string-prop "New path for the file.")}
                                     :required ["source" "dest"]}}}}

   "list_dir"
   {:fn files/list-dir
    :schema {:type "function"
             :function {:name "list_dir"
                        :description "List the entries of a directory at any local path (not restricted to the working directory)."
                        :parameters {:type "object"
                                     :properties {:path (string-prop "Directory path to list. Defaults to the working directory.")}
                                     :required []}}}}

   "web_search"
   {:fn (fn [args] (web/search web/*http-client* args))
    :schema {:type "function"
             :function {:name "web_search"
                        :description "Search the web (DuckDuckGo) for a query and return result titles, URLs, and snippets."
                        :parameters {:type "object"
                                     :properties {:query (string-prop "Search query.")}
                                     :required ["query"]}}}}

   "web_read"
   {:fn (fn [args] (web/read-url web/*http-client* args))
    :schema {:type "function"
             :function {:name "web_read"
                        :description "Fetch a web page by URL and return its content as readable plain text (default) or raw HTML."
                        :parameters {:type "object"
                                     :properties {:url (string-prop "URL of the page to fetch.")
                                                  :format {:type "string"
                                                           :enum ["text" "html"]
                                                           :description "Return format: \"text\" (default, readable text) or \"html\" (raw markup)."}}
                                     :required ["url"]}}}}})

(def tool-defs
  "Vector of OpenAI-format function schemas to send with the chat request."
  (mapv :schema (vals tools)))

(defn- parse-args
  "Parse a tool_call's arguments into a keyword map. Per OpenAI the arguments
  arrive as a JSON string; the test fake may pass a map directly."
  [arguments]
  (cond
    (string? arguments) (json/parse-string arguments true)
    (map? arguments) (json/parse-string (json/generate-string arguments) true)
    :else {}))

(defn dispatch
  "Given a parsed `tool_call` map (with `:id` and `:function` containing
  `:name` and `:arguments`), invoke the matching tool and return a
  `{:role \"tool\" :tool_call_id .. :content <json>}` message. Unknown tools
  and bad arguments produce a structured error result rather than throwing."
  [tool-call]
  (let [id (:id tool-call)
        fn-name (get-in tool-call [:function :name])
        entry (get tools fn-name)
        result (try
                 (if entry
                   (let [args (parse-args (get-in tool-call [:function :arguments]))]
                     ((:fn entry) args))
                   {:error (str "Unknown tool: " fn-name)})
                 (catch Exception e
                   {:error (str "Tool " fn-name " failed: " (.getMessage e))}))]
    {:role "tool"
     :tool_call_id id
     :content (json/generate-string result)}))
