# AWS permissions

There are two types of permissions you need to set to use `1Config`:

  - the permission for the human operator to view/edit configurations entries
  - the permission for the app/service to read its own configurations

## Operator Permissions (for Command line tool user)

``` json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowInitDatabase",
            "Effect": "Allow",
            "Action": "dynamodb:CreateTable",
            "Resource": "arn:aws:dynamodb:*:*:table/1Config"
        },
        {
            "Sid": "AllowListAllConfigEntries",
            "Effect": "Allow",
            "Action": "dynamodb:Scan",
            "Resource": "arn:aws:dynamodb:*:*:table/1Config"
        },
        {
            "Sid": "AllowCreateKeysAndListKeys",
            "Effect": "Allow",
            "Action": [
                "kms:CreateAlias",
                "kms:CreateKey",
                "kms:DescribeKey",
                "kms:ListAliases"
            ],
            "Resource": "*"
        },
        {
            "Sid": "AllowGetConfigEntriesPt1",
            "Effect": "Allow",
            "Action": [
                "dynamodb:Query"
            ],
            "Resource": "arn:aws:dynamodb:*:*:table/1Config"
        },
        {
            "Sid": "AllowSetOnConfigEntryPt1",
            "Effect": "Allow",
            "Action": [
                "dynamodb:PutItem"
            ],
            "Resource": "arn:aws:dynamodb:*:*:table/1Config"
        },
        {
            "Sid": "AllowGetConfigEntriesPt2",
            "Effect": "Allow",
            "Action": "kms:Decrypt",
            "Resource": "*"
        },
        {
            "Sid": "AllowSetOnConfigEntryPt2",
            "Effect": "Allow",
            "Action": [
                "kms:GenerateDataKey"
            ],
            "Resource": "*"
        }
    ]
}
```

**NOTE: if you are running 1Config version <= 0.9.2** you need to add
one more permission.

``` json
   [...]
        {
            "Sid": "AllowDiscoverThemselves",
            "Effect": "Allow",
            "Action": [
                "iam:GetUser"
            ],
            "Resource": "arn:aws:iam::*:user/${aws:username}"
        }
   [...]
```


A simple way to limit which keys can be used by the user/profile
attached to this policy is to list the arn of the keys it can use
(ARNs can be obtained with `1cfg list-keys`):

``` json
   [...]
        {
            "Sid": "AllowGetConfigEntriesPt2",
            "Effect": "Allow",
            "Action": "kms:Decrypt",
            "Resource": [
                "arn:aws:kms:eu-west-1:1234567890:key/aaaaaaa-bbbb-cccc-ddddd-11111111111",
                "arn:aws:kms:eu-west-1:1234567890:key/aaaaaaa-bbbb-cccc-ddddd-22222222222",
                "arn:aws:kms:eu-west-1:1234567890:key/aaaaaaa-bbbb-cccc-ddddd-33333333333"
            ]
        },
        {
            "Sid": "AllowSetOnConfigEntryPt2",
            "Effect": "Allow",
            "Action": [
                "kms:GenerateDataKey"
            ],
            "Resource": [
                "arn:aws:kms:eu-west-1:1234567890:key/aaaaaaa-bbbb-cccc-ddddd-11111111111",
                "arn:aws:kms:eu-west-1:1234567890:key/aaaaaaa-bbbb-cccc-ddddd-22222222222",
                "arn:aws:kms:eu-west-1:1234567890:key/aaaaaaa-bbbb-cccc-ddddd-33333333333"
            ]
        }
   [...]
```

## Permissions for the application

The application only need to be able to query 1Config dynamo table and
to decrypt its own entries.

``` json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowGetConfigEntriesPt1",
            "Effect": "Allow",
            "Action": [
                "dynamodb:Query"
            ],
            "Resource": "arn:aws:dynamodb:*:*:table/1Config"
        },
        {
            "Sid": "AllowGetConfigEntriesPt2",
            "Effect": "Allow",
            "Action": "kms:Decrypt",
            "Resource": "*"
        }
    ]
}
```

Similarly the application can be limited to the key used for its own entries:

``` json
   [...]
        {
            "Sid": "AllowGetConfigEntriesPt2",
            "Effect": "Allow",
            "Action": "kms:Decrypt",
            "Resource": "arn:aws:kms:eu-west-1:1234567890:key/aaaaaaa-bbbb-cccc-ddddd-33333333333"
        }
   [...]
```
