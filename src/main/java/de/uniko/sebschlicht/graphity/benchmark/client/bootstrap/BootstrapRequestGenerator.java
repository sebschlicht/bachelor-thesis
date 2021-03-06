package de.uniko.sebschlicht.graphity.benchmark.client.bootstrap;

import java.io.IOException;
import java.util.Iterator;

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

    protected long _uId;

    public BootstrapRequestGenerator(
            String pathWikiDump,
            MutableState state,
            Configuration config) throws IOException {
        super(pathWikiDump, state, config);
        if (_requestComposition.getFeed() > 0) {
            throw new IllegalArgumentException(
                    "can not bootstrap feed requests!");
        }
        _uId = 0;
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
        User user;
        Subscription subscription;

        try {
            switch (type) {
                case POST:
                    /*
                     * let random user post a fixed-length alphanumeric feed
                     */
                    if (_uId < 1) {
                        return nextRequest(RequestType.USER);
                    }
                    idUser = getRandomExistingUser();
                    return new RequestPost(idUser, null);

                case FOLLOW:
                    /*
                     * let random user follow another user according to longtail
                     * distribution
                     */
                    if (_uId < 2) {
                        return nextRequest(RequestType.USER);
                    }
                    //FIXME implement this in RequestGenerator if working
                    long idFollowed = getExistingUserToFollow();
                    int numSkips = 0;
                    do {
                        idUser = getRandomExistingUser();
                        user = getUser(idUser);
                        if (numSkips > 1000) {// we need more users
                            return nextRequest(RequestType.USER);
                        }
                        numSkips += 1;
                    } while (idUser == idFollowed
                            || !user.addSubscription(idFollowed));
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
                    _users.put(_uId, new User(_uId));
                    return new RequestUser(_uId++);
            }
            throw new IllegalStateException("unknown request type");
        } catch (Exception e) {
            System.err.println(e.getMessage() + ": " + type);
            e.printStackTrace();
            throw e;
        }
    }

    protected long getRandomExistingUser() {
        if (_uId == 0) {
            return 0;
        }
        return RANDOM.nextInt((int) _uId) + 1;
    }

    @Override
    protected long getExistingUserToFollow() {
        /*
         * user identifiers are from 1 to x where x is the number of existing
         * users
         */
        long id;
        do {
            id = getUserToFollow();
        } while (id > _uId - 1);
        return id;
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
