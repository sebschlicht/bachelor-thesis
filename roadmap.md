# Roadmap

## Performance differences

The performance of the current setup is at least 50 times lower than in the Graphity evaluation:
**evaluation performance**: at least 2.500 write requests / s
**actual performance**: 50 write requests / s

Therefore I need to have a closer took at Titan and Neo4j and the setups I put the code in, to understand what I do measure and to make sure that this is what I actually want to measure.

### Reasons expected to cause the differences (ordered by expected significance)
# differences in transaction handling (single transaction vs. bulking)
# IO time
# HTTP latency

I have to profile the server to find out what causes the major differences.
This will enable me to either adapt the setup or explain the differences.

## Scalability

Secondly I have to increase the cluster size in order to analyze the scalability of Neo4j and Titan.
Only if I know what is actually taking time in a Graphity request, I am able to add another unit of the proper kind of resource (thread, core, hard drive etc.).

Therefore I have to check
* API concurrency model (parallel, blocking)
 * change number of server threads if parallel
* internal graph representation

This will also help me to explain the differences between Neo4j and Titan in their ability to scale.
