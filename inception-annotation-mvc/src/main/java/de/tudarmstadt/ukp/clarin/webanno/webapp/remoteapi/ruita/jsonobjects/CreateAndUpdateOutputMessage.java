package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

import java.util.ArrayList;
import java.util.HashMap;

public class CreateAndUpdateOutputMessage
    extends CreateOutputMessage
{
    ArrayList<HashMap<String, Object>> updateResponses;

    public CreateAndUpdateOutputMessage(String text, int createdAnnotationId, long layerId,
            String layerUiName, ArrayList<HashMap<String, Object>> updateResponses)
    {
        super(text, createdAnnotationId, layerId, layerUiName);
        this.updateResponses = updateResponses;
    }

    public ArrayList<HashMap<String, Object>> getUpdateResponses()
    {
        return updateResponses;
    }

    public void setUpdateResponses(ArrayList<HashMap<String, Object>> updateResponses)
    {
        this.updateResponses = updateResponses;
    }

}
