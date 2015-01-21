package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.response;

import java.util.List;

import com.google.gson.JsonObject;

public class TitanListResponse extends TitanResponse {

    private List<JsonObject> responseValue;

    public TitanListResponse() {
    }

    public List<JsonObject> getValue() {
        return responseValue;
    }
}
