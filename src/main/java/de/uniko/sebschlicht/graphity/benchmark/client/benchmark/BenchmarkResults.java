package de.uniko.sebschlicht.graphity.benchmark.client.benchmark;

import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;

public class BenchmarkResults {

    private BenchmarkResult[] results;

    public BenchmarkResults() {
        results = new BenchmarkResult[4];
        for (int i = 0; i < 4; ++i) {
            results[i] = new BenchmarkResult();
        }
    }

    public void addResult(RequestType type, long duration) {
        if (!results[type.getId()].addResult(duration)) {
            BenchmarkResult container = results[type.getId()];
            long[] results = container.getResults();
            container.clear();
            //TODO: push to client-static result collector
        }
    }
}
