package de.tudarmstadt.ukp.inception.experimental.api.messages.request.relation;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public class UpdateRelationRequest
{
    private String clientName;
    private String userName;
    private long projectId;
    private long documentId;
    private VID relationAddress;
    private String newDependencyType;
    private String newFlavor;

    public String getClientName()
    {
        return clientName;
    }

    public void setClientName(String aClientName)
    {
        clientName = aClientName;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String aUserName)
    {
        userName = aUserName;
    }

    public long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(long aProjectId)
    {
        projectId = aProjectId;
    }

    public long getDocumentId()
    {
        return documentId;
    }

    public void setDocumentId(long aDocumentId)
    {
        documentId = aDocumentId;
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
}
