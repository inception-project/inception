package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

public class CreateOutputMessage
    extends Message
{
    int createdAnnotationId;
    long layerId;
    String layerUiName;

    public CreateOutputMessage() {
        
    }
    
    public CreateOutputMessage(String text, int createdAnnotationId, long layerId,
            String layerUiName)
    {
        super(text);
        this.createdAnnotationId = createdAnnotationId;
        this.layerId = layerId;
        this.layerUiName = layerUiName;
    }

    public String getLayerUiName()
    {
        return layerUiName;
    }

    public void setLayerUiName(String layerUiName)
    {
        this.layerUiName = layerUiName;
    }

    public int getCreatedAnnotationId()
    {
        return createdAnnotationId;
    }

    public void setCreatedAnnotationId(int createdAnnotationId)
    {
        this.createdAnnotationId = createdAnnotationId;
    }

    public long getLayerId()
    {
        return layerId;
    }

    public void setLayerId(int layerId)
    {
        this.layerId = layerId;
    }

}
