package de.uniko.sebschlicht.graphity.benchmark.api;

public class ClientConfiguration {

    private long idStart;

    private long idEnd;

    private int lengthFeed;

    private int maxThroughput;

    private int numThreads;

    private RequestComposition requests;

    private String targetAddress;

    private TargetType targetType;

    /**
     * Creates a transferable benchmark client configuration.
     * 
     * @param maxThroughput
     *            maximum number of requests per second per client
     * @param numThreads
     *            number of threads per client
     * @param requestComposition
     *            request composition
     * @param targetAddress
     *            IP address of the target cluster
     */
    public ClientConfiguration(
            long idStart,
            long idEnd,
            int lengthFeed,
            int maxThroughput,
            int numThreads,
            RequestComposition requestComposition,
            String targetAddress,
            TargetType targetType) {
        this.idStart = idStart;
        this.idEnd = idEnd;
        this.lengthFeed = lengthFeed;
        this.maxThroughput = maxThroughput;
        this.numThreads = numThreads;
        requests = requestComposition;
        this.targetAddress = targetAddress;
        this.targetType = targetType;
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

    /**
     * @return IP address of the target cluster
     */
    public String getTargetAddress() {
        return targetAddress;
    }

    public TargetType getTargetType() {
        return targetType;
    }
}
