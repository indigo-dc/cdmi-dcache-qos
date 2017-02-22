CDMI QoS plugin for dCache Storage Backend
======================================
This plugin exposes the dCache Storage end-point to CDMI-Server and allows QoS operations on dCache namespace using [CDMI](https://docs.google.com/document/d/1ovUl8G1SyyAX_pBaiEu7Yu34Vxnc3-sjvPuXIu6E1MM)


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

Properties
---------------------
The plugin looks for a certain properties with the following information,

`dcache.server` = dcache server

`dcache.server.rest.endpoint` = port number on which the rest endpoint is running

`dcache.rest.user` = username for authentication

`dcache.rest.password` = password for authentication

The configuration for the plugin can be done either via command line parameters or in the **config/dcache.properties** file or any other supported way, see Spring Boot.


Test
---------------------

* Query for a certain CDMI Capability 

```http GET https://dcache-qos-01.desy.de:8443/cdmi_capabilities/container/disk Authorization:"Bearer $OIDC" X-CDMI-Specification-Version:"1.1" Accept:"application/cdmi-capability" ```

* Change capability of a file

```http POST https://dcache-qos-01.desy.de:3443/api/v1/qos-management/namespace/random.img Authorization:"Bearer $OIDC" Content-Type:"application/json" Accept:"application/json" "update"="disk+tape" --print=HhBb```
