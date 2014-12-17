package de.uniko.sebschlicht.graphity.benchmark.master.tasks;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.master.ClientWrapper;

public class StartBenchmarkTask extends ClientCommunicationTask<Boolean> {

    private ClientConfiguration config;

    public StartBenchmarkTask(
            ClientWrapper client,
            ClientConfiguration config) {
        super(client);
        this.config = config;
    }

    @Override
    public Boolean call() throws Exception {
        return client.start(config);
    }
}
