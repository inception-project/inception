package de.tudarmstadt.ukp.inception.kb.yaml;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KnowledgeBaseAccess implements Serializable
{
    @JsonProperty("access-url")
    private String accessUrl;

    @JsonProperty("access-type")
    private KnowledgeBaseAccessType accessType;

    public String getAccessUrl()
    {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl)
    {
        this.accessUrl = accessUrl;
    }

    public KnowledgeBaseAccessType getAccessType()
    {
        return accessType;
    }

    public void setAccessType(KnowledgeBaseAccessType accessType)
    {
        this.accessType = accessType;
    }

    @Override public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KnowledgeBaseAccess that = (KnowledgeBaseAccess) o;
        return Objects.equals(accessUrl, that.accessUrl) && Objects
            .equals(accessType, that.accessType);
    }

    @Override public int hashCode()
    {
        return Objects.hash(accessUrl, accessUrl);
    }
}
