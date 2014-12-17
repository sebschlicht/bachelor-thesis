package de.uniko.sebschlicht.graphity.benchmark.api;

public class ClientConfiguration {

    private int maxThroughput;

    private int numThreads;

    private RequestComposition requests;

    private String targetAddress;

    public int getMaxThroughput() {
        return maxThroughput;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public RequestComposition getRequests() {
        return requests;
    }

    public String getTargetAddress() {
        return targetAddress;
    }
}
