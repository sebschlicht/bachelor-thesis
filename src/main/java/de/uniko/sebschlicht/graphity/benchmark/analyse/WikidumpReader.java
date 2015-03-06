package de.uniko.sebschlicht.graphity.benchmark.analyse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.uniko.sebschlicht.graphity.bootstrap.load.CsvParser;

public class WikidumpReader extends CsvParser<WikidumpInfo> {

    public WikidumpReader(
            String filePath) throws FileNotFoundException {
        super(filePath);
    }

    public WikidumpInfo loadCommands(int numCommands) throws IOException {
        String[] entry;
        Map<Long, Integer> numFollowers = new HashMap<Long, Integer>();
        long numTotalWrites = 0;
        long numFollow = 0;
        long numUnfollow = 0;
        long numPost = 0;

        int numParsed = 0;
        while (numParsed < numCommands && ((entry = getEntry()) != null)) {
            if (entry.length == 3 && "U".equals(entry[1])) {
                // update
                numTotalWrites += 1;
                numPost += 1;
            } else if (entry.length == 4 && "A".equals(entry[1])) {
                // add followship
                //long idFollowing = Long.valueOf(entry[2]);
                long idFollowed = Long.valueOf(entry[3]);

                Integer numFollowing = numFollowers.get(idFollowed);
                if (numFollowing == null) {
                    numFollowing = 0;
                }
                numFollowers.put(idFollowed, numFollowing + 1);
                numTotalWrites += 1;
                numFollow += 1;
            } else if (entry.length == 4 && "R".equals(entry[1])) {
                // remove followship
                //long idFollowing = Long.valueOf(entry[2]);
                //long idFollowed = Long.valueOf(entry[3]);

                numTotalWrites += 1;
                numUnfollow += 1;
            } else {
                throw new IllegalArgumentException("unknown entry of length "
                        + entry.length + ": " + entry[1]);
            }

            ++numParsed;
        }
        System.out.println("Entries: " + numParsed);
        return new WikidumpInfo(numFollowers, numTotalWrites, numFollow,
                numUnfollow, numPost);
    }
}
