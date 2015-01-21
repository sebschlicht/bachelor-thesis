package de.uniko.sebschlicht.graphity.benchmark.client.benchmark;

import java.util.LinkedList;
import java.util.Queue;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.api.TargetType;
import de.uniko.sebschlicht.graphity.benchmark.client.AsyncClient;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.client.AsyncBenchmarkClient;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.client.AsyncTitanClient;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.results.ResultManager;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;

public class AsyncBenchmarkClientTask {

    private boolean _isRunning;

    private AsyncClient _owner;

    private ResultManager _resultManager;

    private long[] _startTime;

    protected Queue<String> _endpoints;

    private AsyncBenchmarkClient _client;

    public AsyncBenchmarkClientTask(
            AsyncClient owner,
            ResultManager resultManager,
            ClientConfiguration config) {
        _owner = owner;
        _resultManager = resultManager;
        _startTime = new long[config.getNumThreads()];
        _endpoints = new LinkedList<String>(config.getAddresses());

        if (config.getTargetType() == TargetType.NEO4J) {
            //TODO: implement async Neo4j client
        } else {
            _client = new AsyncTitanClient(this, config);
        }
    }

    public boolean start() {
        if (_isRunning) {
            return false;
        }
        _isRunning = true;
        for (int slotIdentifier = 0; slotIdentifier < _startTime.length; ++slotIdentifier) {
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
        long duration = System.currentTimeMillis() - _startTime[identifier];
        _resultManager.addResult(request, duration);
        if (_isRunning) {
            executeNextRequest(identifier);
        }
    }

    private void executeNextRequest(int identifier) {
        Request request = _owner.nextRequest();
        String endpoint = _endpoints.poll();
        _endpoints.add(endpoint);
        request.setAddress(endpoint);
        _startTime[identifier] = System.currentTimeMillis();
        _client.executeRequest(identifier, request);
    }
}
