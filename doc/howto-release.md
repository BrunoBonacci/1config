# HowTo prepare a Release

## Automated

  - `make all`
  - Tag and push
  - `make depoly`
  - Attach release bundles (`/tmp/1cfg`) in github release
  - Update homebrew-lazy-tools deployment, commit, push
  - update local installation
    ```
    brew update
    brew upgrade one-config
    ```


## Manual steps
  - Test release
  - Update version in version file and README and other articles
  - Update CHANGELOG.md
  - install core library
    ```
    cd 1config-core
    lein do clean, check, midje, install
    ```
  - test end-2-end
    ```
    cd 1config-cli
    export AWS_PROFILE=testaccount
    ../test/bin/end-2-end-test.sh
    ```
  - if everything is ok, then
  - build cli packages
    ```
    cd 1config-cli
    lein do clean, bin, native-config, native
    ```
  - build Linux version of a linux machine
    ```
    cd 1config-cli
    lein do clean, uberjar, native
    ```
  - build UI packages
    ```
    cd 1config-ui
    lein do clean, bin
    ```
  - release to clojars
    ```
    cd 1config-core
    lein deploy clojars
    ```
  - Tag and push
  - Attach release bundles (`/tmp/1cfg`) in github release
  - Update homebrew-lazy-tools deployment, commit, push
  - update local installation
    ```
    brew update
    brew upgrade one-config
    ```
