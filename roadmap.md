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

**Resources**:

| Neo4j | Titan |
| ----- | ----- |
|<ul><li>[HA explanation](http://docs.neo4j.org/chunked/stable/ha-how.html)</li><li>[HA configuration](http://docs.neo4j.org/chunked/stable/ha-configuration.html)</li></ul> | |

### Graph distribution
The distribution of the graph can affect the cluster performance:  
If the graph is splitted there may be additional requests necessary to delegate a request to the correct node.
These requests have to be considered if we examine the benchmark results.
Depending on the cluster management we may not be able to affect this behaviour.

**Questions**:
* Is the graph splitted up?

| Neo4j | Titan |
| ----- | ----- |
| No. Each node holds a copy of the whole database, thus it **can not grow beyond the maximum database size of a single instance**: 34 billon nodes, 34 billion relationships, 68 billion properties in total. | |
* When is request delegation necessary?

| Neo4j | Titan |
| ----- | ----- |
| Requests of same users are routed to the same nodes (cache-based sharding). Additional request delegation does not seem to be necessary. | |
* What graph elements are cached?

| Neo4j | Titan |
| ----- | ----- |
| Each instance caches whatever fits into memory. Since the cluster performs cache-based sharding, a node is mainly targeted of requests where the same graph elements are involved. Thus some elements may be cached in multiple nodes. | |

#### Consistency model
There will be additional effort for the cluster to keep nodes up-to-date, if the data changes.
If we understand when and how the data is kept (eventually-)consistent, we might be able to keep this effort as low as possible.

**Questions**:
* Which nodes are updated by a commit?

  | request target | Neo4j | Titan |
  | -------------- | ----- | ----- |
  | master | The commit updates the master only. | |
  | slave | The commits updates the slave and the master (see above). This request requires the slave to be up-to-date. | |
* What does a commit actually do?

  | request target | Neo4j | Titan |
  | -------------- | ----- | ----- |
  | master | The commit does pretty much the same if we would run a single Neo4j instance. The master generates a transaction identifier `txid` used in synchronization processes. In fact we can configure the master to push committed transactions to any number of slave nodes, which will trigger network request(s). | |
  | slave | The commit forces the slave to be up-to-date. This may trigger a network request, to get the transaction stream. It is not clear if this covers the whole graph or only the graph elements involved. Once the slave is up-to-date it will send the transaction data to the master. The affected graph elements (if existing yet) will get locked, both in master and slave. The master will then act as stated above. If the commit was successful, it will request the target slave to commit, too. | |
* How keeps the cluster its nodes up-to-date, then?

  | Neo4j | Titan |
  | ----- | ----- |
  | Slave nodes can pull a stream of committed transactions. They do this in a fixed interval (ha.pull_interval) but also if mandated, due to a write request. In addition the master can be configured to push committed transactions to any number of slaves (ha.tx_push_factor).  | |
* Can we make the data consitent ourselves? How can we reach that?

  | Neo4j | Titan |
  | ----- | ----- |
  | Yes, we could configure the master to push committed transactions to as many slave nodes as the cluster contains. | |
  
**Note (Neo4j)**:
We can assume the write performance to drop down if we increase the cluster size and allow write requests to target any node, due to the more frequent synchronization of slave nodes.

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
