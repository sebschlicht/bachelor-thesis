package de.uniko.sebschlicht.graphity.benchmark.master.tasks;

import java.util.concurrent.Callable;

import de.uniko.sebschlicht.graphity.benchmark.master.ClientWrapper;

public abstract class ClientCommunicationTask<V > implements Callable<V> {

    protected ClientWrapper client;

    public ClientCommunicationTask(
            ClientWrapper client) {
        this.client = client;
    }
}
