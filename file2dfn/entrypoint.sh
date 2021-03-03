#!/bin/sh

# immediately exit if any command being run exits with a non-zero exit code.
set -e

CLASSPATH="`cat /app/cp.txt`"
echo "Using classpath: $CLASSPATH"

if [[ $# -eq 0 ]]; then
	echo "Startup without arguments"
	java -cp "$CLASSPATH" de.farberg.file2dfn.Main
else
	echo "Startup using arguments: $@"
	java -cp "$CLASSPATH" de.farberg.file2dfn.Main "$@"
fi
