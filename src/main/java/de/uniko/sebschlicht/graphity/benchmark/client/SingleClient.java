package de.uniko.sebschlicht.graphity.benchmark.client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;

import de.uniko.sebschlicht.graphity.benchmark.analyse.WikidumpInfo;
import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.api.RequestComposition;
import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;
import de.uniko.sebschlicht.graphity.benchmark.api.TargetType;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.BenchmarkClient;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.BenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.impl.Neo4jClient;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.impl.TitanClient;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFeed;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFollow;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestPost;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestUnfollow;
import de.uniko.sebschlicht.graphity.benchmark.master.MasterConfiguration;

public class SingleClient {

    public static final Logger LOG = Logger.getLogger(SingleClient.class);

    private static final String PATH_CONFIG =
            "src/main/resources/client-config.properties";

    private static final String PATH_WIKI_DUMP = "wikidump";

    private static final Random RANDOM = new Random();

    private static final Gson gson = new Gson();

    private ClientConfiguration config;

    private RequestComposition requestComposition;

    private TreeMap<Integer, List<Long>> propabilities;

    private Map<Long, List<Long>> subscriptions;

    private ThreadHandler threadHandler;

    public SingleClient() throws IOException {
        subscriptions = new HashMap<Long, List<Long>>();
        // load statistics
        FileReader dumpFileReader = new FileReader(PATH_WIKI_DUMP);
        WikidumpInfo dumpInfo =
                gson.fromJson(dumpFileReader, WikidumpInfo.class);
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
        propabilities = new TreeMap<Integer, List<Long>>();
        for (Entry<Integer, List<Long>> bucket : buckets.entrySet()) {
            crrProp += bucket.getKey() * bucket.getValue().size();
            propabilities.put(crrProp, new ArrayList<Long>(bucket.getValue()));
        }
    }

    public void start() {
        // load config
        MasterConfiguration baseConfig = new MasterConfiguration(PATH_CONFIG);
        if (!baseConfig.isLoaded()) {
            System.err.println("configuration file invalid");
            return;
        }
        requestComposition =
                new RequestComposition(baseConfig.request_feed,
                        baseConfig.request_follow, baseConfig.request_unfollow,
                        baseConfig.request_post);
        config =
                new ClientConfiguration(baseConfig.id_start, baseConfig.id_end,
                        baseConfig.feed_length, baseConfig.maxThroughput,
                        baseConfig.numThreads, requestComposition,
                        baseConfig.targetAddress, baseConfig.getTargetType(),
                        baseConfig.portNeo4j, baseConfig.portTitan);

        // create client threads tasks
        List<BenchmarkClientTask> threadTasks =
                new LinkedList<BenchmarkClientTask>();
        for (int i = 0; i < config.getNumThreads(); ++i) {
            BenchmarkClient benchmarkClient;
            if (config.getTargetType() == TargetType.NEO4J) {
                benchmarkClient = new Neo4jClient(config);
            } else {
                benchmarkClient = new TitanClient(config);
            }
            threadTasks.add(new BenchmarkClientTask(this, benchmarkClient));
        }
        System.out.println("will now attack " + baseConfig.targetAddress);

        // start client threads
        threadHandler = new ThreadHandler(threadTasks);
        threadHandler.start();
    }

    public boolean stop() {
        if (threadHandler != null) {
            threadHandler.stop();
            threadHandler = null;
            subscriptions.clear();
            System.out.println("client stopped.");
            return true;
        }
        return false;
    }

    public Request nextRequest() {
        RequestType type = nextRequestType();
        return nextRequest(type, 10);
    }

    private Request nextRequest(RequestType type, int numTry) {
        long idUser = nextUserId();
        List<Long> userSubscriptions;
        try {
            switch (type) {
                case FEED:
                    return new RequestFeed(idUser);

                case POST:
                    return new RequestPost(idUser,
                            RandomStringUtils.random(config.getFeedLength()));

                case FOLLOW:
                    int iBucket = RANDOM.nextInt(propabilities.lastKey());
                    Entry<Integer, List<Long>> entry =
                            propabilities.ceilingEntry(iBucket);
                    List<Long> bucket = entry.getValue();
                    long idFollowed = bucket.get(RANDOM.nextInt(bucket.size()));

                    userSubscriptions = subscriptions.get(idUser);
                    if (userSubscriptions == null) {
                        userSubscriptions = new LinkedList<Long>();
                        subscriptions.put(idUser, userSubscriptions);
                    }
                    userSubscriptions.add(idFollowed);

                    return new RequestFollow(idUser, idFollowed);

                case UNFOLLOW:
                    userSubscriptions = subscriptions.get(idUser);
                    if (userSubscriptions != null
                            && !userSubscriptions.isEmpty()) {
                        int iFollower =
                                RANDOM.nextInt(userSubscriptions.size());
                        long idUnsubscribe =
                                userSubscriptions.remove(iFollower);
                        return new RequestUnfollow(idUser, idUnsubscribe);
                    }
                    // temporarily not possible for this user
                    if (numTry > 0) {
                        return nextRequest(type, numTry - 1);
                    } else {
                        return nextRequest();
                    }
            }
            throw new IllegalStateException("unknown request type");
        } catch (Exception e) {
            System.err.println(e.getMessage() + ": " + type);
            e.printStackTrace();
            throw e;
        }
    }

    private long nextUserId() {
        return config.getIdStart()
                + RANDOM.nextInt((int) (config.getIdEnd() - config.getIdStart()) + 1);
    }

    private RequestType nextRequestType() {
        float rt = RANDOM.nextFloat() * 100;
        rt -= requestComposition.getFeed();
        if (rt < 0) {
            return RequestType.FEED;
        }
        rt -= requestComposition.getPost();
        if (rt < 0) {
            return RequestType.POST;
        }
        rt -= requestComposition.getFollow();
        if (rt < 0) {
            return RequestType.FOLLOW;
        }
        return RequestType.UNFOLLOW;
    }

    public static void main(String[] args) throws IOException {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(System.in));
        String cmd;

        System.out.println("starting client...");
        SingleClient client = new SingleClient();
        System.out.println("client ready.");

        while ((cmd = reader.readLine()) != null) {
            switch (cmd) {
                case "start":
                    client.start();
                    break;

                case "stop":
                    client.stop();
                    break;

                case "exit":
                    client.stop();
                    return;

                case "foo":
                    PrintWriter writer = new PrintWriter("/tmp/test-s");
                    for (long i = 0; i < 10000000; ++i) {
                        List<Long> bucket =
                                client.propabilities.higherEntry(
                                        RANDOM.nextInt(client.propabilities
                                                .lastKey())).getValue();
                        long id = bucket.get(RANDOM.nextInt(bucket.size()));
                        writer.println(id);
                    }
                    writer.close();
                    break;
            }
        }
    }
}
