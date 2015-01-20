package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.results;

import java.util.concurrent.LinkedBlockingDeque;

import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;
import de.uniko.sebschlicht.graphity.benchmark.client.SingleClient;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFeed;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFollow;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestPost;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestUnfollow;

public class ResultManager implements Runnable {

    private static final long INTERVAL_UPDATE = 10000;

    protected boolean isRunning;

    protected Thread thread;

    private LinkedBlockingDeque<SingleResult> pendingResults;

    private ResultContainer[] results;

    public ResultManager() {
        pendingResults = new LinkedBlockingDeque<SingleResult>();
        results = new ResultContainer[4];
        for (int i = 0; i < 4; ++i) {
            results[i] = new ResultContainer();
        }
    }

    public void addResult(Request request, long duration) {
        pendingResults.add(new SingleResult(request.getType(), duration));

        //TODO: save request object including results
        // timestamp is in [0]
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(request.getType().getId());
        logMessage.append("\t");
        logMessage.append(duration);
        switch (request.getType()) {
            case FEED:
                RequestFeed rfe = (RequestFeed) request;
                logMessage.append("\t");
                logMessage.append(rfe.getId());
                logMessage.append("\t");
                logMessage.append(rfe.getResult());
                break;

            case FOLLOW:
                RequestFollow rf = (RequestFollow) request;
                logMessage.append("\t");
                logMessage.append(rf.getIdSubscriber());
                logMessage.append("\t");
                logMessage.append(rf.getIdFollowed());
                break;

            case POST:
                RequestPost rp = (RequestPost) request;
                logMessage.append("\t");
                logMessage.append(rp.getId());
                break;

            case UNFOLLOW:
                RequestUnfollow ru = (RequestUnfollow) request;
                logMessage.append("\t");
                logMessage.append(ru.getIdSubscriber());
                logMessage.append("\t");
                logMessage.append(ru.getIdFollowed());
                break;
        }
        SingleClient.LOG.info(logMessage);
    }

    public BenchmarkResult getResults() {
        //TODO
        return null;
    }

    @Override
    public void run() {
        long tsStart = System.currentTimeMillis();
        long tsLastUpdate = 0;
        SingleResult result;
        StringBuilder logMessage;

        isRunning = true;
        while (isRunning) {
            result = pendingResults.pollFirst();
            if (result != null) {
                ResultContainer container = results[result.getType().getId()];
                if (!container.addResult(result.getDuration())) {
                    throw new IllegalStateException(
                            "too many benchmark results stacked");
                }
            } else {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (System.currentTimeMillis() >= tsLastUpdate + INTERVAL_UPDATE) {
                logMessage = new StringBuilder();
                // time
                logMessage.append(System.currentTimeMillis() - tsStart);
                for (ResultContainer container : results) {
                    // number of requests
                    logMessage.append("\t" + container.getNumEntries());
                    // duration
                }
                SingleClient.LOG.info(logMessage);
                tsLastUpdate = System.currentTimeMillis();
            }
        }
        for (int i = 0; i < 4; ++i) {
            RequestType type = RequestType.getTypeById(i);
            SingleClient.LOG.info(type + ": " + results[i].getNumEntries()
                    + " requests");
        }
    }

    public void start() {
        //thread = new Thread(this);
        //thread.start();
    }

    public void stop() {
        //isRunning = false;
        //try {
        //    thread.join();
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //}
        //thread = null;
    }
}
