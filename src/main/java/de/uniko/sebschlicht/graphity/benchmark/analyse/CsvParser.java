package de.uniko.sebschlicht.graphity.benchmark.analyse;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public abstract class CsvParser<T > {

    protected BufferedReader reader;

    protected List<T> items;

    public CsvParser(
            String filePath) throws FileNotFoundException {
        reader = new BufferedReader(new FileReader(filePath));
        items = null;
    }

    protected String[] getEntry() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            return null;
        }

        return line.split("\t");
    }

    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
