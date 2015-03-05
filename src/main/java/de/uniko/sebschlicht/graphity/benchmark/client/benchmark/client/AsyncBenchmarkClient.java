package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.client;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response.AsyncRequestHandler;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response.BootstrapRequestHandler;
import de.uniko.sebschlicht.graphity.bootstrap.generate.MutableState;
import de.uniko.sebschlicht.socialnet.Subscription;
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

    protected JsonObject prepareBootstrapRequestBody(MutableState state) {
        JsonArray jaUserIds = new JsonArray();
        JsonArray jaSubscriptions = new JsonArray();
        JsonArray jaNumPosts = new JsonArray();

        TreeSet<Long> sUserIds = new TreeSet<>();

        // convert from mutable to final social network state
        long prevUserId = -1, userId = 0;
        List<Long> userSubs = new LinkedList<>();

        // load subscriptions and followers
        TreeSet<Subscription> subscriptions = state.getSubscriptions();
        int iSubscription = 0;
        int numSubscriptions = subscriptions.size();
        for (Subscription subscription : subscriptions) {
            // switch to current user
            userId = subscription.getIdSubscriber();
            if (userId != prevUserId) {
                // make previous user persistent when switching to a new user
                jaSubscriptions.add(new JsonPrimitive(userSubs.size()));
                for (long userSub : userSubs) {
                    jaSubscriptions.add(new JsonPrimitive(userSub));
                }
                // switch to new user
                sUserIds.add(userId);
                jaUserIds.add(new JsonPrimitive(userId));
                userSubs.clear();
                prevUserId = userId;
            }

            // add subscription for current user
            userSubs.add(subscription.getIdFollowed());

            // make persistent if last user
            if (iSubscription == numSubscriptions - 1) {
                jaSubscriptions.add(new JsonPrimitive(userSubs.size()));
                for (long userSub : userSubs) {
                    jaSubscriptions.add(new JsonPrimitive(userSub));
                }
            }
            iSubscription += 1;
        }

        // load posts and authors
        Map<Long, int[]> numPosts = state.getNumPosts();
        //TODO is it performant to iterate over JsonArray?
        for (JsonElement e : jaUserIds) {// load posts from users with active subscriptions
            int[] numUserPosts = numPosts.get(e.getAsLong());
            jaNumPosts.add(new JsonPrimitive(numUserPosts[1]));// total number of posts // we are bootstrapping!
        }
        for (Map.Entry<Long, int[]> entry : numPosts.entrySet()) {
            userId = entry.getKey();
            if (sUserIds.contains(userId)) {// user with active subscriptions
                continue;
            }
            int[] numUserPosts = entry.getValue();
            jaUserIds.add(new JsonPrimitive(userId));
            jaNumPosts.add(new JsonPrimitive(numUserPosts[1]));// total number of posts // we are bootstrapping!
        }

        JsonObject body = new JsonObject();
        body.add("userIds", jaUserIds);
        body.add("subscriptions", jaSubscriptions);
        body.add("numPosts", jaNumPosts);
        return body;
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
