# Admin Guide

This is the INDIGO CDMI dCache Plugin deployment and administration guide.

Prerequisites
---------------------

INDIGO DataCloud CDMI server runs on RHEL7 platforms.
Our deployment tests have been done on CentOS7.


Install
---------------------
Go to <https://ci.dcache.org/job/Indigo_CDMI_QoS/>

Download the rpm package from the list of artifacts and install

```bash
yum localinstall cdmi-dcache-qos-1.0cdmi1.2-1.el7.x86_64.rpm
```

Configure
---------------------
Append to the CDMI server main configuration file by specifying dcache as storage backend type:

```bash
$ vim /var/lib/cdmi-dcache-qos/config/application.yml
```

```yaml
cdmi:
  qos:
    backend:
      type: dCache
```

Edit CDMI dCache plugin configuration file:

```bash
$ vim /var/lib/cdmi-dcache-qos/config/dcache.properties
```

```properties
dcache.server.rest.scheme=https
dcache.server=dcache-qos-01.desy.de
dcache.server.rest.endpoint=3881

dcache.rest.user=admin
dcache.rest.password=dickerelch

cdmi.dcache.rest=new
```

The definition of the properties can be seen below.


Properties
---------------------
The plugin looks for a certain properties with the following information,

| **Property** | **Description** |
|:-------------|:----------------|
| **dcache.server.rest.scheme** | http or https |
| **dcache.server** | dCache Server Url |
| **dcache.server.rest.endpoint** | port number on which the rest endpoint is running |
| **dcache.rest.user** | username for authentication |
| **dcache.rest.password** | password for authentication |
| **cdmi.dcache.rest.version** | (`new` or `old`) new rest api in the namespce or old legacy api for QoS operations |

The configuration for the plugin can be done either via command line parameters or in the **config/dcache.properties** file or any other supported way, see Spring Boot.

## Run

Run CDMI server:

    systemctl start cdmi-dcache-qos.service
