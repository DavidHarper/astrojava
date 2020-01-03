#!/bin/bash

if [ ! -z "${EPHEMERIS_FILE}" -a -f "${EPHEMERIS_FILE}" ]
then
  EPHEMERIS_OPTS="-ephemeris ${EPHEMERIS_FILE}"
fi

java -Ddebug=true com.obliquity.astronomy.almanac.test.GreatCircleFlight ${EPHEMERIS_OPTS} \
    -startdate '2013-12-18 15:30' -startpos 0,51.5 -enddate '2013-12-19 01:00' -endpos -120,48 \
    -target venus \
    "$@"

java -Ddebug=true com.obliquity.astronomy.almanac.test.GreatCircleFlight ${EPHEMERIS_OPTS} \
    -startdate '2013-12-18 15:30' -startpos 0,51.5 -enddate '2013-12-19 01:00' -endpos -120,48 \
    -target sun \
    "$@"
