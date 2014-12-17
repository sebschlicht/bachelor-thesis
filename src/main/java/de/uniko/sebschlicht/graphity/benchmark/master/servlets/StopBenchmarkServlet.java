package de.uniko.sebschlicht.graphity.benchmark.master.servlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uniko.sebschlicht.graphity.benchmark.master.MasterListener;
import de.uniko.sebschlicht.graphity.benchmark.master.MasterServlet;

public class StopBenchmarkServlet extends MasterServlet {

    private static final long serialVersionUID = 7198112806100106493L;

    public StopBenchmarkServlet(
            MasterListener listener) {
        super(listener);
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) {
        listener.stopBenchmark();
        // 200 OK.
    }
}
