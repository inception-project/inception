package de.tudarmstadt.ukp.clarin.webanno.api.annotation.model;

import java.io.Serializable;
import java.util.ArrayList;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;

public class FeatureModel
    implements Serializable
{
    private static final long serialVersionUID = 3512979848975446735L;
    public final AnnotationFeature feature;
    public Serializable value;

    public FeatureModel(AnnotationFeature aFeature, Serializable aValue)
    {
        feature = aFeature;
        value = aValue;

        // Avoid having null here because otherwise we have to handle null in zillion places!
        if (value == null && MultiValueMode.ARRAY.equals(aFeature.getMultiValueMode())) {
            value = new ArrayList<>();
        }
    }
}