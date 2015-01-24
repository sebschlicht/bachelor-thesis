#!/bin/bash
p=`pgrep circusd`
if [ ${p} ]; then
  kill -15 ${p}
  while ps -p ${p} 1>/dev/null; do sleep 1; done;
fi
~/circus/start.sh
