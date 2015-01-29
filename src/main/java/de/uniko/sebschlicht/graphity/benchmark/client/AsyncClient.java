package de.uniko.sebschlicht.graphity.benchmark.client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
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
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.bootstrap.BootstrapManager;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response.BootstrapRequestHandler;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.results.ResultManager;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFollow;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestPost;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestUnfollow;
import de.uniko.sebschlicht.graphity.benchmark.master.MasterConfiguration;

public class AsyncClient {

    public static final Logger LOG = LogManager.getLogger("requests");

    private static final String PATH_CONFIG =
            "src/main/resources/client-config.properties";

    private static final String PATH_WIKI_DUMP = "wikidump";

    private static final Random RANDOM = new Random();

    private static final Gson gson = new Gson();

    private ClientConfiguration config;

    private RequestComposition requestComposition;

    private TreeMap<Integer, List<Long>> propabilities;

    private Set<Subscription> subscriptions;

    private ResultManager resultManager;

    private AsyncBenchmarkClientTask benchmarkClient;

    public AsyncClient(
            String configPath) throws IOException {
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

        // load config
        MasterConfiguration baseConfig = new MasterConfiguration(configPath);
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
                        baseConfig.getAddresses(), baseConfig.getTargetType(),
                        baseConfig.getTargetBase());

        // load bootstrap manager if necessary
        if (requestComposition.getFeed() > 0) {
            BootstrapManager.loadRequests("bootstrap.log");
        }
    }

    public void start() {
        // spawn client thread
        resultManager = new ResultManager();
        benchmarkClient =
                new AsyncBenchmarkClientTask(this, resultManager, config);

        StringBuilder logMessageStarted = new StringBuilder();
        logMessageStarted.append("will now attack ");
        logMessageStarted.append(config.getTargetType().toString());
        logMessageStarted.append(" on ");
        int numAddresses = config.getAddresses().size();
        if (numAddresses > 1) {
            logMessageStarted.append("cluster [ ");
            int i = 0;
            for (String address : config.getAddresses()) {
                logMessageStarted.append("'");
                logMessageStarted.append(address);
                logMessageStarted.append("'");
                if (i < numAddresses - 1) {
                    logMessageStarted.append(",");
                }
                i += 1;
            }
            logMessageStarted.append(" ]");
        } else {
            logMessageStarted.append(config.getAddresses().get(0));
        }
        logMessageStarted.append(" with ");
        logMessageStarted.append(config.getNumThreads());
        logMessageStarted.append(" client threads.");
        LOG.info(logMessageStarted.toString());
        benchmarkClient.start();
        LOG.info("benchmark started at " + System.currentTimeMillis());
    }

    public boolean stop() {
        if (benchmarkClient != null) {
            benchmarkClient.stop();
            benchmarkClient = null;
            subscriptions.clear();
            System.out.println("client stopped.");
            return true;
        }
        return false;
    }

    public void bootstrap(int numEntries) {
        final Queue<Request> entries = new LinkedList<Request>();
        for (int i = 0; i < numEntries; ++i) {
            entries.add(nextRequest(nextRequestType()));
        }
        resultManager = new ResultManager();
        benchmarkClient =
                new AsyncBenchmarkClientTask(this, resultManager, config);
        BootstrapRequestHandler requestHandler =
                new BootstrapRequestHandler(benchmarkClient, entries, 100000);
        System.out.println("will not bootstrap against "
                + config.getAddresses().get(0));
        requestHandler.startBootstrap();
    }

    public synchronized Request nextRequest() {
        RequestType type = nextRequestType();
        return nextRequest(type);
    }

    private long numRetries = 0;

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
                     * retrieve news feed for random existing user
                     */
                    return BootstrapManager.getFeedRequest();

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
                        numRetries++;
                        return nextRequest();
                    }
                    // get oldest subscription (-> number of status updates per FEED grows slowly)
                    int iSubscription = 0;//RANDOM.nextInt(numSubscriptions);
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
        String[] cmdArgs;

        System.out.println("starting async client...");
        String configPath;
        if (args.length > 0) {
            configPath = args[0];
        } else {
            configPath = PATH_CONFIG;
        }
        AsyncClient client = new AsyncClient(configPath);
        System.out.println("client ready.");

        while ((cmd = reader.readLine()) != null) {
            cmdArgs = cmd.split(" ");
            cmd = cmdArgs[0];
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

                case "bootstrap":
                    if (cmdArgs.length > 1) {
                        int numEntries = Integer.valueOf(cmdArgs[1]);
                        client.bootstrap(numEntries);
                    } else {
                        System.err.println("usage: bootstrap <numEntries>");
                    }
                    break;
            }
        }
    }
}
