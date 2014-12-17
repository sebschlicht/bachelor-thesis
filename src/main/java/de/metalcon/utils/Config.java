package de.metalcon.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

/**
 * This is an interface class to the Config file for this project. For each
 * class field on java property must be defined int config.txt. The fields will
 * be automatically filled! Allowed Types are String, int, String[] and long[]
 * where arrays are defined by semicolon-separated Strings like "array=a;b;c"
 * 
 * @author Jonas Kunze, Rene Pickhardt, Sebastian Schlicht
 * 
 */
public class Config extends Properties {

    private static final long serialVersionUID = -2242894487985781331L;

    private static Config instance = null;

    private boolean loaded;

    public Config(
            final String configPath) {
        loaded = false;
        try {
            BufferedInputStream stream =
                    new BufferedInputStream(new FileInputStream(configPath));
            this.load(stream);
            stream.close();

            try {
                initialize();
                loaded = true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fills all fields with the data defined in the config file.
     * 
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private void initialize() throws IllegalArgumentException,
            IllegalAccessException {
        Field[] fields = this.getClass().getFields();
        for (Field f : fields) {
            if (this.getProperty(f.getName()) == null) {
                System.err.print("Property '" + f.getName()
                        + "' not defined in config file");
            }
            if (f.getType().equals(String.class)) {
                f.set(this, this.getProperty(f.getName()));
            } else if (f.getType().equals(long.class)) {
                f.setLong(this, Long.valueOf(this.getProperty(f.getName())));
            } else if (f.getType().equals(int.class)) {
                f.setInt(this, Integer.valueOf(this.getProperty(f.getName())));
            } else if (f.getType().equals(boolean.class)) {
                f.setBoolean(this,
                        Boolean.valueOf(this.getProperty(f.getName())));
            } else if (f.getType().equals(float.class)) {
                f.setFloat(this, Float.valueOf(this.getProperty(f.getName())));
            } else if (f.getType().equals(String[].class)) {
                f.set(this, this.getProperty(f.getName()).split(";"));
            } else if (f.getType().equals(int[].class)) {
                String[] tmp = this.getProperty(f.getName()).split(";");
                int[] ints = new int[tmp.length];
                for (int i = 0; i < tmp.length; i++) {
                    ints[i] = Integer.parseInt(tmp[i]);
                }
                f.set(this, ints);
            } else if (f.getType().equals(long[].class)) {
                String[] tmp = this.getProperty(f.getName()).split(";");
                long[] longs = new long[tmp.length];
                for (int i = 0; i < tmp.length; i++) {
                    longs[i] = Long.parseLong(tmp[i]);
                }
                f.set(this, longs);
            }
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    public static Config get(final String configPath) {
        if (instance == null) {
            instance = new Config(configPath);
        }
        return instance;
    }

}
