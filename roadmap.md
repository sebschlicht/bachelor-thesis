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
In the [Graphity evaluation](https://github.com/renepickhardt/graphity-evaluation/blob/master/src/de/metalcon/neo/evaluation/GraphityBuilder.java) each Graphity request, such as "follow" and "post", did happen in a single transaction.
Though the measurement did not cover the transaction handling: The watch did only measure the execution time of the Graphity algorithm, not the time to create or commit a transaction.
The current setup does not allow to fully exclude the transaction handling time. It would be possible to reduce the number of used transactions to simulate this effect.

## Scalability

Secondly I have to increase the cluster size in order to analyze the scalability of Neo4j and Titan.
Only if I know what is actually taking time in a Graphity request, I am able to add another unit of the proper kind of resource (thread, core, hard drive etc.).

Therefore I have to check
* API concurrency model (parallel, blocking)
 * change number of server threads if parallel
* internal graph representation

This will also help me to explain the differences between Neo4j and Titan in their ability to scale.
