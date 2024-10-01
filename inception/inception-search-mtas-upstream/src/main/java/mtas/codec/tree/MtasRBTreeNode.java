package mtas.codec.tree;

/**
 * The Class MtasRBTreeNode.
 */
public class MtasRBTreeNode
    extends MtasTreeNode<MtasRBTreeNode>
{

    /** The Constant BLACK. */
    static final int BLACK = 1;

    /** The Constant RED. */
    static final int RED = 0;

    /** The color. */
    public int color;

    /** The n. */
    public int n;

    // node with start and end position
    /**
     * Instantiates a new mtas RB tree node.
     *
     * @param left
     *            the left
     * @param right
     *            the right
     * @param color
     *            the color
     * @param n
     *            the n
     */
    public MtasRBTreeNode(int left, int right, int color, int n)
    {
        super(left, right);
        this.color = color;
        this.n = n;
    }

}
