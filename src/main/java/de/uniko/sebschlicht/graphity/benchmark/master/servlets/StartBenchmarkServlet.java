package de.uniko.sebschlicht.graphity.benchmark.master.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;

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
        if (!listener.startBenchmark()) {
            response.setStatus(HttpStatus.PRECONDITION_FAILED_412);
            try {
                response.getOutputStream().print(
                        "no benchmark clients registered");
            } catch (IOException e) {
                // ignore
            }
        } // else: 200 OK.        
    }
}
