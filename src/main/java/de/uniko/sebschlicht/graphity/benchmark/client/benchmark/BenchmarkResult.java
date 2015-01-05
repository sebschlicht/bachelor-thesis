package de.uniko.sebschlicht.graphity.benchmark.client.benchmark;

public class BenchmarkResult {

    private int i;

    private long[] buffer;

    public BenchmarkResult() {
        clear();
    }

    public long[] getResults() {
        return buffer;
    }

    public void clear() {
        buffer = new long[100];
        i = 0;
    }

    public boolean addResult(long duration) {
        if (i < buffer.length) {
            buffer[i] = duration;
            i += 1;
            return true;
        }
        return false;
    }
}
