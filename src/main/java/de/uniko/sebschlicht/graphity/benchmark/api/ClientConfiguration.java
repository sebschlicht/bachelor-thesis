package de.uniko.sebschlicht.graphity.benchmark.api;

public class ClientConfiguration {

    private long idStart;

    private long idEnd;

    private int lengthFeed;

    private int maxThroughput;

    private int numThreads;

    private RequestComposition requests;

    private TargetType targetType;

    private String targetEndpoint;

    /**
     * Creates a transferable benchmark client configuration.
     * 
     * @param idStart
     *            first user id handled
     * @param idEnd
     *            last user id handled
     * @param lengthFeed
     *            number of characters used in feeds created by request type
     *            FEED
     * @param maxThroughput
     *            maximum number of requests per second per client
     * @param numThreads
     *            number of threads per client
     * @param requestComposition
     *            request composition
     * @param targetType
     *            target cluster type
     * @param endpointNeo4j
     *            endpoint for Neo4j cluster
     * @param endpointTitan
     *            endpoint for Titan cluster
     */
    public ClientConfiguration(
            long idStart,
            long idEnd,
            int lengthFeed,
            int maxThroughput,
            int numThreads,
            RequestComposition requestComposition,
            TargetType targetType,
            String endpointNeo4j,
            String endpointTitan) {
        this.idStart = idStart;
        this.idEnd = idEnd;
        this.lengthFeed = lengthFeed;
        this.maxThroughput = maxThroughput;
        this.numThreads = numThreads;
        requests = requestComposition;
        this.targetType = targetType;
        targetEndpoint =
                (targetType == TargetType.NEO4J)
                        ? endpointNeo4j
                        : endpointTitan;
    }

    /**
     * empty constructor for deserialization
     */
    public ClientConfiguration() {
    }

    public long getIdEnd() {
        return idEnd;
    }

    public long getIdStart() {
        return idStart;
    }

    public int getFeedLength() {
        return lengthFeed;
    }

    /**
     * @return maximum number of requests per second per client
     */
    public int getMaxThroughput() {
        return maxThroughput;
    }

    /**
     * @return number of threads per client
     */
    public int getNumThreads() {
        return numThreads;
    }

    /**
     * @return request composition
     */
    public RequestComposition getRequestComposition() {
        return requests;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    /**
     * @return endpoint of the target cluster
     */
    public String getTargetEndpoint() {
        return targetEndpoint;
    }
}
