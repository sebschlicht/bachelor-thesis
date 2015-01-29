package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.bootstrap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.Random;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uniko.sebschlicht.graphity.benchmark.client.Subscription;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFeed;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFollow;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestPost;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestUnfollow;

public class BootstrapManager {

    public static final Logger LOG = LogManager.getLogger("bootstrap");

    private static final Random RANDOM = new Random();

    private static ArrayList<BootstrapUser> USERS;

    public static void addRequests(Collection<Request> requests) {
        StringBuilder sRequest;
        boolean isFirst;
        for (Request request : requests) {
            isFirst = true;
            sRequest = new StringBuilder();
            for (String column : request.toStringArray()) {
                if (!isFirst) {
                    sRequest.append("\t");
                } else {
                    isFirst = false;
                }
                sRequest.append(column);
            }
            LOG.info(sRequest);
        }
    }

    public static void loadRequests(String filePath) throws IOException {
        BootstrapLoader loader = new BootstrapLoader(filePath);
        Queue<Request> requests = loader.loadRequests();
        System.out.println(requests.size() + " requests loaded from file.");
        mergeRequests(requests);
    }

    private static void mergeRequests(Collection<Request> requests) {
        System.out.println("merging requests...");
        TreeSet<Subscription> subscriptions = new TreeSet<Subscription>();

        /** we know that user id is in Integer range */
        // expand user array
        long highestId = 0;
        long userId = 0;
        for (Request request : requests) {
            switch (request.getType()) {
                case FOLLOW:
                    userId = ((RequestFollow) request).getIdSubscriber();
                    break;

                case POST:
                    userId = ((RequestPost) request).getId();
                    break;

                case UNFOLLOW:
                    userId = ((RequestUnfollow) request).getIdSubscriber();
                    break;
            }
            if (userId > highestId) {
                highestId = userId;
            }
        }
        int capacity = (int) (highestId / 0.75) + 1;
        if (USERS == null) {
            USERS = new ArrayList<BootstrapUser>(capacity);
        } else {
            USERS.ensureCapacity(capacity);
        }
        for (int i = USERS.size(); i < highestId; ++i) {// OMFG what a bug in usability
            USERS.add(null);
        }
        // load current subscription state
        BootstrapUser user;
        for (int i = 0; i < USERS.size(); ++i) {
            userId = i + 1;
            user = USERS.get(i);
            if (user != null && user.getSubscriptions() != null) {
                for (long idFollowed : user.getSubscriptions()) {
                    subscriptions.add(new Subscription(userId, idFollowed));
                }
            }

        }

        RequestFollow rfo;
        RequestPost rp;
        RequestUnfollow ru;

        Subscription subscription;
        for (Request request : requests) {
            switch (request.getType()) {
                case FOLLOW:
                    rfo = (RequestFollow) request;
                    subscription =
                            new Subscription(rfo.getIdSubscriber(),
                                    rfo.getIdFollowed());
                    subscriptions.add(subscription);
                    user = loadUserById(rfo.getIdSubscriber());
                    user.addStatusUpdate();
                    user = loadUserById(rfo.getIdFollowed());
                    user.addStatusUpdate();
                    break;

                case POST:
                    rp = (RequestPost) request;
                    user = loadUserById(rp.getId());
                    user.addStatusUpdate();
                    break;

                case UNFOLLOW:
                    ru = (RequestUnfollow) request;
                    subscription =
                            new Subscription(ru.getIdSubscriber(),
                                    ru.getIdFollowed());
                    subscriptions.remove(subscription);
                    user = loadUserById(ru.getIdSubscriber());
                    user.addStatusUpdate();
                    user = loadUserById(ru.getIdFollowed());
                    user.addStatusUpdate();
            }
        }
        // set new subscription state
        long idPrevious = 0;
        for (Subscription sub : subscriptions) {
            user = getUserById(sub.getIdSubscriber());
            if (user == null) {
                user = new BootstrapUser(sub.getIdSubscriber());
                USERS.set((int) sub.getIdSubscriber() - 1, user);
            }
            if (idPrevious != sub.getIdSubscriber()
                    && user.getSubscriptions() != null) {
                user.getSubscriptions().clear();
            }
            user.addSubscription(sub.getIdFollowed());
            idPrevious = sub.getIdSubscriber();
        }
    }

    private static BootstrapUser getUserById(long id) {
        return USERS.get((int) id - 1);
    }

    private static BootstrapUser loadUserById(long id) {
        BootstrapUser user = getUserById(id);
        if (user == null) {
            user = new BootstrapUser(id);
            USERS.set((int) id - 1, user);
        }
        return user;
    }

    public static RequestFeed getFeedRequest() {
        int iUser;
        BootstrapUser user;
        do {
            iUser = RANDOM.nextInt(USERS.size());
            user = USERS.get(iUser);
        } while (user == null);
        return new RequestFeed(iUser + 1);
    }

    public static int getFeedSize(long idReader) {
        BootstrapUser reader = getUserById(idReader);
        int feedSize = 0;
        BootstrapUser followed;
        if (reader.getSubscriptions() != null) {
            for (long idFollowed : reader.getSubscriptions()) {
                followed = getUserById(idFollowed);
                if (followed != null) {
                    feedSize += followed.getNumStatusUpdates();
                }
            }
        }
        return feedSize;
    }

    public static void main(String[] args) throws IOException {
        loadRequests("bootstrap.log");
        int maxFeedSize = 0;
        for (BootstrapUser user : USERS) {
            if (user != null && user.getNumStatusUpdates() > maxFeedSize) {
                maxFeedSize = user.getNumStatusUpdates();
            }
        }
        System.out.println("max feed length: " + maxFeedSize);
    }
}
