package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

import java.util.ArrayList;

public class CreateAnnotationJSONInfo
{
    private int begin;
    private int end;
    private long layerId;
    ArrayList<UpdateAnnotationJSONInfo<Object>> updateInfoList; // This field is optional

    public ArrayList<UpdateAnnotationJSONInfo<Object>> getUpdateInfoList()
    {
        return updateInfoList;
    }

    public void setUpdateInfoList(ArrayList<UpdateAnnotationJSONInfo<Object>> updateInfoList)
    {
        this.updateInfoList = updateInfoList;
    }

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

    public long getLayerId()
    {
        return layerId;
    }

    public void setLayerId(long layerId)
    {
        this.layerId = layerId;
    }
}
