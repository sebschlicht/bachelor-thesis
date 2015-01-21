package de.uniko.sebschlicht.graphity.benchmark.client.requests;

import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;

public abstract class Request {

    protected RequestType _type;

    protected boolean _hasFailed;

    public Request(
            RequestType type) {
        _type = type;
    }

    public RequestType getType() {
        return _type;
    }

    public void setError(boolean isError) {
        _hasFailed = isError;
    }

    public boolean hasFailed() {
        return _hasFailed;
    }
}
