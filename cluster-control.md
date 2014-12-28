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

## Solution
There will be one cluster controller, maybe the benchmark client machine.
While every cluster node running Neo4j/Titan will run a `circus` server that allows to start/stop and monitor defined applications ("watcher"), the controller runs a [controller script](src/main/python/circusman.py) (Python) that can send commands to the cluster nodes via ZMQ. This allows the controller to start/stop all cluster nodes simultaneously and retrieve statistics.
The cluster is formed by an IP address range defined in the script. (default: 192.168.0.1 - 192.168.0.32)

Since the number of cluster nodes can vary between experiments and the node configuration depends on the cluster nodes, a convenient method of configuring the nodes is necessary.
`circus` allows to define own commands and we will define a `configure` command that configures the two services. This command will contain the nodes's address along with the addresses of all the other cluster nodes.
Whenever a nodes was added/removed we 
* stop all nodes,

  `$>stop`

* define the new IP address range,

  `$>cluster <network> <numNodes>`

* re-configure all nodes using this custom command and

  `$>configure`

* bring them up again

  `$>start`

in the controller script console.

WARNING: This is work in progress. At the moment the controller script does not support the `configure` command.

### `circus` configuration
Two watchers are necessary to control the cluster:
* Neo4j
* Titan

The watchers will be started by the cluster controller on request.
This ensures that the services were configured properly before started.

    [circus]
    TODO
    [watcher:titan]
    TODO
    [watcher:neo4j]
    TODO

### `circus` command (configure)
To update the configuration of the cluster nodes we define a new `circus` command, the [configure command](src/main/python/CommandConfigure.py).

Read the [command creation tutorial](../../wiki/HowTo:-Create-a-custom-circus-command) to see how it is deployed to `circus`. When deployed it can be called using the controller:

    $>configure

If necessary the command could be extended to submit the configuration file patterns rather than using template files existing on each node. This would enable us to change every configuration option simultaneously, in addition to the current use case.

## [Circus](http://circus.readthedocs.org/en/0.11.1/)
`circus` is Python software that uses ZMQ sockets to send/retrieve commands to a node running `circus`. [Commands](http://circus.readthedocs.org/en/0.11.1/for-ops/commands/) can start/stop both processes and scripts and retrieve statistics for a process.
What `circus` lacks in, is a method to detect the cluster of `circus` nodes.

## Current Solution
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


