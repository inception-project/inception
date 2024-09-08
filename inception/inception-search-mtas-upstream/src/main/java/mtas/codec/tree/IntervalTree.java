package mtas.codec.tree;

import java.util.ArrayList;

import mtas.codec.util.CodecSearchTree.MtasTreeHit;

/**
 * The Class IntervalTree.
 *
 * @param <T>
 *            the generic type
 * @param <N>
 *            the number type
 */
abstract public class IntervalTree<T, N extends IntervalTreeNode<T, N>>
{

    /** The root. */
    protected N root;

    /** The current. */
    protected N current;

    /**
     * Instantiates a new interval tree.
     */
    public IntervalTree()
    {
        root = null;
    }

    /**
     * Close.
     *
     * @return the n
     */
    final public N close()
    {
        if (root == null) {
            addRangeEmpty(0, 0);
        }
        return root;
    }

    /**
     * Adds the single point.
     *
     * @param position
     *            the position
     * @param list
     *            the list
     */
    abstract protected void addSinglePoint(int position, ArrayList<MtasTreeHit<T>> list);

    /**
     * Adds the range.
     *
     * @param left
     *            the left
     * @param right
     *            the right
     * @param list
     *            the list
     */
    abstract protected void addRange(int left, int right, ArrayList<MtasTreeHit<T>> list);

    /**
     * Adds the range empty.
     *
     * @param left
     *            the left
     * @param right
     *            the right
     */
    abstract protected void addRangeEmpty(int left, int right);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return printBalance(1, root);
    }

    /**
     * Prints the balance.
     *
     * @param p
     *            the p
     * @param n
     *            the n
     * @return the string
     */
    final private String printBalance(Integer p, N n)
    {
        StringBuilder text = new StringBuilder();
        if (n != null) {
            text.append(printBalance((p + 1), n.leftChild));
            String format = "%" + (3 * p) + "s";
            text.append(String.format(format, ""));
            if (n.left == n.right) {
                text.append("[" + n.left + "] (" + n.max + ") : " + n.lists.size() + " lists\n");
            }
            else {
                text.append("[" + n.left + "-" + n.right + "] (" + n.max + ") : " + n.lists.size()
                        + " lists\n");
            }
            text.append(printBalance((p + 1), n.rightChild));
        }
        return text.toString();
    }

    /**
     * Gets the root.
     *
     * @return the root
     */
    final public N getRoot()
    {
        return root;
    }

    /**
     * Gets the current.
     *
     * @return the current
     */
    final public N getCurrent()
    {
        return current;
    }

    /**
     * Sets the current.
     *
     * @param node
     *            the new current
     */
    final public void setCurrent(N node)
    {
        current = node;
    }

}
