package de.uniko.sebschlicht.graphity.benchmark.client;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import de.uniko.sebschlicht.graphity.benchmark.client.servlets.ControlServlet;

public class ClientUnit {

    public static void main(String[] args) throws Exception {
        ClientUnit client = new ClientUnit();
        Server server = new Server(8081);

        ServletContextHandler context =
                new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new ControlServlet()),
                "/control/*");

        server.start();
        server.join();
    }
}
