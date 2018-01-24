# Effect Network

This project contains smart contracts for the Effect Network.

## Compiling

The main token contracts are implemented in Java, but future smart
contracts might be developed in Python.

### Java

This project requires `neo-devpack-java` to be installed as a local
maven package. The following commands will install the package from
github:

```
$ git clone https://github.com/neo-project/neo-devpack-java
$ cd neo-devpack-java
$ mvn package
$ mvn install:install-file -Dfile=target/neo-devpack-java-2.3.0.jar -DpomFile=pom.xml
```

To compile a Java smart contract you can use Maven:

```
$ cd contracts/java/token
$ mvn package
```

Insert the resulting `jar` file to the lastest version of `neoj` to
get the resulting `avm` file.

## Contracts

The following contracts are included:

- [EffectToken](contracts/java/token)
- [TokenLock](contracts/java/token-lock)

