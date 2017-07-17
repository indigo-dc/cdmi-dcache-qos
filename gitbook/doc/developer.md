# Developer guide

Requirements
---------------------
* JDK 1.8+
* [Maven 3+](https://maven.apache.org/)

Dependency
---------------------
* [INDIGO-DataCloud CDMI Server](https://github.com/indigo-dc/CDMI)
* [INDIGO-DataCloud CDMI-SPI](https://github.com/indigo-dc/cdmi-spi)


Compilation
---------------------
* Read the instructions from CDMI server and CDMI-Spi for setting up

[cdmi-spi](https://github.com/indigo-dc/cdmi-spi) is not available thorough any public packages repository, it has to be cloned, compiled and installed locally:

```
$ git clone https://github.com/indigo-dc/cdmi-spi.git
$ cd cdmi-spi
$ mvn install
```

Compilation of cdmi-dcache-qos plugin

```
$ git clone https://github.com/indigo-dc/cdmi-dcache-qos.git
$ cd cdmi-dcache-qos
$ mvn install
```

Usage
---------------------
The compilation process produces **cdmi-dcache-qos-\<VERSION\>.jar** artifact. This dependency should be resolvable by the INDIGO-DataCloud CDMI server. It leverages spring-boot and is packaged into single, standalone jar file. Hence, we need to add cdmi-dcache-qos as dependency to the server's pom.xml file and re-package the server.

```xml
    <dependency>
        <groupId>org.dcache.spi</groupId>
        <artifactId>cdmi-dcache-qos</artifactId>
        <version>1.0</version>
    </dependency>
```
