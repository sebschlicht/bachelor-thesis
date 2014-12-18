package de.uniko.sebschlicht.graphity.benchmark.client;

import java.util.Random;
import java.util.concurrent.Callable;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import de.uniko.sebschlicht.graphity.benchmark.api.BenchmarkResult;
import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.api.RequestComposition;
import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;

public class ClientThread implements Callable<BenchmarkResult> {

    private boolean isStopped;

    private ClientConfiguration config;

    private RequestComposition requestComposition;

    private Client httpClient;

    private WebResource resTarget;

    public ClientThread(
            ClientConfiguration config) {
        this.config = config;
        requestComposition = config.getRequestComposition();
        resTarget = createResource(this, "/");
    }

    @Override
    public BenchmarkResult call() throws Exception {
        while (!isStopped()) {
            RequestType type = generateRequestType();

            switch (type) {
                case FEED:
                    break;

                case FOLLOW:
                    break;

                case POST:
                    break;

                case UNFOLLOW:
                    break;
            }
            // TODO: execute HTTP request against target cluster

        }
    }

    public boolean isStopped() {
        return isStopped;
    }

    private RequestType generateRequestType() {
        float rt = new Random().nextFloat() * 100;
        if (rt < requestComposition.getFeed()) {
            return RequestType.FEED;
        } else if (rt < requestComposition.getFeed()
                + requestComposition.getFollow()) {
            return RequestType.FOLLOW;
        } else if (rt < requestComposition.getFeed()
                + requestComposition.getFollow() + requestComposition.getPost()) {
            return RequestType.POST;
        } else {
            return RequestType.UNFOLLOW;
        }
    }

    private static WebResource createResource(
            ClientThread clientThread,
            String relativeUrl) {
        return clientThread.httpClient.resource("http://"
                + clientThread.config.getTargetAddress() + relativeUrl);
    }
}
