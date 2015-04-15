#!/bin/bash
SERVICE=$1
WORKLOAD=$2
CLUSTER_SIZE=$3

# check service type argument
if [ "${SERVICE}" == "neo4j" ]; then
  # do nothing
  echo ''
elif [ "${SERVICE}" == "titan" ]; then
  # do nothing
  echo ''
else
  echo 'Service type is missing or invalid. Use "neo4j" or "titan".'
  exit 1
fi
# check workload
if [ "${WORKLOAD}" == "read" ]; then
  # do nothing
  echo ''
elif [ "${WORKLOAD}" == "write" ]; then
  # do nothing
  echo ''
else
  echo 'Workload is missing or invalid. Use "read" or "write".'
  exit 1
fi
# check cluster size argument
if (( CLUSTER_SIZE < 1 )); then
  echo 'Cluster size is missing or too low. Specify a cluster size, with a minimum value of 1.'
  exit 1
fi
# re-link bootstrap log file
rm bootstrap.log
ln -s ../bootstrap${CLUSTER_SIZE}m.log bootstrap.log
# prepare Java main class arguments
NUM_REQUESTS=$(( CLUSTER_SIZE * 100000 ))
CONFIG_PATH="src/main/resources/client-config/"${WORKLOAD}"-"${SERVICE}${CLUSTER_SIZE}".properties"
# start benchmark
mvn exec:java -Dexec.args="${NUM_REQUESTS} ${CONFIG_PATH}"
