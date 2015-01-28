package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.bootstrap;

import java.util.ArrayList;
import java.util.Random;

public class BootstrapUser {

    private static final Random RANDOM = new Random();

    private final long _id;

    private ArrayList<Long> _subscriptions;

    private int _numStatusUpdates;

    public BootstrapUser(
            long id) {
        _id = id;
    }

    public long getId() {
        return _id;
    }

    public void addSubscription(long idFollowed) {
        if (_subscriptions == null) {
            _subscriptions = new ArrayList<Long>(3);
        }
        _subscriptions.add(idFollowed);
    }

    public ArrayList<Long> getSubscriptions() {
        return _subscriptions;
    }

    public long getRandomSubscription() {
        return _subscriptions.get(RANDOM.nextInt(_subscriptions.size()));
    }

    public void addStatusUpdate() {
        _numStatusUpdates += 1;
    }

    public int getNumStatusUpdates() {
        return _numStatusUpdates;
    }
}
