package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response;

import com.google.gson.Gson;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

import de.uniko.sebschlicht.graphity.benchmark.api.TargetType;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.bootstrap.generate.MutableState;

public class BootstrapRequestHandler extends AsyncCompletionHandler<Void> {

    private static final Gson GSON = new Gson();

    private final TargetType _targetType;

    private final AsyncBenchmarkClientTask _client;

    private final MutableState _state;

    public BootstrapRequestHandler(
            AsyncBenchmarkClientTask client,
            TargetType targetType,
            MutableState state) {
        _client = client;
        _targetType = targetType;
        _state = state;
    }

    public void bootstrap() {
        _client.bootstrap(this, _state);
    }

    @Override
    public Void onCompleted(Response response) throws Exception {
        String sResponse = response.getResponseBody();
        if (_targetType == TargetType.NEO4J) {
            if (!"\"true\"".equals(sResponse)) {
                throw new IllegalStateException(sResponse);
            }
        } else {
            try {
                TitanBooleanResponse bResponse =
                        GSON.fromJson(sResponse, TitanBooleanResponse.class);
                if (!bResponse.isSuccess() || !bResponse.getValue()) {
                    throw new IllegalStateException("invalid response: "
                            + sResponse);
                }
            } catch (Exception e) {
                throw new IllegalStateException("invalid response: "
                        + sResponse, e);
            }
        }
        System.out.println("bootstrap finished.");
        return null;
    }

    @Override
    public void onThrowable(Throwable t) {
        t.printStackTrace();
    }
}
