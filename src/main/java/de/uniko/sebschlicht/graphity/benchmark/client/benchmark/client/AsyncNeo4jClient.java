package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.client;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.bootstrap.BootstrapManager;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response.AsyncRequestHandler;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response.Neo4jRequestHandler;
import de.uniko.sebschlicht.graphity.bootstrap.generate.MutableState;
import de.uniko.sebschlicht.socialnet.Subscription;
import de.uniko.sebschlicht.socialnet.requests.Request;
import de.uniko.sebschlicht.socialnet.requests.RequestFeed;
import de.uniko.sebschlicht.socialnet.requests.RequestFollow;
import de.uniko.sebschlicht.socialnet.requests.RequestPost;
import de.uniko.sebschlicht.socialnet.requests.RequestUnfollow;

public class AsyncNeo4jClient extends AsyncBenchmarkClient {

    private static final String URL_PLUGIN =
            "/db/data/ext/GraphityBaselinePlugin/graphdb/";

    private static final String URL_FEED = URL_PLUGIN + "feeds/";

    private static final String URL_FOLLOW = URL_PLUGIN + "follow/";

    private static final String URL_POST = URL_PLUGIN + "post/";

    private static final String URL_UNFOLLOW = URL_PLUGIN + "unfollow/";

    private static final String URL_BOOTSTRAP = URL_PLUGIN + "bootstrap/";

    public AsyncNeo4jClient(
            AsyncBenchmarkClientTask client,
            ClientConfiguration config) {
        super(client, config);
    }

    @Override
    protected AsyncRequestHandler getRequestHandler(int identifier) {
        return new Neo4jRequestHandler(_client, identifier);
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
                .preparePost(urlFromRelativeUrl(address, URL_BOOTSTRAP))
                .setHeader("Content-Type", "application/json")
                .setBody(body.toString());
    }

    private JsonObject prepareBootstrapRequestBody(MutableState state) {
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
}
