package de.tudarmstadt.ukp.inception.experimental.api.messages.response;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;

public class SelectAnnotationResponse
{
    private VID annotationAddress;
    private String coveredText;
    private int begin;
    private int end;
    private Type type;
    private Feature feature;
    private String color;

    public VID getAnnotationAddress()
    {
        return annotationAddress;
    }

    public void setAnnotationAddress(VID aAnnotationAddress) {
        annotationAddress = aAnnotationAddress;
    }

    public String getCoveredText()
    {
        return coveredText;
    }

    public void setCoveredText(String aCoveredText)
    {
        coveredText = aCoveredText;
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

    public Type getType()
    {
        return type;
    }

    public void setType(Type aType)
    {
        type = aType;
    }

    public Feature getFeature()
    {
        return feature;
    }

    public void setFeature(Feature aFeature)
    {
        feature = aFeature;
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
