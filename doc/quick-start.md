# Quick start

Quick start guide to install and use 1Config.

## Install via `brew`

Install command line tool via [Homebrew](https://brew.sh/)
``` bash
brew tap BrunoBonacci/lazy-tools
brew install one-config
```

or update with

``` bash
brew update && brew upgrade one-config
```


## Manual installation

### OSX and Linux

``` bash
mkdir -p ~/bin
wget https://github.com/BrunoBonacci/1config/releases/download/0.20.0/1cfg-`uname` -O ~/bin/1cfg
wget https://github.com/BrunoBonacci/1config/releases/download/0.20.0/1cfg-ui-beta -O ~/bin/1cfg-ui-beta
wget https://github.com/BrunoBonacci/1config/releases/download/0.20.0/1cfgX -O ~/bin/1cfgX
chmod +x ~/bin/1cfg ~/bin/1cfgX ~/bin/1cfg-ui-beta
export PATH=~/bin:$PATH
```

### Windows

 - Open a terminal window
 - Create installation dir `md %userprofile%\1config\bin`
 - Download https://github.com/BrunoBonacci/1config/releases/download/0.20.0/1cfgX and save it in the above folder
 - Download https://github.com/BrunoBonacci/1config/releases/download/0.20.0/1cfg-ui-beta and save it in the above folder
 - Rename file into `1cfg.cmd` with `ren %userprofile%\1config\bin\1cfgX %userprofile%\1config\bin\1cfg.cmd`
 - Rename file into `1cfg-ui-beta.cmd` with `ren %userprofile%\1config\bin\1cfg-ui-beta %userprofile%\1config\bin\1cfg-ui-beta.cmd`
 - Add it to the System path:
     - On the Windows desktop, right-click **My Computer**.
     - In the pop-up menu, click **Properties**.
     - In the **System Properties** window, click the **Advanced** tab, and then click **Environment Variables**.
     - In the **System Variables** window, highlight **Path**, and click **Edit**.
     - In the **Edit System Variables** window, insert the cursor at the end of the **Variable** value field.
     - If the last character is not a semi-colon (;), add one.
     - After the final semi-colon, type `%userprofile%\1config\bin`.
     - Click **OK** in each open window.

## Setup

  * **At this point your installation should be complete and you
    should be able to get the help page by typing `1cfg -h`**
  * Next you need to provide the credentials for the AWS account you
    wish to access.
  * Initialize AWS ENV variables
  ``` bash
  export AWS_ACCESS_KEY_ID=xxx
  export AWS_SECRET_ACCESS_KEY=yyy
  export AWS_REGION=eu-west-1
  ```
  * Initialize DynamoDB table *(only the first time)*
  ``` bash
  1cfg INIT -b dynamo
  ```
  * Set your first secure configuration for application `hello-world`
  ``` bash
  1cfg SET -b dynamo -k hello-world -e test -v 1.0.0 -t txt 'secret password'
  ```
  * List the available configurations
  ``` bash
  1cfg LIST
  ```
  * Retrieve the configuration with the command line tool
  ``` bash
  1cfg GET -b dynamo -k hello-world -e test -v 1.0.0
  ```
  * Retrieve the configuration via the API in your application
  ``` clojure
  (require '[com.brunobonacci.oneconfig :refer [configure]])
  (configure {:key "hello-world" :version "1.0.0" :env "test"})
  ```

There is support for `edn`, `txt`, `json` and Java `properties` format.
and supports Clojure, Java, Groovy, and other JVM languages (more to come)

## UI Setup

If you managed to do the above step successfully you are ready to run
the web UI with:

``` bash
1cfg-ui-beta
Server started: http://127.0.0.1:5300
```

Once the `Server started` message appeared you are ready to open your
favourite browser at: [http://127.0.0.1:5300](http://127.0.0.1:5300)
