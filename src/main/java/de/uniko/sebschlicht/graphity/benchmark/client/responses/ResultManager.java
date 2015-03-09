package de.uniko.sebschlicht.graphity.benchmark.client.responses;

import de.uniko.sebschlicht.graphity.benchmark.client.AsyncClient;
import de.uniko.sebschlicht.graphity.benchmark.client.bootstrap.BootstrapManager;
import de.uniko.sebschlicht.socialnet.requests.Request;
import de.uniko.sebschlicht.socialnet.requests.RequestFeed;
import de.uniko.sebschlicht.socialnet.requests.RequestFollow;
import de.uniko.sebschlicht.socialnet.requests.RequestPost;
import de.uniko.sebschlicht.socialnet.requests.RequestUnfollow;

public class ResultManager {

    public void addResult(Request request, long duration) {
        // timestamp is in [0]
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(request.getType().getId());
        logMessage.append("\t");
        logMessage.append(duration / 1000000);// nanoTime -> ms
        if (request.hasFailed()) {
            logMessage.append("\t");
            logMessage.append(-1);
            logMessage.append("\t");
            logMessage.append(request.getError().getMessage());
            AsyncClient.LOG.info(logMessage);
            return;
        }
        switch (request.getType()) {
            case FEED:
                RequestFeed rfe = (RequestFeed) request;
                int expectedFeedSize =
                        Math.min(BootstrapManager.getFeedSize(rfe.getId()), 15);
                logMessage.append("\t");
                logMessage.append(rfe.getId());
                logMessage.append("\t");
                logMessage.append(rfe.getResult());
                if (rfe.getResult() != expectedFeedSize) {
                    String errorMessage =
                            "unexpected feed size, expected "
                                    + expectedFeedSize;
                    logMessage.append("\n");
                    logMessage.append(errorMessage);
                    AsyncClient.LOG.info(logMessage);
                    throw new IllegalStateException(errorMessage);
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
