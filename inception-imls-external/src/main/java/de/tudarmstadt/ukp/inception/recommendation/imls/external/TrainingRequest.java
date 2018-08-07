package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrainingRequest {
    @JsonProperty("documents")
    private List<Document> documents;

    public List<Document> getDocuments()
    {
        return documents;
    }

    public void setDocuments(List<Document> aDocuments)
    {
        documents = aDocuments;
    }

    public static class Document
    {
        @JsonProperty("layer")
        private String layer;

        @JsonProperty("feature")
        private String feature;

        @JsonProperty("typeSystem")
        private String typeSystem;

        @JsonProperty("cas")
        private String xmi;

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

        public String getXmi()
        {
            return xmi;
        }

        public void setXmi(String aXmi)
        {
            xmi = aXmi;
        }
    }
}
