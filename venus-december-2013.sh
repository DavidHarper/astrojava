#!/bin/bash

java -Ddebug=true com.obliquity.astronomy.almanac.test.GreatCircleFlight -ephemeris ~/data/jpl/de406/unxp1800.406 -startdate '2013-12-18 15:30' -startpos 0,51.5 -enddate '2013-12-19 01:00' -endpos -120,48 -target venus

java -Ddebug=true com.obliquity.astronomy.almanac.test.GreatCircleFlight -ephemeris ~/data/jpl/de406/unxp1800.406 -startdate '2013-12-18 15:30' -startpos 0,51.5 -enddate '2013-12-19 01:00' -endpos -120,48 -target sun
