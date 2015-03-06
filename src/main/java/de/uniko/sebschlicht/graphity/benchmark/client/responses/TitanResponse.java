package de.uniko.sebschlicht.graphity.benchmark.client.responses;

public abstract class TitanResponse {

    private String message;

    public boolean isSuccess() {
        return (message == null);
    }
}
