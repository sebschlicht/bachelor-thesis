package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.write;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.lang3.RandomStringUtils;

import com.google.gson.Gson;

import de.uniko.sebschlicht.graphity.benchmark.analyse.WikidumpInfo;
import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.api.RequestComposition;
import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.bootstrap.BootstrapManager;
import de.uniko.sebschlicht.graphity.bootstrap.generate.MutableState;
import de.uniko.sebschlicht.socialnet.Subscription;
import de.uniko.sebschlicht.socialnet.requests.Request;
import de.uniko.sebschlicht.socialnet.requests.RequestFollow;
import de.uniko.sebschlicht.socialnet.requests.RequestPost;
import de.uniko.sebschlicht.socialnet.requests.RequestUnfollow;

public class RequestGenerator {

    protected static final Random RANDOM = new Random();

    protected static final Gson GSON = new Gson();

    protected TreeMap<Integer, List<Long>> _propabilities;

    protected MutableState _state;

    protected int _numSkipsSubscriptionRemoval;

    protected ClientConfiguration _config;

    protected RequestComposition _requestComposition;

    public RequestGenerator(
            String pathWikiDump,
            MutableState state,
            ClientConfiguration config) throws IOException {
        _state = state;
        _numSkipsSubscriptionRemoval = 0;
        _config = config;
        _requestComposition = config.getRequestComposition();
        loadWikiDump(pathWikiDump);
    }

    protected void loadWikiDump(String pathWikiDump) throws IOException {
        // load statistics
        FileReader dumpFileReader = new FileReader(pathWikiDump);
        WikidumpInfo dumpInfo =
                GSON.fromJson(dumpFileReader, WikidumpInfo.class);
        dumpFileReader.close();

        // create non-empty buckets
        Map<Integer, List<Long>> buckets = new HashMap<Integer, List<Long>>();
        for (Entry<Long, Integer> numFollowers : dumpInfo.getNumFollowers()
                .entrySet()) {
            if (numFollowers.getValue() == 0) {
                continue;
            }
            List<Long> bucket = buckets.get(numFollowers.getValue());
            if (bucket == null) {
                bucket = new LinkedList<Long>();
                buckets.put(numFollowers.getValue(), bucket);
            }
            bucket.add(numFollowers.getKey());
        }

        // fill buckets into sorted map
        int crrProp = 0;
        _propabilities = new TreeMap<Integer, List<Long>>();
        for (Entry<Integer, List<Long>> bucket : buckets.entrySet()) {
            crrProp += bucket.getKey() * bucket.getValue().size();
            _propabilities.put(crrProp, new ArrayList<Long>(bucket.getValue()));
        }
    }

    /**
     * CURRENT APPROACH:
     * 1. choose user (random)
     * 2. choose next action
     * * if POST: post fixed-length String
     * * if FOLLOW: subscribe to a user (longtail)
     * * if UNFOLLOW: unsubscribe a subscription (random) of this user
     * NEW APPROACH to prevent UNFOLLOW from failing:
     * 1. choose action
     * * if POST: choose user (random), post fixed-length String
     * * if FOLLOW: choose user (random), subscribe to a user (longtail)
     * * if UNFOLLOW: unsubscribe a subscription (random)
     */
    public Request nextRequest(RequestType type) {
        long idUser;
        Subscription subscription;

        try {
            switch (type) {
                case FEED:
                    /*
                     * retrieve news feed for random existing user
                     */
                    return BootstrapManager.getFeedRequest();

                case POST:
                    /*
                     * let random user post a fixed-length alphanumeric feed
                     */
                    idUser = nextUserId();
                    int feedLength = _config.getFeedLength();
                    String message =
                            RandomStringUtils.randomAlphanumeric(feedLength);
                    return new RequestPost(idUser, message);

                case FOLLOW:
                    /*
                     * let random user follow another user according to longtail
                     * distribution
                     */
                    idUser = nextUserId();
                    int iBucket = RANDOM.nextInt(_propabilities.lastKey());
                    Entry<Integer, List<Long>> entry =
                            _propabilities.ceilingEntry(iBucket);
                    List<Long> bucket = entry.getValue();
                    long idFollowed = bucket.get(RANDOM.nextInt(bucket.size()));

                    subscription = new Subscription(idUser, idFollowed);
                    _state.addSubscription(subscription);
                    return new RequestFollow(idUser, idFollowed);

                case UNFOLLOW:
                    /*
                     * unsubcribe a random subscription created before
                     */
                    int numSubscriptions = _state.getSubscriptions().size();
                    if (numSubscriptions == 0) {
                        // no subscriptions available, request not possible atm.
                        _numSkipsSubscriptionRemoval++;
                        return nextRequest();
                    }
                    // get oldest subscription (-> number of status updates per FEED grows slowly)
                    int iSubscription = 0;//RANDOM.nextInt(numSubscriptions);
                    Iterator<Subscription> iter =
                            _state.getSubscriptions().iterator();
                    for (int i = 0; i < iSubscription; i++) {
                        iter.next();
                    }
                    subscription = iter.next();
                    _state.removeSubscription(subscription);
                    return new RequestUnfollow(subscription.getIdSubscriber(),
                            subscription.getIdFollowed());
            }
            throw new IllegalStateException("unknown request type");
        } catch (Exception e) {
            System.err.println(e.getMessage() + ": " + type);
            e.printStackTrace();
            throw e;
        }
    }

    public Request nextRequest() {
        RequestType type = nextRequestType();
        return nextRequest(type);
    }

    protected long nextUserId() {
        return _config.getIdStart()
                + RANDOM.nextInt((int) (_config.getIdEnd() - _config
                        .getIdStart()) + 1);
    }

    protected RequestType nextRequestType() {
        float rt = RANDOM.nextFloat() * 100;
        rt -= _requestComposition.getFeed();
        if (rt < 0) {
            return RequestType.FEED;
        }
        rt -= _requestComposition.getPost();
        if (rt < 0) {
            return RequestType.POST;
        }
        rt -= _requestComposition.getFollow();
        if (rt < 0) {
            return RequestType.FOLLOW;
        }
        return RequestType.UNFOLLOW;
    }
}
