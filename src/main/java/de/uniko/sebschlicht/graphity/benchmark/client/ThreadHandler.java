package de.uniko.sebschlicht.graphity.benchmark.client;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.uniko.sebschlicht.graphity.benchmark.client.benchmark.BenchmarkClientTask;

public class ThreadHandler implements Runnable {

    private ExecutorService threadpool;

    private Thread thread;

    private Collection<BenchmarkClientTask> tasks;

    public ThreadHandler(
            Collection<BenchmarkClientTask> tasks) {
        this.tasks = tasks;
    }

    public boolean start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
            return true;
        }
        return false;
    }

    public boolean stop() {
        if (thread != null) {
            for (BenchmarkClientTask task : tasks) {
                task.stop();
            }
            try {
                thread.join();
                thread = null;
                threadpool.shutdownNow();
                threadpool = null;
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void run() {
        try {
            SingleClient.LOG.info("starting " + tasks.size()
                    + " client threads...");
            threadpool = Executors.newFixedThreadPool(tasks.size());
            SingleClient.LOG.info("benchmark started at "
                    + System.currentTimeMillis());
            threadpool.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
