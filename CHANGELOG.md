# Changelog

## Release: v0.22.0 (2022-07-14)

  * [NEW] drop dependency from Jsonista and Jackson in favour or cnuernber/charred
  * [NEW] add possibility to customise `1config.home` dir via java property (#35)

## Release: v0.21.0 (2021-01-14)

  * [**BREAKING**] Expand version numbers to support more digits from 5 to 20
    See [migration procedure](https://cljdoc.org/d/com.brunobonacci/oneconfig/CURRENT/doc/user-guides/migration-procedure)

## Release: v0.20.0 (2021-01-03) *(DON'T USE THIS VERSION, use: `0.21.0` or newer)*

  * [**BREAKING**] Dropped `aws-ancryption-sdk` in favour of direct KMS API use.
    See [migration procedure](https://cljdoc.org/d/com.brunobonacci/oneconfig/CURRENT/doc/user-guides/migration-procedure)
  * Switched from Amazonica to Cognitect's aws-api client for AWS.
  * Native binaries for command line tool.
  * Switched from Cheshire to Jasonista (to reduce library version clash)

## Release: v0.17.1 (2021-01-14)

  * Transition version in preparation for `v0.21.0`. **Use this
    version if you have an existing installation of 1Config,
    jump to the new version if you are starting on a new account.**
  * Migration tool to migrate database to new format
  * `SET` command saves in both versions

## Release: v0.17.0 (2021-01-03) *(use: `v0.17.1`)*

  * Transition version in preparation for `v0.2x.0`.
  * Migration tool to migrate database to new format
  * `SET` command saves in both versions

## Release: v0.16.4 (2020-04-04)

  * Added AOT compiled library Jar
  * Delayed use of DefaultAwsRegionProviderChain which blocks outside AWS

## Release: v0.16.2 (2020-03-29)

  * UI: Added ability to compare two entries (DIFF)

## Release: v0.16.1 (2020-03-18)

  * CORE: remove `safely` as dependency to reduce dependencies clash

## Release: v0.16.0 (2020-03-13)

  * CLI: Added ability to compare two entries (DIFF)
  * NEW: Added support for YAML configurations


## Release: v0.15.0 (2020-02-09)

  * NEW: **1Config GUI (thanks to [Eugene Tolbakov @etolbakov](https://github.com/etolbakov))**
  * UI: Added syntax highlighting (thanks to [Eugene Tolbakov @etolbakov](https://github.com/etolbakov))
  * CLI: added ability to read latest configuration without specify the version

## Release: v0.10.3 (2019-09-18)

  * Fixed various issue with Windows usage
  * Improved region detection
  * Improved error messages for when HOME directory is not set

## Release: v0.10.2 (2019-07-09)

  * Fixed issue with regions when running on EC2 when AWS_REGION is not set

## Release: v0.10.1 (2019-06-09)

  * Fixed missing content-type when providing a configuration via `$ONECONFIG_FILE`

## Release: v0.10.0 (2019-06-09)

  * Added ability to restrict keys, envs etc via `user-profiles.edn`
  * Turned keys into string for Java client
  * Added getIn() capability into Java client

## Release: v0.9.2 (2019-04-10)

  * Retrieve AWS user using SecurityTokens APIs instead of IAM GetUser

## Release: v0.9.1 (2019-04-09)

  * Improved handling of SET on un-initialized fs backend.

## Release: v0.9.0 (2019-04-08)

  * Added ability to `save`/`SET` configuration entries into
    filesystem backend via the command line tool.

## Release: v0.8.0 (2019-04-02)

  * Added the `content-type` in the entry listing
  * Added backend selection
  * Added environment variable to select default backend
  * Added ability to provide the value directly from a file
  * Added `deepMerge` in java client

## Release: v0.7.0 (2019-03-28)

  * Added user into the encryption context (anti tampering)
  * Store values in verbatim to preserve comments
  * Added optional pretty-printing for values

## Release: v0.6.1 (2019-03-25)

  * Fixed issue when ~/.1config doesn't exists

## Release: v0.6.0 (2019-03-25)

  * Storing user details on save

## Release: v0.5.0 (2019-03-24)

  * Simple, multi-environment, multi-version, configuration control
  * AWS Cloud native
  * AWS KMS double encryption
  * Anti-tampering control
  * Support for `edn`,  `json`, `txt` and `properties` files
  * Scalable and fast DynamoDB backend storage
  * Command line tool to manage configurations
  * Support for Clojure, Java and other JVM languages
