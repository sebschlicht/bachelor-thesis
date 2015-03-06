package de.uniko.sebschlicht.graphity.benchmark.client.results;

public class BenchmarkResult {

    private long numEntries;

    private double avgDuration;

    public BenchmarkResult(
            long numEntries,
            double avgDuration) {
        this.numEntries = numEntries;
        this.avgDuration = avgDuration;
    }

    public long getNumEntries() {
        return numEntries;
    }

    public double getAvgDuration() {
        return avgDuration;
    }
}
