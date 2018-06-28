package de.tudarmstadt.ukp.inception.kb.yaml;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KnowledgeBaseProfile implements Serializable
{

    private static final long serialVersionUID = -2684575269500649910L;

    @JsonProperty("name")
    private String name;
    
    @JsonProperty("sparql-url")
    private String sparqlUrl;
    
    @JsonProperty("mapping")
    private KnowledgeBaseMapping mapping;

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public String getSparqlUrl()
    {
        return sparqlUrl;
    }

    public void setSparqlUrl(String aSparqlUrl)
    {
        sparqlUrl = aSparqlUrl;
    }

    public KnowledgeBaseMapping getMapping()
    {
        return mapping;
    }

    public void setMapping(KnowledgeBaseMapping aMapping)
    {
        mapping = aMapping;
    }
}
