#!/bin/bash

if [ ! -z "${EPHEMERIS_FILE}" -a -f "${EPHEMERIS_FILE}" ]
then
  EPHEMERIS_OPTS="-ephemeris ${EPHEMERIS_FILE}"
fi

java com.obliquity.astronomy.almanac.test.GreatCircleFlight \
  ${EPHEMERIS_OPTS} \
  -startdate '2019-12-12 14:00' -startpos -5.71,57.8 \
  -enddate '2019-12-12 20:00' -endpos -107.25,60.12 \
  -target moon \
  "$@"
