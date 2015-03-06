package de.uniko.sebschlicht.graphity.benchmark.client.results;

public class ResultContainer {

    private long numEntries;

    private long sumDuration;

    public ResultContainer() {
        numEntries = 0;
        sumDuration = 0;
    }

    public long getNumEntries() {
        return numEntries;
    }

    public long getSumDuration() {
        return sumDuration;
    }

    /**
     * Add a result to the container.
     * 
     * @param duration
     *            operation duration in milliseconds
     * @return true - if the result was added successfully<br>
     *         false - if the container is full
     */
    public boolean addResult(long duration) {
        if (duration >= Long.MAX_VALUE - sumDuration) {
            return false;
        }
        sumDuration += duration;
        numEntries += 1;
        return true;
    }
}
