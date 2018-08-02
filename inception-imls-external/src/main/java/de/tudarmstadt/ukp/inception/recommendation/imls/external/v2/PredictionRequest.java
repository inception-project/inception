package de.tudarmstadt.ukp.inception.recommendation.imls.external.v2;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PredictionRequest {
    @JsonProperty("typeSystem")
    private String typeSystem;

    @JsonProperty("cas")
    private String xmi;

    @JsonProperty("layer")
    private String layer;

    @JsonProperty("feature")
    private String feature;

    public String getTypeSystem() {
        return typeSystem;
    }

    public void setTypeSystem(String typeSystem) {
        this.typeSystem = typeSystem;
    }

    public String getXmi() {
        return xmi;
    }

    public void setXmi(String xmi) {
        this.xmi = xmi;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }
}
