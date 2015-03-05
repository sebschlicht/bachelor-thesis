package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.bootstrap;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.write.RequestGenerator;
import de.uniko.sebschlicht.graphity.bootstrap.generate.MutableState;
import de.uniko.sebschlicht.socialnet.Subscription;
import de.uniko.sebschlicht.socialnet.requests.Request;
import de.uniko.sebschlicht.socialnet.requests.RequestFollow;
import de.uniko.sebschlicht.socialnet.requests.RequestPost;
import de.uniko.sebschlicht.socialnet.requests.RequestUnfollow;

public class BootstrapRequestGenerator extends RequestGenerator {

    public BootstrapRequestGenerator(
            String pathWikiDump,
            MutableState state,
            ClientConfiguration config) throws IOException {
        super(pathWikiDump, state, config);
        if (_requestComposition.getFeed() > 0) {
            throw new IllegalArgumentException(
                    "can not bootstrap feed requests!");
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
    @Override
    public Request nextRequest(RequestType type) {
        long idUser;
        Subscription subscription;

        try {
            switch (type) {
                case POST:
                    /*
                     * let random user post a fixed-length alphanumeric feed
                     */
                    idUser = nextUserId();
                    return new RequestPost(idUser, null);

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

    @Override
    protected RequestType nextRequestType() {
        float rt = RANDOM.nextFloat() * 100;
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
