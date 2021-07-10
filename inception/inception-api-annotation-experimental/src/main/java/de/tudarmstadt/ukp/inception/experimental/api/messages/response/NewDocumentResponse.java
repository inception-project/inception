package de.tudarmstadt.ukp.inception.experimental.api.messages.response;

import java.util.List;

import de.tudarmstadt.ukp.inception.experimental.api.model.Span;

public class NewDocumentResponse
{
    private int documentId;
    private Character[] viewportText;
    private List<Span> spanAnnotations;

    public int getDocumentId()
    {
        return documentId;
    }

    public void setDocumentId(int aDocumentId)
    {
        documentId = aDocumentId;
    }

    public Character[] getViewportText()
    {
        return viewportText;
    }

    public void setViewportText(Character[] aViewportText)
    {
        this.viewportText = aViewportText;
    }

    public List<Span> getSpanAnnotations()
    {
        return spanAnnotations;
    }

    public void setSpanAnnotations(List<Span> aSpanAnnotations)
    {
        spanAnnotations = aSpanAnnotations;
    }
}
