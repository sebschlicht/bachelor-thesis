#!/bin/bash
p=`pgrep circusd`
kill -15 ${p}
while ps -p ${p} 1>/dev/null; do sleep 1; done;
~/circus/start.sh

