#!/bin/sh

java -cp `cat /app/cp.txt` de.farberg.file2dfn.Main "$@"