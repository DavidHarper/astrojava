#!/bin/bash

java com.obliquity.astronomy.almanac.test.GreatCircleFlight \
  -ephemeris /Users/adh/data/jpl/de406/unxp1800.406 \
  -startdate '2019-12-12 14:00' -startpos -5.71,57.8 \
  -enddate '2019-12-12 20:00' -endpos -107.25,60.12 \
  -target moon
