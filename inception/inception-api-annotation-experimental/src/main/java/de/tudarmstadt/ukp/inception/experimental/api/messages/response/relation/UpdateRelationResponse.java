package de.tudarmstadt.ukp.inception.experimental.api.messages.response.relation;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public class UpdateRelationResponse
{
    private VID relationAddress;
    private String newDependencyType;
    private String newFlavor;
    private String color;

    public UpdateRelationResponse(VID aRelationAddress, String aNewDependencyType, String aNewFlavor, String aColor)
    {
        relationAddress = aRelationAddress;
        newDependencyType = aNewDependencyType;
        newFlavor = aNewFlavor;
        color = aColor;
    }

    public VID getRelationAddress()
    {
        return relationAddress;
    }

    public void setRelationAddress(VID aRelationAddress)
    {
        relationAddress = aRelationAddress;
    }

    public String getNewDependencyType()
    {
        return newDependencyType;
    }

    public void setNewDependencyType(String aNewDependencyType)
    {
        newDependencyType = aNewDependencyType;
    }

    public String getNewFlavor()
    {
        return newFlavor;
    }

    public void setNewFlavor(String aNewFlavor)
    {
        newFlavor = aNewFlavor;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String aColor)
    {
        color = aColor;
    }
}
