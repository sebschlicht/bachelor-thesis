package de.uniko.sebschlicht.graphity.benchmark.master;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.uniko.sebschlicht.graphity.benchmark.api.FrameworkUrls;

public class ClientWrapper {

    private String address;

    private Client httpClient;

    private WebResource resStart;

    private WebResource resStatus;

    private WebResource resStop;

    public ClientWrapper(
            String address) {
        this.address = address;
        httpClient = Client.create();
        resStart =
                httpClient.resource(address + FrameworkUrls.Client.URL_START);
        resStatus =
                httpClient.resource(address + FrameworkUrls.Client.URL_STATUS);
        resStop = httpClient.resource(address + FrameworkUrls.Client.URL_STOP);
    }

    public String getAddress() {
        return address;
    }

    public boolean start() {
        // TODO
        return false;
    }

    public String getStatus() {
        ClientResponse httpResponse = resStatus.get(ClientResponse.class);
        return httpResponse.getEntity(String.class);
    }

    @Override
    public boolean equals(Object c) {
        if (c == null) {
            return false;
        }
        if (c == this) {
            return true;
        }
        if (!(c instanceof ClientWrapper)) {
            return false;
        }
        ClientWrapper client = (ClientWrapper) c;
        return client.getAddress().equals(address);
    }
}
