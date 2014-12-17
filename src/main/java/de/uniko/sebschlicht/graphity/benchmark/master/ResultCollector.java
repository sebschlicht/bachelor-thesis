package de.uniko.sebschlicht.graphity.benchmark.master;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ResultCollector {

    private Master master;

    private List<Callable<String>> tasks;

    public ResultCollector(
            Master master) {
        this.master = master;
        tasks = new LinkedList<Callable<String>>();
    }

    /**
     * collect benchmark results from all clients in client list
     * 
     * @param timeout
     *            timeout for the result retrieval
     * @param timeUnit
     *            time unit used for the timeout
     * @return list of all benchmark results (JSON)<br>
     *         <b>null</b> if not all results were collected within the given
     *         timeout
     */
    public List<String> collectResults(long timeout, TimeUnit timeUnit) {
        // TODO
        return null;
    }

    /**
     * Displays the clients' benchmark results in a fixed interval
     * in the console.
     * 
     * @param interval
     *            interval to retrieve and display results in
     * @param timeUnit
     *            time unit used for the interval
     */
    public void displayResultsPeriodically(long interval, TimeUnit timeUnit) {
        // TODO
    }

    /**
     * Writes the clients' benchmark results to the log.
     */
    public void logResults() {
        // TODO
    }
}
