package de.uniko.sebschlicht.graphity.benchmark.client.config;

import java.util.List;

/**
 * Configuration for the Graphity benchmark client.
 * 
 * @author sebschlicht
 * 
 */
public class Configuration {

    private int lengthFeed;

    private int maxThroughput;

    private int numThreads;

    private RequestComposition requests;

    private List<String> addresses;

    private TargetType targetType;

    private String targetBase;

    /**
     * Creates a benchmark client configuration.
     * 
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
    public Configuration(
            int lengthFeed,
            int maxThroughput,
            int numThreads,
            RequestComposition requestComposition,
            List<String> addresses,
            TargetType targetType,
            String targetBase) {
        this.lengthFeed = lengthFeed;
        this.maxThroughput = maxThroughput;
        this.numThreads = numThreads;
        requests = requestComposition;
        this.addresses = addresses;
        this.targetType = targetType;
        this.targetBase = targetBase;
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
