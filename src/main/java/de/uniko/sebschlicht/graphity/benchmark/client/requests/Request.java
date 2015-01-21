package de.uniko.sebschlicht.graphity.benchmark.client.requests;

import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;

public abstract class Request {

    protected RequestType _type;

    protected String _address;

    protected boolean _hasFailed;

    public Request(
            RequestType type) {
        _type = type;
    }

    public RequestType getType() {
        return _type;
    }

    public void setAddress(String address) {
        _address = address;
    }

    public String getAddress() {
        return _address;
    }

    public void setError(boolean isError) {
        _hasFailed = isError;
    }

    public boolean hasFailed() {
        return _hasFailed;
    }
}
