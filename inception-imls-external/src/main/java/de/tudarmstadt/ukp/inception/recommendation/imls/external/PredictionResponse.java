package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PredictionResponse
{
    @JsonProperty("document")
    private String document;

    public String getDocument()
    {
        return document;
    }

    public void setDocument(String aDocument)
    {
        document = aDocument;
    }
}
