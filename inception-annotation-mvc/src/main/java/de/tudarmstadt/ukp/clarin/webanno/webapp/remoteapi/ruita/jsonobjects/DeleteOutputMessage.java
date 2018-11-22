package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

import java.util.ArrayList;

public class DeleteOutputMessage
    extends Message
{
    ArrayList<Integer> deletedAttachedAnnos;
    long layerId;
    String layerUiName;
    long deletedAnnotationId;

    public DeleteOutputMessage(String text, ArrayList<Integer> deletedAttachedAnnos, long layerId,
            String layerUiName, long annotationId)
    {
        super(text);
        this.deletedAttachedAnnos = deletedAttachedAnnos;
        this.layerId = layerId;
        this.layerUiName = layerUiName;
        this.deletedAnnotationId = annotationId;
    }

    public long getLayerId()
    {
        return layerId;
    }

    public void setLayerId(long layerId)
    {
        this.layerId = layerId;
    }

    public String getLayerUiName()
    {
        return layerUiName;
    }

    public void setLayerUiName(String layerUiName)
    {
        this.layerUiName = layerUiName;
    }

    public long getAnnotationId()
    {
        return deletedAnnotationId;
    }

    public void setAnnotationId(long annotationId)
    {
        this.deletedAnnotationId = annotationId;
    }

    public ArrayList<Integer> getDeletedAttachedAnnos()
    {
        return deletedAttachedAnnos;
    }

    public void setDeletedAttachedAnnos(ArrayList<Integer> deletedAttachedAnnos)
    {
        this.deletedAttachedAnnos = deletedAttachedAnnos;
    }

}
