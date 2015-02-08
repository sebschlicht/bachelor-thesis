package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response;

import java.util.LinkedList;
import java.util.Queue;

import com.google.gson.Gson;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

import de.uniko.sebschlicht.graphity.benchmark.api.TargetType;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;

public class BootstrapRequestHandler extends AsyncCompletionHandler<Void> {

    private static final Gson GSON = new Gson();

    private final TargetType _targetType;

    private final AsyncBenchmarkClientTask _client;

    private final Queue<Request> _requests;

    private int _blockSize;

    public BootstrapRequestHandler(
            AsyncBenchmarkClientTask client,
            TargetType targetType,
            Queue<Request> requests,
            int blockSize) {
        _client = client;
        _targetType = targetType;
        _requests = requests;
        _blockSize = blockSize;
    }

    private void bootstrapNextBlock() {
        Queue<Request> block = new LinkedList<Request>();
        for (int i = 0; i < _blockSize && !_requests.isEmpty(); ++i) {
            block.add(_requests.remove());
        }
        if (!block.isEmpty()) {
            System.out
                    .println("Bootstrapping " + block.size() + " elements...");
            _client.bootstrap(this, block);
        } else {
            System.out.println("done.");
        }
    }

    public void startBootstrap() {
        bootstrapNextBlock();
    }

    @Override
    public Void onCompleted(Response response) throws Exception {
        String sResponse = response.getResponseBody();
        if (_targetType == TargetType.NEO4J) {
            if ("\"true\"".equals(sResponse)) {
                bootstrapNextBlock();
            } else {
                throw new IllegalStateException(sResponse);
            }
        } else {
            TitanBooleanResponse bResponse =
                    GSON.fromJson(sResponse, TitanBooleanResponse.class);
            if (bResponse.isSuccess() && bResponse.getValue()) {
                bootstrapNextBlock();
            } else {
                throw new IllegalStateException(sResponse);
            }
        }
        return null;
    }

    @Override
    public void onThrowable(Throwable t) {
        t.printStackTrace();
    }
}
