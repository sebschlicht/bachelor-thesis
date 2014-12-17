package de.uniko.sebschlicht.graphity.benchmark.client;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

public class ControlServlet extends ClientServlet {

    public ControlServlet() {
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response) {
        Gson gson = new Gson();
        gson.fromJson(request.getParameter("value"), null);

        // TODO: load config from JSON (percentages of actions, percentages for each follow target id)
        // TODO: start to fire requests against ids assigned

        // 200 OK.
    }
}
