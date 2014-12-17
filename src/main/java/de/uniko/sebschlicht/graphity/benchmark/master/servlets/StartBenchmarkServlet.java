package de.uniko.sebschlicht.graphity.benchmark.master.servlets;

import java.io.IOException;

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
        try {
            listener.startBenchmark();
        } catch (IllegalStateException e) {
            // is user message
            try {
                response.getOutputStream().print(e.getMessage());
            } catch (IOException e1) {
                // ignore
            }
        }
        // 200 OK.        
    }
}
