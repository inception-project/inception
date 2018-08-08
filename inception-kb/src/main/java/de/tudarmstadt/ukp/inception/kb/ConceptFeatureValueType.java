package de.tudarmstadt.ukp.inception.kb;

public enum ConceptFeatureValueType
{
    ANY_OBJECT("<Any Concept/Instance>"), CONCEPT("Only Concepts"), INSTANCE("Only Instances");

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
