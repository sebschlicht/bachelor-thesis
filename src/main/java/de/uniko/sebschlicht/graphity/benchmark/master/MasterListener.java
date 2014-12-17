package de.uniko.sebschlicht.graphity.benchmark.master;

public interface MasterListener {

    /**
     * Adds a client to the client list.
     * 
     * @param clientAddress
     *            IP address of the client
     */
    void registerClient(String clientAddress);

    /**
     * Removes a client from the client list.
     * 
     * @param clientAddress
     *            IP address of the client
     */
    void deregisterClient(String clientAddress);

    /**
     * Starts the benchmark with all clients in the client list.
     * 
     * @return true - if all clients were reached successfully<br>
     *         false - otherwise
     */
    boolean startBenchmark();

    /**
     * Stops a running benchmark.
     */
    void stopBenchmark();
}
