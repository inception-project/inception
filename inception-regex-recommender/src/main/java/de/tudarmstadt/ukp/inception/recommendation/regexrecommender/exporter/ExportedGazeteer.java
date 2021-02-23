package de.tudarmstadt.ukp.inception.recommendation.regexrecommender.exporter;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedGazeteer
{
    @JsonProperty("id")
    private long id;
    
    @JsonProperty("name")
    private String name;

    @JsonProperty("recommender")
    private String recommender;

    public long getId()
    {
        return id;
    }

    public void setId(long aId)
    {
        id = aId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public String getRecommender()
    {
        return recommender;
    }

    public void setRecommender(String aRecommender)
    {
        recommender = aRecommender;
    }
}
