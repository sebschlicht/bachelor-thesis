package de.uniko.sebschlicht.graphity.benchmark.master.tasks;

import de.uniko.sebschlicht.graphity.benchmark.master.ClientWrapper;

public class ResultCollectionTask extends ClientCommunicationTask<String> {

    public ResultCollectionTask(
            ClientWrapper client) {
        super(client);
    }

    @Override
    public String call() throws Exception {
        return client.getStatus();
    }
}
