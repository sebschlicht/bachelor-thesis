package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response;

import java.util.LinkedList;
import java.util.Queue;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;

public class BootstrapRequestHandler extends AsyncCompletionHandler<Void> {

    private final AsyncBenchmarkClientTask _client;

    private final Queue<Request> _requests;

    public BootstrapRequestHandler(
            AsyncBenchmarkClientTask client,
            Queue<Request> requests) {
        _client = client;
        _requests = requests;
    }

    private void bootstrapNextBlock() {
        Queue<Request> block = new LinkedList<Request>();
        for (int i = 0; i < 1000 && !_requests.isEmpty(); ++i) {
            block.add(_requests.remove());
        }
        if (!block.isEmpty()) {
            _client.bootstrap(this, block);
        }
    }

    public void startBootstrap() {
        bootstrapNextBlock();
    }

    @Override
    public Void onCompleted(Response arg0) throws Exception {
        bootstrapNextBlock();
        return null;
    }
}
