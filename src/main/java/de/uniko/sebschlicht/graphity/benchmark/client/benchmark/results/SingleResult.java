package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.results;

import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;

public class SingleResult {

    private RequestType type;

    private long duration;

    public SingleResult(
            RequestType type,
            long duration) {
        this.type = type;
        this.duration = duration;
    }

    public RequestType getType() {
        return type;
    }

    public long getDuration() {
        return duration;
    }
}
