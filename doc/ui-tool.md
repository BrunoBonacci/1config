# User interface (`1cfg-ui`)

`1Config` comes with a graphical user interface which allows you to
initialise and set values in the given backend.

You can install the tool via Homebrew or manually, please check the
[Quick Start](quick-start.md) page.

Download latest release from github and save it in your `~/bin` folder:

  * https://github.com/BrunoBonacci/1config/releases

**NOTE: It requires JDK/JRE 8+ to be installed and in the PATH.**

Then give it permissions to run:

``` shell
chmod -x ~/bin/1cfg-ui-beta
```

Here how to use it:

  * Initialize AWS ENV variables
  ``` bash
  export AWS_ACCESS_KEY_ID=xxx
  export AWS_SECRET_ACCESS_KEY=yyy
  export AWS_DEFAULT_REGION=eu-west-1
  ```

  If you have the AWS CLI setup then you can use switch to the given profile with:
  ``` bash
  export AWS_PROFILE=xxx
  ```

  * Then start the UI
  ``` bash
  $ 1cfg-ui-beta

  Initialize encryption libs
  Server started: http://127.0.0.1:5300
  ```

  * Finally open your browser to [http://localhost:5300/](http://localhost:5300/)
