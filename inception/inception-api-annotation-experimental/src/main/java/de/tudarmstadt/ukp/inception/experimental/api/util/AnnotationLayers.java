package de.tudarmstadt.ukp.inception.experimental.api.util;

import java.util.List;

import de.tudarmstadt.ukp.inception.experimental.api.AnnotationSystemAPIService;

public class AnnotationLayers
{
    private AnnotationSystemAPIService annotationSystemAPIService;
    private List<String> annotationLayers;

    public AnnotationLayers(AnnotationSystemAPIService aAnnotationSystemAPIService)
    {
        annotationSystemAPIService = aAnnotationSystemAPIService;
        annotationLayers = getAnnotationLayers();

    }

    public List<String> getAnnotationLayers()
    {
        return annotationSystemAPIService.getAllAnnotationLayers();
    }
}
