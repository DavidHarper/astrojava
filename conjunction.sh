#!/bin/bash

SCRIPT_DIR=`dirname $0`

JAR_FILE=${SCRIPT_DIR}/build/libs/astrojava-2.0.jar

if [ -f ${JAR_FILE} ]
then
  CLASSPATH=${JAR_FILE}
else
  CLASSPATH=${SCRIPT_DIR}/build/classes/java/main
fi

MAIN_CLASS=com.obliquity.astronomy.almanac.test.ConjunctionFinder

java ${JAVA_OPTS} -classpath ${CLASSPATH} ${MAIN_CLASS} "$@"