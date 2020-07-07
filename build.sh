#!/bin/bash

which gradle > /dev/null 2>&1

RC=$?

if [ $RC -ne 0 ]
then
    echo "You must install Gradle on this computer."
    exit 1
fi

gradle build

RC=$?

if [ $RC -eq 0 ]
then
    echo "The JAR file is in build/libs"
    ls -l build/libs
fi

exit $?