#!/bin/sh

set -e

if [ -z $CLASSPATH ]; then
  export CLASSPATH="../../../build/classes/main:../../../../com.endpoint.lg.support/build/classes/main"
  echo "Guessing classpath to be: $CLASSPATH"
else
  echo "Classpath set to $CLASSPATH"
fi

if [ -z ${JAVA_PATH} ]; then
  echo "JAVA_PATH must be set to e.g. /usr/lib/jvm/java-1.7.0-openjdk-amd64/"
  exit 1
else
  echo "JAVA_PATH is set to $JAVA_PATH"
  ${JAVA_PATH}/bin/javah -classpath $CLASSPATH com.endpoint.lg.evdev.writer.UinputDevice
  g++ -fPIC -o libispaces-uinput.so -lc -shared -I${JAVA_PATH}/include -I${JAVA_PATH}/include/linux ispaces-uinput.cpp
fi
