package de.uniko.sebschlicht.graphity.benchmark.client.benchmark;

import org.sonatype.spice.jersey.client.ahc.config.DefaultAhcConfig;

import com.google.gson.Gson;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Realm;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;

public abstract class AbstractBenchmarkClient implements BenchmarkClient {

    protected static final Gson GSON = new Gson();

    protected ClientConfiguration config;

    protected Client httpClient;

    protected WebResource resFeed;

    protected WebResource resFollow;

    protected WebResource resPost;

    protected WebResource resUnfollow;

    protected AbstractBenchmarkClient(
            ClientConfiguration config) {
        this.config = config;
        DefaultAhcConfig clientConfig = new DefaultAhcConfig();
        clientConfig.getAsyncHttpClientConfigBuilder().setRealm(
                new Realm.RealmBuilder().setScheme(Realm.AuthScheme.SPNEGO)
                        .setUsePreemptiveAuth(false).build());
        clientConfig.getAsyncHttpClientConfigBuilder().setMaxConnections(
                config.getNumThreads());
        httpClient = Client.create(clientConfig);
        httpClient.setConnectTimeout(1000);
        //httpClient.setReadTimeout(5000);

        AsyncHttpClientConfig httpClientConfig =
                new AsyncHttpClientConfig.Builder().setConnectTimeout(1000)
                        .setMaxConnections(100).build();
        AsyncHttpClient c = new AsyncHttpClient(httpClientConfig);
    }

    protected WebResource resourceFromUrl(String url) {
        return httpClient
                .resource("http://" + config.getTargetEndpoint() + url);
    }

    protected boolean parseBoolean(String bValue) {
        return "true".equals(bValue);
    }
}
