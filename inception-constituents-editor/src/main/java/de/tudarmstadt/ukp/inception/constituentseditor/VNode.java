/*
 * Thesis : Collaborative Web-based Tool for the Annotation of Syntactic Parse Trees
 * @author : Asha Joshi
 * Technische Universität Darmstadt 
 */

package de.tudarmstadt.ukp.inception.constituentseditor;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * This class is used for storing information related to POS and Constituent annotations represented
 * at tree nodes as the front end
 * 
 * @author Asha Joshi
 */
public class VNode
    implements Serializable
{
    private static final long serialVersionUID = 4L;
    private int id;
    private int[] offset;
    private String text;

    /**
     * AnnotationTreeNode constructor
     */
    VNode()
    {
        this.id = 0;
        this.offset = new int[2];
        this.text = "";
    }

    /**
     * AnnotationTreeNode constructor
     * 
     * @param node_id
     * @param type
     * @param offset
     * @param text
     * @param color
     */
    public VNode(int node_id, String type, int[] offset, String text, String color)
    {
        this.id = node_id;
        this.offset = offset;
        this.text = text;
    }

    /**
     * @return the id
     */
    public int getId()
    {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(int id)
    {
        this.id = id;
    }

    /**
     * @return the offset
     */
    public int[] getOffset()
    {
        return offset;
    }

    /**
     * @param offset
     *            the offset to set
     */
    public void setOffset(int[] offset)
    {
        this.offset = offset;
    }

    /**
     * @return the text
     */
    public String getText()
    {
        return text;
    }

    /**
     * @param text
     *            the text to set
     */
    public void setText(String text)
    {
        this.text = text;
    }

    @Override
    public String toString()
    {
        return "\n text " + getText() + ", id:" + getId();
    }

    public boolean alreadyExist(List<VNode> nodes)
    {
        boolean exist = false;
        Iterator<VNode> nodesIterator = nodes.iterator();
        while (nodesIterator.hasNext()) {
            VNode existingnode = nodesIterator.next();
            if (existingnode.getId() == this.getId()) {
                exist = true;
                break;
            }
        }
        return exist;
    }
}
