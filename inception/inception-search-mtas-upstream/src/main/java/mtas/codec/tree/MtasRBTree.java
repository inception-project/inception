package mtas.codec.tree;

import java.util.HashMap;

/**
 * The Class MtasRBTree.
 */
public class MtasRBTree
    extends MtasTree<MtasRBTreeNode>
{

    /** The index. */
    private final HashMap<String, MtasRBTreeNode> index;

    /**
     * Instantiates a new mtas RB tree.
     *
     * @param singlePoint
     *            the single point
     * @param storePrefixId
     *            the store prefix id
     */
    public MtasRBTree(boolean singlePoint, boolean storePrefixId)
    {
        super(singlePoint, storePrefixId);
        index = new HashMap<>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.tree.MtasTree#addRangeEmpty(int, int)
     */
    @Override
    final protected void addRangeEmpty(int left, int right, int additionalId, long additionalRef)
    {
        String key = left + "_" + right;
        if (index.containsKey(key)) {
            // do nothing (empty...)
        }
        else {
            root = addRange(root, left, right, additionalId, additionalRef, null, null);
            root.color = MtasRBTreeNode.BLACK;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.tree.MtasTree#addSinglePoint(int, java.lang.Integer, java.lang.Long)
     */
    @Override
    final protected void addSinglePoint(int position, int additionalId, long additionalRef,
            Integer id, Long ref)
    {
        addRange(position, position, additionalId, additionalRef, id, ref);
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.tree.MtasTree#addRange(int, int, java.lang.Integer, java.lang.Long)
     */
    @Override
    final protected void addRange(int left, int right, int additionalId, long additionalRef,
            Integer id, Long ref)
    {
        String key = left + "_" + right;
        if (index.containsKey(key)) {
            index.get(key).addIdAndRef(id, ref, additionalId, additionalRef);
        }
        else {
            root = addRange(root, left, right, additionalId, additionalRef, id, ref);
            root.color = MtasRBTreeNode.BLACK;
        }
    }

    /**
     * Adds the range.
     *
     * @param n
     *            the n
     * @param left
     *            the left
     * @param right
     *            the right
     * @param additionalId
     *            the additional id
     * @param additionalRef
     *            the additional ref
     * @param id
     *            the id
     * @param ref
     *            the ref
     * @return the mtas RB tree node
     */
    private MtasRBTreeNode addRange(MtasRBTreeNode n, Integer left, Integer right, int additionalId,
            long additionalRef, Integer id, Long ref)
    {
        MtasRBTreeNode localN = n;
        if (localN == null) {
            String key = left.toString() + "_" + right.toString();
            localN = new MtasRBTreeNode(left, right, MtasRBTreeNode.RED, 1);
            localN.addIdAndRef(id, ref, additionalId, additionalRef);
            index.put(key, localN);
        }
        else {
            if (left <= localN.left) {
                localN.leftChild = addRange(localN.leftChild, left, right, additionalId,
                        additionalRef, id, ref);
                updateMax(localN, localN.leftChild);
            }
            else {
                localN.rightChild = addRange(localN.rightChild, left, right, additionalId,
                        additionalRef, id, ref);
                updateMax(localN, localN.rightChild);
            }
            if (isRed(localN.rightChild) && !isRed(localN.leftChild)) {
                localN = rotateLeft(localN);
            }
            if (isRed(localN.leftChild) && isRed(localN.leftChild.leftChild)) {
                localN = rotateRight(localN);
            }
            if (isRed(localN.leftChild) && isRed(localN.rightChild)) {
                flipColors(localN);
            }
            localN.n = size(localN.leftChild) + size(localN.rightChild) + 1;
        }
        return localN;
    }

    /**
     * Update max.
     *
     * @param n
     *            the n
     * @param c
     *            the c
     */
    private void updateMax(MtasRBTreeNode n, MtasRBTreeNode c)
    {
        if (c != null) {
            if (n.max < c.max) {
                n.max = c.max;
            }
        }
    }

    // make a left-leaning link lean to the right
    /**
     * Rotate right.
     *
     * @param n
     *            the n
     * @return the mtas RB tree node
     */
    private MtasRBTreeNode rotateRight(MtasRBTreeNode n)
    {
        assert (n != null) && isRed(n.leftChild);
        MtasRBTreeNode x = n.leftChild;
        n.leftChild = x.rightChild;
        x.rightChild = n;
        x.color = x.rightChild.color;
        x.rightChild.color = MtasRBTreeNode.RED;
        x.n = n.n;
        n.n = size(n.leftChild) + size(n.rightChild) + 1;
        setMax(n);
        setMax(x);
        return x;
    }

    // make a right-leaning link lean to the left
    /**
     * Rotate left.
     *
     * @param n
     *            the n
     * @return the mtas RB tree node
     */
    private MtasRBTreeNode rotateLeft(MtasRBTreeNode n)
    {
        assert (n != null) && isRed(n.rightChild);
        MtasRBTreeNode x = n.rightChild;
        n.rightChild = x.leftChild;
        x.leftChild = n;
        x.color = x.leftChild.color;
        x.leftChild.color = MtasRBTreeNode.RED;
        x.n = n.n;
        n.n = size(n.leftChild) + size(n.rightChild) + 1;
        setMax(n);
        setMax(x);
        return x;
    }

    // flip the colors of a node and its two children
    /**
     * Flip colors.
     *
     * @param n
     *            the n
     */
    private void flipColors(MtasRBTreeNode n)
    {
        // n must have opposite color of its two children
        assert (n != null) && (n.leftChild != null) && (n.rightChild != null);
        assert (!isRed(n) && isRed(n.leftChild) && isRed(n.rightChild))
                || (isRed(n) && !isRed(n.leftChild) && !isRed(n.rightChild));
        n.color ^= 1;
        n.leftChild.color ^= 1;
        n.rightChild.color ^= 1;
    }

    /**
     * Checks if is red.
     *
     * @param n
     *            the n
     * @return true, if is red
     */
    private boolean isRed(MtasRBTreeNode n)
    {
        if (n == null) {
            return false;
        }
        return n.color == MtasRBTreeNode.RED;
    }

    /**
     * Size.
     *
     * @param n
     *            the n
     * @return the int
     */
    private int size(MtasRBTreeNode n)
    {
        if (n == null)
            return 0;
        return n.n;
    }

    /**
     * Sets the max.
     *
     * @param n
     *            the new max
     */
    private void setMax(MtasRBTreeNode n)
    {
        n.max = n.right;
        if (n.leftChild != null) {
            n.max = Math.max(n.max, n.leftChild.max);
        }
        if (n.rightChild != null) {
            n.max = Math.max(n.max, n.rightChild.max);
        }
    }

}
