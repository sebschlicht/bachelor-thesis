package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response;

import java.io.IOException;

import org.apache.commons.lang3.NotImplementedException;

import com.google.gson.Gson;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;

public abstract class AsyncRequestHandler extends AsyncCompletionHandler<Void> {

    protected static final Gson GSON = new Gson();

    protected AsyncBenchmarkClientTask _client;

    protected int _identifier;

    protected Request _request;

    public AsyncRequestHandler(
            AsyncBenchmarkClientTask client,
            int identifier) {
        _client = client;
        _identifier = identifier;
    }

    public void setRequest(Request request) {
        _request = request;
    }

    @Override
    public Void onCompleted(Response response) throws Exception {
        switch (_request.getType()) {
            case FEED:
                handleFeedResponse(response);
                break;

            case FOLLOW:
                handleFollowResponse(response);
                break;

            case POST:
                handlePostResponse(response);
                break;

            case UNFOLLOW:
                handleUnfollowResponse(response);
                break;

            default:
                throw new NotImplementedException(
                        "response handling failed: unknown request type "
                                + _request.getType());
        }
        _client.handleResponse(_identifier, _request);
        return null;
    }

    @Override
    public void onThrowable(Throwable t) {
        _request.setError(t);
        _client.handleResponse(_identifier, _request);
    }

    protected abstract void handleFeedResponse(Response response)
            throws IOException;

    protected abstract void handleFollowResponse(Response response)
            throws IOException;

    protected abstract void handlePostResponse(Response response)
            throws IOException;

    protected abstract void handleUnfollowResponse(Response response)
            throws IOException;
}
