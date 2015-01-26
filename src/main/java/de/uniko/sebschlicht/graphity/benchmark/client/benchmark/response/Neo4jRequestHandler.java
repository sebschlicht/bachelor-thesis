package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response;

import java.io.IOException;

import org.apache.commons.lang3.NotImplementedException;

import com.ning.http.client.Response;

import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFollow;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestPost;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestUnfollow;

public class Neo4jRequestHandler extends AsyncRequestHandler {

    public Neo4jRequestHandler(
            AsyncBenchmarkClientTask client,
            int identifier) {
        super(client, identifier);
    }

    @Override
    protected void handleFeedResponse(Response response) throws IOException {
        // TODO Auto-generated method stub
        throw new NotImplementedException(
                "handling of Neo4j FEED responses is not implemented");
    }

    @Override
    protected void handleFollowResponse(Response response) throws IOException {
        String sResponse = response.getResponseBody();
        try {
            boolean result = parseBoolean(sResponse);
            ((RequestFollow) _request).setResult(result);
        } catch (IllegalArgumentException e) {
            _request.setError(new IllegalStateException(e));
        }
    }

    @Override
    protected void handlePostResponse(Response response) throws IOException {
        String sResponse = response.getResponseBody();
        try {
            long postId = Long.valueOf(sResponse);
            ((RequestPost) _request).setResult(postId != 0L);
        } catch (NumberFormatException e) {
            _request.setError(new IllegalStateException(e));
        }
    }

    @Override
    protected void handleUnfollowResponse(Response response) throws IOException {
        String sResponse = response.getResponseBody();
        try {
            boolean result = parseBoolean(sResponse);
            ((RequestUnfollow) _request).setResult(result);
        } catch (IllegalArgumentException e) {
            _request.setError(new IllegalStateException(e));
        }
    }

    protected static boolean parseBoolean(String sBoolean) {
        if ("true".equalsIgnoreCase(sBoolean)) {
            return true;
        } else if ("false".equalsIgnoreCase(sBoolean)) {
            return false;
        }
        throw new IllegalArgumentException(
                "String does not represent a boolean value: \"" + sBoolean
                        + "\"");
    }
}
