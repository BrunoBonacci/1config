#!/bin/bash

export BASE=${1:-.}
export PLATFORM=${2}

# using GraalVM CE 20.3.0 java 11
native-image --report-unsupported-elements-at-runtime \
             --no-server \
             --no-fallback \
             -H:+PrintClassInitialization \
             -H:ConfigurationFileDirectories=$BASE/graalvm-config/ \
             --initialize-at-build-time \
             --allow-incomplete-classpath \
             --enable-http \
             --enable-https \
             --enable-all-security-services \
             -jar `ls -1 $BASE/target/*-standalone.jar` \
             -H:Name=$BASE/target/1cfg$PLATFORM
