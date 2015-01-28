package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response;

import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;

import com.ning.http.client.Response;

import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFeed;
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
        String sResponse = response.getResponseBody();
        if (sResponse.length() > 2) {// remove "s in Neo4j response and escaping of "
            sResponse =
                    sResponse.substring(1, sResponse.length() - 1).replace(
                            "\\", "");
        }
        try {
            JSONArray activities = (JSONArray) jsonParser.parse(sResponse);
            ((RequestFeed) _request).setResult(activities.size());
        } catch (ParseException e) {
            System.err.println(sResponse);
            _request.setError(new IllegalStateException(e));
        }
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
