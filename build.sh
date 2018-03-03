#!/bin/bash

SRC_DIR=com/obliquity/astronomy/almanac

JAR_FILE=almanac.jar

MANIFEST_FILE=MANIFEST.MF

if [ -e ${JAR_FILE} ]
then
    rm -f ${JAR_FILE}
fi

rm -f ${SRC_DIR}/*.class ${SRC_DIR}/*/*.class

javac ${SRC_DIR}/*.java ${SRC_DIR}/*/*.java

RC=$?

if [ $RC -ne 0 ]
then
    echo "Compilation stage failed."
    exit $RC
fi

jar cvmf ${MANIFEST_FILE} ${JAR_FILE} ${SRC_DIR}/*.class ${SRC_DIR}/*/*.class

exit $?