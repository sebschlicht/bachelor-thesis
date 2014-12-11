# Setup a cluster

## Titan cluster @ Cassandra

### Overview
The architecture of the setup can be found [in the Titan's Cassandra documentation](http://s3.thinkaurelius.com/docs/titan/current/cassandra.html) in section "15.3 Remote Server Mode with Rexster".
This seems to be the setup used in the [Titan Twitter stress test](http://thinkaurelius.github.io/titan/doc/titan-stress-poster.pdf).

### Installation
Titan can be downloaded together with storage and indexing backends and Rexster [from the Titan github page](https://github.com/thinkaurelius/titan/wiki/Downloads). The version in use is Titan Server `0.5.2` with Hadoop 2 and version `2.0.8` of Cassandra.

Unzip the archive file to a directory of your choice referred to as `$TITAN_SERVER_HOME`. Relative paths are always relative to this directory. The database will be stored in `db`.

#### Plugin
`$TITAN_SERVER_HOME/ext`: Graphity extension

### Code changes

### Configuration
#### Rexster
When using the Titan Server that bases upon Rexster, you have to configurate the Rexster server at first.

**conf/rexster-cassandra-cluster.xml** (derived from `conf/rexster-cassandra.xml`):

    <?xml version="1.0" encoding="UTF-8"?>
    <rexster>
      ...
      <graphs>
        <graph>
          <graph-name>graph</graph-name>
          <graph-type>com.thinkaurelius.titan.tinkerpop.rexster.TitanGraphConfiguration</graph-type>
          <graph-read-only>false</graph-read-only>
          <properties>
            <!-- Titan configuration -->
            <cluster.max-partitions>128</cluster.max-partitions>
            <storage.backend>cassandra</storage.backend>
            <storage.hostname>127.0.0.1</storage>
            <storage.cassandra.read-consistency-level>ONE</storage.cassandra.read-consistency-level>
            <storage.cassandra.write-consistency-level>QUORUM</storage.cassandra.write-consistency-level>
            <storage.cassandra.replication-factor>1</storage.cassandra.replication-factor>
            <storage.cassandra.keyspace>test1</storage.cassandra.keyspace>
          </properties>
          <extensions>
            <allows>
              <!-- allow Graphity extension -->
              <allow>*:*</allow>
            </allows>
          </extensions>
        </graph>
      </graphs>
    </rexster>

Consistency is provided:
* write consistency level `W = (N + 1) / 2` (QUORUM)
* read consistency level `R = 1` (ONE)
* replication factor `N = 1`

`W + R > N: (1 + 1) / 2 + 1 > 1` is true

Thoug a replication factor of `3` is recommended in production systems. Its value is fixed per keyspace.
Explicit partitioning is recommended when 10s billion of edges are expected, which we do not reach using the Wikipedia dump.

#### Cassandra
Storage backend specific configuration has to be done in its own configuration file.
Unfortunately Titan's `storage.conf-file` does not seem to work using Cassandra. The default Cassandra configuration `conf/cassandra.yaml` is loaded no matter what. I created two copies of this file in order to have an editable and a copy of the default configuration

    cd conf
    cp cassandra.yaml cassandra-default.yaml
    cp cassandra.yaml cassandra-cluster.yaml
    rm cassandra.yaml
    ln -s cassandra-cluster.yaml cassandra.yaml

and made a symbolic link in order to switch the config when needed.

**conf/cassandra-cluster.yaml** (derived from `conf/cassandra.yaml`):
##### Master

    num_tokens: 4
    seed_provider:
      - class_name: org.apache.cassandra.locator.SimpleSeedProvider
        parameters:
          - seeds: "neoslave1,10.93.130.108,10.93.130.109"
    concurrent_reads: 16
    concurrent_writes: 16
    listen_address: neoslave1
    rpc_address: neoslave1
    
##### Slave 1

    num_tokens: 4
    seed_provider:
      - class_name: org.apache.cassandra.locator.SimpleSeedProvider
        parameters:
          - seeds: "neoslave1,10.93.130.108,10.93.130.109"
    concurrent_reads: 16
    concurrent_writes: 16
    listen_address: 10.93.130.108
    rpc_address: 10.93.130.108

##### Slave 2

    num_tokens: 4
    seed_provider:
      - class_name: org.apache.cassandra.locator.SimpleSeedProvider
        parameters:
          - seeds: "neoslave1,10.93.130.108,10.93.130.109"
    concurrent_reads: 16
    concurrent_writes: 16
    listen_address: 10.93.130.109
    rpc_address: 10.93.130.109

In production `num_tokens` should be higher, e.g. `256`.
Concurrent reads is set to `16 * num_drives`, concurrent writes to `8 * num_cores`.

data: `db/cassandra/data`  
commit log: `db/cassandra/commitlog`

### Startup and Shutdown
Titan has a startup script `bin/titan.sh` to start Rexster, Titan, Cassandra and ElasticSearch.
Since I do not need ElasticSearch I made a copy of this script where ElasticSearch was removed.
The startup scripts can start Rexster using a specific configuration file `conf/rexster-<appendix>` via

    $ bin/titan.sh -c <appendix> start

where the default appendix is `cassandra-cluster` for my custom script `rexster-titan-cassandra.sh`.
The scripts can watch the status and also stop the components via

    $ bin/titan.sh status
    $ bin/titan.sh stop

### Cluster access

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

Only changes in the configuration files are noted.
The configuration files can be found at `$NEO_HOME/conf` with `$NEO_HOME` being `/var/lib/neo4j` when installed via package manager.

#### Master (neoslave1)
**conf/neo4j.properties**

    # unique cluster instance identifier
    ha.server_id=1
    # endpoint for cluster communication
    ha.cluster_server=neoslave1:5001
    # initial cluster nodes
    ha.initial_hosts=neoslave1:5001,10.93.130.108:5001,10.93.130.109:5001
    # endpoint for synchronization with master
    ha.server=neoslave1:6001

**conf/neo4j-server.properties**

    org.neo4j.server.database.mode=HA

#### Slave #1 (neoslave2)
**conf/neo4j.properties**

    # unique cluster instance identifier
    ha.server_id=2
    # endpoint for cluster communication
    ha.cluster_server=10.93.130.108:5001
    # initial cluster nodes
    ha.initial_hosts=neoslave1:5001,10.93.130.108:5001,10.93.130.109:5001
    # endpoint for synchronization with master
    ha.server=10.93.130.108:6001
    # node can not be elected to master node
    ha.slave_only=true
    
**conf/neo4j-server.properties**

    org.neo4j.server.database.mode=HA

#### Slave #2 (neoslave3)
**conf/neo4j.properties**

    # unique cluster instance identifier
    ha.server_id=3
    # endpoint for cluster communication
    ha.cluster_server=10.93.130.109:5001
    # initial cluster nodes
    ha.initial_hosts=neoslave1:5001,10.93.130.108:5001,10.93.130.109:5001
    # endpoint for synchronization with master
    ha.server=10.93.130.109:6001
    # node can not be elected to master node
    ha.slave_only=true
    
**conf/neo4j-server.properties**

    org.neo4j.server.database.mode=HA

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

### Cluster access
At the moment we have a cluster with size 3. In my setting only the master is accessible from outside the cluster.
In production you would want to have a single endpoint to use for requests that uses load balancing behind the scenes.
Another aspect is that the REST endpoint should be accessible from outside the cluster but not the admin interface.
Thus the Neo4j team suggests to use an Apache server to proxy the cluster in the [documentation's security section](http://neo4j.com/docs/stable/security-server.html). This allows load balancing, fine control of accessible endpoints and e.g. the usage of HTACCESS.

We install an Apache2 server

    sudo apt-get install apache2
    
on the master node which will be accessible from anywhere and balance the load across the cluster.

#### Configuration
We create a copy of the default page configuration

    cd /etc/apache2/sites/available
    cp default neo4j

and insert a load balancer for the REST endpoints of the cluster instances

    <VirtualHost *:82>
      <Proxy balancer://neo4j>
      BalancerMember http://localhost:7474/db/data
      BalancerMember http://10.93.130.108:7474/db/data
      BalancerMember http://10.93.130.109:7474/db/data
      </Proxy>
      
      ProxyPass /neo4j balancer://neo4j
      ProxyPassReverse /neo4j balancer://neo4j
      
      ProxyPass /neo4j-write http://localhost:7474/db/data
      ProxyPassReverse /neo4j-write http://localhost:7474/db/data
      
      # logging configuration follows
    </VirtualHost>

at port 82.


**/etc/apache2/ports.conf**

    NameVirtualHost *:82
    Listen 82

While the first endpoint balances all requests across the cluster, the second endpoint does not include any nodes other than the master. The intention is to have a separate endpoint for write requests. The cluster may perform better when using only the master node for writes: Slaves are forced to synchronize with the master in order to execute a write request.
