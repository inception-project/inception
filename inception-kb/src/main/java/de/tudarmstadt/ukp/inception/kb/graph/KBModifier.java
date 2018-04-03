package de.tudarmstadt.ukp.inception.kb.graph;

import java.io.Serializable;

public class KBModifier implements Serializable
{
    private static final long serialVersionUID = 4648563545691138244L;

    private KBStatement kbStatement;

    private KBHandle kbProperty;

    private Object value;

    public KBStatement getKbStatement()
    {
        return kbStatement;
    }

    public void setKbStatement(KBStatement kbStatement)
    {
        this.kbStatement = kbStatement;
    }

    public KBHandle getKbProperty()
    {
        return kbProperty;
    }

    public void setKbProperty(KBHandle kbProperty)
    {
        this.kbProperty = kbProperty;
    }

    public Object getValue()
    {
        return value;
    }

    public void setValue(Object value)
    {
        this.value = value;
    }
}
