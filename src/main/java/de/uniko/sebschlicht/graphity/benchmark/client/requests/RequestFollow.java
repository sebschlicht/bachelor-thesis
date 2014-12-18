package de.uniko.sebschlicht.graphity.benchmark.client.requests;

import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;

public class RequestFollow extends Request {

    private long idSubscriber;

    private long idFollowed;

    public RequestFollow(
            long idSubscriber,
            long idFollowed) {
        super(RequestType.FOLLOW);
        this.idSubscriber = idSubscriber;
        this.idFollowed = idFollowed;
    }

    public long getIdSubscriber() {
        return idSubscriber;
    }

    public long getIdFollowed() {
        return idFollowed;
    }
}
