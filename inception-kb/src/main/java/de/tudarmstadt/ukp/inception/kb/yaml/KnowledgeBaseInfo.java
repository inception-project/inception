package de.tudarmstadt.ukp.inception.kb.yaml;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KnowledgeBaseInfo implements Serializable
{

    private static final long serialVersionUID = -2667645577002890141L;

    @JsonProperty("description")
    private String description;

    @JsonProperty("host-institution-name")
    private String hostInstitutionName;

    @JsonProperty("author-name")
    private String authorName;

    @JsonProperty("website-url")
    private String websiteURL;

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    public String getHostInstitutionName()
    {
        return hostInstitutionName;
    }

    public void setHostInstitutionName(String hostInstitutionName)
    {
        this.hostInstitutionName = hostInstitutionName;
    }

    public String getAuthorName()
    {
        return authorName;
    }

    public void setAuthorName(String authorName)
    {
        this.authorName = authorName;
    }

    public String getWebsiteURL()
    {
        return websiteURL;
    }

    public void setWebsiteURL(String websiteURL)
    {
        this.websiteURL = websiteURL;
    }

    @Override public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KnowledgeBaseInfo that = (KnowledgeBaseInfo) o;
        return Objects.equals(description, that.description) && Objects
            .equals(hostInstitutionName, that.hostInstitutionName) && Objects
            .equals(authorName, that.authorName) && Objects.equals(websiteURL, that.websiteURL);
    }

    @Override public int hashCode()
    {
        return Objects.hash(description, hostInstitutionName, authorName, websiteURL);
    }
}
