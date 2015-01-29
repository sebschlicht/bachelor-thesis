package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.client;

import java.util.Queue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.bootstrap.BootstrapManager;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response.AsyncRequestHandler;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response.Neo4jRequestHandler;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFeed;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFollow;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestPost;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestUnfollow;

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
    protected BoundRequestBuilder prepareBootstrapRequest(
            Queue<Request> requests) {
        JsonObject body = new JsonObject();
        JsonArray entries = new JsonArray();
        String address = null;
        for (Request request : requests) {
            if (address == null) {
                address = request.getAddress();
            }
            for (String element : request.toStringArray()) {
                entries.add(new JsonPrimitive(element));
            }
        }
        body.add("entries", entries);
        BootstrapManager.addRequests(requests);
        //System.out.println(body); //~6MB
        return _httpClient
                .preparePost(urlFromRelativeUrl(address, URL_BOOTSTRAP))
                .setHeader("Content-Type", "application/json")
                .setBody(body.toString());
    }
}
