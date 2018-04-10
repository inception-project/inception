package de.tudarmstadt.ukp.inception.kb.reification;

public enum Reification {
    NONE(false),
    WIKIDATA(true);

    private final boolean supportsQualifier;

    Reification(boolean supportsQualifier)
    {
        this.supportsQualifier = supportsQualifier;
    }

    public boolean supportsQualifier() {
        return supportsQualifier;
    }
}
