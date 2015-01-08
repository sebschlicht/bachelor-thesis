package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.impl;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.client.SingleClient;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AbstractBenchmarkClient;

public class TitanClient extends AbstractBenchmarkClient {

    private static final String URL_EXTENSION = "/graphs/graph/graphity/";

    private static final String URL_FEED = "feeds/";

    private static final String URL_FOLLOW = "follow/";

    private static final String URL_POST = "post/";

    private static final String URL_UNFOLLOW = "unfollow/";

    public TitanClient(
            ClientConfiguration config) {
        super(config);
        resFeed = resourceFromUrl(URL_EXTENSION + URL_FEED);
        resFollow = resourceFromUrl(URL_EXTENSION + URL_FOLLOW);
        resPost = resourceFromUrl(URL_EXTENSION + URL_POST);
        resUnfollow = resourceFromUrl(URL_EXTENSION + URL_UNFOLLOW);
    }

    @Override
    public boolean retrieveNewsFeed(long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean subscribe(long idSubscriber, long idFollowed) {
        ClientResponse response = null;
        try {
            String jsonString =
                    "{\"following\":\"" + idSubscriber + "\",\"followed\":\""
                            + idFollowed + "\"}";
            response =
                    resFollow.accept(MediaType.APPLICATION_JSON)
                            .type(MediaType.APPLICATION_JSON)
                            .entity(jsonString).post(ClientResponse.class);
            ResponseBoolean responseFollow =
                    GSON.fromJson(response.getEntity(String.class),
                            ResponseBoolean.class);
            return responseFollow.getResponseValue() || true;
        } catch (ClientHandlerException e) {// connection failed
            SingleClient.LOG
                    .error("FOLLOW: client thread failed due to HTTP issue");
            SingleClient.LOG.error(e.getMessage());
            throw new IllegalStateException(
                    "FOLLOW: client thread failed due to HTTP issue");
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (ClientHandlerException e) {
                // ignore
            }
        }
        //return false;
    }

    @Override
    public boolean postStatusUpdate(long id, String message) {
        ClientResponse response = null;
        try {
            String jsonString =
                    "{\"author\":\"" + id + "\",\"message\":\"" + message
                            + "\"}";
            response =
                    resPost.accept(MediaType.APPLICATION_JSON)
                            .type(MediaType.APPLICATION_JSON)
                            .entity(jsonString).post(ClientResponse.class);
            ResponseLong responsePost =
                    GSON.fromJson(response.getEntity(String.class),
                            ResponseLong.class);
            return responsePost.getResponseValue() != 0;
        } catch (ClientHandlerException e) {// connection failed
            SingleClient.LOG
                    .error("POST: client thread failed due to HTTP issue");
            SingleClient.LOG.error(e.getMessage());
            throw new IllegalStateException(
                    "POST: client thread failed due to HTTP issue");
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (ClientHandlerException e) {
                // ignore
            }
        }
        //return false;
    }

    @Override
    public boolean unsubscribe(long idSubscriber, long idFollowed) {
        ClientResponse response = null;
        try {
            String jsonString =
                    "{\"following\":\"" + idSubscriber + "\",\"followed\":\""
                            + idFollowed + "\"}";
            response =
                    resUnfollow.accept(MediaType.APPLICATION_JSON)
                            .type(MediaType.APPLICATION_JSON)
                            .entity(jsonString).post(ClientResponse.class);
            ResponseBoolean responseUnfollow =
                    GSON.fromJson(response.getEntity(String.class),
                            ResponseBoolean.class);
            return responseUnfollow.getResponseValue() || true;
        } catch (ClientHandlerException e) {// connection failed
            SingleClient.LOG
                    .error("UNFOLLOW: client thread failed due to HTTP issue");
            SingleClient.LOG.error(e.getMessage());
            throw new IllegalStateException(
                    "UNFOLLOW: client thread failed due to HTTP issue");
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (ClientHandlerException e) {
                // ignore
            }
        }
        //return false;
    }
}
