package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.client;

import java.util.Queue;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response.AsyncRequestHandler;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response.BootstrapRequestHandler;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFeed;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFollow;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestPost;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestUnfollow;

public abstract class AsyncBenchmarkClient {

    protected AsyncBenchmarkClientTask _client;

    protected ClientConfiguration _config;

    protected AsyncHttpClient _httpClient;

    protected String urlFeed;

    protected String urlFollow;

    protected String urlPost;

    protected String urlUnfollow;

    protected AsyncBenchmarkClient(
            AsyncBenchmarkClientTask client,
            ClientConfiguration config) {
        _client = client;
        _config = config;
        AsyncHttpClientConfig httpClientConfig =
                new AsyncHttpClientConfig.Builder().setConnectTimeout(1000)
                        .setIOThreadMultiplier(1).build();
        _httpClient = new AsyncHttpClient(httpClientConfig);
    }

    //TODO: make public and reuse request handlers
    abstract protected AsyncRequestHandler getRequestHandler(int identifier);

    abstract protected BoundRequestBuilder prepareFeedRequest(
            RequestFeed request);

    abstract protected BoundRequestBuilder prepareFollowRequest(
            RequestFollow request);

    abstract protected BoundRequestBuilder preparePostRequest(
            RequestPost request);

    abstract protected BoundRequestBuilder prepareUnfollowRequest(
            RequestUnfollow request);

    protected String urlFromRelativeUrl(String address, String url) {
        return "http://" + address + _config.getTargetBase() + url;
    }

    public void executeRequest(int identifier, Request request) {
        AsyncRequestHandler requestHandler = getRequestHandler(identifier);
        requestHandler.setRequest(request);

        BoundRequestBuilder httpRequest;
        switch (request.getType()) {
            case FEED:
                httpRequest = prepareFeedRequest((RequestFeed) request);
                httpRequest.execute(requestHandler);
                break;

            case FOLLOW:
                httpRequest = prepareFollowRequest((RequestFollow) request);
                httpRequest.execute(requestHandler);
                break;

            case POST:
                httpRequest = preparePostRequest((RequestPost) request);
                httpRequest.execute(requestHandler);
                break;

            case UNFOLLOW:
                httpRequest = prepareUnfollowRequest((RequestUnfollow) request);
                httpRequest.execute(requestHandler);
                break;
        }
    }

    abstract protected BoundRequestBuilder prepareBootstrapRequest(
            Queue<Request> requests);

    public void bootstrap(
            Queue<Request> requests,
            BootstrapRequestHandler requestHandler) {
        BoundRequestBuilder httpRequest = prepareBootstrapRequest(requests);
        httpRequest.execute(requestHandler);
    }
}
