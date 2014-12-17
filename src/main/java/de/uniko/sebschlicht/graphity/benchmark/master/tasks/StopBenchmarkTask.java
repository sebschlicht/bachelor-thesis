package de.uniko.sebschlicht.graphity.benchmark.master.tasks;

import de.uniko.sebschlicht.graphity.benchmark.master.ClientWrapper;

public class StopBenchmarkTask extends ClientCommunicationTask<String> {

    public StopBenchmarkTask(
            ClientWrapper client) {
        super(client);
    }

    @Override
    public String call() throws Exception {
        return client.stop();
    }
}
