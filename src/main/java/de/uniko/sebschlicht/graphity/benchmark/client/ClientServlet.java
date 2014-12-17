package de.uniko.sebschlicht.graphity.benchmark.client;

import javax.servlet.http.HttpServlet;

public class ClientServlet extends HttpServlet {

    private static final long serialVersionUID = -8985873311892505498L;

    protected ClientListener listener;

    public void setListener(ClientListener listener) {
        this.listener = listener;
    }
}
