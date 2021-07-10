package de.tudarmstadt.ukp.inception.experimental.api.messages.request;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import org.apache.uima.cas.Type;

public class UpdateAnnotationRequest
{
   private String clientName;
   private String userName;
   private long projectId;
   private long documentId;
   private VID annotationAddress;
   private Type newType;

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

    public VID getAnnotationAddress()
    {
        return annotationAddress;
    }

    public void setAnnotationAddress(VID aAnnotationAddress) {
        annotationAddress = aAnnotationAddress;
    }

    public Type getNewType()
    {
        return newType;
    }

    public void setNewType(Type aNewType)
    {
        newType = aNewType;
    }
}
