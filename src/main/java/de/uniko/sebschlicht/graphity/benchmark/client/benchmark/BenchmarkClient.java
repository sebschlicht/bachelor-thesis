package de.uniko.sebschlicht.graphity.benchmark.client.benchmark;

public interface BenchmarkClient {

    int retrieveNewsFeed(long id);

    boolean subscribe(long idSubscriber, long idFollowed);

    boolean postStatusUpdate(long id, String message);

    boolean unsubscribe(long idSubscriber, long idFollowed);
}
