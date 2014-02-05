#!/bin/sh

set -e

export CLASSPATH="../../../build/classes/main:../../../../com.endpoint.lg.support/build/classes/main"
JAVA_PATH=/usr/lib/jvm/java-7-openjdk-amd64

${JAVA_PATH}/bin/javah com.endpoint.lg.evdev.writer.UinputDevice
g++ -fPIC -o libispaces-uinput.so -lc -shared -I${JAVA_PATH}/include -I${JAVA_PATH}/include/linux ispaces-uinput.cpp
