# jakarta-lemon-cli
The new generation of Jakarta Lemon, to create Jakarta EE projects

## Create project

```shell
java -jar jakarta-lemon-cli.jar createproject --arch=HEXA \
   --groupId=<groupId> \
   --artifactId=<artifactId> \
   --package=<packageName> \
   project_name
```

Example:
```shell
java -jar jakarta-lemon-cli.jar createproject --arch=HEXA --groupId=com.example --artifactId=hexagonal --package=com.example.hexagonal example_lemon
```

## Add Model
Create model in project folder

```shell
java -jar jakarta-lemon-cli.jar addmodel MODEL_DEFINITION.json

```

MODEL_DEFINITION.json format 
```json
{
  "Person": {
    "fields": {
      "firstName": "String",
      "lastName": "String",
      "birthdate": "LocalDateTime",
      "dni": {
        "primaryKey": true,
        "type": "String"
      }
    }
  },
  "Account": {
    "fields": {
      "client": "Person",
      "bank": "Bank",
      "amount": "double",
      "transactions": "java.util.List<Transaction>",
      "accountId": {
        "primaryKey": true,
        "type": "UUID"
      }
    },
    "finders": {
      "byClient": {
        "parameters": [
          "Person"
        ],
        "return": "Account",
        "isCollection": true
      }
    }
  },
  "Bank": {
    "fields": {
      "address": "String",
      "name": "String",
      "bankId": {
        "primaryKey": true,
        "type": "UUID"
      }
    },
    "finders": {
      "byId": {
        "parameters": [
          "String"
        ],
        "return": "Bank"
      }
    }
  },
  "Transaction": {
    "fields": {
      "transactionId": {
        "primaryKey": true,
        "type": "UUID"
      },
      "date": "LocalDateTime",
      "amount": "double",
      "description": "String"
    }
  }
}
```

## Add Use case
Create service in project folder

```shell
java -jar jakarta-lemon-cli.jar addusecase USECASE_DEFINITION.json

```

USECASE_DEFINITION.json:

```json
{
    "AtmService": {
        "injects": [
            "BankRepository"
        ],
        "methods": {
            "withdrawal": {
                "amount": "double",
                "customer": "Person",
                "return": "double:0"
            },
            "payment": {
                "amount": "double",
                "customer": "Person"
            }
        }
    }
}

```
`return` has type and default value. i.e: `double:0`