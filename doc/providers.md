# Configuration providers

`configure` will try a number of different location to find a configuration provider.
It will attempt to read a file or dynamo table in the following order.

  * Java System property `1config.file`, if set and the file exists
    it will be used as *sole* configuration
  * Environment variable `$ONECONFIG_FILE`, if set and the file exists
    it will be used as *sole* configuration
  * Java Resource bundle `1config.edn` (or `.json`, `.txt`,
    `.properties`), if present it will be used as *sole* configuration
  * `~/.1config/<service-key>/<env>/<version>/<service-key>.<ext>`
    (home dir) - if present it will be used as configuration.
    Entries in the `~/.1config/` will have precendence over the DynamoDB table.
  * DynamoDB table called `1Config` in the "current" region.

The name of the DynamoDB table can be customized with
`$ONECONFIG_DYNAMO_TABLE` environment variable (or
`1config.dynamo.table` property).  It will use the machine role to
access the database. The AWS region can be controlled via the
environment variable `$AWS_DEFAULT_REGION`. For the AWS credentials we
use the [Default Credential Provider
Chain](http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#id6).
Therefore the credentials can be provided in one of the following
ways:

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
