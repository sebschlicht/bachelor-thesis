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

#### API concurrency model
Multiple threads access exactly 2 objects of the plugin in Neo4j.
The number of threads does not increase with every request, so threads seem to be reused.
The total number of threads does increase when multiple request are fired concurrently or with a very short delay (RTT).

questions: (Neo4j)
* is the first object really used or is it just for initialization purpose?  
  : The first object is just for initialization purpose and gets destroyed during the startup phase.
  
* can multiple threads write concurrently when they affect different parts of the graph?

* is there a way to affect the thread pool size?

* does Neo4j keep all threads alive? (just being curious)  
  : No it doesn't, it uses up to three threads.
