package de.uniko.sebschlicht.graphity.benchmark.master;

import de.metalcon.utils.Config;

public class MasterConfiguration extends Config {

    private static final long serialVersionUID = -7526079851535857832L;

    public float request_feed;

    public float request_follow;

    public float request_unfollow;

    public float request_post;

    public int maxThroughput;

    public int numThreads;

    public String targetAddress;

    public MasterConfiguration(
            String configPath) {
        super(configPath);

        if (isLoaded()) {
            if (request_feed < 0 || request_follow < 0 || request_unfollow < 0
                    || request_post < 0) {
                throw new IllegalArgumentException(
                        "request composition: percentages can not be less than zero");
            }
            if (request_feed + request_follow + request_unfollow + request_post != 100.0) {
                throw new IllegalArgumentException(
                        "request composition: percentages must add to 100.0");
            }
            if (maxThroughput <= 0) {
                throw new IllegalArgumentException(
                        "maximum throughput must be greater than zero");
            }
            if (numThreads <= 0) {
                throw new IllegalArgumentException(
                        "number of threads must be greater than zero");
            }
            if (targetAddress.equals("")) {
                throw new IllegalArgumentException(
                        "target cluster address has to specified");
            }
        }
    }
}
