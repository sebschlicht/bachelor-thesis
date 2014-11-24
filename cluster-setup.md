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

**Aliases for configuration files**

| Key | File |
| --- | ---- |
| neo4j | conf/neo4j.properties |
| neo4j-server | conf/neo4j-server.properties |
| neo4j-wrapper | conf/neo4j-wrapper.conf |

`IP` is the IP address of the current node that all cluster nodes can access.
`IP:M` is the IP address of the master node. `IP:SN` is the IP address of the n-th slave node.

#### All cluster nodes
| Location | Command | Description |
| -------- | ------- | ----------- |
| neo4j    | ha.initial_hosts=`IP:M`,`IP:S1`,...,`IP:SN` | IP addresses of initial cluster nodes |
||           ha.cluster_server = `IP` | IP endpoint to listen at for cluster communication (default port: 5001) |
| neo4j-server | org.neo4j.server.database.mode=HA | enable HA mode of the database |
||               org.neo4j.server.webserver.address=0.0.0.0 | enable web interface listening on IP specified |
| neo4j-wrapper | wrapper.java.additional=-server | Start JVM in server mode. |
||                wrapper.java.additional=-XX:+UseConcMarkSweepGC | Enable concurrent garbage collector. |
| /etc/security/limits.conf | neo4j  soft  nofile  40000<br>neo4j  hard  nofile  40000 | Increase maximum number of open files to 40.000. |
| /etc/pam.d/su | session required pam_limits.so | see above |

#### Master only
| Location | Command | Description |
| -------- | ------- | ----------- |
| neo4j    | ha.server_id = 1 | unique cluster instance identifier |

#### Slave #1 only
| Location | Command | Description |
| -------- | ------- | ----------- |
| neo4j    | ha.server_id = 2 | unique cluster instance identifier |
||           ha.server = `IP` | IP endpoint to listen at for transaction synchronization with master (default port: 6001), **must not** equal `ha.cluster_server` |
||           ha.slave_only = true | can not be elected to master node |

### Startup
Using the package manager Neo4j installs its service `neo4j-service` starting automatically at system startup.
This behaviour can also be applied after a different installation method and switched on/off.
Using the service mechanism the server can be started, stopped and restarted.
