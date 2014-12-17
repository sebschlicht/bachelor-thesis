package de.uniko.sebschlicht.graphity.benchmark.master;

import javax.ws.rs.core.MediaType;

import org.eclipse.jetty.http.HttpStatus;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

import de.uniko.sebschlicht.graphity.benchmark.api.ClientConfiguration;
import de.uniko.sebschlicht.graphity.benchmark.api.http.Urls;

public class ClientWrapper {

    private static Gson gson = new Gson();

    private String address;

    private Client httpClient;

    private WebResource resStart;

    private WebResource resStatus;

    private WebResource resStop;

    public ClientWrapper(
            String address) {
        this.address = address;
        httpClient = Client.create();
        resStart = httpClient.resource(address + Urls.Client.URL_START);
        resStatus = httpClient.resource(address + Urls.Client.URL_STATUS);
        resStop = httpClient.resource(address + Urls.Client.URL_STOP);
    }

    public String getAddress() {
        return address;
    }

    /**
     * Starts the benchmark client.
     * 
     * @param config
     *            client's benchmark configuration
     * @return true - if the client has been reached<br>
     *         false - otherwise
     */
    public boolean start(ClientConfiguration config) {
        try {
            String jsonConfig = gson.toJson(config);
            ClientResponse httpResponse =
                    resStart.type(MediaType.APPLICATION_JSON)
                            .entity(jsonConfig).post(ClientResponse.class);
            return (httpResponse.getStatus() == HttpStatus.OK_200);
        } catch (UniformInterfaceException e) {
            // HTTP status code >= 300
            return false;
        }
    }

    /**
     * Retrieves the benchmark progress of the client.
     * 
     * @return benchmark result (JSON)<br>
     *         <b>null</b> if client not reachable
     */
    public String getStatus() {
        try {
            ClientResponse httpResponse = resStatus.get(ClientResponse.class);
            return httpResponse.getEntity(String.class);
        } catch (UniformInterfaceException e) {
            // HTTP status code >= 300
            return null;
        }
    }

    /**
     * Stops the benchmark client.
     * 
     * @return benchmark result (JSON)<br>
     *         <b>null</b> if client not reachable
     */
    public String stop() {
        try {
            ClientResponse httpResponse = resStop.get(ClientResponse.class);
            return httpResponse.getEntity(String.class);
        } catch (UniformInterfaceException e) {
            // HTTP status code >= 300
            return null;
        }
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
