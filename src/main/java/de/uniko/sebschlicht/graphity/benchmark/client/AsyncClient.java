package de.uniko.sebschlicht.graphity.benchmark.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.api.RequestComposition;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.AsyncBenchmarkClientTask;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.bootstrap.BootstrapManager;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.bootstrap.BootstrapRequestGenerator;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response.BootstrapRequestHandler;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.results.ResultManager;
import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.write.RequestGenerator;
import de.uniko.sebschlicht.graphity.benchmark.master.MasterConfiguration;
import de.uniko.sebschlicht.graphity.bootstrap.generate.MutableState;
import de.uniko.sebschlicht.socialnet.requests.Request;

public class AsyncClient {

    public static final Logger LOG = LogManager.getLogger("requests");

    private static final String PATH_CONFIG =
            "src/main/resources/client-config.properties";

    private static final String PATH_WIKI_DUMP = "wikidump";

    private ClientConfiguration config;

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
                new ClientConfiguration(baseConfig.id_start, baseConfig.id_end,
                        baseConfig.feed_length, baseConfig.maxThroughput,
                        baseConfig.numThreads, requestComposition,
                        baseConfig.getAddresses(), baseConfig.getTargetType(),
                        baseConfig.getTargetBase());
    }

    public void start() throws IOException {
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
        benchmarkClient.start();
        LOG.info("benchmark started at " + System.currentTimeMillis());
    }

    public boolean stop() {
        if (benchmarkClient != null) {
            benchmarkClient.stop();
            benchmarkClient = null;
            System.out.println("client stopped.");
            return true;
        }
        return false;
    }

    public void bootstrap(int numEntries) throws IOException {
        // load wikidump and create bootstrap request generator
        _requestGenerator =
                new BootstrapRequestGenerator(PATH_WIKI_DUMP,
                        new MutableState(), config);

        final Queue<Request> entries = new LinkedList<Request>();
        for (int i = 0; i < numEntries; ++i) {
            entries.add(_requestGenerator.nextRequest());
        }
        MutableState state = new MutableState();
        state.setRequests(entries, true);

        resultManager = new ResultManager();
        benchmarkClient =
                new AsyncBenchmarkClientTask(this, resultManager, config);
        BootstrapRequestHandler requestHandler =
                new BootstrapRequestHandler(benchmarkClient,
                        config.getTargetType(), state);
        System.out.println("will now bootstrap against "
                + config.getAddresses().get(0));
        requestHandler.bootstrap();
    }

    public synchronized Request nextRequest() {
        return _requestGenerator.nextRequest();
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
            System.out.println("[INFO] loading default configuration");
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
