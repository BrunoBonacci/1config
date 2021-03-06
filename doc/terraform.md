# Terraform scripts

If you are using [terraform](https://www.terraform.io/) to manage your
infrastructure, and you wish to create the `1Config` DynamoDB table as
a terraform resource, here there is a snippet with the configuration
required.

## Terraform v0.12+

``` terraform
##
## Creates a dynamodb table for 1Config configuration tool
##

variable "read_capacity" {
  default = 10
}

variable "write_capacity" {
  default = 5
}

variable "tags" {
  default = {}
}


resource "aws_dynamodb_table" "config_table" {

  name           = "1Config"

  ## select this for On-Demand billing
  billing_mode   = "PAY_PER_REQUEST"

  ## Uncomment these for Provisioned billing
  # billing_mode   = "PROVISIONED"
  # read_capacity  = var.read_capacity
  # write_capacity = var.write_capacity

  hash_key       = "__sys_key"
  range_key      = "__ver_key"

  # enable Point-in-time Recovery
  point_in_time_recovery {
    enabled = true
  }

  attribute {
    name = "__sys_key"
    type = "S"
  }

  attribute {
    name = "__ver_key"
    type = "S"
  }

  tags   = var.tags
}


output "config_table_id" {
  value = aws_dynamodb_table.config_table.id
}

output "config_table_arn" {
  value = aws_dynamodb_table.config_table.arn
}

```



## Terraform v0.11 (or prior)

``` terraform
##
## Creates a dynamodb table for 1Config configuration tool
##

variable "read_capacity" {
  default = "10"
}

variable "write_capacity" {
  default = "5"
}

variable "tags" {
  default = {}
}


resource "aws_dynamodb_table" "config_table" {

  name           = "1Config"

  ## select this for On-Demand billing
  billing_mode   = "PAY_PER_REQUEST"

  ## Uncomment these for Provisioned billing
  # billing_mode   = "PROVISIONED"
  # read_capacity  = "${var.read_capacity}"
  # write_capacity = "${var.write_capacity}"

  hash_key       = "__sys_key"
  range_key      = "__ver_key"

  # enable Point-in-time Recovery
  point_in_time_recovery {
    enabled = true
  }

  attribute {
    name = "__sys_key"
    type = "S"
  }

  attribute {
    name = "__ver_key"
    type = "S"
  }

  tags   = "${var.tags}"
}


output "config_table_id" {
  value = "${aws_dynamodb_table.config_table.id}"
}

output "config_table_arn" {
  value = "${aws_dynamodb_table.config_table.arn}"
}

```
