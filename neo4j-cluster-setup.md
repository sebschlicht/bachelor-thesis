# Setup a Neo4j cluster

## Overview
The architecture of the Neo4j HA cluster can be found [in the documentation](http://neo4j.com/docs/stable/ha-architecture.html).

## Installation
The Neo4j server can be [installed using your package manager](http://debian.neo4j.org/?_ga=1.174493282.1166350782.1407319663). The enterprise version is the only one supporting HA mode and thus is necessary for this bachelor thesis.

Using the package manager a user `neo4j` is created automatically. However, some I/O action seemed to be executed as root.

`$NEO_HOME` (`/var/lib/neo4j` when installed via package manager) is the root directory of Neo4j. Relative paths are always relative to this directory.

## Plugin
To deploy a plugin to Neo4j you have to put it in the Neo4j plugin directory `$NEO_HOME/plugins`.
Make sure to use a uber-JAR that provides all dependencies your application needs, if any.

## Code changes
Changing `GraphDatabaseFactory` to `HighlyAvailableGraphDatabaseFactory` in the creation process of the `GraphDatabaseService` should be sufficient.

## Configuration
This configuration is a result of the [HA setup tutorial](http://neo4j.com/docs/stable/ha-setup-tutorial.html) and the [performance guide](http://docs.neo4j.org/chunked/stable/performance-guide.html).
Detailed information can be found in the [HA configuration section](http://neo4j.com/docs/stable/ha-configuration.html) and the [server configuration section](http://neo4j.com/docs/stable/server-configuration.html) of the documentation.

Only changes in the configuration files are noted.
The configuration files can be found at `$NEO_HOME/conf`.

**conf/neo4j.properties** (changes only):

    # unique cluster instance identifier
    ha.server_id=${identifier}
    # endpoint for cluster communication
    ha.cluster_server=${address}:5001
    # initial cluster nodes
    ha.initial_hosts=${initial_hosts}
    # endpoint for synchronization with master
    ha.server=${address}:6001
    ha.slave_only=${slave_only}

**conf/neo4j-server.properties** (changes only):

    org.neo4j.server.database.mode=HA

### OS configuration
In addition to Neo4j the OS has to be configured.

| Location | Command | Description |
| -------- | ------- | ----------- |
| /etc/security/limits.conf | neo4j  soft  nofile  40000<br>neo4j  hard  nofile  40000 | Increase maximum number of open files to 40.000. |
| /etc/pam.d/su | session required pam_limits.so | see above |

### Startup
Using the package manager Neo4j installs its service `neo4j-service` starting automatically at system startup.
This behaviour can also be applied after a different installation method and switched on/off.
Using the service mechanism the server can be started, stopped and restarted.

To avoid manual logins to each cluster node I derived a synchronous [Neo4j script](src/main/resources/neo4j-circus.sh) that [can be used with Circus](../../wiki/Control-Neo4j-via-process-management-tool-Circus), a process management tool that allows remote control.

