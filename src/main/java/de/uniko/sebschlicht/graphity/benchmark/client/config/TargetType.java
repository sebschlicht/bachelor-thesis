package de.uniko.sebschlicht.graphity.benchmark.client.config;

public enum TargetType {

    NEO4J("NEO4J"),

    TITAN("TITAN");

    private String identifier;

    private TargetType(
            String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return getIdentifier();
    }

    /**
     * Retrieves the target type with the given identifier.
     * 
     * @param identifier
     *            target type identifier
     * @return target type with the identifier passed
     * @throws IllegalArgumentException
     *             if no such target type
     */
    public static TargetType fromString(String identifier) {
        if (NEO4J.getIdentifier().equals(identifier)) {
            return NEO4J;
        } else if (TITAN.getIdentifier().equals(identifier)) {
            return TITAN;
        }
        throw new IllegalArgumentException("unknown target type identifier \""
                + identifier + "\"");
    }
}
