package de.uniko.sebschlicht.graphity.benchmark.master.tasks;

import de.uniko.sebschlicht.graphity.benchmark.master.ClientWrapper;

public class StartBenchmarkTask extends ClientCommunicationTask<Boolean> {

    public StartBenchmarkTask(
            ClientWrapper client) {
        super(client);
    }

    @Override
    public Boolean call() throws Exception {
        return client.start();
    }
}
