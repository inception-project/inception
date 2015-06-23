package de.tudarmstadt.ukp.clarin.webanno.constraints.visitor;

public class QuoteUtil
{
    public static String unquote(String aString)
    {
        return aString.substring(1, aString.length() - 1).replace("\\\"", "\"");
    }
}
