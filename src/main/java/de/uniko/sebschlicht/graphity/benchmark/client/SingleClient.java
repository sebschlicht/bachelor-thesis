package de.uniko.sebschlicht.graphity.benchmark.client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;

import de.uniko.sebschlicht.graphity.benchmark.analyse.WikidumpInfo;
import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.api.RequestComposition;
import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;
import de.uniko.sebschlicht.graphity.benchmark.api.TargetType;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFeed;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFollow;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestPost;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestUnfollow;
import de.uniko.sebschlicht.graphity.benchmark.master.MasterConfiguration;

public class SingleClient {

    private static final String PATH_CONFIG =
            "src/main/resources/master-config.properties";

    private static final String PATH_WIKI_DUMP = "wikidump";

    private static final Random RANDOM = new Random();

    private static final Gson gson = new Gson();

    private ClientConfiguration config;

    private RequestComposition requestComposition;

    private WikidumpInfo dumpInfo;

    private long[] userIds;

    private Map<Long, List<Long>> subscriptions;

    private List<Measurement> measurements;

    private ExecutorService threadpool;

    public SingleClient(
            ClientConfiguration config) throws IOException {
        this.config = config;
        // load statistics
        FileReader dumpFileReader = new FileReader(PATH_WIKI_DUMP);
        dumpInfo = gson.fromJson(dumpFileReader, WikidumpInfo.class);
        dumpFileReader.close();
        threadpool = Executors.newFixedThreadPool(config.getNumThreads());
    }

    public void start() {
        // TODO
        List<BenchmarkClientTask> threadTasks =
                new LinkedList<BenchmarkClientTask>();
        for (int i = 0; i < config.getNumThreads(); ++i) {
            if (config.getTargetType() == TargetType.NEO4J) {

            } else {

            }
        }
    }

    public Request nextRequest() {
        long idUser = nextUserId();
        switch (nextRequestType()) {
            case FEED:
                return new RequestFeed(idUser);

            case POST:
                // TODO: generate message
                return new RequestPost(idUser, null);

            case FOLLOW:
                // TODO: generate idFollowed
                return new RequestFollow(idUser, 0);

            case UNFOLLOW:
                // TODO: generate idFollowed
                return new RequestUnfollow(idUser, 0);
        }
        throw new IllegalStateException("unknown request type");
    }

    private long nextUserId() {
        int i = RANDOM.nextInt(userIds.length);
        return userIds[i];
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

        while ((cmd = reader.readLine()) != null) {
            switch (cmd) {
                case "start":
                    // load config
                    MasterConfiguration config =
                            new MasterConfiguration(PATH_CONFIG);
                    if (!config.isLoaded()) {
                        System.err.println("configuration file invalid");
                        return;
                    }

                    break;
            }
        }
    }
}
