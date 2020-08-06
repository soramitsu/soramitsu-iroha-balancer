# Iroha balancer

DevOps
* Java 11 required
* Shell Command to lunch application jar on Linux(from jar directory): java -jar iroha-balancer-{version}.jar
* Must have ability to connect RabbitMq
* Must have ability to connect Iroha nodes

# Overview

Iroha balancer service reads input exchange:
```
rabbitmq:iroha-balancer
```

Used routing keys:

```yaml
torii - send byte array with transaction that shall be balanced to torii method  

listTorii - send json array with byte arrays or String base64 bytes of transaciton list that shall be balanced to listTorii method 

```



