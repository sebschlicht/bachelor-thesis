package de.uniko.sebschlicht.graphity.benchmark.client.benchmark;

import java.util.concurrent.Callable;

import de.uniko.sebschlicht.graphity.benchmark.api.BenchmarkResult;
import de.uniko.sebschlicht.graphity.benchmark.client.SingleClient;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.results.ResultManager;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFeed;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFollow;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestPost;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestUnfollow;

public class BenchmarkClientTask implements Callable<BenchmarkResult> {

    private boolean isStopped;

    private SingleClient client;

    private ResultManager man;

    private BenchmarkClient benchmarkClient;

    public BenchmarkClientTask(
            SingleClient client,
            ResultManager man,
            BenchmarkClient benchmarkClient) {
        this.client = client;
        this.man = man;
        this.benchmarkClient = benchmarkClient;
    }

    private boolean isStopped() {
        return isStopped;
    }

    public void stop() {
        isStopped = true;
    }

    @Override
    public BenchmarkResult call() {
        Request request;
        RequestFeed requestFeed;
        RequestFollow requestFollow;
        RequestPost requestPost;
        RequestUnfollow requestUnfollow;

        while (!isStopped()) {
            request = client.nextRequest();
            // TODO: start measurement
            long msStart = System.currentTimeMillis();

            switch (request.getType()) {
                case FEED:
                    requestFeed = (RequestFeed) request;
                    requestFeed.setResult(benchmarkClient
                            .retrieveNewsFeed(requestFeed.getId()));
                    break;

                case FOLLOW:
                    requestFollow = (RequestFollow) request;
                    requestFollow.setResult(benchmarkClient.subscribe(
                            requestFollow.getIdSubscriber(),
                            requestFollow.getIdFollowed()));
                    break;

                case POST:
                    requestPost = (RequestPost) request;
                    requestPost.setResult(benchmarkClient.postStatusUpdate(
                            requestPost.getId(), requestPost.getMessage()));
                    break;

                case UNFOLLOW:
                    requestUnfollow = (RequestUnfollow) request;
                    requestUnfollow.setResult(benchmarkClient.unsubscribe(
                            requestUnfollow.getIdSubscriber(),
                            requestUnfollow.getIdFollowed()));
                    break;
            }
            long msCrr = System.currentTimeMillis();
            long duration = msCrr - msStart;
            man.addResult(request, duration);
        }

        return null;
    }
}
