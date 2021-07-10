package de.tudarmstadt.ukp.inception.experimental.api.messages.request;

public class NewDocumentRequest
{
    private String clientName;
    private String userName;
    private long projectId;
    private long documentId;
    private String viewportType;
    private int[][] viewport;
    private boolean recommenderEnabled;

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

    public String getViewportType()
    {
        return viewportType;
    }

    public void setViewportType(String aViewportType)
    {
        viewportType = aViewportType;
    }

    public int[][] getViewport()
    {
        return viewport;
    }

    public void setViewport(int[][] aViewport)
    {
        viewport = aViewport;
    }

    public boolean isRecommenderEnabled()
    {
        return recommenderEnabled;
    }

    public void setRecommenderEnabled(boolean aRecommenderEnabled) {
        recommenderEnabled = aRecommenderEnabled;
    }
}
