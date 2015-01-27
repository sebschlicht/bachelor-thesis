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
        for (int i = 0; i < 10000 && !_requests.isEmpty(); ++i) {
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
        if ("\"true\"".equals(sResponse)) {
            bootstrapNextBlock();
        } else {
            throw new IllegalStateException(sResponse);
        }
        return null;
    }

    @Override
    public void onThrowable(Throwable t) {
        t.printStackTrace();
    }
}
