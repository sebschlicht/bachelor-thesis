# Setup a Titan cluster

## Overview
The architecture of the setup can be found [in the Titan's Cassandra documentation](http://s3.thinkaurelius.com/docs/titan/current/cassandra.html) in section "15.3 Remote Server Mode with Rexster".
This seems to be the setup used in the [Titan Twitter stress test](http://thinkaurelius.github.io/titan/doc/titan-stress-poster.pdf).

## Installation
Titan can be downloaded together with storage and indexing backends and Rexster [from the Titan github page](https://github.com/thinkaurelius/titan/wiki/Downloads). The version in use is Titan Server `0.5.2` with Hadoop 2 and version `2.0.8` of Cassandra.

Unzip the archive file to a directory of your choice referred to as `$TITAN_SERVER_HOME`. Relative paths are always relative to this directory. The database will be stored in `db`.

### Plugin
To deploy a plugin to Rexster you have to put it in the Rexster extension directory `$TITAN_SERVER_HOME/ext`.
Make sure to use a uber-JAR that provides all dependencies your application needs, if any.
Secondly you have to allow the extension in the Rexster configuration. The configuration below allows all extensions by default.

## Code changes
The Graphity code was rewritten using the Blueprints API in order to work on Titan. However, some Titan functions exceed the Blueprints API and in these cases casting `Graph` to `TitanGraph` was necessary.

## Configuration
This configuration is a result of the [single DC intialization documentation](http://www.datastax.com/documentation/cassandra/2.1/cassandra/initialize/initializeSingleDS.html).

### Rexster
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
              <!-- allow all extensions -->
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

### Cassandra
Storage backend specific configuration has to be done in its own configuration file.
Unfortunately Titan's `storage.conf-file` does not seem to work using Cassandra. The default Cassandra configuration `conf/cassandra.yaml` is loaded no matter what. I created two copies of this file in order to have an editable and a copy of the default configuration

    $ cd conf
    $ cp cassandra.yaml cassandra-default.yaml
    $cp cassandra.yaml cassandra-cluster.yaml
    $ rm cassandra.yaml
    $ ln -s cassandra-cluster.yaml cassandra.yaml

and made a symbolic link in order to switch the config when needed.

**conf/cassandra-cluster.yaml** (derived from `conf/cassandra.yaml`):

    num_tokens: 256
    seed_provider:
      - class_name: org.apache.cassandra.locator.SimpleSeedProvider
        parameters:
          - seeds: "$seeds"
    concurrent_reads: 32
    concurrent_writes: 32
    listen_address: $address
    rpc_address: $address
    endpoint_snitch: GossipingPropertyFileSnitch

[GossipingPropertyFileSnitch](http://www.datastax.com/documentation/cassandra/2.1/cassandra/architecture/architectureSnitchGossipPF_c.html) uses a separate file to identify the node area:

**conf/cassandra-rackdc.properties**:

    dc=DC1
    rack=RAC1

Concurrent reads is set to `16 * num_drives`, concurrent writes to `8 * num_cores`. `num_drives` is guessed to be `2` when using virtualization, I may have to experiment with its value.

## Startup and Shutdown
Titan has a startup script `bin/titan.sh` to start Rexster, Titan, Cassandra and ElasticSearch.
Since I do not need ElasticSearch I made a copy of this script where ElasticSearch was removed.
The startup scripts can start Rexster using a specific configuration file `conf/rexster-<appendix>` via

    $ bin/titan.sh -c <appendix> start

where the default appendix is `cassandra-cluster` for my custom script `rexster-titan-cassandra.sh`.
The scripts can watch the status and also stop the components via

    $ bin/titan.sh stop

To avoid manual logins to each cluster node I derived a [Titan script](src/main/resources/titan-circus.sh) that [can be used with Circus](../../wiki/Control-Titan-via-process-management-tool-Circus), a process management tool that allows remote control.
