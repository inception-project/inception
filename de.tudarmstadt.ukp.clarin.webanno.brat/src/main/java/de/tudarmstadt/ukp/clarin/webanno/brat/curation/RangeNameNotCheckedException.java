package de.tudarmstadt.ukp.clarin.webanno.brat.curation;

import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;

public class RangeNameNotCheckedException extends BratAnnotationException{
    /**
     *
     */
    private static final long serialVersionUID = 9006025257536279474L;

    public RangeNameNotCheckedException(String message)
    {
        super(message);
    }
}