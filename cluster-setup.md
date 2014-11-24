# Setup a cluster

## Neo4j HA cluster

### Overview
The architecture of the Neo4j HA cluster can be found [in the documentation](http://neo4j.com/docs/stable/ha-architecture.html).

### Installation
The Neo4j server can be [installed using your package manager](http://debian.neo4j.org/?_ga=1.174493282.1166350782.1407319663). In this bachelor thesis the Enterprise version of the Neo4j server is used. Some features are available in this version only and I remember it was something necessary but have not found it again yet.

Using the package manager a user `neo4j` is created automatically. However, some I/O action seemed to be executed as root.

### Code changes
Changing `GraphDatabaseFactory` to `HighlyAvailableGraphDatabaseFactory` in the creation process of the `GraphDatabaseService` should be sufficient.

### Configuration
This configuration is a result of the [HA setup tutorial](http://neo4j.com/docs/stable/ha-setup-tutorial.html) and the [performance guide](http://docs.neo4j.org/chunked/stable/performance-guide.html).
Detailed information can be found in the [HA configuration section](http://neo4j.com/docs/stable/ha-configuration.html) and the [server configuration section](http://neo4j.com/docs/stable/server-configuration.html) of the documentation.

The configuration files can be found at `$NEO_HOME/conf` with `$NEO_HOME` being `/var/lib/neo4j` when installed via package manager.

#### Master
**conf/neo4j.properties**

    # unique cluster instance identifier
    ha.server_id=1
    # endpoint for cluster communication
    ha.cluster_server=127.0.0.1:5001
    # initial cluster nodes
    ha.initial_hosts=127.0.0.1:5001,127.0.0.1:5002
    # endpoint for synchronization with master
    ha.server=127.0.0.1:6001

#### Slave #1
**conf/neo4j.properties**

    # unique cluster instance identifier
    ha.server_id=2
    # endpoint for cluster communication
    ha.cluster_server=127.0.0.1:5002
    # initial cluster nodes
    ha.initial_hosts=127.0.0.1:5001,127.0.0.1:5002
    # endpoint for synchronization with master
    ha.server=127.0.0.1:6002
    # node can not be elected to master node
    ha.slave_only=true

#### Shared configuration
In addition several changes were made on each node.

| Location | Command | Description |
| -------- | ------- | ----------- |
| conf/neo4j-server.properties | org.neo4j.server.database.mode=HA | enable HA mode of the database |
| conf/neo4j-wrapper.conf | wrapper.java.additional=-server | Start JVM in server mode. |
||                          wrapper.java.additional=-XX:+UseConcMarkSweepGC | Enable concurrent garbage collector. |
| /etc/security/limits.conf | neo4j  soft  nofile  40000<br>neo4j  hard  nofile  40000 | Increase maximum number of open files to 40.000. |
| /etc/pam.d/su | session required pam_limits.so | see above |

### Startup
Using the package manager Neo4j installs its service `neo4j-service` starting automatically at system startup.
This behaviour can also be applied after a different installation method and switched on/off.
Using the service mechanism the server can be started, stopped and restarted.
