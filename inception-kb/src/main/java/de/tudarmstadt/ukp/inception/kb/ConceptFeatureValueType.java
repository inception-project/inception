package de.tudarmstadt.ukp.inception.kb;

public enum ConceptFeatureValueType
{
    ANY_OBJECT("<Any Object>"), CONCEPT("Concepts"), INSTANCE("Instances");

    private final String uiLabel;

    ConceptFeatureValueType(String aUiLabel)
    {
        uiLabel = aUiLabel;
    }

    public String toString()
    {
        return uiLabel;
    }
}
