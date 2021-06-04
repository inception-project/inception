package de.tudarmstadt.ukp.inception.experimental.api.message;

public class ClientMessage
{
    private String username;
    private long project;
    private long document;
    private int[][] viewport;
    private int annotationAddress;


    public ClientMessage(String aUsername, long aProject, int[][] aViewport, long aDocument, int aAnnotationAddress)
    {
        this.username = aUsername;
        this.project = aProject;
        this.document = aDocument;
        this.viewport = aViewport;
        this.annotationAddress = aAnnotationAddress;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getProject() {
        return project;
    }

    public void setProject(long project) {
        this.project = project;
    }

    public long getDocument() {
        return document;
    }

    public void setDocument(long document) {
        this.document = document;
    }

    public int[][] getViewport() {
        return viewport;
    }

    public void setViewport(int[][] viewport) {
        this.viewport = viewport;
    }

    public int getAnnotationAddress() {
        return annotationAddress;
    }

    public void setAnnotationAddress(int annotationAddress) {
        this.annotationAddress = annotationAddress;
    }
}
