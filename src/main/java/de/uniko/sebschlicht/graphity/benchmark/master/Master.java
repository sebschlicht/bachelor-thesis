package de.uniko.sebschlicht.graphity.benchmark.master;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import de.uniko.sebschlicht.graphity.benchmark.master.servlets.DeregistrationServlet;
import de.uniko.sebschlicht.graphity.benchmark.master.servlets.RegistrationServlet;

public class Master implements MasterListener {

    private static final String PATH_CONFIG =
            "src/main/resources/config.properties";

    private Set<String> clients;

    public Master() {
        clients = new LinkedHashSet<String>();
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

        // register clients
        context.addServlet(new ServletHolder(new RegistrationServlet(master)),
                "/register/*");

        // deregister clients
        context.addServlet(
                new ServletHolder(new DeregistrationServlet(master)),
                "/deregister/*");

        server.start();
        server.join();
    }

    @Override
    public void registerClient(String clientAddress) {
        clients.add(clientAddress);
        System.out.println("client registered (" + clientAddress + ")");
    }

    @Override
    public void deregisterClient(String clientAddress) {
        clients.remove(clientAddress);
        System.out.println("client deregistered (" + clientAddress + ")");
    }
}
