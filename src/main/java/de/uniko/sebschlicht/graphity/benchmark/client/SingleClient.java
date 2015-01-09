package de.uniko.sebschlicht.graphity.benchmark.client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.results.ResultManager;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFeed;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFollow;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestPost;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestUnfollow;
import de.uniko.sebschlicht.graphity.benchmark.master.MasterConfiguration;

public class SingleClient {

    public static final Logger LOG = LogManager.getLogger("requests-async");

    private static final String PATH_CONFIG =
            "src/main/resources/client-config.properties";

    private static final String PATH_WIKI_DUMP = "wikidump";

    private static final Random RANDOM = new Random();

    private static final Gson gson = new Gson();

    private ClientConfiguration config;

    private RequestComposition requestComposition;

    private TreeMap<Integer, List<Long>> propabilities;

    private Set<Subscription> subscriptions;

    private ThreadHandler threadHandler;

    private ResultManager resultManager;

    public SingleClient() throws IOException {
        subscriptions = new TreeSet<Subscription>();
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
                        baseConfig.getTargetType(), baseConfig.endpointNeo4j,
                        baseConfig.endpointTitan);

        // create client threads tasks
        List<BenchmarkClientTask> threadTasks =
                new LinkedList<BenchmarkClientTask>();
        resultManager = new ResultManager();
        for (int i = 0; i < config.getNumThreads(); ++i) {
            BenchmarkClient benchmarkClient;
            if (config.getTargetType() == TargetType.NEO4J) {
                benchmarkClient = new Neo4jClient(config);
            } else {
                benchmarkClient = new TitanClient(config);
            }
            threadTasks.add(new BenchmarkClientTask(this, resultManager,
                    benchmarkClient));
        }
        LOG.info("will now attack " + config.getTargetType() + " on "
                + config.getTargetEndpoint() + " with "
                + config.getNumThreads() + " client threads...");

        // start client threads
        threadHandler = new ThreadHandler(threadTasks);
        threadHandler.start();
        LOG.info("benchmark started at " + System.currentTimeMillis());
        resultManager.start();
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

    public synchronized Request nextRequest() {
        RequestType type = nextRequestType();
        return nextRequest(type);
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
    private Request nextRequest(RequestType type) {
        long idUser;
        Subscription subscription;

        try {
            switch (type) {
                case FEED:
                    /*
                     * retrieve news feed for random user
                     */
                    idUser = nextUserId();
                    return new RequestFeed(idUser);

                case POST:
                    /*
                     * let random user post a fixed-length alphanumeric feed
                     */
                    idUser = nextUserId();
                    int feedLength = config.getFeedLength();
                    String message =
                            RandomStringUtils.randomAlphanumeric(feedLength);
                    return new RequestPost(idUser, message);

                case FOLLOW:
                    /*
                     * let random user follow another user according to longtail
                     * distribution
                     */
                    idUser = nextUserId();
                    int iBucket = RANDOM.nextInt(propabilities.lastKey());
                    Entry<Integer, List<Long>> entry =
                            propabilities.ceilingEntry(iBucket);
                    List<Long> bucket = entry.getValue();
                    long idFollowed = bucket.get(RANDOM.nextInt(bucket.size()));

                    subscription = new Subscription(idUser, idFollowed);
                    subscriptions.add(subscription);

                    return new RequestFollow(idUser, idFollowed);

                case UNFOLLOW:
                    /*
                     * unsubcribe a random subscription created before
                     */
                    int numSubscriptions = subscriptions.size();
                    if (numSubscriptions == 0) {
                        // no subscriptions available, request not possible atm.
                        return nextRequest();
                    }
                    idUser = nextUserId();
                    // get random subscription
                    int iSubscription = RANDOM.nextInt(numSubscriptions);
                    Iterator<Subscription> iter = subscriptions.iterator();
                    for (int i = 0; i < iSubscription; i++) {
                        iter.next();
                    }
                    subscription = iter.next();
                    subscriptions.remove(subscription);
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
