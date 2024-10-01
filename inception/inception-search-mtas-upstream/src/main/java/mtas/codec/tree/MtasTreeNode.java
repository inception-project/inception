package mtas.codec.tree;

import java.util.HashMap;

/**
 * The Class MtasTreeNode.
 *
 * @param <N>
 *            the number type
 */
abstract public class MtasTreeNode<N extends MtasTreeNode<N>>
{

    /** The left. */
    public int left;

    /** The right. */
    public int right;

    /** The max. */
    public int max;

    /** The left child. */
    public N leftChild;

    /** The right child. */
    public N rightChild;

    /** The ids. */
    public HashMap<Integer, MtasTreeNodeId> ids;

    // node with start and end position
    /**
     * Instantiates a new mtas tree node.
     *
     * @param left
     *            the left
     * @param right
     *            the right
     */
    public MtasTreeNode(int left, int right)
    {
        this.left = left;
        this.right = right;
        this.max = right;
        this.ids = new HashMap<Integer, MtasTreeNodeId>();
    }

    // add id to node
    /**
     * Adds the id and ref.
     *
     * @param id
     *            the id
     * @param ref
     *            the ref
     * @param additionalId
     *            the additional id
     * @param additionalRef
     *            the additional ref
     */
    final public void addIdAndRef(Integer id, Long ref, int additionalId, long additionalRef)
    {
        if (id != null) {
            MtasTreeNodeId tnId = new MtasTreeNodeId(ref, additionalId, additionalRef);
            ids.put(id, tnId);
        }
    }

}
