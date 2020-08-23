#!/bin/bash

SCRIPT_DIR=`dirname $0`

export APPCLASS=com.obliquity.astronomy.almanac.test.NightlyPlanetNotes

exec ${SCRIPT_DIR}/runapp.sh "$@"
