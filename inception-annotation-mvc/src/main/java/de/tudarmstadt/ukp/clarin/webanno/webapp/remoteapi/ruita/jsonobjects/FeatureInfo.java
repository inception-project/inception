package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

public class FeatureInfo
{
    private long id;
    private String uiName;
    private String name;
    private String type;
    private boolean required;
    private boolean isMulti;
    private String description;
    private long tagSetId;

    public boolean isMulti()
    {
        return isMulti;
    }

    public void setMulti(boolean isMulti)
    {
        this.isMulti = isMulti;
    }

    public long getTagSetId()
    {
        return tagSetId;
    }

    public void setTagSetId(long tagSetId)
    {
        this.tagSetId = tagSetId;
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getUiName()
    {
        return uiName;
    }

    public void setUiName(String uiName)
    {
        this.uiName = uiName;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public boolean isRequired()
    {
        return required;
    }

    public void setRequired(boolean required)
    {
        this.required = required;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
