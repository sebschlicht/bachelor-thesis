package de.uniko.sebschlicht.graphity.benchmark;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import de.metalcon.utils.Config;
import de.uniko.sebschlicht.graphity.benchmark.client.config.TargetType;

public class MasterConfiguration extends Config {

    private static final long serialVersionUID = -7526079851535857832L;

    public float request_feed;

    public float request_follow;

    public float request_unfollow;

    public float request_post;

    public int maxThroughput;

    public int numThreads;

    public String targetType;

    private TargetType _targetType;

    public String pathAddresses;

    private List<String> _addresses;

    public String baseNeo4j;

    public String baseTitan;

    public long id_start;

    public long id_end;

    public int feed_length;

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
            if (maxThroughput < 0) {
                throw new IllegalArgumentException(
                        "maximum throughput must be greater than zero or zero to indicate unlimited throughput");
            }
            if (numThreads <= 0) {
                throw new IllegalArgumentException(
                        "number of threads must be greater than zero");
            }
            _targetType = TargetType.fromString(targetType);
            _addresses = new LinkedList<String>();
            try {
                BufferedReader reader =
                        new BufferedReader(new FileReader(pathAddresses));
                String address;
                while ((address = reader.readLine()) != null) {
                    _addresses.add(address.trim());
                }
                reader.close();
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException(
                        "cluster addresses file was not found at \""
                                + pathAddresses + "\"");
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            if (id_start > id_end) {
                throw new IllegalArgumentException(
                        "lower ID range border (start ID) is higher than upper ID range border (end ID)");
            }
        }
    }

    public TargetType getTargetType() {
        return _targetType;
    }

    public List<String> getAddresses() {
        return _addresses;
    }

    public String getTargetBase() {
        if (_targetType == TargetType.NEO4J) {
            return baseNeo4j;
        } else {
            return baseTitan;
        }
    }
}
