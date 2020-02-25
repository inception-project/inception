package de.tudarmstadt.ukp.clarin.webanno.api.annotation.model;

import java.io.Serializable;

public abstract class AnnotatorStateMetaDataKey<T>
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Override
    public int hashCode()
    {
        return getClass().hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object aObj)
    {
        return aObj != null && getClass().equals(aObj.getClass());
    }
    
}
