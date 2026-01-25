#!/bin/bash

SCRIPT_DIR=`dirname $0`

export APPCLASS=com.obliquity.astronomy.almanac.test.MoonAndSunPhenomena

exec ${SCRIPT_DIR}/runapp.sh "$@"
