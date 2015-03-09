package de.uniko.sebschlicht.graphity.benchmark.client;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uniko.sebschlicht.graphity.benchmark.MasterConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.client.bootstrap.BootstrapManager;
import de.uniko.sebschlicht.graphity.benchmark.client.bootstrap.BootstrapRequestGenerator;
import de.uniko.sebschlicht.graphity.benchmark.client.config.Configuration;
import de.uniko.sebschlicht.graphity.benchmark.client.config.RequestComposition;
import de.uniko.sebschlicht.graphity.benchmark.client.responses.ResultManager;
import de.uniko.sebschlicht.graphity.benchmark.client.write.RequestGenerator;
import de.uniko.sebschlicht.graphity.bootstrap.generate.MutableState;
import de.uniko.sebschlicht.socialnet.requests.Request;
import de.uniko.sebschlicht.socialnet.requests.RequestType;

public class AsyncClient {

    public static final Logger LOG = LogManager.getLogger("requests");

    private static final String PATH_CONFIG =
            "src/main/resources/client-config.properties";

    private static final String PATH_WIKI_DUMP = "wikidump";

    private Object _sync;

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
        _sync = new Object();
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
            System.out.println("loading bootstrap log...");
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
        LOG.info("benchmark started at " + System.currentTimeMillis());
        benchmarkClient.start(numRequests);
    }

    public void bootstrap(int numEntries) throws IOException {
        // load wikidump and create bootstrap request generator
        _requestGenerator =
                new BootstrapRequestGenerator(PATH_WIKI_DUMP,
                        new MutableState(), config);
        BootstrapManager.clearLog();

        System.out.println("generating " + numEntries + " requests...");
        final Queue<Request> entries = new LinkedList<Request>();
        for (int i = 0; i < numEntries; ++i) {
            entries.add(_requestGenerator.nextRequest());
        }
        BootstrapManager.addRequests(entries);
        System.out.println("request generation finished.");
    }

    public Request nextRequest() {
        synchronized (_sync) {
            return _requestGenerator.nextRequest();
        }
    }

    /**
     * Updates the social network state after a request was executed.
     * 
     * @param request
     *            request executed
     */
    public void handleResponse(Request request) {
        if (request.hasFailed() || request.getType() == RequestType.FEED) {
            return;
        }
        synchronized (_sync) {
            _requestGenerator.mergeRequest(request);
        }
    }

    private static void printUsage() {
        System.out
                .println("usage: AsyncClient [{bootstrap|start}] <numRequests> [<configPath>]");
    }

    private static boolean isNumber(String string) {
        return string.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    public static void main(String[] args) throws IOException {
        int i = 0;
        boolean bootstrap = false;
        if (args.length > i) {
            String type = args[i];
            if ("bootstrap".equals(type)) {
                bootstrap = true;
                i += 1;
            } else if ("start".equals(type)) {
                i += 1;
            } else {
                if (!isNumber(type)) {// illegal type identifier
                    printUsage();
                    return;
                }// is numRequests
            }
        }// optional

        int numRequests = 0;
        if (args.length > i) {
            numRequests = Integer.valueOf(args[i]);
            i += 1;
        } else {// mandatory
            printUsage();
            return;
        }

        String configPath;
        if (args.length > i) {
            configPath = args[i];
        } else {
            System.out.println("[INFO] loading default configuration");
            configPath = PATH_CONFIG;
        }
        AsyncClient client = new AsyncClient(configPath);
        if (bootstrap) {
            client.bootstrap(numRequests);
        } else {
            client.start(numRequests);
        }
    }
}
