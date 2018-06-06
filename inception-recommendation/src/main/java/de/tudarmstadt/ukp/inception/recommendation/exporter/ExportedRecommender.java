package de.tudarmstadt.ukp.inception.recommendation.exporter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedRecommender
{

    @JsonProperty("name")
    private String name;

    @JsonProperty("feature")
    private String feature;

    @JsonProperty("layerName")
    private String layerName;

    @JsonProperty("tool")
    private String tool;

    @JsonProperty("threshold")
    private double threshold;

    @JsonProperty("alwaysSelected")
    private boolean alwaysSelected;

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("traits")
    private String traits;

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public String getFeature()
    {
        return feature;
    }

    public void setFeature(String aFeature)
    {
        feature = aFeature;
    }

    public String getLayerName()
    {
        return layerName;
    }

    public void setLayerName(String aLayerName)
    {
        layerName = aLayerName;
    }

    public String getTool()
    {
        return tool;
    }

    public void setTool(String aTool)
    {
        tool = aTool;
    }

    public double getThreshold()
    {
        return threshold;
    }

    public void setThreshold(double aThreshold)
    {
        threshold = aThreshold;
    }

    public boolean isAlwaysSelected()
    {
        return alwaysSelected;
    }

    public void setAlwaysSelected(boolean aAlwaysSelected)
    {
        alwaysSelected = aAlwaysSelected;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean aEnabled)
    {
        enabled = aEnabled;
    }

    public String getTraits()
    {
        return traits;
    }

    public void setTraits(String aTraits)
    {
        traits = aTraits;
    }
}
