package de.tudarmstadt.ukp.inception.search;

import java.io.Serializable;
import java.util.List;

public class ResultsGroup
    implements Serializable
{

    private static final long serialVersionUID = -4448435773623997560L;
    private final String groupKey;
    private final List<SearchResult> results;

    public ResultsGroup(String aGroupKey, List<SearchResult> aResults)
    {
        groupKey = aGroupKey;
        results = aResults;
    }

    public String getGroupKey()
    {
        return groupKey;
    }

    public List<SearchResult> getResults()
    {
        return results;
    }

}
