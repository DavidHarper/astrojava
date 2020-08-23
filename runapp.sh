#!/bin/bash

if [ -z "${APPCLASS}" ]
then
    echo "Set APPCLASS and re-run"
    exit 1
fi

SCRIPT_DIR=`dirname $0`

JAR_FILE=${SCRIPT_DIR}/build/libs/astrojava-2.0.jar

if [ -f "${JAR_FILE}" ]
then
  CLASSPATH="${JAR_FILE}"
else
  CLASSPATH="${SCRIPT_DIR}/build/classes/java/main"
fi

if [ ! -z "${EPHEMERIS_FILE}" -a -f "${EPHEMERIS_FILE}" ]
then
  EPHEMERIS_OPTS="-ephemeris ${EPHEMERIS_FILE}"
fi

java ${JAVA_OPTS} -classpath ${CLASSPATH} ${APPCLASS} ${EPHEMERIS_OPTS} "$@"
