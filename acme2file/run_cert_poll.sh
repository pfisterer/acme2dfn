#!/bin/bash

# Check if this script is already running
for pid in $(pidof -x "$0"); do
    if [ $pid != $$ ]; then
        echo "[$(date)] $0 : Process is already running with PID $pid"
        exit 0
    fi
done

# Run cert_poll.py forever
while true; do
	sleep 15s;
	echo "Running cert_poll.py"
	(cd /var/www/acme2certifier/tools; python3 cert_poll.py)
done
