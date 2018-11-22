package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

public class UpdateAnnotationJSONInfo<T>
{
    private int annotationId;
    private int featureId;
    private T value;

    public int getAnnotationId()
    {
        return annotationId;
    }

    public void setAnnotationId(int annotationId)
    {
        this.annotationId = annotationId;
    }

    public int getFeatureId()
    {
        return featureId;
    }

    public void setFeatureId(int featureId)
    {
        this.featureId = featureId;
    }

    public T getValue()
    {
        return value;
    }

    public void setValue(T value)
    {
        this.value = value;
    }

}
