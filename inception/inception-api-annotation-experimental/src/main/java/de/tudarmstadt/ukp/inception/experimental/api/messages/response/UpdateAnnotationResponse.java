package de.tudarmstadt.ukp.inception.experimental.api.messages.response;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public class UpdateAnnotationResponse
{
    private VID annotationAddress;
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
