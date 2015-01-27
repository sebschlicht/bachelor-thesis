package de.uniko.sebschlicht.graphity.benchmark.client.requests;

import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;

public abstract class Request {

    protected RequestType _type;

    protected String _address;

    protected Throwable _throwable;

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

    public void setError(Throwable throwable) {
        _throwable = throwable;
    }

    public boolean hasFailed() {
        return (_throwable != null);
    }

    public Throwable getError() {
        return _throwable;
    }

    abstract public String[] toStringArray();
}
