# jakarta-lemon-cli
The new generation of Jakarta Lemon, to create Jakarta EE projects

## Create project

```createproject --arch=HEXA \
   --groupId=<groupId> \
   --artifactId=<artifactId> \
   --package=<packageName> \
   project_name
```

Example:
```
createproject --arch=HEXA --groupId=com.example --artifactId=hexagonal --package=com.example.hexagonal example_lemon
```

## Add Model
Create model in project folder

```
addmodel MODEL_DEFINITION.json

```

MODEL_DEFINITION.json format 
```
{
    "Person": {
        "firstName": "String",
        "lastName": "String",
        "birthdate": "LocalDateTime",
        "dni": {
            "primaryKey": true,
            "type": "String"
        }
    },
    "Bank": {
        "address": "String",
        "name": "String",
        "bankId": {
            "primaryKey": true,
            "type": "UUID"
        }
    }
}
```

## Add Use case
Create service in project folder

```
addusecase USECASE_DEFINITION.json

```

USECASE_DEFINITION.json
```
{
    "AtmService": {
        "injects": [
            "BankRepository"
        ],
        "methods": {
            "withdrawal": {
                "amount": "double",
                "customer": "Person",
                "return": "double"
            },
            "payment": {
                "amount": "double",
                "customer": "Person"
            }
        }
    }
}

```
