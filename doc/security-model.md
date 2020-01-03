# The security model

*1Config* uses the same security model as Amazon S3 server-side
encryption, EBS volumes encryption and Amazon RDS encryption.  It uses
Amazon KMS to generate a **master encryption key** for each
application managed by *1Config*. Then for each configuration entry a
new encryption key is generated, it is used to encrypt the
configuration entry, then the key itself is encrypted using the master
encryption key, and it is stored along with the encrypted payload.

![key management](./images/key-hierarchy-cmk.png)

It means that **every configuration entry is encrypted with its own
key**.  With the above strategy we benefit from all the KMS security
features, such as: the ability to rotate keys, we minimalize the
impact of getting one key compromised, and the ability to have fine
grained control on how can access the key to encrypt/decrypt
configuration entries.

![encryption process](./images/1config.png)

The diagram explains how to security model works. Here the steps involved:

  - An operator wants to store a new configuration entry for a application
  - The operator, using the command line tool (`1cfg`) creates a new
    **master encryption key** for the Application.
  - If IAM permissions allow it the operation will succeed.
  - Then it uses the *master encryption key* to generate a data key.
  - The data key will be used to encrypt the plaintext configuration
  - If IAM permissions allow it the operation will succeed.
  - Then the *data key* itself will be encrypted using the *master key*.
  - Finally it stores the encrypted payload and the encrypted data key
    together into DynamoDB table (`1Config`).
  - At this point the operator is done and the application is ready to
    retrieve the configuration.
  - The application will lookup the correct entry for the environment
    and version to use and fetch the encrypted payload with the
    encrypted encryption key.
  - To decrypt the payload it will have to contact KMS and attempt to
    decrypt the data encryption key.
  - If the application has the correct IAM roles to use the master key
    the operation will succeed.
  - Once the data key has bee decrypted by KMS, then the Application
    can decrypt the configuration payload and retrieve the plaintext
    information.
  - **Luckly, all above steps are done automatically by `1Config`.**
