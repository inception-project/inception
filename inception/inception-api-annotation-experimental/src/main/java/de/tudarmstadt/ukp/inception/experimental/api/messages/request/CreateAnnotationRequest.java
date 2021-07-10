package de.tudarmstadt.ukp.inception.experimental.api.messages.request;

import org.apache.uima.cas.Type;

public class CreateAnnotationRequest
{
    private String clientName;
    private String userName;
    private long projectId;
    private long documentId;

    private int begin;
    private int end;

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

    public int getBegin()
    {
        return begin;
    }

    public void setBegin(int aBegin)
    {
        begin = aBegin;
    }

    public int getEnd()
    {
        return end;
    }

    public void setEnd(int aEnd)
    {
        end = aEnd;
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
