package de.tudarmstadt.ukp.clarin.webanno.constraints.model;

public class Restriction
{
    private final String path;
    private final String value;
    private final boolean flagImportant;

    public Restriction(String aPath, String aValue, boolean aFlagImportant)
    {
        path = aPath;
        value = aValue;
        flagImportant = aFlagImportant;
    }

    public String getPath()
    {

        return path;

    }

    public String getValue()
    {
        return value;
    }

    public boolean isFlagImportant()
    {
        return flagImportant;
    }

    @Override
    public String toString()
    {
        return "Restriction [[" + path + "] = [" + value + "] important=" + flagImportant + "]";
    }
}
