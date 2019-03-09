(ns com.brunobonacci.oneconfig.backend
  (:refer-clojure :exclude [find load list]))

(defprotocol IConfigBackend

  (find [this config-entry])

  (load [this config-entry])

  (save [this config-entry])

  (list [this filters]))
