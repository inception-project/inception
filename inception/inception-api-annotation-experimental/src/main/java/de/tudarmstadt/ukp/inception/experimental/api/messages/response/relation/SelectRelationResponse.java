package de.tudarmstadt.ukp.inception.experimental.api.messages.response.relation;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public class SelectRelationResponse
{
    private VID relationAddress;
    private String governorCoveredText;
    private String dependentCoveredText;
    private String flavor;
    private String relation;

    public SelectRelationResponse(VID aRelationAddress, String aGovernorCoveredText, String aDependentCoveredText, String aFlavor, String aRelation)
    {
        relationAddress = aRelationAddress;
        governorCoveredText = aGovernorCoveredText;
        dependentCoveredText = aDependentCoveredText;
        flavor = aFlavor;
        relation = aRelation;
    }

    public VID getRelationAddress()
    {
        return relationAddress;
    }

    public void setRelationAddress(VID aRelationAddress)
    {
        relationAddress = aRelationAddress;
    }

    public String getGovernorCoveredText()
    {
        return governorCoveredText;
    }

    public void setGovernorCoveredText(String aGovernorCoveredText)
    {
        governorCoveredText = aGovernorCoveredText;
    }

    public String getDependentCoveredText()
    {
        return dependentCoveredText;
    }

    public void setDependentCoveredText(String aDependentCoveredText)
    {
        dependentCoveredText = aDependentCoveredText;
    }

    public String getFlavor()
    {
        return flavor;
    }

    public void setFlavor(String aFlavor)
    {
        flavor = aFlavor;
    }

    public String getRelation()
    {
        return relation;
    }

    public void setRelation(String aRelation)
    {
        relation = aRelation;
    }
}
