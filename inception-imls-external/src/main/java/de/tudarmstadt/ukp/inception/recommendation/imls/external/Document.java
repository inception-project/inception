package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Document {

    @JsonProperty("xmi")
    private String xmi;

    @JsonProperty("documentId")
    private Long documentId;

    @JsonProperty("userId")
    private String userId;

    public String getXmi()
    {
        return xmi;
    }

    public void setXmi(String aXmi)
    {
        xmi = aXmi;
    }

    public Long getDocumentId()
    {
        return documentId;
    }

    public void setDocumentId(Long aDocumentId)
    {
        documentId = aDocumentId;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String aUserId)
    {
        userId = aUserId;
    }
}
