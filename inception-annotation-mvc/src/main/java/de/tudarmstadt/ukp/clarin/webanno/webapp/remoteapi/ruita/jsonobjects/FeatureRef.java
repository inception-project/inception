package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

public class FeatureRef<T>
{
    long id;
    T value;
    boolean isMulti;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public T getValue()
    {
        return value;
    }

    public void setValue(T value)
    {
        this.value = value;
    }

    public boolean isMulti()
    {
        return isMulti;
    }

    public void setMulti(boolean isMulti)
    {
        this.isMulti = isMulti;
    }

}
