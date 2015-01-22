package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonObject;
import com.ning.http.client.Response;

import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
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
        TitanListResponse lResponse =
                GSON.fromJson(sResponse, TitanListResponse.class);
        if (lResponse.isSuccess()) {
            List<JsonObject> statusUpdates = lResponse.getValue();
            ((RequestFeed) _request).setResult((statusUpdates == null)
                    ? -1
                    : statusUpdates.size());
        } else {
            _request.setError(new IllegalStateException(sResponse));
        }
    }

    @Override
    protected void handleFollowResponse(Response response) throws IOException {
        String sResponse = response.getResponseBody();
        TitanBooleanResponse bResponse =
                GSON.fromJson(sResponse, TitanBooleanResponse.class);
        if (bResponse.isSuccess()) {
            ((RequestFollow) _request).setResult(bResponse.getValue());
        } else {
            _request.setError(new IllegalStateException(sResponse));
        }
    }

    @Override
    protected void handlePostResponse(Response response) throws IOException {
        String sResponse = response.getResponseBody();
        TitanLongResponse lResponse =
                GSON.fromJson(sResponse, TitanLongResponse.class);
        if (lResponse.isSuccess()) {
            ((RequestPost) _request).setResult(lResponse.getValue() != 0L);
        } else {
            _request.setError(new IllegalStateException(sResponse));
        }
    }

    @Override
    protected void handleUnfollowResponse(Response response) throws IOException {
        String sResponse = response.getResponseBody();
        TitanBooleanResponse bResponse =
                GSON.fromJson(sResponse, TitanBooleanResponse.class);
        if (bResponse.isSuccess()) {
            ((RequestUnfollow) _request).setResult(bResponse.getValue());
        } else {
            _request.setError(new IllegalStateException(sResponse));
        }
    }
}
