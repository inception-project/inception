package de.tudarmstadt.ukp.inception.experimental.editor.websocket.model;

import java.util.List;

import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model.Annotation;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model.Range;

public class SelectAnnotationMessage
{
    private String clientId;
    private String id;
    private List<Range> ranges;
    private String quote;
    private String text;
    private String color;

    public SelectAnnotationMessage(Annotation aAnnotation, String aCliendId)
    {
        this.clientId = aCliendId;
        this.id = aAnnotation.getId();
        this.ranges = aAnnotation.getRanges();
        this.quote = aAnnotation.getQuote();
        this.text = aAnnotation.getText();
        this.color = aAnnotation.getColor();
    }

    public String getClientId()
    {
        return clientId;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }
    public String getId()
    {
        return id;
    }

    public void setId(String aId)
    {
        id = aId;
    }

    public List<Range> getRanges()
    {
        return ranges;
    }

    public void setRanges(List<Range> aRanges)
    {
        ranges = aRanges;
    }

    public String getQuote()
    {
        return quote;
    }

    public void setQuote(String aQuote)
    {
        quote = aQuote;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String aText)
    {
        text = aText;
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
