(ns com.brunobonacci.oneconfig.backend
  (:refer-clojure :exclude [find load list]))

(defprotocol IConfigBackend

  ;;
  ;; takes a `config-entry` to find in input and returns the best
  ;; match for it.  where a best match is a config entry in the given
  ;; backend with the specified `:key` and `:env` and a `:version`
  ;; which is exactly the same or a earlier `:version`
  ;; it returns `nil` if not found.
  ;;
  ;; `config-entry` example:
  ;; `{:key "system1" :env "dev" :version "6.5.4"}`
  ;;
  ;; returns:
  ;; {:env "dev", :key "system1", :version "6.2.0",
  ;;  :content-type "text/plain", :value "foo",
  ;;  :change-num 1552740481566}
  ;;
  (find [this config-entry])


  ;; it loads a specific `config-entry`. It must be an exact match
  ;; or `nil` is returned.
  ;; It is possible also to specify a `:change-num` for retrieving
  ;; a specific change, otherwise the latest one is returned.
  ;;
  ;;
  ;; `config-entry` example:
  ;; `{:key "system1" :env "dev" :version "6.2.0"}`
  ;; or
  ;; `{:key "system1" :env "dev" :version "6.2.0" :change-num 1552740481566}`
  ;;
  ;; returns:
  ;; {:env "dev", :key "system1", :version "6.2.0",
  ;;  :content-type "text/plain", :value "foo",
  ;;  :change-num 1552740481566}
  ;;
  (load [this config-entry])


  ;;
  ;; it saves a new config entry into the give backed
  ;; None of the existing entries will be overwritten
  ;; as each entry as a unique `:change-num` assigned.
  ;;
  ;; `config-entry` example:
  ;; {:key "system1" :env "dev" :version "6.2.0"
  ;;  :content-type "text/plain" :value "secret"}
  ;;
  (save [this config-entry])


  ;;
  ;; Returns a lazy sequence of all the config entries
  ;; stored in the backend except the values
  ;;
  ;; filters: is a map with the following keys
  ;; `:key` `:env` `:version` and are used are prefix
  ;; for the keys to search:
  ;; for example:
  ;; {:key "syst" :version "6."}
  ;;
  ;; returns
  ;;  ({ :key "system1"
  ;;     :env "dev",
  ;;     :version "6.2.4",
  ;;     :content-type "text/plain",
  ;;     :change-num 1552740164553,}
  ;;   ...)
  (list [this {:keys [key env version] :as filters}]))
