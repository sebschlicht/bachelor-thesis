package de.uniko.sebschlicht.graphity.benchmark.client.requests;

import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;

public class RequestPost extends Request {

    private long id;

    private String message;

    private boolean resSuccess;

    public RequestPost(
            long id,
            String message) {
        super(RequestType.POST);
        this.id = id;
        this.message = message;
    }

    public long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public void setResult(boolean success) {
        resSuccess = success;
    }

    public boolean getResult() {
        return resSuccess;
    }
}
