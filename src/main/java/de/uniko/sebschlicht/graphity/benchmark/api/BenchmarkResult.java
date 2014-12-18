package de.uniko.sebschlicht.graphity.benchmark.api;

public class BenchmarkResult {

    private long duration;

    private RequestComposition numRequests;

    private RequestComposition avgRequestLatencies;

    /**
     * Creates a transferable client benchmark result.
     * 
     * @param duration
     *            benchmark duration in milliseconds
     * @param numRequests
     *            number of executed requests (per request type)
     * @param avgRequestLatencies
     *            average request latency (per request type)
     */
    public BenchmarkResult(
            long duration,
            RequestComposition numRequests,
            RequestComposition avgRequestLatencies) {
        this.duration = duration;
        this.numRequests = numRequests;
        this.avgRequestLatencies = avgRequestLatencies;
    }

    /**
     * empty constructor for deserialization
     */
    public BenchmarkResult() {
    }

    /**
     * @return benchmark duration in milliseconds
     */
    public long getDuration() {
        return duration;
    }

    /**
     * @return number of executed requests (per request type)
     */
    public RequestComposition getNumRequests() {
        return numRequests;
    }

    /**
     * @return average request latency (per request type)
     */
    public RequestComposition getAvgRequestLatencies() {
        return avgRequestLatencies;
    }
}
