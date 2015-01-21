package de.uniko.sebschlicht.graphity.benchmark.api;

import java.util.List;

public class ClientConfiguration {

    private long idStart;

    private long idEnd;

    private int lengthFeed;

    private int maxThroughput;

    private int numThreads;

    private RequestComposition requests;

    private List<String> addresses;

    private TargetType targetType;

    private String targetBase;

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
     * @param addresses
     *            IP addresses forming the target cluster
     * @param targetType
     *            target cluster type
     * @param targetBase
     *            base path of the target application relative to the endpoints
     */
    public ClientConfiguration(
            long idStart,
            long idEnd,
            int lengthFeed,
            int maxThroughput,
            int numThreads,
            RequestComposition requestComposition,
            List<String> addresses,
            TargetType targetType,
            String targetBase) {
        this.idStart = idStart;
        this.idEnd = idEnd;
        this.lengthFeed = lengthFeed;
        this.maxThroughput = maxThroughput;
        this.numThreads = numThreads;
        requests = requestComposition;
        this.addresses = addresses;
        this.targetType = targetType;
        this.targetBase = targetBase;
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

    public List<String> getAddresses() {
        return addresses;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    /**
     * @return base path of the target application relative to the addresses
     */
    public String getTargetBase() {
        return targetBase;
    }
}
