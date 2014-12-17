package de.uniko.sebschlicht.graphity.benchmark.master;

import java.util.concurrent.Callable;

public class ResultCollectionTask implements Callable<String> {

    private ClientWrapper client;

    public ResultCollectionTask(
            ClientWrapper client) {
        this.client = client;
    }

    @Override
    public String call() throws Exception {
        return client.getStatus();
    }
}
