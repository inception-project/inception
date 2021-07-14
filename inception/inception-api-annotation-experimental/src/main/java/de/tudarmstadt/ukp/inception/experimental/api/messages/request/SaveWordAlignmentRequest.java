package de.tudarmstadt.ukp.inception.experimental.api.messages.request;

public class SaveWordAlignmentRequest
{
    private String clientName;
    private String userName;
    private long projectId;
    private int sentence;
    private String alignments;

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

    public int getSentence()
    {
        return sentence;
    }

    public void setSentence(int aSentence)
    {
        sentence = aSentence;
    }

    public String getAlignments()
    {
        return alignments;
    }

    public void setAlignments(String aAlignments)
    {
        alignments = aAlignments;
    }
}
