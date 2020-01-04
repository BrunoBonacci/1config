# Best practices

In this section we will discuss some of the best practices managing
configurations for your applications.

Firstly, define a **default configuration**, document the various
configuration options and set defaults which are best suited for a
production environment.  **Don't put secrets in here!!!**. If you have
username, passwords, or appilcation keys don't add them in the
defaults values (see database username/password).  A good default
configuration should contain good default values for all the
configurable properties with the exception of secrets and endpoints of
dependencies. In such way the a typical user of your application will
only have to enter the secrets in most of the cases.

The following is a made-up example of an application which uses
a database and exposes some api.

``` clojure
(ns your.namespace
  (:require [com.brunobonacci.oneconfig :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; default configuration, values defined here
;; should be the one you wish to use in a
;; production environment.
(def ^:const DEFAULT-CONFIG
  { ;; HTTP server listener configuration
   :server   {:port 80 :bind "0.0.0.0"}

   ;; database connection configuration
   :database {:host "mydatabase" :port 1234
              :dbname "mydata"
              ;; REQUIRED!!!
              ;;:user "username"
              ;;:pasword "secret"
              :use-encryption true
              :connection-pool-size 10}

   ;; application limits
   :limits {:max-items-x-page  25
            :max-login-attempt 5
            :max-session-time  300000
            :max-idle-time     60000
            :max-upload-size   8000000}

   ;; Deep storage configuration
   :media-storage {:type   :s3
                   :bucket "my-media-storage"
                   :prefix "media/storage/"}
   })

```

Once you defined a good default configuration you need to retrieve
which environment you are running on. This information might come from
different places and it mostly depends on how you deploy your
software. Let's assume that we have environment variable called `$ENV`
(could be different) which contains the current environment, if the
environment variable is not present then we assume a developer
machine.

``` clojure

(defn env
  "returns the current environmet the system is running in.
   This has to be provided by the infrastructure"
  []
  (or (System/getenv "ENV") "dev"))

```

Next we need to retrieve which version our system is currently on.
Again you might store this information in various way, the simplest
one which I recommend to use is to store it in a resource file in your
`resources/` folder, something like `<your-project-name>.versoin` and
then read it from your system as below.  This approach allows you to
have only one place where you write the version number as you can use
the same file for your `project.clj` version.  Please see the
following link for a leiningen example: https://github.com/BrunoBonacci/1config/blob/master/1config-core/project.clj

``` clojure
;;
;; Better to store the version of the project as a resource file
;;
(defn version
  "returns the version of the current version of the project
   from the resource bundle."
  []
  (some->> (io/resource "my-project.version")
           slurp
           str/trim))
```

Finally, you can retrieve the user configuration and merge it with
the default configuration as below:

``` clojure
;; Overall system config
(defonce config
  (->> (configure {:key "system1" :env (env) :version (version)})
     (deep-merge DEFAULT-CONFIG)))
```

Assuming that the user configuration looks like this:

``` clojure
{:server {:port 9000}
 :database {:user "testuser" :password "testpass"}}
```

Then the final, overall `config` will look like the following:

``` clojure
{:server {:port 9000, :bind "0.0.0.0"},
 :database
 {:host "mydatabase",
  :port 1234,
  :dbname "mydata",
  :use-encryption true,
  :connection-pool-size 10,
  :user "testuser",
  :password "testpass"},
 :limits
 {:max-items-x-page 25,
  :max-login-attempt 5,
  :max-session-time 300000,
  :max-idle-time 60000,
  :max-upload-size 8000000},
 :media-storage
 {:type :s3, :bucket "my-media-storage", :prefix "media/storage/"}}
```

Please note that the `:port`, the `:user` and the `:password` reflect
the user choice while the other keys are as defined in the
`DEFAULT-CONFIG`.

Now to set the user configuration you could use the `1cfg` command
line tool to set the value, however it is recommended that the you
keep the `dev` configuration only on your machine and you don't set it
in the shared DynamoDB table. The reason why this is best is because
different developers might need different configurations.

Luckily, **1Config** solves this problem by allowing you to have a
file-system configuration which overrides values from the DynamoDB
(see configuration providers below).  There are many options, however
the best suited for development purposes is to create a file in your
home directory with the following template `~/.1config/<service-key>/<env>/<version>/<service-key>.<ext>`

``` bash
# template ~/.1config/<service-key>/<env>/<version>/<service-key>.<ext>
mkdir -p ~/.1config/system1/dev/1.0.0/

# and create a fine with your configuration inside
cat > ~/.1config/system1/dev/1.0.0/system1.edn <<\EOF
;; my dev config
{:server {:port 9000}
 :database {:user "testuser" :password "testpass"}}
EOF
```

With this configuration file in place **1Config** will use as value
for the configuration ignoring possible other matching entries in the
dynamo table.

It is possible to test it via the command line tool:

``` bash
$ 1cfg GET -k system1 -e dev -v 1.0.0

;; my dev config
{:server {:port 9000}
 :database {:user "testuser" :password "testpass"}}
```

The advantage of this approach, is that the configuration lives
outside of the project directory structure so it won't be committed in
your version control system accidentally revealing your secrets to
other people.

It is good practice to keep the development environment separated from
the other environments. You can pick a name/label like `dev` or
`local` (or anything else) and just use it for your local development.

For development purposes, if you wish to use only the filesystem based
configuration provider (see below) you can either set the environment
variable `ONECONFIG_DEFAULT_BACKEND` or the JVM system property
`1config.default.backend` with the value `fs` (for example:
`export ONECONFIG_DEFAULT_BACKEND=fs` **to be used for development
purposes only**).
