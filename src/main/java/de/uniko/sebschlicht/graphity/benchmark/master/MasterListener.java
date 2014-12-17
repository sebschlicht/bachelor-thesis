package de.uniko.sebschlicht.graphity.benchmark.master;

public interface MasterListener {

    void registerClient(String clientAddress);

    void deregisterClient(String clientAddress);

    boolean startBenchmark();

    void stopBenchmark();
}
