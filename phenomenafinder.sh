#!/bin/bash

SCRIPT_DIR=`dirname $0`

export APPCLASS=com.obliquity.astronomy.almanac.phenomena.PhenomenaFinder

exec ${SCRIPT_DIR}/runapp.sh "$@"
