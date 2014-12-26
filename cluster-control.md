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

## Solution
It seems to create a "service daemon" is [more difficult than I thought](http://stackoverflow.com/questions/27623916/create-a-service-process-using-python).
My current solution is a [Python script](src/main/python/circusman.py) that
* generates a file with IP addresses of the nodes that form the circus cluster (e.g. 192.168.0.1 to 192.168.0.32) on startup,
* connects to the circus instances running on all these nodes and
* generates a HTML document that is auto-refreshing every second, containing the nodes and their status.

### Startup

    $ cd src/main/python
    $ python circusman.py

### Status
If a node isn't reachable it is considered offline. If it is reachable there will be a list of running circus watchers shown.
### Commands
You can start and stop a watcher on all nodes simultaneously.

    $ start myapp
    $ stop myapp

This is not a nice solution. The status is shown in the browser while commands are accepted by the terminal.
It would be nice to have a webapp that supports to start/stop a certain/all watchers on a certain/all nodes and use circus' event subscription system to determine the watcher status while checking the circus instance status in a less frequent interval than I am doing now. One could also make the log files of each watcher readable or even downstream them to a local directory using a circus plugin.

However for me it is acceptable, because a heartbeat of 1s is not too bad.
Based on this script I could now collect statistics from each node.
