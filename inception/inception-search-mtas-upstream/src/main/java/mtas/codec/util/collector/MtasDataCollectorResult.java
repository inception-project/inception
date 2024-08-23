package mtas.codec.util.collector;

import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import mtas.codec.util.CodecUtil;
import mtas.codec.util.DataCollector;

/**
 * The Class MtasDataCollectorResult.
 *
 * @param <T1>
 *            the generic type
 * @param <T2>
 *            the generic type
 */
public class MtasDataCollectorResult<T1 extends Number & Comparable<T1>, T2 extends Number & Comparable<T2>>
    implements Serializable
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The list. */
    private SortedMap<String, MtasDataItem<T1, T2>> list;

    /** The item. */
    private MtasDataItem<T1, T2> item;

    /** The sort type. */
    private String sortType;

    /** The sort direction. */
    private String sortDirection;

    /** The collector type. */
    private String collectorType;

    /** The last sort value. */
    private MtasDataItemNumberComparator lastSortValue;

    /** The start key. */
    String startKey;

    /** The end key. */
    String endKey;

    /**
     * Instantiates a new mtas data collector result.
     *
     * @param collectorType
     *            the collector type
     * @param sortType
     *            the sort type
     * @param sortDirection
     *            the sort direction
     * @param basicList
     *            the basic list
     * @param start
     *            the start
     * @param number
     *            the number
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public MtasDataCollectorResult(String collectorType, String sortType, String sortDirection,
            NavigableMap<String, MtasDataItem<T1, T2>> basicList, Integer start, Integer number)
        throws IOException
    {
        this(collectorType, sortType, sortDirection);
        if (sortType == null || sortType.equals(CodecUtil.SORT_TERM)) {
            if (sortDirection == null || sortDirection.equals(CodecUtil.SORT_ASC)) {
                list = basicList;
            }
            else if (sortDirection.equals(CodecUtil.SORT_DESC)) {
                list = basicList.descendingMap();
            }
            else {
                throw new IOException("unknown sort direction " + sortDirection);
            }
        }
        else if (CodecUtil.isStatsType(sortType)) {
            // comperator
            Comparator<String> valueComparator = new Comparator<String>()
            {
                @Override
                public int compare(String k1, String k2)
                {
                    int compare = basicList.get(k1).compareTo(basicList.get(k2));
                    return compare == 0 ? k1.compareTo(k2) : compare;
                }
            };
            SortedMap<String, MtasDataItem<T1, T2>> sortedByValues = new TreeMap<>(valueComparator);
            sortedByValues.putAll(basicList);
            list = sortedByValues;
        }
        else {
            throw new IOException("unknown sort type " + sortType);
        }
        int listStart = start == null ? 0 : start;
        if (number == null || (start == 0 && number >= list.size())) {
            // do nothing, full list is ok
        }
        else if (listStart < list.size() && number > 0) {
            // subset
            String boundaryEndKey = null;
            int counter = 0;
            MtasDataItem<T1, T2> previous = null;
            for (Entry<String, MtasDataItem<T1, T2>> entry : list.entrySet()) {
                if (listStart == counter) {
                    startKey = entry.getKey();
                }
                else if (listStart + number <= counter) {
                    if (sortType == null || sortType.equals(CodecUtil.SORT_TERM)) {
                        endKey = entry.getKey();
                        boundaryEndKey = entry.getKey();
                        break;
                    }
                    else if (previous != null) {
                        if (previous.compareTo(entry.getValue()) != 0) {
                            // ready, previous not equal to this item
                            break;
                        }
                        else {
                            // register this as possible boundaryEndKey, but continue
                            boundaryEndKey = entry.getKey();
                        }
                    }
                    else {
                        // possibly ready, but check next
                        endKey = entry.getKey();
                        boundaryEndKey = entry.getKey();
                        previous = entry.getValue();
                    }
                }
                counter++;
            }
            if (startKey != null) {
                if (boundaryEndKey != null) {
                    list = list.subMap(startKey, boundaryEndKey);
                }
                else {
                    list = list.tailMap(startKey);
                }
            }
            else {
                list = new TreeMap<>();
            }
        }
        else {
            list = new TreeMap<>();
        }
        if (list.size() > 0 && sortType != null) {
            lastSortValue = list.get(list.lastKey()).getComparableValue();
        }
    }

    /**
     * Instantiates a new mtas data collector result.
     *
     * @param collectorType
     *            the collector type
     * @param item
     *            the item
     */
    public MtasDataCollectorResult(String collectorType, MtasDataItem<T1, T2> item)
    {
        this(collectorType, null, null);
        this.item = item;
    }

    /**
     * Instantiates a new mtas data collector result.
     *
     * @param collectorType
     *            the collector type
     * @param sortType
     *            the sort type
     * @param sortDirection
     *            the sort direction
     */
    public MtasDataCollectorResult(String collectorType, String sortType, String sortDirection)
    {
        list = null;
        item = null;
        lastSortValue = null;
        this.collectorType = collectorType;
        this.sortType = sortType;
        this.sortDirection = sortDirection;
    }

    /**
     * Gets the list.
     *
     * @return the list
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public final SortedMap<String, MtasDataItem<T1, T2>> getList() throws IOException
    {
        return getList(true);
    }

    /**
     * Gets the list.
     *
     * @param reduce
     *            the reduce
     * @return the list
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public final SortedMap<String, MtasDataItem<T1, T2>> getList(boolean reduce) throws IOException
    {
        if (collectorType.equals(DataCollector.COLLECTOR_TYPE_LIST)) {
            if (reduce && startKey != null && endKey != null) {
                return list.subMap(startKey, endKey);
            }
            else {
                return list;
            }
        }
        else {
            throw new IOException("type " + collectorType + " not supported");
        }
    }

    /**
     * Gets the comparator list.
     *
     * @return the comparator list
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("rawtypes")
    public final Map<String, MtasDataItemNumberComparator> getComparatorList() throws IOException
    {
        if (collectorType.equals(DataCollector.COLLECTOR_TYPE_LIST)) {
            LinkedHashMap<String, MtasDataItemNumberComparator> comparatorList = new LinkedHashMap<>();
            for (Entry<String, MtasDataItem<T1, T2>> entry : list.entrySet()) {
                comparatorList.put(entry.getKey(), entry.getValue().getComparableValue());
            }
            return comparatorList;
        }
        else {
            throw new IOException("type " + collectorType + " not supported");
        }
    }

    /**
     * Gets the last sort value.
     *
     * @return the last sort value
     */
    @SuppressWarnings("rawtypes")
    public final MtasDataItemNumberComparator getLastSortValue()
    {
        return lastSortValue;
    }

    /**
     * Gets the data.
     *
     * @return the data
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public final MtasDataItem<T1, T2> getData() throws IOException
    {
        if (collectorType.equals(DataCollector.COLLECTOR_TYPE_DATA)) {
            return item;
        }
        else {
            throw new IOException("type " + collectorType + " not supported");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.getClass().getSimpleName() + "(");
        buffer.append(collectorType + "," + sortType + "," + sortDirection);
        buffer.append(")");
        return buffer.toString();
    }

}
