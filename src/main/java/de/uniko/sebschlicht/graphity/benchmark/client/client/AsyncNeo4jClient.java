package de.uniko.sebschlicht.graphity.benchmark.client.client;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

import de.uniko.sebschlicht.graphity.benchmark.client.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.config.Configuration;
import de.uniko.sebschlicht.graphity.benchmark.client.responses.AsyncRequestHandler;
import de.uniko.sebschlicht.graphity.benchmark.client.responses.Neo4jRequestHandler;
import de.uniko.sebschlicht.socialnet.requests.RequestFeed;
import de.uniko.sebschlicht.socialnet.requests.RequestFollow;
import de.uniko.sebschlicht.socialnet.requests.RequestPost;
import de.uniko.sebschlicht.socialnet.requests.RequestUnfollow;
import de.uniko.sebschlicht.socialnet.requests.RequestUser;

public class AsyncNeo4jClient extends AsyncBenchmarkClient {

    private static final String URL_PLUGIN =
            "/db/data/ext/GraphityBaselinePlugin/graphdb/";

    private static final String URL_FEED = URL_PLUGIN + "feeds/";

    private static final String URL_FOLLOW = URL_PLUGIN + "follow/";

    private static final String URL_POST = URL_PLUGIN + "post/";

    private static final String URL_UNFOLLOW = URL_PLUGIN + "unfollow/";

    private static final String URL_USER = URL_PLUGIN + "user/";

    public AsyncNeo4jClient(
            AsyncBenchmarkClientTask client,
            Configuration config) {
        super(client, config);
    }

    @Override
    protected AsyncRequestHandler getRequestHandler(int identifier) {
        return new Neo4jRequestHandler(_client, identifier);
    }

    @Override
    public BoundRequestBuilder createFeedRequest(RequestFeed request) {
        String jsonString = "{\"reader\":\"" + request.getId() + "\"}";
        return _httpClient
                .preparePost(urlFromRelativeUrl(request.getAddress(), URL_FEED))
                .setHeader("Content-Type", "application/json")
                .setBody(jsonString);
    }

    @Override
    public BoundRequestBuilder createFollowRequest(RequestFollow request) {
        String jsonString =
                "{\"following\":\"" + request.getIdSubscriber()
                        + "\",\"followed\":\"" + request.getIdFollowed()
                        + "\"}";
        return _httpClient
                .preparePost(
                        urlFromRelativeUrl(request.getAddress(), URL_FOLLOW))
                .setHeader("Content-Type", "application/json")
                .setBody(jsonString);
    }

    @Override
    public BoundRequestBuilder createPostRequest(RequestPost request) {
        String jsonString =
                "{\"author\":\"" + request.getId() + "\",\"message\":\""
                        + request.getMessage() + "\"}";
        return _httpClient
                .preparePost(urlFromRelativeUrl(request.getAddress(), URL_POST))
                .setHeader("Content-Type", "application/json")
                .setBody(jsonString);
    }

    @Override
    public BoundRequestBuilder createUnfollowRequest(RequestUnfollow request) {
        String jsonString =
                "{\"following\":\"" + request.getIdSubscriber()
                        + "\",\"followed\":\"" + request.getIdFollowed()
                        + "\"}";
        return _httpClient
                .preparePost(
                        urlFromRelativeUrl(request.getAddress(), URL_UNFOLLOW))
                .setHeader("Content-Type", "application/json")
                .setBody(jsonString);
    }

    @Override
    public BoundRequestBuilder createUserRequest(RequestUser request) {
        String jsonString = "{\"id\":\"" + request.getId() + "\"}";
        return _httpClient
                .preparePost(urlFromRelativeUrl(request.getAddress(), URL_USER))
                .setHeader("Content-Type", "application/json")
                .setBody(jsonString);
    }
}
