package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

import java.util.ArrayList;

public class TagSetJSONObject
    extends JSONOutput
{
    private long id;
    private String name;
    private String description;
    private ArrayList<String> tagNames;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public ArrayList<String> getTagNames()
    {
        return tagNames;
    }

    public void setTagNames(ArrayList<String> tagNames)
    {
        this.tagNames = tagNames;
    }

}
