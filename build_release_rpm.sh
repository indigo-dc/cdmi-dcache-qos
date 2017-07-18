#!/bin/bash

# This script is heavily based on the works of Gracjan Jankowski 
# https://github.com/indigo-dc/cdmi-s3-qos/blob/master/build_release_rpm.sh
#
# read commits or branches names which are to be used in packaging process
#
. ./PACKAGING_COMMITS

NAME=cdmi-dcache-qos
TOPDIR=`pwd`/rpm



QOS_VERSION=$(mvn help:evaluate -Dexpression=project.version | grep -v " " | grep -o "[0-9.]*" )
QOS_VERSION_ERR=$?

if [ $QOS_VERSION_ERR -ne 0 ] ||  [ "x$QOS_VERSION" == "x" ]; then
echo "ERROR: Could not determine the version of maven project."
echo "Please, check by hand if this command succeed:"
echo "mvn help:evaluate -Dexpression=project.version | grep -v \" \" | grep -o \"[0-9.]*\""
exit 1
fi




#
# compile and install cdmi-spi
#
git clone https://github.com/indigo-dc/cdmi-spi.git
cd cdmi-spi
git checkout $CDMI_SPI_COMMIT
mvn clean install -Dgpg.skip=true



#
# compile and install cdmi-dcache-qos (as a library)
#
cd ..
mvn clean install -Dmaven.test.skip=true



#
# clone, configure (to use cdmi-dcache-qos) and package CDMI server
#
rm -rf CDMI 
git clone https://github.com/indigo-dc/CDMI.git
cd CDMI
git checkout $CDMI_COMMIT
cd ..
cp -rf config CDMI/ 
rm -f CDMI/config/dcache.properties

cd CDMI


sed -i 's/dummy_filesystem/dCache/g' config/application.yml
sed -i 's/<dependencies>/<dependencies>\r\n<dependency>\r\n<groupId>org.dcache.spi<\/groupId>\r\n<artifactId>cdmi-dcache-qos<\/artifactId>\r\n<version>1.0<\/version>\r\n<\/dependency>/g' pom.xml

mvn clean package -Dmaven.test.skip=true

#
# determine version of CDMI server
#
CDMI_JAR_VERSION=$(mvn help:evaluate -Dexpression=project.version | grep -v " " )
CDMI_JAR_VERSION_ERR=$?

CDMI_VERSION=$(echo $CDMI_JAR_VERSION | grep -o "[0-9.]*")

SERVICE_VERSION=${QOS_VERSION}cdmi${CDMI_VERSION}

#
# copy cdmi server to its final name (it is cdmi server with included cdmi-dcache-qos module)
#
cp -f target/cdmi-server-$CDMI_JAR_VERSION.jar target/$NAME-${SERVICE_VERSION}.jar



#
# create TOP dir structure
#
mkdir -p $TOPDIR
mkdir -p $TOPDIR/SOURCES
mkdir -p $TOPDIR/SPECS


cd ..


mkdir -p $TOPDIR/SOURCES/var/lib/$NAME/config/
cp -fr config/dcache.properties $TOPDIR/SOURCES/var/lib/$NAME/config/
cp -f  CDMI/target/$NAME-${SERVICE_VERSION}.jar $TOPDIR/SOURCES/var/lib/$NAME/
cp -f CDMI/config/* $TOPDIR/SOURCES/var/lib/$NAME/config/


#
# prepare files and folders required by rpmbuild
#

#rpm/SOURCES/cdmi-dcache-qos.service
sed "s/@SERVICE_VERSION@/$SERVICE_VERSION/g" templates/rpm/SOURCES/cdmi-dcache-qos.service > rpm/SOURCES/cdmi-dcache-qos.service

#rpm/SPECS/cdmi-dcache-qos.spec
sed "s/@SERVICE_VERSION@/$SERVICE_VERSION/g" templates/rpm/SPECS/cdmi-dcache-qos.spec > rpm/SPECS/cdmi-dcache-qos.spec
rpmbuild --define "_topdir ${TOPDIR}" -ba $TOPDIR/SPECS/$NAME.spec
#cdmi-dcache-qos-1.0cdmi1.2-1.x86_64.rpm
cp ${TOPDIR}/RPMS/x86_64/cdmi-dcache-qos-$SERVICE_VERSION-1.*x86_64*.rpm .

rm -rf ./rpm CDMI cdmi-spi

