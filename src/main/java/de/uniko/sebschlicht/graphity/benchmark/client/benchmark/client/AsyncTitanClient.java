package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.client;

import com.google.gson.JsonObject;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.bootstrap.BootstrapManager;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response.AsyncRequestHandler;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response.TitanRequestHandler;
import de.uniko.sebschlicht.graphity.bootstrap.generate.MutableState;
import de.uniko.sebschlicht.socialnet.requests.Request;
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

    private static final String URL_BOOTSTRAP = URL_EXTENSION + "bootstrap/";

    public AsyncTitanClient(
            AsyncBenchmarkClientTask client,
            ClientConfiguration config) {
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

    @Override
    protected BoundRequestBuilder prepareBootstrapRequest(MutableState state) {
        JsonObject body = prepareBootstrapRequestBody(state);
        String address = null;
        Request firstRequest = state.getRequests().element();
        address = firstRequest.getAddress();
        BootstrapManager.addRequests(state.getRequests());
        //System.out.println(body); //~6MB
        return _httpClient
                .preparePut(urlFromRelativeUrl(address, URL_BOOTSTRAP))
                .setHeader("Content-Type", "application/json")
                .setBody(body.toString());
    }
}
