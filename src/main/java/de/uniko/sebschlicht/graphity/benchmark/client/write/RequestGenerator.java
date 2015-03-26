package de.uniko.sebschlicht.graphity.benchmark.client.write;

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
import java.util.TreeSet;

import org.apache.commons.lang3.RandomStringUtils;

import com.google.gson.Gson;

import de.uniko.sebschlicht.graphity.benchmark.analyse.WikidumpInfo;
import de.uniko.sebschlicht.graphity.benchmark.client.bootstrap.BootstrapManager;
import de.uniko.sebschlicht.graphity.benchmark.client.config.Configuration;
import de.uniko.sebschlicht.graphity.benchmark.client.config.RequestComposition;
import de.uniko.sebschlicht.graphity.bootstrap.generate.MutableState;
import de.uniko.sebschlicht.socialnet.Subscription;
import de.uniko.sebschlicht.socialnet.requests.Request;
import de.uniko.sebschlicht.socialnet.requests.RequestFollow;
import de.uniko.sebschlicht.socialnet.requests.RequestPost;
import de.uniko.sebschlicht.socialnet.requests.RequestType;
import de.uniko.sebschlicht.socialnet.requests.RequestUnfollow;
import de.uniko.sebschlicht.socialnet.requests.RequestUser;

public class RequestGenerator {

    protected static final Random RANDOM = new Random();

    protected static final Gson GSON = new Gson();

    protected TreeMap<Integer, List<Long>> _propabilities;

    protected MutableState _state;

    protected long _uId;

    protected int _numSkipsSubscriptionRemoval;

    protected Configuration _config;

    protected RequestComposition _requestComposition;

    protected Map<Long, User> _users;

    public RequestGenerator(
            String pathWikiDump,
            MutableState state,
            Configuration config) throws IOException {
        _state = state;
        _uId = 1;
        _numSkipsSubscriptionRemoval = 0;
        _config = config;
        _requestComposition = config.getRequestComposition();
        _users = new HashMap<>();
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
        User user;
        Subscription subscription;

        try {
            switch (type) {
                case FEED:
                    /*
                     * retrieve news feed for random existing user
                     */
                    return BootstrapManager.getFeedRequest();
                    /*
                     * idUser = getRandomUser();
                     * if (idUser == 0) {
                     * return nextRequest();
                     * }
                     * return new RequestFeed(idUser);
                     */

                case POST:
                    /*
                     * let random user post a fixed-length alphanumeric feed
                     */
                    idUser = getRandomUser();
                    if (idUser == 0) {
                        return nextRequest();
                    }

                    int feedLength = _config.getFeedLength();
                    String message =
                            RandomStringUtils.randomAlphanumeric(feedLength);
                    return new RequestPost(idUser, message);

                case FOLLOW:
                    /*
                     * let random user follow another user according to longtail
                     * distribution
                     */
                    if (_uId - 1 < 2) {
                        return nextRequest(RequestType.USER);
                    }
                    idUser = getRandomUser();
                    long idFollowed = getFollowedUserExisting();
                    int numSkips = 0;
                    do {
                        idUser = getRandomUser();
                        user = getUser(idUser);
                        if (numSkips > 1000) {// we need more users
                            return nextRequest(RequestType.USER);
                        }
                        numSkips += 1;
                    } while (idUser == idFollowed
                            || user.hasSubscription(idFollowed));
                    // we update the state when request was fired, may throw exceptions if server too slow
                    user.addSubscription(idFollowed);
                    _state.addSubscription(new Subscription(idUser, idFollowed));
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
                    user = getUser(subscription.getIdSubscriber());
                    user.removeSubscription(subscription.getIdFollowed());
                    _state.removeSubscription(subscription);
                    return new RequestUnfollow(subscription.getIdSubscriber(),
                            subscription.getIdFollowed());

                case USER:
                    /*
                     * create a user
                     */
                    _state.addUser(_uId);
                    _uId += 1;
                    return new RequestUser(_uId - 1);
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

    public void mergeRequest(Request request) {
        _state.mergeRequest(request, false);
    }

    protected long getRandomUser() {
        if (_uId == 1) {
            return 0;
        }
        return RANDOM.nextInt((int) _uId - 1) + 1;
    }

    protected long getFollowedUser() {
        int iBucket = RANDOM.nextInt(_propabilities.lastKey());
        Entry<Integer, List<Long>> entry = _propabilities.ceilingEntry(iBucket);
        List<Long> bucket = entry.getValue();
        return bucket.get(RANDOM.nextInt(bucket.size()));
    }

    protected long getFollowedUserExisting() {
        long id;
        do {
            id = getFollowedUser();
        } while (id > _uId - 1);
        return id;
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
        rt -= _requestComposition.getUnfollow();
        if (rt < 0) {
            return RequestType.UNFOLLOW;
        }
        return RequestType.USER;
    }

    protected User getUser(long id) {
        return _users.get(id);
    }

    protected class User {

        private TreeSet<Long> _subscriptions;

        public User() {
            _subscriptions = new TreeSet<>();
        }

        public boolean addSubscription(long idFollowed) {
            return _subscriptions.add(idFollowed);
        }

        public boolean removeSubscription(long idFollowed) {
            return _subscriptions.remove(idFollowed);
        }

        public TreeSet<Long> getSubscriptions() {
            return _subscriptions;
        }

        public boolean hasSubscription(long idFollowed) {
            return _subscriptions.contains(idFollowed);
        }
    }
}
