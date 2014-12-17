package de.uniko.sebschlicht.graphity.benchmark.master;

import java.util.concurrent.Callable;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.uniko.sebschlicht.graphity.benchmark.api.FrameworkUrls;

public class ResultCollectionTask implements Callable<String> {

    private WebResource resStatus;

    public ResultCollectionTask(
            String clientAddress) {
        resStatus =
                Client.create().resource(
                        clientAddress + FrameworkUrls.Client.URL_STATUS);
    }

    @Override
    public String call() throws Exception {
        ClientResponse response = resStatus.get(ClientResponse.class);
        return response.getEntity(String.class);
    }
}
