<p align="center"><img src="logo.png" width="96px"></p>

<h1 align="center">Effect Network</h1>

<p align="center">Smart contracts for the Effect Network</p>

## Compiling

The main token contracts are currently implemented in Java. Maven is
used to compile the contract into a jar file that is then converted to
avm bytecode.

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

This will create a `jar` file in the `target` directory. This file can
be used as input to
[neoj](https://github.com/neo-project/neo-compiler/tree/master/neoj)
to get the a resulting `avm` file that can be deployed.

## Contracts

The following contracts are included

- [EffectToken](contracts/java/token)

