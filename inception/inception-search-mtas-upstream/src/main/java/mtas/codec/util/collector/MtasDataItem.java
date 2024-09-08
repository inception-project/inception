package mtas.codec.util.collector;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class MtasDataItem.
 *
 * @param <T1>
 *            the generic type
 * @param <T2>
 *            the generic type
 */
public abstract class MtasDataItem<T1 extends Number & Comparable<T1>, T2 extends Number & Comparable<T2>>
    implements Serializable, Comparable<MtasDataItem<T1, T2>>
{

    /** The log. */
    private static final Logger log = LoggerFactory.getLogger(MtasDataItem.class);

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The sub. */
    protected MtasDataCollector<?, ?> sub;

    /** The stats items. */
    private Set<String> statsItems;

    /** The sort type. */
    protected String sortType;

    /** The sort direction. */
    protected String sortDirection;

    /** The error number. */
    protected int errorNumber;

    /** The error list. */
    private Map<String, Integer> errorList;

    /** The comparable sort value. */
    protected MtasDataItemNumberComparator<?> comparableSortValue;

    /** The recompute comparable sort value. */
    protected boolean recomputeComparableSortValue;

    /** The source number. */
    protected int sourceNumber;

    /**
     * Instantiates a new mtas data item.
     *
     * @param sub
     *            the sub
     * @param statsItems
     *            the stats items
     * @param sortType
     *            the sort type
     * @param sortDirection
     *            the sort direction
     * @param errorNumber
     *            the error number
     * @param errorList
     *            the error list
     * @param sourceNumber
     *            the source number
     */
    public MtasDataItem(MtasDataCollector<?, ?> sub, Set<String> statsItems, String sortType,
            String sortDirection, int errorNumber, Map<String, Integer> errorList, int sourceNumber)
    {
        this.sub = sub;
        this.statsItems = statsItems;
        this.sortType = sortType;
        this.sortDirection = sortDirection;
        this.errorNumber = errorNumber;
        this.errorList = errorList;
        this.sourceNumber = sourceNumber;
        this.comparableSortValue = null;
        this.recomputeComparableSortValue = true;
    }

    /**
     * Adds the.
     *
     * @param newItem
     *            the new item
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public abstract void add(MtasDataItem<T1, T2> newItem) throws IOException;

    /**
     * Rewrite.
     *
     * @param showDebugInfo
     *            the show debug info
     * @return the map
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public abstract Map<String, Object> rewrite(boolean showDebugInfo) throws IOException;

    /**
     * Gets the sub.
     *
     * @return the sub
     */
    public MtasDataCollector getSub()
    {
        return sub;
    }

    /**
     * Gets the compare value type.
     *
     * @return the compare value type
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    protected abstract int getCompareValueType() throws IOException;

    /**
     * Gets the stats items.
     *
     * @return the stats items
     */
    protected final Set<String> getStatsItems()
    {
        return statsItems;
    }

    /**
     * Gets the error list.
     *
     * @return the error list
     */
    protected final Map<String, Integer> getErrorList()
    {
        return errorList;
    }

    /**
     * Compute comparable value.
     */
    private void computeComparableValue()
    {
        recomputeComparableSortValue = false;
        try {
            int type = getCompareValueType();
            switch (type) {
            case 0:
                comparableSortValue = getCompareValue0();
                break;
            case 1:
                comparableSortValue = getCompareValue1();
                break;
            case 2:
                comparableSortValue = getCompareValue2();
                break;
            default:
                comparableSortValue = null;
                break;
            }
        }
        catch (IOException e) {
            log.debug("Error", e);
            comparableSortValue = null;
        }
    }

    /**
     * Gets the comparable value.
     *
     * @return the comparable value
     */
    protected final MtasDataItemNumberComparator getComparableValue()
    {
        if (recomputeComparableSortValue) {
            computeComparableValue();
        }
        return comparableSortValue;
    }

    /**
     * Gets the compare value 0.
     *
     * @return the compare value 0
     */
    protected abstract MtasDataItemNumberComparator<Long> getCompareValue0();

    /**
     * Gets the compare value 1.
     *
     * @return the compare value 1
     */
    protected abstract MtasDataItemNumberComparator<T1> getCompareValue1();

    /**
     * Gets the compare value 2.
     *
     * @return the compare value 2
     */
    protected abstract MtasDataItemNumberComparator<T2> getCompareValue2();

    /**
     * The Class NumberComparator.
     *
     * @param <T>
     *            the generic type
     */

}
