package de.uniko.sebschlicht.graphity.benchmark.master;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import de.uniko.sebschlicht.graphity.benchmark.api.FrameworkUrls;
import de.uniko.sebschlicht.graphity.benchmark.master.servlets.DeregistrationServlet;
import de.uniko.sebschlicht.graphity.benchmark.master.servlets.RegistrationServlet;
import de.uniko.sebschlicht.graphity.benchmark.master.servlets.StartBenchmarkServlet;
import de.uniko.sebschlicht.graphity.benchmark.master.servlets.StopBenchmarkServlet;
import de.uniko.sebschlicht.graphity.benchmark.master.tasks.StartBenchmarkTask;

public class Master implements MasterListener {

    private static final String PATH_CONFIG =
            "src/main/resources/config.properties";

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
        Server server = new Server(8081);

        ServletContextHandler context =
                new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // start benchmark
        context.addServlet(
                new ServletHolder(new StartBenchmarkServlet(master)),
                FrameworkUrls.Master.URL_START);
        context.addFilter(new FilterHolder(new LocalityFilter()),
                FrameworkUrls.Master.URL_START,
                EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST));

        // stop benchmark
        context.addServlet(new ServletHolder(new StopBenchmarkServlet(master)),
                FrameworkUrls.Master.URL_STOP);
        context.addFilter(new FilterHolder(new LocalityFilter()),
                FrameworkUrls.Master.URL_STOP,
                EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST));

        // register clients
        context.addServlet(new ServletHolder(new RegistrationServlet(master)),
                FrameworkUrls.Master.URL_REGISTER);

        // deregister clients
        context.addServlet(
                new ServletHolder(new DeregistrationServlet(master)),
                FrameworkUrls.Master.URL_DEREGISTER);

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
        MasterConfiguration config = new MasterConfiguration(PATH_CONFIG);
        if (!config.isLoaded()) {
            return false;
        }
        int numClients = clients.size();
        int numThreadsPerClient = config.numThreads / numClients;
        int numThreadsTotal = numThreadsPerClient * numClients;

        List<Callable<Boolean>> tasksStart =
                new LinkedList<Callable<Boolean>>();
        for (ClientWrapper client : clients) {
            tasksStart.add(new StartBenchmarkTask(client));
        }

        return false;
    }

    @Override
    public void stopBenchmark() {
        // TODO Auto-generated method stub
    }
}
