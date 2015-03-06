package de.uniko.sebschlicht.graphity.benchmark.client.client;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

import de.uniko.sebschlicht.graphity.benchmark.client.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.config.Configuration;
import de.uniko.sebschlicht.graphity.benchmark.client.responses.AsyncRequestHandler;
import de.uniko.sebschlicht.graphity.benchmark.client.responses.TitanRequestHandler;
import de.uniko.sebschlicht.socialnet.requests.RequestFeed;
import de.uniko.sebschlicht.socialnet.requests.RequestFollow;
import de.uniko.sebschlicht.socialnet.requests.RequestPost;
import de.uniko.sebschlicht.socialnet.requests.RequestUnfollow;

public class AsyncTitanClient extends AsyncBenchmarkClient {

    private static final String URL_EXTENSION = "/graphs/graph/graphity/";

    private static final String URL_FEED = URL_EXTENSION + "feeds/";

    private static final String URL_FOLLOW = URL_EXTENSION + "follow/";

    private static final String URL_POST = URL_EXTENSION + "post/";

    private static final String URL_UNFOLLOW = URL_EXTENSION + "unfollow/";

    public AsyncTitanClient(
            AsyncBenchmarkClientTask client,
            Configuration config) {
        super(client, config);
    }

    @Override
    protected AsyncRequestHandler getRequestHandler(int identifier) {
        return new TitanRequestHandler(_client, identifier);
    }

    @Override
    protected BoundRequestBuilder prepareFeedRequest(RequestFeed request) {
        String jsonString = "{\"reader\":\"" + request.getId() + "\"}";
        return _httpClient
                .preparePost(urlFromRelativeUrl(request.getAddress(), URL_FEED))
                .setHeader("Content-Type", "application/json")
                .setBody(jsonString);
    }

    @Override
    protected BoundRequestBuilder prepareFollowRequest(RequestFollow request) {
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
    protected BoundRequestBuilder preparePostRequest(RequestPost request) {
        String jsonString =
                "{\"author\":\"" + request.getId() + "\",\"message\":\""
                        + request.getMessage() + "\"}";
        return _httpClient
                .preparePost(urlFromRelativeUrl(request.getAddress(), URL_POST))
                .setHeader("Content-Type", "application/json")
                .setBody(jsonString);
    }

    @Override
    protected BoundRequestBuilder
        prepareUnfollowRequest(RequestUnfollow request) {
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
}
