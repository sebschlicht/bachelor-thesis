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
    $ ./CircusMan.py

will bring you into the controller console.
Type `help` to get a list of available commands.
You can get the documentation of any command by passing `-h` to it. The documentation below is copied from these help texts.  
The CircusMan console supports command history.

#### Restart Circus
Restarts Circus on all cluster nodes where it is running. Start Circus on all cluster nodes where Circus is not running.
Updates all scripts/service plugins/config templates at first.
    
    $ restartCircus

#### Start watchers
Starts the Circus arbiter/a Circus watcher on all cluster nodes. Circus must be running on the nodes in order to use this command.
The command does not block until the arbiter/watcher is actually running but returns immediately.

    $ start -h
    usage: start [-h] [name]
    
    Starts the Circus arbiter/a Circus watcher on all cluster nodes. Circus must be
    running on the nodes in order to use this command.
    
    positional arguments:
      name        Start a certain watcher only. By default the arbiter is started.

#### Stop watchers
Stops the Circus arbiter/a Circus watcher on all cluster nodes. Circus must be running on the nodes in order to use this command.
The command does not block until the arbiter/watcher has stopped but returns immediately.

    $ stop -h
    usage: stop [-h] [name]
    
    Stops the Circus arbiter/a Circus watcher on all cluster nodes. Circus must be
    running on the nodes in order to use this command.
    
    positional arguments:
      name        Stop a certain watcher only. By default the arbiter is stopped.

#### Update the cluster
The cluster is loaded from a specific file. If you changed the cluster file or
want to use another file you can use this command in order to redefine the cluster.

    $ cluster -h
    usage: cluster [-h] [filePath] [port]
    
    Redefines the cluster addresses and the Circus port.
    
    positional arguments:
      filePath    Path to the cluster file. Each line must contain one node
                  address. (default: <repository-dir>/src/main/resources/nodes)
      port        remote Circus port (default: 5555)

#### Configure watchers
Whenever a node was added/removed or we want to change a configuration option we 
* stop all nodes,

  `$ stop`

* (only if nodes were added/removed:) reload the cluster,

  `$ cluster`

* re-configure all nodes using our [Circus command `configure`](#circus-command-configure) and

  `$ configure`

* bring them up again

  `$ start`

### Circus node configuration
Two watchers are necessary to control the cluster:
* Neo4j
* Titan

The watchers will be started by the cluster controller on request.
This ensures that the services were configured properly before started.

### Circus command (configure)
To update the configuration of the cluster nodes we define a new `circus` command, the [configure command](src/main/python/CommandConfigure.py). After [adding the command](../../wiki/HowTo:-Create-a-custom-circus-command) to the Circus nodes it can be called using the controller:

    $ configure

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

