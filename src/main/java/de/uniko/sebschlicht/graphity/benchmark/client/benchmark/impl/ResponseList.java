package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.impl;

import java.util.List;

import com.google.gson.JsonObject;

public class ResponseList {

    private List<JsonObject> responseValue;

    /**
     * Empty constructor for deserialization.
     */
    public ResponseList() {
    }

    public List<JsonObject> getResponseValue() {
        return responseValue;
    }
}
