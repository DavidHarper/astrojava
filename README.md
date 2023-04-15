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

## Examples

