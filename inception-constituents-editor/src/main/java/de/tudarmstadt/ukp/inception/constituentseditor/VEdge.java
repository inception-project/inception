/*
 * Thesis : Collaborative Web-based Tool for the Annotation of Syntactic Parse Trees
 * @author : Asha Joshi
 * Technische Universität Darmstadt 
 */

package de.tudarmstadt.ukp.inception.constituentseditor;

import java.io.Serializable;

/**
 * This class is used for storing information related to parent-child relations between annotations
 * 
 * @author Asha Joshi
 */
public class VEdge
    implements Serializable
{

    private static final long serialVersionUID = 3L;
    private String id;
    private String label;
    private int from;
    private int to;

    /**
     * AnnotationTreeEdge default constructor
     */
    VEdge()
    {
        this.id = "";
        this.label = "";
        this.from = 0;
        this.to = 0;
    }

    /**
     * AnnotationTreeEdge constructor
     * 
     * @param id
     * @param label
     * @param from
     * @param to
     * @param color
     */
    public VEdge(String id, String label, int from, int to, String color)
    {
        this.id = id;
        this.label = label;
        this.from = from;
        this.to = to;
    }

    /**
     * @return the id
     */
    public String getId()
    {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the label
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * @param label
     *            the label to set
     */
    public void setLabel(String label)
    {
        this.label = label;
    }

    /**
     * @return the from
     */
    public int getFrom()
    {
        return from;
    }

    /**
     * @param from
     *            the from to set
     */
    public void setFrom(int from)
    {
        this.from = from;
    }

    /**
     * @return the to
     */
    public int getTo()
    {
        return to;
    }

    /**
     * @param to
     *            the to to set
     */
    public void setTo(int to)
    {
        this.to = to;
    }

    @Override
    public String toString()
    {
        return "\n text " + getLabel() + ", id:" + getId();
    }
}
