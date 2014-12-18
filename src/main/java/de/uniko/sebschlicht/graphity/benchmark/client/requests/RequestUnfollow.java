package de.uniko.sebschlicht.graphity.benchmark.client.requests;

import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;

public class RequestUnfollow extends Request {

    private long idSubscriber;

    private long idFollowed;

    public RequestUnfollow(
            long idSubscriber,
            long idFollowed) {
        super(RequestType.UNFOLLOW);
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
