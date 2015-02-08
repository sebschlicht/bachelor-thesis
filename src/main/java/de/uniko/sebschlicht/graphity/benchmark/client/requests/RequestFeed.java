package de.uniko.sebschlicht.graphity.benchmark.client.requests;

import org.apache.commons.lang3.NotImplementedException;

import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;

public class RequestFeed extends Request {

    private long id;

    private int resNumFeeds;

    public RequestFeed(
            long id) {
        super(RequestType.FEED);
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setResult(int numFeeds) {
        resNumFeeds = numFeeds;
    }

    public int getResult() {
        return resNumFeeds;
    }

    @Override
    public String[] toStringArray() {
        throw new NotImplementedException(
                "Read requests not intended for bootstrapping at the moment.");
    }
}
