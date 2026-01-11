# astrojava

A Java library to read JPL Development Ephemeris files and calculate
planetary positions from the data.

## Building the library

This project uses Gradle as its build tool.  You will need a recent
version of Gradle and a Java compiler capable of generating Java 16
code.  The source code is in the standard location for a Gradle
project, namely

`src/main/java`

The simplest option is to generate a JAR file containing the library
and all of the test classes.

`gradle jar`

The JAR file can be found in **build/libs**

To generate a Maven artefact, run

`gradle publish`

The generated files can be found in **build/repo**

## Documentation

The Javadoc documentation for the classes in this library is sparse
to non-existent.  If you wish to use this library, you should look
at the examples in the com.obliquity.astronomy.almanac.test package
to understand how the library works.

## Examples

### Prerequisites

**No data files are bundled with this project!**

You **must** download a JPL Development Ephemeris file from the
JPL Solar System Dynamics FTP server before you can use any of the
example programs.  The base URL is

`ftp://ssd.jpl.nasa.gov/pub/eph/planets/Linux`

Note that these data files can be used on Windows or macOS as well
as on Linux.  The directory name simply denotes that the binary
data files are in little-endian format.

The DE430 ephemeris is a good first choice, as it covers the period
from 1550 CE to 2650 CE.

If you need an ephemeris which covers a very long time span, you should
use DE431, which covers 12999 BCE to 17000 CE.  Be aware, however, that
the ephemeris data file is almost 3 GB in size.

### A Simple Almanac Program

The class *com.obliquity.astronomy.almanac.test.SimpleAlmanac* generates
a basic almanac listing for a selected planet or the Sun or the Moon.
After building the JAR file for this project, you can invoke this
class by running the shell script **almanac.sh**

If you run the script with no arguments, or with the **-help** option,
it will display lists of mandatory and optional arguments and an
explanation of the output format.

### A Planetary Conjunction Finder

The class *com.obliquity.astronomy.almanac.test.ConjunctionFinder*
calculates the dates and times of conjunctions between pairs of
Solar System bodies as seen from Earth.  After building the JAR
file for this project, you can invoke this class by running the
shell script **conjunctionfinder.sh**

If you run the script with no arguments, or with the **-help** option,
it will display lists of mandatory and optional arguments and an
explanation of the output format.

### Angular Separation Between Planets

The class *com.obliquity.astronomy.almanac.test.PlanetSeparation*
calculates the angular separation between two Solar System bodies
as seen from the Earth.  After building the JAR file for this project,
you can invoke this class by running the shell script **planetseparation.sh**

If you run the script with no arguments, or with the **-help** option,
it will display lists of mandatory and optional arguments and an
explanation of the output format.

### Dates of Saturn Ring-Plane Crossing

The class *com.obliquity.astronomy.almanac.test.SaturnRingPlaneCrossingFinder*
calculates the dates and times when the Earth or Sun cross the plane of
Saturn's rings.  After building the JAR file for this project,
you can invoke this class by running the shell script **ringplanecrossing.sh**

If you run the script with no arguments, or with the **-help** option,
it will display lists of mandatory and optional arguments.

### Visibility of the New Crescent Moon

The class *com.obliquity.astronomy.almanac.test.MoonVisibility*
calculates the visibility of the new crescent Moon on successive evenings
after astronomical New Moon, using Yallop's Moon visibility criterion.
After building the JAR file for this project, you can invoke this class by
running the shell script **moonvisibility.sh**

If you run the script with no arguments, or with the **-help** option,
it will display lists of mandatory and optional arguments.

## DISCLAIMER

THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY
APPLICABLE LAW.  EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT
HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY
OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE.  THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM
IS WITH YOU.  SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF
ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
