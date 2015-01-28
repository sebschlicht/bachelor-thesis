package de.uniko.sebschlicht.graphity.benchmark.client.benchmark.bootstrap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import de.uniko.sebschlicht.graphity.benchmark.analyse.CsvParser;
import de.uniko.sebschlicht.graphity.benchmark.api.RequestType;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.Request;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFeed;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestFollow;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestPost;
import de.uniko.sebschlicht.graphity.benchmark.client.requests.RequestUnfollow;

public class BootstrapLoader extends CsvParser<Request> {

    public BootstrapLoader(
            String filePath) throws FileNotFoundException {
        super(filePath);
    }

    public Queue<Request> loadRequests() throws IOException {
        String[] entry;
        Queue<Request> requests = new LinkedList<Request>();

        RequestType type;
        long idActor, idFollowed;
        while ((entry = getEntry()) != null) {
            if (entry.length == 0) {// skip empty lines
                continue;
            }
            type = RequestType.getTypeById(Integer.valueOf(entry[0]));
            idActor = Long.valueOf(entry[1]);
            switch (type) {
                case FEED:
                    requests.add(new RequestFeed(idActor));
                    break;

                case FOLLOW:
                    idFollowed = Long.valueOf(entry[2]);
                    requests.add(new RequestFollow(idActor, idFollowed));
                    break;

                case POST:
                    requests.add(new RequestPost(idActor, entry[2]));
                    break;

                case UNFOLLOW:
                    idFollowed = Long.valueOf(entry[2]);
                    requests.add(new RequestUnfollow(idActor, idFollowed));
                    break;
            }
        }
        return requests;
    }
}
