package de.uniko.sebschlicht.graphity.benchmark.client.requests;

import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;

public class RequestFeed extends Request {

    private long id;

    public RequestFeed(
            long id) {
        super(RequestType.FEED);
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
