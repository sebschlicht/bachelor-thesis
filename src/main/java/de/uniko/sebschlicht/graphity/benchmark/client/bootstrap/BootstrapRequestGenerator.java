package de.uniko.sebschlicht.graphity.benchmark.client.bootstrap;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import de.uniko.sebschlicht.graphity.benchmark.client.config.Configuration;
import de.uniko.sebschlicht.graphity.benchmark.client.write.RequestGenerator;
import de.uniko.sebschlicht.graphity.bootstrap.generate.MutableState;
import de.uniko.sebschlicht.socialnet.Subscription;
import de.uniko.sebschlicht.socialnet.requests.Request;
import de.uniko.sebschlicht.socialnet.requests.RequestFollow;
import de.uniko.sebschlicht.socialnet.requests.RequestPost;
import de.uniko.sebschlicht.socialnet.requests.RequestType;
import de.uniko.sebschlicht.socialnet.requests.RequestUnfollow;
import de.uniko.sebschlicht.socialnet.requests.RequestUser;

public class BootstrapRequestGenerator extends RequestGenerator {

    public BootstrapRequestGenerator(
            String pathWikiDump,
            MutableState state,
            Configuration config) throws IOException {
        super(pathWikiDump, state, config);
        if (_requestComposition.getFeed() > 0) {
            throw new IllegalArgumentException(
                    "can not bootstrap feed requests!");
        }
        System.out.println(_requestComposition.getUser());
        System.out.println(_requestComposition.getFollow());
        System.out.println(_requestComposition.getPost());
        System.out.println(_requestComposition.getUnfollow());
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
                    idUser = getRandomUser();
                    if (idUser == 0) {
                        return nextRequest();
                    }
                    return new RequestPost(idUser, null);

                case FOLLOW:
                    /*
                     * let random user follow another user according to longtail
                     * distribution
                     */
                    idUser = getRandomUser();
                    if (idUser == 0) {
                        return nextRequest();
                    }
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
                        if (_numSkipsSubscriptionRemoval % 1000 == 0) {
                            System.out.println(_numSkipsSubscriptionRemoval
                                    + " U skipped!");
                        }
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

                case USER:
                    /*
                     * create a user
                     */
                    _state.addUser(_uId);
                    return new RequestUser(_uId++);
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
        rt -= _requestComposition.getUnfollow();
        if (rt < 0) {
            return RequestType.UNFOLLOW;
        }
        return RequestType.USER;
    }
}
