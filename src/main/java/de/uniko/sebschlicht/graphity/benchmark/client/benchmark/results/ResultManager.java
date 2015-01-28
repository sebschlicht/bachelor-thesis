package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.results;

import de.uniko.sebschlicht.graphity.benchmark.client.AsyncClient;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.bootstrap.BootstrapManager;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFeed;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFollow;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestPost;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestUnfollow;

public class ResultManager {

    public void addResult(Request request, long duration) {
        //TODO: save request object including results
        // timestamp is in [0]
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(request.getType().getId());
        logMessage.append("\t");
        if (!request.hasFailed()) {
            logMessage.append(duration);
        } else {
            logMessage.append(-1);
            logMessage.append("\t");
            logMessage.append(request.getError().getMessage());
        }
        switch (request.getType()) {
            case FEED:
                RequestFeed rfe = (RequestFeed) request;
                if (rfe.getResult() == BootstrapManager
                        .getFeedSize(rfe.getId())) {
                    logMessage.append("\t");
                    logMessage.append(rfe.getId());
                    logMessage.append("\t");
                    logMessage.append(rfe.getResult());
                } else {
                    System.err.println("unexpected feed size");
                    throw new IllegalStateException("unexpected feed size");
                }
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
        AsyncClient.LOG.info(logMessage);
    }
}
