package de.uniko.sebschlicht.graphity.benchmark.analyse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeSet;

public class DataAnalzer {

    public static void main(String[] args) throws IOException {
        WikidumpReader reader = new WikidumpReader("/tmp/de-events.log");

        Set<Long> ids = new TreeSet<Long>();
        String[] entry;
        while ((entry = reader.getEntry()) != null) {
            if (entry.length == 3) {
                ids.add(Long.valueOf(entry[2]));
            } else if (entry.length == 4 && "A".equals(entry[1])) {
                ids.add(Long.valueOf(entry[2]));
                ids.add(Long.valueOf(entry[3]));
            }
        }
        PrintWriter writer = new PrintWriter("/tmp/dump-ids");
        for (Long id : ids) {
            writer.println(id);
        }
        writer.flush();
        writer.close();
        System.out.println(ids.size());

        /*
         * WikidumpReader dumper = new WikidumpReader("/tmp/de-events.log");
         * WikidumpInfo info = dumper.loadCommands(200000000);
         * System.out.println("Total: " + info.getNumTotalWrites());
         * System.out
         * .println("\tFollow: "
         * + info.getNumFollow()
         * + " ("
         * + (info.getNumFollow()
         * / (float) info.getNumTotalWrites() * 100)
         * + "%)");
         * System.out
         * .println("\tUnfollow: "
         * + info.getNumUnfollow()
         * + " ("
         * + (info.getNumUnfollow()
         * / (float) info.getNumTotalWrites() * 100)
         * + "%)");
         * System.out.println("\tPost: " + info.getNumPost() + " ("
         * + (info.getNumPost() / (float) info.getNumTotalWrites() * 100)
         * + "%)");
         * // build (sorted) tree map: (num followers) -> [id, id, ...]
         * SortedMap<Integer, List<Long>> popularNodes =
         * new TreeMap<Integer, List<Long>>();
         * for (Entry<Long, Integer> e : info.getNumFollowers().entrySet()) {
         * List<Long> sameFame = popularNodes.get(e.getValue());
         * if (sameFame == null) {
         * sameFame = new LinkedList<Long>();
         * popularNodes.put(e.getValue(), sameFame);
         * }
         * sameFame.add(e.getKey());
         * }
         * for (int i = 0; i < 5; ++i) {
         * int numFollowers = popularNodes.lastKey();
         * List<Long> sameFame = popularNodes.get(numFollowers);
         * popularNodes.remove(numFollowers);
         * System.out.println(numFollowers + " (" + sameFame.size() + ")");
         * }
         */

        //        File file = new File("/tmp/de-events.json");
        //        Gson gson = new Gson();
        //               PrintWriter writer = new PrintWriter(file);
        //                writer.print(gson.toJson(info));
        //                writer.flush();
        //                writer.close();
        //
        //        BufferedReader reader =
        //                new BufferedReader(new InputStreamReader(new FileInputStream(
        //                        file)));
        //        WikidumpInfo info2 = gson.fromJson(reader, WikidumpInfo.class);
        //        reader.close();
        //
        //        System.out.println("Total: " + info2.getNumTotalWrites());
        //        // build (sorted) tree map: (num followers) -> [id, id, ...]
        //        SortedMap<Integer, List<Long>> popularNodes =
        //                new TreeMap<Integer, List<Long>>();
        //        for (Entry<Long, Integer> e : info2.getNumFollowers().entrySet()) {
        //            List<Long> sameFame = popularNodes.get(e.getValue());
        //            if (sameFame == null) {
        //                sameFame = new LinkedList<Long>();
        //                popularNodes.put(e.getValue(), sameFame);
        //            }
        //            sameFame.add(e.getKey());
        //        }
        //        for (int i = 0; i < 5; ++i) {
        //            int numFollowers = popularNodes.lastKey();
        //            List<Long> sameFame = popularNodes.get(numFollowers);
        //            popularNodes.remove(numFollowers);
        //            System.out.println(numFollowers + " (" + sameFame.size() + ")");
        //        }
    }
}
