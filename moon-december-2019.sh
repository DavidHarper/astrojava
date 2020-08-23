#!/bin/bash

SCRIPT_DIR=`dirname $0`

export APPCLASS=com.obliquity.astronomy.almanac.test.GreatCircleFlight

exec ${SCRIPT_DIR}/runapp.sh \
  -startdate '2019-12-12 14:00' -startpos -5.71,57.8 \
  -enddate '2019-12-12 20:00' -endpos -107.25,60.12 \
  -target moon \
  "$@"
