package de.tudarmstadt.ukp.inception.experimental.api.messages.response;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public class DeleteAnnotationResponse
{
    private VID annotationAddress;


    public VID getAnnotationAddress()
    {
        return annotationAddress;
    }

    public void setAnnotationAddress(VID aAnnotationAddress) {
        annotationAddress = aAnnotationAddress;
    }
}
