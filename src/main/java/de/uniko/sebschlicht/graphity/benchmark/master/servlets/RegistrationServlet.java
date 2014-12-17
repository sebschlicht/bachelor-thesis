package de.uniko.sebschlicht.graphity.benchmark.master.servlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uniko.sebschlicht.graphity.benchmark.master.MasterListener;
import de.uniko.sebschlicht.graphity.benchmark.master.MasterServlet;

public class RegistrationServlet extends MasterServlet {

    private static final long serialVersionUID = -1952125883059314904L;

    public RegistrationServlet(
            MasterListener listener) {
        super(listener);
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) {
        listener.registerClient(getClientIpAddr(request));

        // 200 OK.
    }
}
