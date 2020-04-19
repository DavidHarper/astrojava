#!/bin/bash

SCRIPT_DIR=`dirname $0`

JAR_FILE=${SCRIPT_DIR}/almanac.jar

if [ -f ${JAR_FILE} ]
then
  CLASSPATH=${JAR_FILE}
else
  CLASSPATH=${SCRIPT_DIR}
fi

MAIN_CLASS=com.obliquity.astronomy.almanac.test.TwilightExplorer

java ${JAVA_OPTS} -classpath ${CLASSPATH} ${MAIN_CLASS} "$@"
