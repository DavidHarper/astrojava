#!/bin/bash

SCRIPT_DIR=`dirname $0`

JAR_FILE=${SCRIPT_DIR}/almanac.jar

APPCLASS=com.obliquity.astronomy.almanac.test.NightlyPlanetNotes

if [ ! -f ${JAR_FILE} ]
then
  echo "Cannot find JAR file ${JAR_FILE}. Run the build script."
  exit 1
fi

if [ ! -z "${EPHEMERIS_FILE}" -a -f "${EPHEMERIS_FILE}" ]
then
  EPHEMERIS_OPTS="-ephemeris ${EPHEMERIS_FILE}"
fi

java ${JAVA_OPTS} -classpath ${JAR_FILE} ${APPCLASS} ${EPHEMERIS_OPTS} "$@"
