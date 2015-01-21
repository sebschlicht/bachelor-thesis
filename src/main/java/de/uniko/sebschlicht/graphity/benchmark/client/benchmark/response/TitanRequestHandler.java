package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonObject;
import com.ning.http.client.Response;

import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.impl.ResponseList;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFeed;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFollow;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestPost;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestUnfollow;

public class TitanRequestHandler extends AsyncRequestHandler {

    public TitanRequestHandler(
            AsyncBenchmarkClientTask client,
            int identifier) {
        super(client, identifier);
    }

    @Override
    protected void handleFeedResponse(Response response) throws IOException {
        String sResponse = response.getResponseBody();
        ResponseList lResponse = GSON.fromJson(sResponse, ResponseList.class);
        List<JsonObject> statusUpdates = lResponse.getResponseValue();
        ((RequestFeed) _request).setResult((statusUpdates == null)
                ? -1
                : statusUpdates.size());
    }

    @Override
    protected void handleFollowResponse(Response response) throws IOException {
        String sResponse = response.getResponseBody();
        TitanBooleanResponse bResponse =
                GSON.fromJson(sResponse, TitanBooleanResponse.class);
        if (bResponse.isSuccess()) {
            System.out.println(sResponse);
            ((RequestFollow) _request).setResult(bResponse.getValue());
        }
    }

    @Override
    protected void handlePostResponse(Response response) throws IOException {
        String sResponse = response.getResponseBody();
        TitanLongResponse lResponse =
                GSON.fromJson(sResponse, TitanLongResponse.class);
        if (lResponse.isSuccess()) {
            System.out.println(sResponse);
            ((RequestPost) _request).setResult(lResponse.getValue() != 0L);
        }
    }

    @Override
    protected void handleUnfollowResponse(Response response) throws IOException {
        String sResponse = response.getResponseBody();
        TitanBooleanResponse bResponse =
                GSON.fromJson(sResponse, TitanBooleanResponse.class);
        if (bResponse.isSuccess()) {
            System.out.println(sResponse);
            ((RequestUnfollow) _request).setResult(bResponse.getValue());
        }
    }
}
