package de.uniko.sebschlicht.graphity.benchmark.client;

import de.uniko.sebschlicht.graphity.benchmark.client.client.AsyncBenchmarkClient;
import de.uniko.sebschlicht.graphity.benchmark.client.client.AsyncNeo4jClient;
import de.uniko.sebschlicht.graphity.benchmark.client.client.AsyncTitanClient;
import de.uniko.sebschlicht.graphity.benchmark.client.config.Configuration;
import de.uniko.sebschlicht.graphity.benchmark.client.config.TargetType;
import de.uniko.sebschlicht.graphity.benchmark.client.responses.ResultManager;
import de.uniko.sebschlicht.socialnet.requests.Request;

/**
 * Client wrapper to measure latencies and handle target endpoints of
 * asynchronous benchmark clients.
 * 
 * @author sebschlicht
 * 
 */
public class AsyncBenchmarkClientTask {

    private boolean _isRunning;

    private AsyncClient _owner;

    private ResultManager _resultManager;

    private long[] _startTime;

    private String[] _endpoints;

    private int _iCrrEndpoint;

    private AsyncBenchmarkClient _client;

    private int _numMaxRequests;

    private int _numRequests;

    public AsyncBenchmarkClientTask(
            AsyncClient owner,
            ResultManager resultManager,
            Configuration config) {
        _owner = owner;
        _resultManager = resultManager;
        _startTime = new long[config.getNumThreads()];
        _endpoints =
                config.getAddresses().toArray(
                        new String[config.getAddresses().size()]);

        if (config.getTargetType() == TargetType.NEO4J) {
            _client = new AsyncNeo4jClient(this, config);
        } else {
            _client = new AsyncTitanClient(this, config);
        }
    }

    public boolean start(int numRequests) {
        if (_isRunning) {
            return false;
        }
        _isRunning = true;
        _numRequests = 0;
        _numMaxRequests = numRequests;
        for (int slotIdentifier = 0; slotIdentifier < _startTime.length
                && slotIdentifier < numRequests; ++slotIdentifier) {
            executeNextRequest(slotIdentifier);
        }
        return true;
    }

    public boolean stop() {
        if (!_isRunning) {
            return false;
        }
        _isRunning = false;
        //TODO: can we store the futures and cancel them to have sync stop?
        return true;
    }

    public void handleResponse(int identifier, Request request) {
        long duration = System.nanoTime() - _startTime[identifier];
        _resultManager.addResult(request, duration);
        _owner.handleResponse(request);
        if (_numRequests >= _numMaxRequests && _isRunning) {
            _isRunning = false;
            String message = "request limit reached.";
            System.out.println(message);
            AsyncClient.LOG.info(message);
        } else if (_isRunning) {
            executeNextRequest(identifier);
        }
    }

    private void executeNextRequest(int identifier) {
        _numRequests += 1;
        Request request = _owner.nextRequest();
        String address = getNextTargetEndpoint();
        request.setAddress(address);
        _startTime[identifier] = System.nanoTime();
        _client.executeRequest(identifier, request);
    }

    /**
     * Retrieves the target endpoint for the next request.
     * Default implementation does a round-robin across all endpoints specified.
     * 
     * @return target endpoint for next request
     */
    protected String getNextTargetEndpoint() {
        String address = _endpoints[_iCrrEndpoint];
        if (++_iCrrEndpoint >= _endpoints.length) {
            _iCrrEndpoint = 0;
        }
        return address;
    }
}
