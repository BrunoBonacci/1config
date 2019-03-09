# 1config
[![Clojars Project](https://img.shields.io/clojars/v/com.brunobonacci/oneconfig.svg)](https://clojars.org/com.brunobonacci/oneconfig) ![CircleCi](https://img.shields.io/circleci/project/BrunoBonacci/oneconfig.svg) ![last-commit](https://img.shields.io/github/last-commit/BrunoBonacci/oneconfig.svg) [![Dependencies Status](https://jarkeeper.com/BrunoBonacci/safely/status.svg)](https://jarkeeper.com/BrunoBonacci/oneconfig)

A library to manage environments configuration at application level.

## Usage

In order to use the library add the dependency to your `project.clj`

``` clojure
;; Leiningen project
[com.brunobonacci/oneconfig "0.1.0-SNAPSHOT"]

;; deps.edn format
{:deps { com.brunobonacci/oneconfig "0.1.0-SNAPSHOT" }}
```

Current version: [![Clojars Project](https://img.shields.io/clojars/v/com.brunobonacci/oneconfig.svg)](https://clojars.org/com.brunobonacci/oneconfig)


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

### Configuration resolution

`configure` will try a number of different location to find a configuration provider.
It will attempt to read a file or dynamo table in the following order.

  * Environment variable `$ONECONFIG_FILE`, if set and the file exists
    it will be used as configuration
  * Java System property `1config.file`, if set and the file exists
    it will be used as configuration
  * Java Resource bundle `1config.edn`, if present it will be used
    as configuration
  * `./1config/1config.edn` if present it will be used as
    configuration
  * `~/.1config/1config.edn` (home dir) - if present it will be
    used as configuration
  * DynamoDB table called `1Config` in the "current" region.

The name of the DynamoDB table can be customized with
`$ONECONFIG_DYNAMO_TABLE` environment variable. It will use the
machine role to access the database. The AWS region can be controlled
via the environment variable `$AWS_REGION` which defaults to
`eu-west-1`. For the AWS credentials we use the
[Default Credential Provider Chain](http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#id6).
Therefore the credentials can be provided in one of the following ways:

  - **Environment Variables** – `AWS_ACCESS_KEY_ID` and
    `AWS_SECRET_ACCESS_KEY`. The AWS SDK for Java uses the
    `EnvironmentVariableCredentialsProvider` class to load these
    credentials.

  - **Java System Properties** – `aws.accessKeyId` and
    `aws.secretKey`. The AWS SDK for Java uses the
    `SystemPropertiesCredentialsProvider` to load these credentials.

  - **The default credential profiles file** – typically located at
    `~/.aws/credentials` (this location may vary per platform), this
    credentials file is shared by many of the AWS SDKs and by the AWS
    CLI. The AWS SDK for Java uses the ProfileCredentialsProvider to
    load these credentials.

    You can create a credentials file by using the aws configure
    command provided by the AWS CLI, or you can create it by
    hand-editing the file with a text editor. For information about
    the credentials file format, see AWS Credentials File Format.

  - Instance profile credentials – these credentials can be used on
    EC2 instances, and are delivered through the Amazon EC2 metadata
    service. The AWS SDK for Java uses the
    `InstanceProfileCredentialsProvider` to load these credentials.


### File based configuration.

This is mostly intended for local development, you can create a file
named `~/.1config/1config.edn` (in your home) and put the
configuration for one or more services in one or more environments
with the following format:

``` clojure
;;
;; It is a collection of entries where, each entry has the following form
;;
[
 {
  ;; name of the environment - this can be any string which identifies
  ;; an environment in your infrastructure
  :env "dev"

  ;; identifier of your system/service of configuration key to read/store
  :key "system1"

  ;; version of your system. This must be in the following format "x.y.z"
  ;; where `x` is the major version, `y` is the minor version and `z`
  ;; is the patch level. Versions are compare numerically on their elements
  ;; for example "3.12.5" comes after "3.5.0".
  :version "3.1.0"

  ;; content type of the value
  ;; currently we support:
  ;;   - application/edn - which is the default
  ;;   - application/json - a JSON encoded string
  ;;   - text/plain - for plain text string.
  :content-type "application/edn"

  ;;
  ;; value is the actual value of the configuration item
  ;; it must be encoded following the `content-type` field.
  ;;
  :value {:db {:host "10.2.23.32" :port 1234}}
  }
]
```

A single `1config.edn` file can contain configuration for:

  - one or more systems
  - for every system you can have one or more environments
  - for every environment you can one or more versions

A configuration entry is uniquely identified by **environment, key and version**.
While resolving the specific configuration the system if going  to look for a
exact version match or a version which is smaller than the given one.

For this reason you don't have to publish a new configuration for every
version change. For example: let's assume you have the following data.

``` clojure
[
{:env "dev" :key "system1" :version "3.2.0" :value {:host "my.db.local" :port 1234 :user "test1" :pass "test1"}}
{:env "dev" :key "system1" :version "3.7.0" :value {:host "my.db.local" :port 1234 :user "test2" :pass "test2"}}
{:env "dev" :key "system1" :version "3.10.0" :value {:host "another.db"  :port 1234 :user "foo"   :pass "bar"}}
]
```

If you ask of a precisely matching configuration you get that specific
config entry or nil if not found:

``` clojure
;; exact match
(configure {:key "system1" :env "dev" :version "3.1.0"})
;;=>
;; {:content-type "application/edn",
;;  :env "dev",
;;  :key "system1",
;;  :version "3.1.0",
;;  :value {:host "localhost", :port 1234},
;;  :change-num 0}


;; version not found
(configure {:key "system-not-present" :env "dev" :version "3.1.0"})
;;=> nil
```

If an exact match isn't found the system retrieve the previous configuration is available

``` clojure
;; exact match not found, but previous version found

(configure {:key "system1" :env "dev" :version "3.6.2"})
;;=>
;; {:content-type "application/edn",
;;  :env "dev",
;;  :key "system1",
;;  :version "3.1.0",
;;  :value {:host "localhost", :port 1234},
;;  :change-num 0}


;; version not found

(configure {:key "system1" :env "dev" :version "1.1.0"})
;;=> nil

;; exact match not found, but previous version found
;; in this case the most recent (previous) version is selected.

(configure {:key "system1" :env "dev" :version "3.8.0"})
;;=>
;; {:content-type "application/edn",
;;  :env "dev",
;;  :key "system1",
;;  :version "3.7.0",
;;  :value {:host "my.db.local", :port 1234, :user "test2", :pass "test2"},
;;  :change-num 0}

```

As mentioned earlier, versions are sorted by numerical elements and not
by alphanumeric values.

### Limitations

There are a number of limitations to consider:

  * 1Config doesn't support `SNAPSHOT` versions, and I don't
    believe there is any valid use case for supporting them, so you
    must strip the `-SNAPSHOT` prior the call to `configure`.
  * 1Config only supports three-legged numerical versions such
    as `"1.12.2"` no other version qualifiers such as `alpha`, `beta`
    etc.


### Command line tool

1config comes with a command line tool which allows you to
initialise and set values in the given backend.

Download latest release from github and save it in your `~/bin` folder:

  * https://github.com/BrunoBonacci/1config/releases

Then give it permissions to run:

``` shell
chmod -x ~/bin/cfg1
```

Here how to use it:

``` text
  A command line tool for managing configurations in different environments.

Usage:

   cfg1 <OPERATION> -e <ENVIRONMENT> -k <SERVICE> -v <VERSION> [-b <BACKEND>] [-t <TYPE>] <VALUE>

   WHERE:
   ---------

   OPERATION:
      - GET       : retrieve the current configuration value for
                  : the given env/service/version combination
      - SET       : sets the value of the given env/service/version combination
      - LIST      : lists the available keys for the given backend
      - INIT      : initialises the given backend (like create the table if necessary)

   OPTIONS:
   ---------
   -h   --help                 : this help
   -b   --backend   BACKEND    : only "dynamo" is currently supported
   -e   --env   ENVIRONMENT    : the name of the environment like "prod", "dev", "st1" etc
   -k   --key       SERVICE    : the name of the system or key for which the configuration if for,
                               : exmaple: "service1", "db.pass" etc
   -v   --version   VERSION    : a version number for the given key in the following format: "2.12.4"
   -c   --change-num CHANGENUM : used with GET returns a specific configuration change.
        --with-meta            : whether to include meta data for GET operation
        --output-format FORMAT : either "table" or "cli" default is "table" (only for list)
   -C                          : same as `--output-format=cli`
   -t   --content-type TYPE    : one of "edn", "text" or "json", default is "edn"

Example:

   (*) To initialise a given backend
   cfg1 INIT -b dynamo

   (*) To set the configuration value of a service called 'service1' use:
   cfg1 SET -b dynamo -e test -k 'service1' -v '1.6.0' -t edn '{:port 8080}'

   (*) To read last configuration value for a service called 'service1' use:
   cfg1 GET -b dynamo -e test -k 'service1' -v '1.6.0'

   (*) To read a specific changeset for a service called 'service1' use:
   cfg1 GET -b dynamo -e test -k 'service1' -v '1.6.0' -c '3563412132'

   (*) To list configuration with optional filters
   cfg1 LIST -b dynamo -e prod -k ser -v 1.


NOTE: use AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY or AWS_PROFILE to
      set the access to the target AWS account.
```

### AWS permissions

If you are using role based permissions then ensure that your role
has the following permissions included:

``` json
{
  "Statement": [
    {
        "Action": [
            "dynamodb:GetItem",
            "dynamodb:BatchGetItem",
            "dynamodb:Query"
        ],
        "Effect": "Allow",
        "Resource": "arn:aws:dynamodb:eu-west-1:*:table/1Config"
    }
  ]
}
```

For the command line tool you will need add the permission to create a
table as well.

## License

Copyright © 2019 Bruno Bonacci - Distributed under the [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0)
