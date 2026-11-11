#!/usr/bin/env sh
DIRNAME=$(dirname "$0")
PROGNAME=$(basename "$0")
JAVACMD="java"
exec "$JAVACMD" -jar "$DIRNAME/gradle/wrapper/gradle-wrapper.jar" "$@"
