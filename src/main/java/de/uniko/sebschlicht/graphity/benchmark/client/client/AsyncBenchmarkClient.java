package de.uniko.sebschlicht.graphity.benchmark.client.client;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;

import de.uniko.sebschlicht.graphity.benchmark.client.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.config.Configuration;
import de.uniko.sebschlicht.graphity.benchmark.client.responses.AsyncRequestHandler;
import de.uniko.sebschlicht.socialnet.requests.Request;
import de.uniko.sebschlicht.socialnet.requests.RequestFeed;
import de.uniko.sebschlicht.socialnet.requests.RequestFollow;
import de.uniko.sebschlicht.socialnet.requests.RequestPost;
import de.uniko.sebschlicht.socialnet.requests.RequestUnfollow;

public abstract class AsyncBenchmarkClient {

    protected AsyncBenchmarkClientTask _client;

    protected Configuration _config;

    protected AsyncHttpClient _httpClient;

    protected String urlFeed;

    protected String urlFollow;

    protected String urlPost;

    protected String urlUnfollow;

    protected AsyncBenchmarkClient(
            AsyncBenchmarkClientTask client,
            Configuration config) {
        _client = client;
        _config = config;
        AsyncHttpClientConfig httpClientConfig =
                new AsyncHttpClientConfig.Builder().setConnectTimeout(10000)
                        .setRequestTimeout(-1).setIOThreadMultiplier(1).build();
        _httpClient = new AsyncHttpClient(httpClientConfig);
    }

    //TODO: make public and reuse request handlers
    abstract protected AsyncRequestHandler getRequestHandler(int identifier);

    abstract public BoundRequestBuilder createFeedRequest(RequestFeed request);

    abstract public BoundRequestBuilder createFollowRequest(
            RequestFollow request);

    abstract public BoundRequestBuilder createPostRequest(RequestPost request);

    abstract public BoundRequestBuilder createUnfollowRequest(
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
                httpRequest = createFeedRequest((RequestFeed) request);
                httpRequest.execute(requestHandler);
                break;

            case FOLLOW:
                httpRequest = createFollowRequest((RequestFollow) request);
                httpRequest.execute(requestHandler);
                break;

            case POST:
                httpRequest = createPostRequest((RequestPost) request);
                httpRequest.execute(requestHandler);
                break;

            case UNFOLLOW:
                httpRequest = createUnfollowRequest((RequestUnfollow) request);
                httpRequest.execute(requestHandler);
                break;
        }
    }
}
