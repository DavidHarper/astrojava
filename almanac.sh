#!/bin/bash

SCRIPT_DIR=`dirname $0`

JAR_FILE=${SCRIPT_DIR}/build/libs/astrojava-2.0.jar

if [ ! -f ${JAR_FILE} ]
then
  echo "Cannot find JAR file ${JAR_FILE}. Run 'gradle build' to create it."
  exit 1
fi

if [ ! -z "${EPHEMERIS_FILE}" -a -f "${EPHEMERIS_FILE}" ]
then
  EPHEMERIS_OPTS="-ephemeris ${EPHEMERIS_FILE}"
fi

java ${JAVA_OPTS} -jar ${JAR_FILE} ${EPHEMERIS_OPTS} "$@"
