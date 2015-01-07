package de.uniko.sebschlicht.graphity.benchmark.client;

public class Subscription implements Comparable<Subscription> {

    protected long idSubscriber;

    protected long idFollowed;

    public Subscription(
            long idSubscriber,
            long idFollowed) {
        this.idSubscriber = idSubscriber;
        this.idFollowed = idFollowed;
    }

    public long getIdSubscriber() {
        return idSubscriber;
    }

    public long getIdFollowed() {
        return idFollowed;
    }

    @Override
    public int compareTo(Subscription s) {
        if (getIdSubscriber() > s.getIdSubscriber()) {
            return 1;
        } else if (getIdSubscriber() < s.getIdSubscriber()) {
            return -1;
        } else {
            if (getIdFollowed() > s.getIdFollowed()) {
                return 1;
            } else if (getIdFollowed() < s.getIdFollowed()) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
