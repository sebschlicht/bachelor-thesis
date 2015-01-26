#!/bin/bash
#
# Author: Sebastian Schlicht
#
# Clears remote service data and/or log files.
#
if [ "$1" = '' ] || [ "$1" = 'data' ]; then
  rm -rf /var/lib/neo4j/data/*
  rm -rf /var/lib/titan/db/*
fi
if [ "$1" = '' ] || [ "$1" = 'log' ]; then
  >/var/log/neo4j/console.log
  >/var/log/titan/cassandra.log
  >/var/log/titan/rexstitan.log
fi

