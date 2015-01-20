package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.impl;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.api.TargetType;
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
    }//2765

    @Override
    public int retrieveNewsFeed(long id) {
        ClientResponse response = null;
        try {
            String jsonString = "{\"reader\":\"" + id + "\"}";
            response =
                    resFeed.accept(MediaType.APPLICATION_JSON)
                            .type(MediaType.APPLICATION_JSON)
                            .entity(jsonString).post(ClientResponse.class);
            //TODO: handle result
            String sResponse = response.getEntity(String.class);
            System.out.println(sResponse);
            ResponseList responseFeeds =
                    GSON.fromJson(sResponse, ResponseList.class);
            return responseFeeds.getResponseValue().size();
        } catch (ClientHandlerException e) {// connection failed
            SingleClient.LOG
                    .error("FEED: client thread failed due to HTTP issue");
            SingleClient.LOG.error(e.getMessage());
            throw new IllegalStateException(
                    "FEED: client thread failed due to HTTP issue");
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (ClientHandlerException e) {
                // ignore
            }
        }
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
            String sResponse = response.getEntity(String.class);
            ResponseBoolean responseFollow =
                    GSON.fromJson(sResponse, ResponseBoolean.class);
            return responseFollow.getResponseValue();
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
            String sResponse = response.getEntity(String.class);
            ResponseLong responsePost =
                    GSON.fromJson(sResponse, ResponseLong.class);
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
            String sResponse = response.getEntity(String.class);
            ResponseBoolean responseUnfollow =
                    GSON.fromJson(sResponse, ResponseBoolean.class);
            return responseUnfollow.getResponseValue();
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

    public static void main(String[] args) {
        boolean exit = false;
        String test = "{\"error\":null}";
        ResponseBoolean responseUnfollow =
                GSON.fromJson(test, ResponseBoolean.class);
        System.out.println(responseUnfollow.getResponseValue());
        if (exit) {
            return;
        }
        ;

        ClientConfiguration config =
                new ClientConfiguration(0, 0, 0, 0, 0, null, TargetType.TITAN,
                        null, "141.26.208.4:82/titan");
        TitanClient c = new TitanClient(config);
        //        System.out.println(c.postStatusUpdate(1000, "blargh"));
        for (int i = 0; i < 40000; ++i) {
            int numFeeds = c.retrieveNewsFeed(i);
            if (numFeeds > 0) {
                System.out.println(i + ": " + numFeeds);
            }
        }
        System.out.println(c.retrieveNewsFeed(4));
        //        System.out.println(c.subscribe(1001, 1000));
        //        System.out.println(c.postStatusUpdate(1000, "blurgh"));
        //        System.out.println(c.subscribe(1001, 1000));
        //        System.out.println(c.retrieveNewsFeed(1001));
        //        System.out.println(c.unsubscribe(1001, 1000));
        //        System.out.println(c.postStatusUpdate(1000, "blergh"));
        //        System.out.println(c.retrieveNewsFeed(1001));
    }
}
