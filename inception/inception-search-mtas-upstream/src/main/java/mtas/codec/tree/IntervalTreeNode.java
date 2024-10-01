package mtas.codec.tree;

import java.util.ArrayList;

import mtas.codec.util.CodecSearchTree.MtasTreeHit;

/**
 * The Class IntervalTreeNode.
 *
 * @param <T>
 *            the generic type
 * @param <N>
 *            the number type
 */
abstract public class IntervalTreeNode<T, N extends IntervalTreeNode<T, N>>
{

    /** The left. */
    public int left;

    /** The right. */
    public int right;

    /** The max. */
    public int max;

    /** The min. */
    public int min;

    /** The left child. */
    public N leftChild;

    /** The right child. */
    public N rightChild;

    /** The lists. */
    public ArrayList<ArrayList<MtasTreeHit<T>>> lists;

    // node with start and end position
    /**
     * Instantiates a new interval tree node.
     *
     * @param left
     *            the left
     * @param right
     *            the right
     */
    public IntervalTreeNode(int left, int right)
    {
        this.left = left;
        this.right = right;
        min = left;
        max = right;
        lists = new ArrayList<ArrayList<MtasTreeHit<T>>>();
    }

    // add id to node
    /**
     * Adds the list.
     *
     * @param list
     *            the list
     */
    final public void addList(ArrayList<MtasTreeHit<T>> list)
    {
        if (list != null) {
            lists.add(list);
        }
    }

}
