### 1Config UI

UI for `1Config` library which is meant to simplify `1Config` user experience.

###How to launch Launch
1. Navigate to `1config-ui` folder
2. export aws environment variables
    ```
    export AWS_ACCESS_KEY_ID=your_access_key
    export AWS_SECRET_ACCESS_KEY=your_private_key
    export AWS_SESSION_TOKEN=your_token
    export AWS_REGION=eu-west-1
    ```

    Build and run the core:
    ```
    cd ../1config-core
    lein do clean, install
    ```

Build and run the ui backend:
    ```
    cd ../1config-ui
    lein start
    ```
 3. Open the following link http://127.0.0.1:5300/

## License

Copyright © 2019-2020 Bruno Bonacci & Eugene Tolbakov Distributed under the [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0)
