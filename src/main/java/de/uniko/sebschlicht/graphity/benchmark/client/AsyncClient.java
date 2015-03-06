package de.uniko.sebschlicht.graphity.benchmark.client;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uniko.sebschlicht.graphity.benchmark.MasterConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.client.bootstrap.BootstrapManager;
import de.uniko.sebschlicht.graphity.benchmark.client.config.Configuration;
import de.uniko.sebschlicht.graphity.benchmark.client.config.RequestComposition;
import de.uniko.sebschlicht.graphity.benchmark.client.results.ResultManager;
import de.uniko.sebschlicht.graphity.benchmark.client.write.RequestGenerator;
import de.uniko.sebschlicht.graphity.bootstrap.generate.MutableState;
import de.uniko.sebschlicht.socialnet.requests.Request;

public class AsyncClient {

    public static final Logger LOG = LogManager.getLogger("requests");

    private static final String PATH_CONFIG =
            "src/main/resources/client-config.properties";

    private static final String PATH_WIKI_DUMP = "wikidump";

    private Configuration config;

    private RequestComposition requestComposition;

    private RequestGenerator _requestGenerator;

    private ResultManager resultManager;

    private AsyncBenchmarkClientTask benchmarkClient;

    public AsyncClient(
            String configPath) throws IOException {
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
                new Configuration(baseConfig.id_start, baseConfig.id_end,
                        baseConfig.feed_length, baseConfig.maxThroughput,
                        baseConfig.numThreads, requestComposition,
                        baseConfig.getAddresses(), baseConfig.getTargetType(),
                        baseConfig.getTargetBase());
    }

    public void start(int numRequests) throws IOException {
        // load wikidump and create request generator
        _requestGenerator =
                new RequestGenerator(PATH_WIKI_DUMP, new MutableState(), config);
        // load bootstrap manager if necessary
        if (requestComposition.getFeed() > 0) {
            BootstrapManager.loadRequests("bootstrap.log");
        }

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
        benchmarkClient.start(numRequests);
        LOG.info("benchmark started at " + System.currentTimeMillis());
    }

    public synchronized Request nextRequest() {
        return _requestGenerator.nextRequest();
    }

    private static void printUsage() {
        System.out
                .println("usage: AsyncClient {bootstrap|start} <numRequests> [<configPath>]");
    }

    public static void main(String[] args) throws IOException {
        int numRequests = 0;
        if (args.length > 0) {
            numRequests = Integer.valueOf(args[0]);
        } else {
            printUsage();
        }

        System.out.println("starting async client...");
        String configPath;
        if (args.length > 1) {
            configPath = args[1];
        } else {
            System.out.println("[INFO] loading default configuration");
            configPath = PATH_CONFIG;
        }
        AsyncClient client = new AsyncClient(configPath);
        client.start(numRequests);
    }
}
