package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.client;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response.AsyncRequestHandler;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response.BootstrapRequestHandler;
import de.uniko.sebschlicht.graphity.bootstrap.generate.MutableState;
import de.uniko.sebschlicht.socialnet.requests.Request;
import de.uniko.sebschlicht.socialnet.requests.RequestFeed;
import de.uniko.sebschlicht.socialnet.requests.RequestFollow;
import de.uniko.sebschlicht.socialnet.requests.RequestPost;
import de.uniko.sebschlicht.socialnet.requests.RequestUnfollow;

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
                        .setRequestTimeout(-1).setIOThreadMultiplier(1).build();
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
            MutableState state);

    public void bootstrap(
            MutableState state,
            BootstrapRequestHandler requestHandler) {
        BoundRequestBuilder httpRequest = prepareBootstrapRequest(state);
        httpRequest.execute(requestHandler);
    }
}
