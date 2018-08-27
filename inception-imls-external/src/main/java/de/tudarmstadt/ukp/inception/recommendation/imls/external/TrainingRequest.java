package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrainingRequest {
    @JsonProperty("layer")
    private String layer;

    @JsonProperty("feature")
    private String feature;

    @JsonProperty("typeSystem")
    private String typeSystem;

    @JsonProperty("documents")
    private List<String> documents;

    public String getLayer()
    {
        return layer;
    }

    public void setLayer(String aLayer)
    {
        layer = aLayer;
    }

    public String getFeature()
    {
        return feature;
    }

    public void setFeature(String aFeature)
    {
        feature = aFeature;
    }

    public String getTypeSystem()
    {
        return typeSystem;
    }

    public void setTypeSystem(String aTypeSystem)
    {
        typeSystem = aTypeSystem;
    }

    public List<String> getDocuments()
    {
        return documents;
    }

    public void setDocuments(List<String> aDocuments)
    {
        documents = aDocuments;
    }
}
