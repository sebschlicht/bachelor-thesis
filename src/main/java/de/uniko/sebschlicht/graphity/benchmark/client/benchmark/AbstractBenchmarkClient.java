package de.uniko.sebschlicht.graphity.benchmark.client.benchmark;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;

public abstract class AbstractBenchmarkClient implements BenchmarkClient {

    protected ClientConfiguration config;

    protected Client httpClient;

    protected WebResource resFeed;

    protected WebResource resFollow;

    protected WebResource resPost;

    protected WebResource resUnfollow;

    protected AbstractBenchmarkClient(
            ClientConfiguration config) {
        this.config = config;
        httpClient = Client.create();
        httpClient.setConnectTimeout(500);
        httpClient.setReadTimeout(1500);
    }

    protected WebResource resourceFromUrl(String url) {
        return httpClient
                .resource("http://" + config.getTargetEndpoint() + url);
    }

    protected boolean parseBoolean(String bValue) {
        return "true".equals(bValue);
    }
}
