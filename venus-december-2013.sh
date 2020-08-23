#!/bin/bash

SCRIPT_DIR=`dirname $0`

export APPCLASS=com.obliquity.astronomy.almanac.test.GreatCircleFlight

export JAVA_OPTS="-Ddebug=true"

${SCRIPT_DIR}/runapp.sh \
    -startdate '2013-12-18 15:30' -startpos 0,51.5 -enddate '2013-12-19 01:00' -endpos -120,48 \
    -target venus \
    "$@"

${SCRIPT_DIR}/runapp.sh \
    -startdate '2013-12-18 15:30' -startpos 0,51.5 -enddate '2013-12-19 01:00' -endpos -120,48 \
    -target sun \
    "$@"
