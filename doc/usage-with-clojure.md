# Usage with Clojure projects

In order to use the library add the dependency to your `project.clj`

``` clojure
;; Leiningen project
[com.brunobonacci/oneconfig "0.10.3"]

;; deps.edn format
{:deps { com.brunobonacci/oneconfig "0.10.3" }}
```

Latest version: [![Clojars Project](https://img.shields.io/clojars/v/com.brunobonacci/oneconfig.svg)](https://clojars.org/com.brunobonacci/oneconfig)


Then require the namespace:

``` clojure
(ns foo.bar
  (:require [com.brunobonacci.oneconfig :refer [configure]]))
```

Finally get the configuration for your service.

``` clojure
(configure {:key "service-name" :version "1.2.3" :env "prod"})
;;=> {...}
```