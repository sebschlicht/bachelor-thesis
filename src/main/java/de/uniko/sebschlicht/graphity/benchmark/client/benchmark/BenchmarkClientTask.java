package de.uniko.sebschlicht.graphity.benchmark.client.benchmark;

import java.util.concurrent.Callable;

import de.uniko.sebschlicht.graphity.benchmark.api.BenchmarkResult;
import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;
import de.uniko.sebschlicht.graphity.benchmark.client.SingleClient;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFeed;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFollow;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestPost;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestUnfollow;

public class BenchmarkClientTask implements Callable<BenchmarkResult> {

    private boolean isStopped;

    private SingleClient client;

    private BenchmarkClient benchmarkClient;

    public BenchmarkClientTask(
            SingleClient client,
            BenchmarkClient benchmarkClient) {
        this.client = client;
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

        long msStarted = System.currentTimeMillis();
        long msLastCheckpoint = 0;
        long[] numRequests = new long[4];

        while (!isStopped()) {
            request = client.nextRequest();
            // TODO: start measurement
            long msStart = System.currentTimeMillis();

            switch (request.getType()) {
                case FEED:
                    requestFeed = (RequestFeed) request;
                    benchmarkClient.retrieveNewsFeed(requestFeed.getId());
                    break;

                case FOLLOW:
                    requestFollow = (RequestFollow) request;
                    benchmarkClient.subscribe(requestFollow.getIdSubscriber(),
                            requestFollow.getIdFollowed());
                    break;

                case POST:
                    requestPost = (RequestPost) request;
                    benchmarkClient.postStatusUpdate(requestPost.getId(),
                            requestPost.getMessage());
                    break;

                case UNFOLLOW:
                    requestUnfollow = (RequestUnfollow) request;
                    benchmarkClient.unsubscribe(
                            requestUnfollow.getIdSubscriber(),
                            requestUnfollow.getIdFollowed());
                    break;
            }
            long msCrr = System.currentTimeMillis();
            long duration = msCrr - msStart;
            numRequests[request.getType().getId()] += 1;
            //            SingleClient.LOG.debug(request.getType() + ": " + duration);

            if (msCrr > msLastCheckpoint + 10000) {
                SingleClient.LOG.debug("progress after " + (msCrr - msStarted)
                        + "ms");
                for (int i = 0; i < 4; ++i) {
                    SingleClient.LOG.debug(RequestType.getTypeById(i) + ": "
                            + numRequests[i]);
                }
                msLastCheckpoint = msCrr;
            }

            // TODO: stop and save measurement
        }

        // TODO: merge measurements if still necessary
        return null;
    }
}
