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
While every cluster node running Neo4j/Titan will run a Circus server that allows to start/stop and monitor defined applications ("watcher"), the controller runs a [controller script](src/main/python/circusman.py) (Python) that can send commands to the cluster nodes via ZMQ. This allows the controller to start/stop all cluster nodes simultaneously and retrieve statistics. The cluster is defined via the [`cluster` command](#change-the-cluster-range).

Since the number of cluster nodes can vary between experiments and the node configuration depends on the cluster nodes, a convenient method of configuring the nodes is necessary.
Circus allows to define own commands and we will define a `configure` command that configures the two services.

### Controller usage
#### Startup

    $ cd src/main/python
    $ python circusman.py

will bring you into the controller console.

#### Start Circus
You can start a Circus node using the following command. It uploads the `configure` command and then executes the startup script located in `/home/node/circus/start.sh` via parallel SSH.

    $ >startCircus

Please note that the command does not wait for Circus to start up. It returns immediately after executing the script. You can watch the status of the node in the status HTML file generated.

#### Restart Circus
You can restart a Circus node using the following command. It uploads the `configure` command and then executes the restart script located in `/home/node/circus/restart.sh` via parallel SSH.

    $ >restartCircus

Please note that the command does not wait for Circus to shutdown or start up. It returns immediately after executing the script. You can watch the status of the node in the status HTML file generated.

#### Upload configuration file templates
If you made changes to the configuration file templates, e.g. updated a port that will effect all nodes, you can upload the templates again via parallel SSH.

    $ >upload

#### Start watchers

    $ >start [watcher_name]

Please note that the command does not wait for the watcher startup. It returns immediately after getting a response from the Circus node. You can watch the status of each watcher in the status HTML file generated.

#### Stop watchers

    $ >stop [watcher_name]

Please note that the command does not wait for the watcher shutdown. It returns immediately after getting a response from the Circus node. You can watch the status of each watcher in the status HTML file generated.

#### Change the cluster range
The default cluster contains your local machine only. You can change the cluster, e.g. when adding/removing nodes. Specify a network and the number of nodes.

    $ >cluster network numNodes

The IP addresses start with 1 and increase up to 254. The identifier equal the last part of the IP address.  
For instance

    $ >cluster 192.168.0 32

will lead to a cluster from 192.168.0.1 to 192.168.0.32.

More than 254 nodes is not supported, since only the last portion of the IP address is altered by the script.

#### Configure watchers
Whenever a node was added/removed or we want to change a configuration option we 
* stop all nodes,

  `$ >stop`

* (only if nodes were added/removed:) define the new IP address range,

  `$ >cluster <network> <numNodes>`

* re-configure all nodes using our [Circus command `configure`](#circus-command-configure) and

  `$ >configure`

* bring them up again

  `$ >start`

### Circus node configuration
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

### Circus command (configure)
To update the configuration of the cluster nodes we define a new `circus` command, the [configure command](src/main/python/CommandConfigure.py). After [adding the command](../../wiki/HowTo:-Create-a-custom-circus-command) to the Circus nodes it can be called using the controller:

    $>configure

but before executing the command on the node, the controller updates the configuration file templates via parallel SSH.

Along with the command some information is sent to each node:
 * node identifier `identifier`
 * node address `address`
 * Neo4j master election flag `isMaster`
 * list containing addresses of all nodes `cluster`

These information can be used in the configuration file creation process using the (updated) templates and thus we can
* update the configuration when nodes were added/removed
* update any configuration option simultaneously

One can add additional information to the command if necessary.

#### Configuration
To write the configuration files using templates you need to specify where the configuration and template files are located. At the moment this is hard-coded in the command:

    ...
    # paths to configuration file templates
    PATH_TMPL = '/usr/local/etc/templates/'
    ...
    # paths to configuration files
    PATH_CONF_NEO4J = '/etc/neo4j/'
    PATH_CONF_TITAN = '/etc/titan/'
    ...

## [Circus](http://circus.readthedocs.org/en/0.11.1/)
Circus is Python software that uses ZMQ sockets to send/retrieve commands to a node running Circus. [Commands](http://circus.readthedocs.org/en/0.11.1/for-ops/commands/) can start/stop both processes and scripts and retrieve statistics for a process.
What Circus lacks in, is a method to detect the cluster nodes.

## Current Solution
It seems to create a "service daemon" is [more difficult than I thought](http://stackoverflow.com/questions/27623916/create-a-service-process-using-python).
My current solution is a [Python script](src/main/python/circusman.py) that
* generates a file with IP addresses of the nodes that form the circus cluster (e.g. 192.168.0.1 to 192.168.0.32) on startup,
* connects to the circus instances running on all these nodes and
* generates a HTML document that is auto-refreshing every second, containing the nodes and their status.

### Status
If a node isn't reachable it is considered offline. If it is reachable there will be a list of running circus watchers shown.

This is not a nice solution. The status is shown in the browser while commands are accepted by the terminal.
It would be nice to have a webapp that supports to start/stop a certain/all watchers on a certain/all nodes and use circus' event subscription system to determine the watcher status while checking the circus instance status in a less frequent interval than I am doing now. One could also make the log files of each watcher readable or even downstream them to a local directory using a circus plugin.

However for me it is acceptable, because a heartbeat of 1s is not too bad.
Based on this script I could now collect statistics from each node.
