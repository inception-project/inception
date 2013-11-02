package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

public class ArcCrossedMultipleSentenceException
    extends BratAnnotationException
{
    private static final long serialVersionUID = 1280015349963924638L;

    public ArcCrossedMultipleSentenceException(String message)
    {
        super(message);
    }
}