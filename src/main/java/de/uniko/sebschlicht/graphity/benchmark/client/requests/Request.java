package de.uniko.sebschlicht.graphity.benchmark.client.requests;

import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;

public abstract class Request {

    protected RequestType type;

    public Request(
            RequestType type) {
        this.type = type;
    }

    public RequestType getType() {
        return type;
    }
}
