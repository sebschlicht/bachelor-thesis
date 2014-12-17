package de.uniko.sebschlicht.graphity.benchmark.analyse;

import java.util.Map;

public class WikidumpInfo {

    private Map<Long, Integer> numFollowers;

    private long numTotalWrites;

    private long numFollow;

    private long numUnfollow;

    private long numPost;

    public WikidumpInfo() {
        // no-args constructor for JSON deserialization
    }

    public WikidumpInfo(
            Map<Long, Integer> numFollowers,
            long numTotalWrites,
            long numFollow,
            long numUnfollow,
            long numPost) {
        this.numFollowers = numFollowers;
        this.numTotalWrites = numTotalWrites;
        this.numFollow = numFollow;
        this.numUnfollow = numUnfollow;
        this.numPost = numPost;
    }

    public Map<Long, Integer> getNumFollowers() {
        return numFollowers;
    }

    public long getNumTotalWrites() {
        return numTotalWrites;
    }

    public long getNumFollow() {
        return numFollow;
    }

    public long getNumUnfollow() {
        return numUnfollow;
    }

    public long getNumPost() {
        return numPost;
    }
}
