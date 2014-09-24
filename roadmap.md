# Roadmap

## Performance differences

The performance of the current setup, performing the short benchmark (10 seconds <=> 420-520 Graphity actions), is at least 50 times lower than in the Graphity evaluation:

| Benchmark | Write requests per second |
| --------- | ------------------------- |
| evaluation | &ge; 2.500 write requests/s |
| current setup | 50 write requests/s |

Therefore I need to have a closer took at Titan and Neo4j and the setups I put the code in, to understand what I do measure and to make sure that this is what I actually want to measure.

### Reasons expected to cause the differences (ordered by expected significance)
1. differences in transaction handling (single transaction vs. bulking)
2. IO time
3. HTTP latency
4. memory limit

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

#### IO time
The IO percentage according to `iotop` does not exceed ~33% during the short benchmark in the local VM.
This does not seem to be the bottleneck here.

#### HTTP latency
The benchmark can not be expected to exceed `1 / RTT` requests per second, which is why  
1000ms / 5ms req/s = 200 req/s  
should be the upper limit of performance.

| Description | req/s |
| ----------- | ----- |
| execute Graphity action (default) | 42-52 |
| do not execute Graphity action | 147,6 |
| do nothing | 162,6 |

This is a bit lower than what is expected to be the limit of the HTTP performance.
Tests were repeated with a single standalone HTTP client do ensure my ZMQ cluster does not harm the performance.

| Description | req/s |
| ----------- | ----- |
| execute Graphity action (default) | 60,5 |
| do not execute Graphity action | 296,6 |
| do nothing | 297,1 |

The ZMQ setup does clearly create an overhead which is up to twice as low as the standalone client.
But this might also be due to the missing concurrency, there is only one client firing requests rather than four in the ZMQ setup, which could cause trouble in the Neo4j server.  
**This setup will be used now to find this issue, since it is much easier to control.**  
300 requests per second seems fine if the server would run at a separate machine, but it seems quite slow for a local VM to me. I have in mind that we were at 10.000 req/s with a Tomcat server running locally.

#### CPU power
CPU: Intel(R) Core(TM) i5-4200U CPU @ 1.60GHz

The VM has currently one core and it is at 80-90% during the benchmark.
A second core reduced the utilization to 75% at maximum but did not increase the throughput.

#### Memory
Total Memory: 8GB

The VM has currently 512MB RAM and the used memory does not increase significantly during the benchmark (again: just firing the HTTP requests, no Neo4j execution on the server). Maybe the VM is not able to use more memory due to the single core and not able to be faster with more cores, due to low memory. Lets combine:
4GB RAM together with 3 cores did not increase the throughput rate.
The issue seems to be located somewhere else.

#### VM networking
The VirtualBox VM uses a host-only-adapter to make the VM accessible for the host.
Maybe this method of networking causes the low performance.
Therefore standard HTTP server instances were used together with a single standalone client, again, firing `POST` requests at the same way as in the tests before.

| Service | VM (req/s) | Local (req/s) | Ratio |
| ------- | ---------- | ------------- | ----- |
| Neo4j Server | ~300 | - | - |
| Apache2 | ~900 | ~3400 | 3.8 |
| Tomcat7 | ~500 | ~4000 | 8 |

What you can see is that the VM is indeed slower than the host, but there must be additional reasons:
Tomcat is faster on host, but slower on the VM, whereas it is the other way round with Apache.
This can not be just due to delays in VM networking.

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
