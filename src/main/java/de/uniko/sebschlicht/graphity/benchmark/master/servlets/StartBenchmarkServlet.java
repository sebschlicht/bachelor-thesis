package de.uniko.sebschlicht.graphity.benchmark.master.servlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uniko.sebschlicht.graphity.benchmark.master.MasterListener;
import de.uniko.sebschlicht.graphity.benchmark.master.MasterServlet;

public class StartBenchmarkServlet extends MasterServlet {

    private static final long serialVersionUID = -7895185877523037668L;

    public StartBenchmarkServlet(
            MasterListener listener) {
        super(listener);
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) {
        listener.startBenchmark();
        // 200 OK.        
    }
}
