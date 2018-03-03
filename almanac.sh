#!/bin/bash

SCRIPT_DIR=`dirname $0`

JAR_FILE=${SCRIPT_DIR}/almanac.jar

if [ ! -f ${JAR_FILE} ]
then
  echo "Cannot find JAR file ${JAR_FILE}. Run the build script."
  exit 1
fi

java ${JAVA_OPTS} -jar ${JAR_FILE} "$@"