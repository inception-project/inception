package mtas.codec.tree;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import mtas.analysis.token.MtasPosition;
import mtas.analysis.token.MtasToken;

/**
 * The Class MtasTree.
 *
 * @param <N>
 *            the number type
 */
abstract public class MtasTree<N extends MtasTreeNode<N>>
{

    /** The Constant SINGLE_POSITION_TREE. */
    final public static byte SINGLE_POSITION_TREE = 1;

    /** The Constant STORE_ADDITIONAL_ID. */
    final public static byte STORE_ADDITIONAL_ID = 2;

    /** The root. */
    protected N root;

    /** The closed. */
    private Boolean closed;

    /** The single point. */
    protected Boolean singlePoint;

    /** The store prefix and term ref. */
    protected Boolean storePrefixAndTermRef;

    /**
     * Instantiates a new mtas tree.
     *
     * @param singlePoint
     *            the single point
     * @param storePrefixAndTermRef
     *            the store prefix and term ref
     */
    public MtasTree(boolean singlePoint, boolean storePrefixAndTermRef)
    {
        root = null;
        closed = false;
        this.singlePoint = singlePoint;
        this.storePrefixAndTermRef = storePrefixAndTermRef;
    }

    /**
     * Adds the id from doc.
     *
     * @param docId
     *            the doc id
     * @param reference
     *            the reference
     */
    final public void addIdFromDoc(Integer docId, Long reference)
    {
        if (!closed && (docId != null)) {
            addSinglePoint(docId, 0, 0, docId, reference);
        }
    }

    /**
     * Adds the parent from token.
     *
     * @param token
     *            the token
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    final public void addParentFromToken(MtasToken token) throws IOException
    {
        if (!closed && (token != null)) {
            if (token.checkParentId()) {
                addSinglePoint(token.getParentId(), token.getPrefixId(), token.getTermRef(),
                        token.getId(), token.getTokenRef());
            }
        }
    }

    /**
     * Adds the position and object from token.
     *
     * @param token
     *            the token
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    final public void addPositionAndObjectFromToken(MtasToken token) throws IOException
    {
        addPositionFromToken(token, token.getTokenRef());
    }

    // final public <T> void addPositionAndTermFromToken(MtasToken<T> token) {
    // addPositionFromToken(token, token.getTermRef());
    // }

    /**
     * Adds the position from token.
     *
     * @param token
     *            the token
     * @param ref
     *            the ref
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    final private void addPositionFromToken(MtasToken token, Long ref) throws IOException
    {
        int prefixId = storePrefixAndTermRef ? token.getPrefixId() : 0;
        if (!closed && (token != null)) {
            if (token.checkPositionType(MtasPosition.POSITION_SINGLE)) {
                addSinglePoint(token.getPositionStart(), prefixId, token.getTermRef(),
                        token.getId(), ref);
            }
            else if (token.checkPositionType(MtasPosition.POSITION_RANGE)) {
                addRange(token.getPositionStart(), token.getPositionEnd(), prefixId,
                        token.getTermRef(), token.getId(), ref);
            }
            else if (token.checkPositionType(MtasPosition.POSITION_SET)) {
                // split set into minimum number of single points and ranges
                SortedMap<Integer, Integer> list = new TreeMap<>();
                int[] positions = token.getPositions();
                Integer lastPoint = null;
                Integer startPoint = null;
                for (int position : positions) {
                    if (lastPoint == null) {
                        startPoint = position;
                        lastPoint = position;
                    }
                    else if ((position - lastPoint) != 1) {
                        list.put(startPoint, lastPoint);
                        startPoint = position;
                    }
                    lastPoint = position;
                }
                if (lastPoint != null) {
                    list.put(startPoint, lastPoint);
                }
                for (Entry<Integer, Integer> entry : list.entrySet()) {
                    if (entry.getKey().equals(entry.getValue())) {
                        addSinglePoint(entry.getKey(), prefixId, token.getTermRef(), token.getId(),
                                ref);
                    }
                    else {
                        addRange(entry.getKey(), entry.getValue(), prefixId, token.getTermRef(),
                                token.getId(), ref);
                    }
                }
            }
        }
    }

    /**
     * Close.
     *
     * @return the n
     */
    final public N close()
    {
        if (root == null) {
            addRangeEmpty(0, 0, 0, 0);
        }
        closed = true;
        return root;
    }

    /**
     * Adds the single point.
     *
     * @param position
     *            the position
     * @param additionalId
     *            the additional id
     * @param additionalRef
     *            the additional ref
     * @param id
     *            the id
     * @param ref
     *            the ref
     */
    abstract protected void addSinglePoint(int position, int additionalId, long additionalRef,
            Integer id, Long ref);

    /**
     * Adds the range.
     *
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
     */
    abstract protected void addRange(int left, int right, int additionalId, long additionalRef,
            Integer id, Long ref);

    /**
     * Adds the range empty.
     *
     * @param left
     *            the left
     * @param right
     *            the right
     * @param additionalId
     *            the additional id
     * @param additionalRef
     *            the additional ref
     */
    abstract protected void addRangeEmpty(int left, int right, int additionalId,
            long additionalRef);

    /**
     * Checks if is single point.
     *
     * @return true, if is single point
     */
    final public boolean isSinglePoint()
    {
        return singlePoint;
    }

    /**
     * Checks if is store prefix and term ref.
     *
     * @return true, if is store prefix and term ref
     */
    final public boolean isStorePrefixAndTermRef()
    {
        return storePrefixAndTermRef;
    }

    /**
     * Prints the balance.
     */
    final public void printBalance()
    {
        printBalance(1, root);
    }

    /**
     * Prints the balance.
     *
     * @param p
     *            the p
     * @param n
     *            the n
     */
    final private void printBalance(Integer p, N n)
    {
        if (n != null) {
            printBalance((p + 1), n.leftChild);
            String format = "%" + (3 * p) + "s";
            System.out.print(String.format(format, ""));
            if (n.left == n.right) {
                System.out
                        .println("[" + n.left + "] (" + n.max + ") : " + n.ids.size() + " tokens");
            }
            else {
                System.out.println("[" + n.left + "-" + n.right + "] (" + n.max + ") : "
                        + n.ids.size() + " tokens");
            }
            printBalance((p + 1), n.rightChild);
        }
    }

}
