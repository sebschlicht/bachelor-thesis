package de.uniko.sebschlicht.graphity.benchmark.master;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ResultCollector {

    private Master master;

    private ExecutorService threadpool;

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
        int numClients = master.getClients().size();
        if (threadpool == null) {
            threadpool = Executors.newFixedThreadPool(numClients);
            for (ClientWrapper client : master.getClients()) {
                tasks.add(new ResultCollectionTask(client));
            }
        }
        try {
            List<Future<String>> taskStack =
                    threadpool.invokeAll(tasks, timeout, timeUnit);
            List<String> results = new LinkedList<String>();
            for (Future<String> taskResult : taskStack) {
                try {
                    results.add(taskResult.get());
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
