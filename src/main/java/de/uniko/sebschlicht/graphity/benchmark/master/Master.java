package de.uniko.sebschlicht.graphity.benchmark.master;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.DispatcherType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.api.RequestComposition;
import de.uniko.sebschlicht.graphity.benchmark.api.http.Framework;
import de.uniko.sebschlicht.graphity.benchmark.master.servlets.DeregistrationServlet;
import de.uniko.sebschlicht.graphity.benchmark.master.servlets.RegistrationServlet;
import de.uniko.sebschlicht.graphity.benchmark.master.servlets.StartBenchmarkServlet;
import de.uniko.sebschlicht.graphity.benchmark.master.servlets.StopBenchmarkServlet;
import de.uniko.sebschlicht.graphity.benchmark.master.tasks.StartBenchmarkTask;
import de.uniko.sebschlicht.graphity.benchmark.master.tasks.StopBenchmarkTask;

public class Master implements MasterListener {

    private static final String PATH_CONFIG = "master-config.properties";

    protected static final Logger LOG = LogManager.getLogger(Master.class);

    private Set<ClientWrapper> clients;

    /**
     * thread pool used for client communication
     */
    private ExecutorService threadpool;

    public Master() {
        clients = new LinkedHashSet<ClientWrapper>();
    }

    public static void main(String[] args) throws Exception {
        // test configuration
        try {
            new MasterConfiguration(PATH_CONFIG);
            System.out.println("configuration is valid");
        } catch (IllegalArgumentException e) {
            System.err
                    .println("configuration is invalid, please correct it in order to start benchmarks:\n"
                            + e.getMessage());
        }

        Master master = new Master();
        Server server = new Server(Framework.Master.PORT);

        ServletContextHandler context =
                new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // start benchmark
        context.addServlet(
                new ServletHolder(new StartBenchmarkServlet(master)),
                Framework.Master.URL_START);
        context.addFilter(new FilterHolder(new LocalityFilter()),
                Framework.Master.URL_START,
                EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST));

        // stop benchmark
        context.addServlet(new ServletHolder(new StopBenchmarkServlet(master)),
                Framework.Master.URL_STOP);
        context.addFilter(new FilterHolder(new LocalityFilter()),
                Framework.Master.URL_STOP,
                EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST));

        // register clients
        context.addServlet(new ServletHolder(new RegistrationServlet(master)),
                Framework.Master.URL_REGISTER);

        // deregister clients
        context.addServlet(
                new ServletHolder(new DeregistrationServlet(master)),
                Framework.Master.URL_DEREGISTER);

        server.start();
        server.join();
    }

    public Set<ClientWrapper> getClients() {
        return clients;
    }

    @Override
    public void registerClient(String clientAddress) {
        clients.add(new ClientWrapper(clientAddress));
        System.out.println("client registered (" + clientAddress + ")");
    }

    @Override
    public void deregisterClient(String clientAddress) {
        clients.remove(new ClientWrapper(clientAddress));
        System.out.println("client deregistered (" + clientAddress + ")");
    }

    @Override
    public boolean startBenchmark() {
        // load master config and prepare client config
        MasterConfiguration config = new MasterConfiguration(PATH_CONFIG);
        if (!config.isLoaded()) {
            return false;
        }
        int numClients = clients.size();
        if (numClients == 0) {
            throw new IllegalStateException("no benchmark clients registered");
        }

        int maxThroughputPerClient = config.maxThroughput / numClients;
        RequestComposition requestComposition =
                new RequestComposition(config.request_feed,
                        config.request_follow, config.request_unfollow,
                        config.request_post);

        // create threadpool
        if (threadpool == null) {
            threadpool = Executors.newFixedThreadPool(numClients);
        }

        List<Callable<Boolean>> tasksStart =
                new LinkedList<Callable<Boolean>>();
        for (ClientWrapper client : clients) {
            // create benchmark start task
            ClientConfiguration clientConfig =
                    new ClientConfiguration(config.id_start, config.id_end,
                            config.feed_length, maxThroughputPerClient,
                            config.numThreads, requestComposition,
                            config.getTargetType(), config.endpointNeo4j,
                            config.endpointTitan);
            tasksStart.add(new StartBenchmarkTask(client, clientConfig));
        }
        LOG.debug("starting " + tasksStart.size() + " clients...");

        try {
            List<Future<Boolean>> taskResults =
                    threadpool.invokeAll(tasksStart);
            for (Future<Boolean> taskResult : taskResults) {
                try {
                    taskResult.get();
                } catch (ExecutionException e) {
                    e.getCause().printStackTrace();
                    return false;
                }
            }
            // all clients reached
            // TODO: start reporting status
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void stopBenchmark() {
        List<Callable<String>> tasksStop = new LinkedList<Callable<String>>();
        for (ClientWrapper client : clients) {
            tasksStop.add(new StopBenchmarkTask(client));
        }
        try {
            List<Future<String>> taskResults = threadpool.invokeAll(tasksStop);
            // collect client results
            List<String> results = new LinkedList<String>();
            for (Future<String> taskResult : taskResults) {
                try {
                    results.add(taskResult.get());
                } catch (ExecutionException e) {
                    e.getCause().printStackTrace();
                }
            }
            // TODO: merge single client results
            // TODO: log results
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // TODO: stop reporting status
    }
}
