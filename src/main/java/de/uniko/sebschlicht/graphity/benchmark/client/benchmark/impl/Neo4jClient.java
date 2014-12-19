package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.impl;

import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AbstractBenchmarkClient;

public class Neo4jClient extends AbstractBenchmarkClient {

    @Override
    public boolean retrieveNewsFeed(long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean subscribe(long idSubscriber, long idFollowed) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean postStatusUpdate(long id, String message) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean unsubscribe(long idSubscriber, long idFollowed) {
        // TODO Auto-generated method stub
        return false;
    }
}
