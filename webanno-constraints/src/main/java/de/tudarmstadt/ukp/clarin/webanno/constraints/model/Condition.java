package de.tudarmstadt.ukp.clarin.webanno.constraints.model;

import java.util.ArrayList;
import java.util.LinkedList;

public class Condition
{
    private final String path;
    private final String value;

    public Condition(String aPath, String aValue)
    {
        path = aPath;
        value = aValue;
    }

    public String getPath()
    {
        return path;
    }

    public String getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return "Condition [[" + path + "] = [" + value + "]]";
    }

    public boolean matches(ArrayList<String> listOfValues)
    {
        boolean doesItMatch = false;
        for (String input : listOfValues) {
            if (value.equals(input)) {
                doesItMatch = true;
                break;
            }

        }
        return doesItMatch;
    }
}
