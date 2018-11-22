package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

import java.util.ArrayList;

public class AnnotationJSONObject
    extends JSONOutput
{

    private int begin;
    private int end;
    private int annotationId;
    private ArrayList<Long> coveredTokens;
    private long layerId;
    private ArrayList<FeatureRef<Object>> features;

    public int getBegin()
    {
        return begin;
    }

    public void setBegin(int begin)
    {
        this.begin = begin;
    }

    public int getEnd()
    {
        return end;
    }

    public void setEnd(int end)
    {
        this.end = end;
    }

    public int getAnnotationId()
    {
        return annotationId;
    }

    public void setAnnotationId(int annotationId)
    {
        this.annotationId = annotationId;
    }

    public ArrayList<Long> getCoveredTokens()
    {
        return coveredTokens;
    }

    public void setCoveredTokens(ArrayList<Long> coveredTokens)
    {
        this.coveredTokens = coveredTokens;
    }

    public long getLayerId()
    {
        return layerId;
    }

    public void setLayerId(long layerId)
    {
        this.layerId = layerId;
    }

    public ArrayList<FeatureRef<Object>> getFeatures()
    {
        return features;
    }

    public void setFeatures(ArrayList<FeatureRef<Object>> features)
    {
        this.features = features;
    }

}
