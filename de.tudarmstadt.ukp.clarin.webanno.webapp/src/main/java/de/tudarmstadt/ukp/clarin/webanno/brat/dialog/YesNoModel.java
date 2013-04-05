package de.tudarmstadt.ukp.clarin.webanno.brat.dialog;

import java.io.Serializable;

public class YesNoModel
    implements Serializable
{
    private static final long serialVersionUID = 2726479145407475099L;
    private boolean yesFinishAnnotation;

    public boolean isYesFinishAnnotation()
    {
        return yesFinishAnnotation;
    }

    public void setYesFinishAnnotation(boolean aYesFinishAnnotation)
    {
        yesFinishAnnotation = aYesFinishAnnotation;
    }

}
