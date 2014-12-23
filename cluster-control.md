## Introduction
When a cluster contains more than three nodes it gets annoying to execute commands on each single node.
Thus an automatic process of starting and stopping the services (Neo4j/Titan) is required.
We might also want to reset the stored data created during the experiments, in order to make clean restarts, e.g. to repeat experiments.
In addition to these actions, it would be nice to gain statistics for each node using the same method.
It would be great if we would not have to specify each cluster node on our own, so we could add and remove cluster nodes dynamically.

## Requirements
* start/stop and monitor processes
 * Neo4j
 * Titan
* start scripts
 * configuration
 * database reset
* statistics for processes
* detect cluster nodes

## [Circus](http://circus.readthedocs.org/en/0.11.1/)
`circus` is Python software that uses ZMQ sockets to send/retrieve commands to a node running `circus`. [Commands](http://circus.readthedocs.org/en/0.11.1/for-ops/commands/) can start/stop both processes and scripts and retrieve statistics for a process.
What `circus` lacks in, is a method to detect the cluster of `circus` nodes.

## Solution Concept
All nodes are expected to be in the same network. Their IP addresses will form a range.
A simple Python script could do a IP range scan in order to retrieve a list of running `circus` nodes and make this list available e.g. via a web service.
This script can be executed whenever nodes were added or removed.
There will be a [daemon](https://pypi.python.org/pypi/python-daemon/) running that updates its node list whenever this script was executed.
The script uses the daemon to send commands to all nodes on request and automatically retrieves process statistics in a fixed interval.
