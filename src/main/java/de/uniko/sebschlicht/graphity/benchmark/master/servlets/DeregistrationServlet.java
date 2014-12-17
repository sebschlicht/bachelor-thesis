package de.uniko.sebschlicht.graphity.benchmark.master.servlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uniko.sebschlicht.graphity.benchmark.master.MasterListener;
import de.uniko.sebschlicht.graphity.benchmark.master.MasterServlet;

public class DeregistrationServlet extends MasterServlet {

    private static final long serialVersionUID = 947305597966823959L;

    public DeregistrationServlet(
            MasterListener listener) {
        super(listener);
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) {
        listener.deregisterClient(getClientIpAddr(request));

        // 200 OK.
    }
}
