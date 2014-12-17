package de.uniko.sebschlicht.graphity.benchmark.api.http;

public class Framework {

    public class Client {

        public static final int PORT = 8082;

        public static final String URL_START = "/start";

        public static final String URL_STATUS = "/status";

        public static final String URL_STOP = "/stop";
    }

    public class Master {

        public static final int PORT = 8081;

        public static final String URL_DEREGISTER = "/deregister";

        public static final String URL_REGISTER = "/register";

        public static final String URL_START = "/start";

        public static final String URL_STOP = "/stop";
    }
}
