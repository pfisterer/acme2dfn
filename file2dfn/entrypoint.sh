#!/bin/sh

# immediately exit if any command being run exits with a non-zero exit code.
set -e

if [[ $# -eq 0 ]]; then
	echo "Startup without arguments"
	mvn exec:java -Dexec.mainClass=de.farberg.file2dfn.Main

else
	echo "Startup using arguments: $@"
   mvn exec:java -Dexec.mainClass=de.farberg.file2dfn.Main -Dexec.args="$@"
fi
