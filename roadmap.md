# Roadmap

## Performance differences

The performance of the current setup is at least 50 times lower than in the Graphity evaluation:

| Benchmark | Write requests per second |
| --------- | ------------------------- |
| evaluation | &ge; 2.500 write requests/s |
| current setup | 50 write requests/s |

Therefore I need to have a closer took at Titan and Neo4j and the setups I put the code in, to understand what I do measure and to make sure that this is what I actually want to measure.

### Reasons expected to cause the differences (ordered by expected significance)
1. differences in transaction handling (single transaction vs. bulking)
2. IO time
3. HTTP latency

I have to profile the server to find out what causes the major differences.
This will enable me to either adapt the setup or explain the differences.

#### differences in transaction handling
In the [Graphity evaluation](https://github.com/renepickhardt/graphity-evaluation/blob/master/src/de/metalcon/neo/evaluation/GraphityBuilder.java) (SGT.run @ line 423) each Graphity action, such as `follow` and `post`, did happen in a single transaction.  
Though the measurement did not cover the transaction handling: The watch did only measure the execution time of the Graphity algorithm, not the time to create or commit a transaction.

The current setup does not allow to fully exclude the transaction handling time.  
It would be possible to reduce the number of used transactions to simulate this effect:  

| Benchmark | Write requests per second |
| --------- | ------------------------- |
| current setup (1 action / transaction) | 50 write requests/s |
| current setup @ 100 actions / transaction | ? write requests/s |
| current setup @ 1000 actions / transaction | ? write requests/s |

This only seems to be valid if a single master handles all write requests in Neo4j. In case the cluster propagation is commit-triggered I would have to choose the queue size wisely or use an additional timer.
The data in Titan is eventually consistent due to Cassandra, so there is no problem with such a transaction queue at first sight. I have to understand the concurrency handling first to make valid statements.

## Scalability
Secondly I have to increase the cluster size in order to analyze the scalability of Neo4j and Titan.
Only if I know what is actually taking time in a Graphity request, I can identify the limiting resource (thread, core, hard drive etc.) and can add another unit of it.

Therefore I have to take a closer look at the technologies concerning:
* API concurrency model (parallel, blocking)
 * change number of server threads if parallel
* internal graph representation

This will also help me to explain the differences between Neo4j and Titan in their ability to scale.

### API concurrency model
The number of clients a node can handle depends on the concurrency model used in the database API.
If a node can host multiple threads accessing the database, there might be performance gains by increasing the number of CPU cores:
The API may allow different threads to access/write to different parts of the graph and operate concurrently. If not, there can still be performance gains by handling the HTTP requests in parallel.

**Questions**:
* Does a node host multiple threads?

  | Neo4j | Titan |
  | ----- | ----- |
  | Yes it seems that a node hosts up to 3 threads to serve clients.  The number of threads increases when requests come in simultaneusly/with very short delay (RTT). Threads are reused but recycled from time to time. | |
* Do these threads access the database theirselves or is this delegated to a single thread?

  | Neo4j | Titan |
  | ----- | ----- |
  | The threads access the database theirselves. | |
* If yes: Can different threads access/write to different graph areas?

  | Neo4j | Titan |
  | ----- | ----- |
  | Yes they can access different graph areas, but it is not yet clear whether they can access it simulatenously or if they are synchronized internally. | |
* Is the plugin object a singleton? (helps to improve implementation)

  | Neo4j | Titan |
  | ----- | ----- |
  | Yes, but another instance is created (constructor called) and destroyed (`finalize` called) during startup. | |
* What HTTP server is used? (-> configuration) / Can we increase the maximum thread pool size?

  | Neo4j | Titan |
  | ----- | ----- |
  | | |

#### Neo4j HA behaviour
Resources:
* [HA explanation](http://docs.neo4j.org/chunked/stable/ha-how.html)
* [HA configuration](http://docs.neo4j.org/chunked/stable/ha-configuration.html)

##### commits
Does a commit apply the transaction to the whole cluster and trigger a network request?

In short: It will not apply the transaction to the whole cluster but maybe to some slaves and can trigger multiple network requests if the write request targets a slave.

The long version:  
If a write request targets the master node, the commit will affect the master only. There is a configuration option (ha.tx_push_factor) to set the replication factor. Though the slaves will request a stream of occurred transactions in a fixed interval (ha.pull_interval) or if mandated (see below) to keep their data up-to-date.  
If a write request targets a slave node, the commit

1. forces the slave to synchronize the affected nodes with the master if behind master's branch
2. lock the affected graph elements at master and slave
3. apply to master at first
4. apply to slave if successful

Thus we can assume the write performance to drop down if we increase the cluster size and allow write requests to target any node, due to more frequent synchronization of the slave nodes.
