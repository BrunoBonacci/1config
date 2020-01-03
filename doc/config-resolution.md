# Configuration resolution

A configuration entry is uniquely identified by **key, environment and version**.
While resolving the specific configuration the system if going  to look for a
exact version match or a version which is smaller than the given one.

For this reason you don't have to publish a new configuration for every
version change. For example: let's assume you have the following data.

| Config key | Env   | Version  | value                                                          |
|------------|-------|----------|----------------------------------------------------------------|
| `service1` | `dev` | `2.1.0`  | `{:host "localhost", :port 1234}`                              |
| `service1` | `dev` | `3.7.0`  | `{:host "my.db.local" :port 1234 :user "test2" :pass "test2"}` |
| `service1` | `dev` | `3.10.0` | `{:host "my.db.local" :port 1234 :user "foo" :pass "bar"}`     |
|            |       |          |                                                                |


If you ask of a precisely matching configuration you get that specific
config entry or nil if not found:

``` clojure
;; key not found
(configure {:key "system-not-present" :env "dev" :version "3.1.0"})
;;=> nil


;; exact match
(configure {:key "system1" :env "dev" :version "2.1.0"})
;;=>
;; {:content-type "edn",
;;  :env "dev",
;;  :key "system1",
;;  :version "2.1.0",
;;  :value {:host "localhost", :port 1234},
;;  :change-num 0}

```

If an exact match isn't found the system retrieve the previous
configuration is available

``` clojure
;; exact match not found, but previous version found
;; even across major versions
(configure {:key "system1" :env "dev" :version "3.6.2"})
;;=>
;; {:content-type "edn",
;;  :env "dev",
;;  :key "system1",
;;  :version "2.1.0",
;;  :value {:host "localhost", :port 1234},
;;  :change-num 0}


;; if there aren't previous versions it returns nil
(configure {:key "system1" :env "dev" :version "1.1.0"})
;;=> nil

;; exact match not found, but previous version found
;; in this case the most recent (previous) version is selected.

(configure {:key "system1" :env "dev" :version "3.8.0"})
;;=>
;; {:content-type "edn",
;;  :env "dev",
;;  :key "system1",
;;  :version "3.7.0",
;;  :value {:host "my.db.local", :port 1234, :user "test2", :pass "test2"},
;;  :change-num 0}

```

As mentioned earlier, versions are sorted by numerical elements and not
by alphanumeric values.

## Limitations

There are a number of limitations to consider:

  * 1Config doesn't support `SNAPSHOT` versions, and I don't
    believe there is any valid use case for supporting them, so you
    must strip the `-SNAPSHOT` prior the call to `configure`.
  * 1Config only supports three-legged numerical versions such
    as `"1.12.2"` no other version qualifiers such as `alpha`, `beta`
    etc.
