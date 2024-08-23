package mtas.codec.util.collector;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;

import mtas.codec.util.CodecUtil;
import mtas.codec.util.DataCollector;

/**
 * The Class MtasDataBasic.
 *
 * @param <T1>
 *            the generic type
 * @param <T2>
 *            the generic type
 */
abstract class MtasDataBasic<T1 extends Number & Comparable<T1>, T2 extends Number & Comparable<T2>>
    extends MtasDataCollector<T1, T2>
    implements Serializable
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The basic value sum list. */
    protected T1[] basicValueSumList = null;

    /** The basic value N list. */
    protected long[] basicValueNList = null;

    /** The new basic value sum list. */
    protected transient T1[] newBasicValueSumList = null;

    /** The new basic value N list. */
    protected transient long[] newBasicValueNList = null;

    /** The operations. */
    protected MtasDataOperations<T1, T2> operations;

    /**
     * Instantiates a new mtas data basic.
     *
     * @param collectorType
     *            the collector type
     * @param dataType
     *            the data type
     * @param statsItems
     *            the stats items
     * @param sortType
     *            the sort type
     * @param sortDirection
     *            the sort direction
     * @param start
     *            the start
     * @param number
     *            the number
     * @param subCollectorTypes
     *            the sub collector types
     * @param subDataTypes
     *            the sub data types
     * @param subStatsTypes
     *            the sub stats types
     * @param subStatsItems
     *            the sub stats items
     * @param subSortTypes
     *            the sub sort types
     * @param subSortDirections
     *            the sub sort directions
     * @param subStart
     *            the sub start
     * @param subNumber
     *            the sub number
     * @param operations
     *            the operations
     * @param segmentRegistration
     *            the segment registration
     * @param boundary
     *            the boundary
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public MtasDataBasic(String collectorType, String dataType, SortedSet<String> statsItems,
            String sortType, String sortDirection, Integer start, Integer number,
            String[] subCollectorTypes, String[] subDataTypes, String[] subStatsTypes,
            SortedSet<String>[] subStatsItems, String[] subSortTypes, String[] subSortDirections,
            Integer[] subStart, Integer[] subNumber, MtasDataOperations<T1, T2> operations,
            String segmentRegistration, String boundary)
        throws IOException
    {
        super(collectorType, dataType, CodecUtil.STATS_BASIC, statsItems, sortType, sortDirection,
                start, number, subCollectorTypes, subDataTypes, subStatsTypes, subStatsItems,
                subSortTypes, subSortDirections, subStart, subNumber, segmentRegistration,
                boundary);
        this.operations = operations;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataCollector#error(java.lang.String)
     */
    @Override
    public final void error(String error, int number) throws IOException
    {
        getSubCollector(false);
        setError(newCurrentPosition, error, number, newCurrentExisting);
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataCollector#error(java.lang.String[],
     * java.lang.String)
     */
    @Override
    public final void error(String key, String error, int number) throws IOException
    {
        if (key != null) {
            getSubCollector(key, false);
            setError(newCurrentPosition, error, number, newCurrentExisting);
        }
    }

    /**
     * Sets the error.
     *
     * @param newPosition
     *            the new position
     * @param error
     *            the error
     * @param number
     *            the number of occurrences
     * @param currentExisting
     *            the current existing
     */
    protected void setError(int newPosition, String error, int number, boolean currentExisting)
    {
        if (!currentExisting) {
            newBasicValueSumList[newPosition] = operations.getZero1();
            newBasicValueNList[newPosition] = 0;
        }
        newErrorNumber[newPosition] += number;
        if (newErrorList[newPosition].containsKey(error)) {
            newErrorList[newPosition].put(error, newErrorList[newPosition].get(error) + number);
        }
        else {
            newErrorList[newPosition].put(error, number);
        }
    }

    /**
     * Sets the value.
     *
     * @param newPosition
     *            the new position
     * @param valueSum
     *            the value sum
     * @param valueN
     *            the value N
     * @param currentExisting
     *            the current existing
     */
    protected void setValue(int newPosition, T1 valueSum, long valueN, boolean currentExisting)
    {
        if (valueN > 0) {
            if (currentExisting) {
                newBasicValueSumList[newPosition] = operations
                        .add11(newBasicValueSumList[newPosition], valueSum);
                newBasicValueNList[newPosition] += valueN;
            }
            else {
                newBasicValueSumList[newPosition] = valueSum;
                newBasicValueNList[newPosition] = valueN;
            }
        }
    }

    /**
     * Sets the value.
     *
     * @param newPosition
     *            the new position
     * @param values
     *            the values
     * @param number
     *            the number
     * @param currentExisting
     *            the current existing
     */
    protected void setValue(int newPosition, T1[] values, int number, boolean currentExisting)
    {
        if (number > 0) {
            T1 valueSum = null;
            for (int i = 0; i < number; i++) {
                valueSum = (i == 0) ? values[i] : operations.add11(valueSum, values[i]);
            }
            if (currentExisting) {
                newBasicValueSumList[newPosition] = operations
                        .add11(newBasicValueSumList[newPosition], valueSum);
                newBasicValueNList[newPosition] += number;
            }
            else {
                newBasicValueSumList[newPosition] = valueSum;
                newBasicValueNList[newPosition] = number;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataCollector#increaseNewListSize()
     */
    @Override
    protected final void increaseNewListSize() throws IOException
    {
        // register old situation
        int tmpOldSize = newKeyList.length;
        int tmpNewPosition = newPosition;
        // increase
        super.increaseNewListSize();
        // reconstruct
        T1[] tmpNewBasicValueList = newBasicValueSumList;
        long[] tmpNewBasicValueNList = newBasicValueNList;
        newBasicValueSumList = operations.createVector1(newSize);
        newBasicValueNList = new long[newSize];
        newPosition = tmpNewPosition;
        System.arraycopy(tmpNewBasicValueList, 0, newBasicValueSumList, 0, tmpOldSize);
        System.arraycopy(tmpNewBasicValueNList, 0, newBasicValueNList, 0, tmpOldSize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.collector.MtasDataCollector#reduceToSegmentKeys()
     */
    @Override
    public void reduceToSegmentKeys()
    {
        if (segmentRegistration != null && size > 0) {
            int sizeCopy = size;
            String[] keyListCopy = keyList.clone();
            T1[] basicValueSumListCopy = basicValueSumList.clone();
            long[] basicValueNListCopy = basicValueNList.clone();
            size = 0;
            for (int i = 0; i < sizeCopy; i++) {
                if (segmentKeys.contains(keyListCopy[i])) {
                    keyList[size] = keyListCopy[i];
                    basicValueSumList[size] = basicValueSumListCopy[i];
                    basicValueNList[size] = basicValueNListCopy[i];
                    size++;
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.collector.MtasDataCollector#reduceToKeys(java.util.Set)
     */
    @SuppressWarnings("unchecked")
    public void reduceToKeys(Set<String> keys)
    {
        if (size > 0) {
            int sizeCopy = size;
            String[] keyListCopy = keyList.clone();
            int[] errorNumberCopy = errorNumber.clone();
            HashMap<String, Integer>[] errorListCopy = errorList.clone();
            int[] sourceNumberListCopy = sourceNumberList.clone();
            T1[] basicValueSumListCopy = basicValueSumList.clone();
            long[] basicValueNListCopy = basicValueNList.clone();
            keyList = new String[keys.size()];
            errorNumber = new int[keys.size()];
            errorList = new HashMap[keys.size()];
            sourceNumberList = new int[keys.size()];
            basicValueSumList = operations.createVector1(keys.size());
            basicValueNList = new long[keys.size()];
            size = 0;
            for (int i = 0; i < sizeCopy; i++) {
                if (keys.contains(keyListCopy[i])) {
                    keyList[size] = keyListCopy[i];
                    errorNumber[size] = errorNumberCopy[i];
                    errorList[size] = errorListCopy[i];
                    sourceNumberList[size] = sourceNumberListCopy[i];
                    basicValueSumList[size] = basicValueSumListCopy[i];
                    basicValueNList[size] = basicValueNListCopy[i];
                    size++;
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataCollector#copyToNew(int, int)
     */
    @Override
    protected void copyToNew(int position, int newPosition)
    {
        newBasicValueSumList[newPosition] = basicValueSumList[position];
        newBasicValueNList[newPosition] = basicValueNList[position];
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataCollector#copyFromNew()
     */
    @Override
    protected void copyFromNew()
    {
        basicValueSumList = newBasicValueSumList;
        basicValueNList = newBasicValueNList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataCollector#remapData(int[][])
     */
    @Override
    protected void remapData(int[][] mapping) throws IOException
    {
        super.remapData(mapping);
        T1[] originalBasicValueSumList = basicValueSumList.clone();
        long[] originalBasicValueNList = basicValueNList.clone();
        basicValueSumList = operations.createVector1(mapping.length);
        basicValueNList = new long[mapping.length];
        for (int i = 0; i < mapping.length; i++) {
            for (int j = 0; j < mapping[i].length; j++) {
                if (j == 0) {
                    setValue(i, originalBasicValueSumList[mapping[i][j]],
                            originalBasicValueNList[mapping[i][j]], false);
                }
                else {
                    setValue(i, originalBasicValueSumList[mapping[i][j]],
                            originalBasicValueNList[mapping[i][j]], true);
                }
            }
        }
        basicValueSumList = newBasicValueSumList;
        basicValueNList = newBasicValueNList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataCollector#merge(mtas.codec.util.
     * DataCollector.MtasDataCollector)
     */
    @Override
    public void merge(MtasDataCollector<?, ?> newDataCollector, boolean increaseSourceNumber)
        throws IOException
    {
        closeNewList();
        if (!collectorType.equals(newDataCollector.getCollectorType())
                || !dataType.equals(newDataCollector.getDataType())
                || !statsType.equals(newDataCollector.getStatsType())
                || !(newDataCollector instanceof MtasDataBasic)) {
            throw new IOException("cannot merge different dataCollectors");
        }
        else {
            segmentRegistration = null;
            @SuppressWarnings("unchecked")
            MtasDataBasic<T1, T2> newMtasDataBasic = (MtasDataBasic<T1, T2>) newDataCollector;
            newMtasDataBasic.closeNewList();
            initNewList(newMtasDataBasic.getSize());
            newDataCollector.mergedInto = this;
            if (collectorType.equals(DataCollector.COLLECTOR_TYPE_LIST)) {
                for (int i = 0; i < newMtasDataBasic.getSize(); i++) {
                    MtasDataCollector<?, ?> subCollector = getSubCollector(
                            newMtasDataBasic.keyList[i], increaseSourceNumber);
                    setError(newCurrentPosition, newMtasDataBasic.errorNumber[i],
                            newMtasDataBasic.errorList[i], newCurrentExisting);
                    setValue(newCurrentPosition, newMtasDataBasic.basicValueSumList[i],
                            newMtasDataBasic.basicValueNList[i], newCurrentExisting);
                    if (hasSub() && newMtasDataBasic.hasSub()) {
                        // single key implies exactly one subCollector if hasSub
                        subCollector.merge(newMtasDataBasic.subCollectorListNextLevel[i],
                                increaseSourceNumber);
                    }
                }
                closeNewList();
            }
            else if (collectorType.equals(DataCollector.COLLECTOR_TYPE_DATA)) {
                if (newMtasDataBasic.getSize() > 0) {
                    MtasDataCollector<?, ?> subCollector = getSubCollector(increaseSourceNumber);
                    setError(newCurrentPosition, newMtasDataBasic.errorNumber[0],
                            newMtasDataBasic.errorList[0], newCurrentExisting);
                    setValue(newCurrentPosition, newMtasDataBasic.basicValueSumList[0],
                            newMtasDataBasic.basicValueNList[0], newCurrentExisting);
                    if (hasSub() && newMtasDataBasic.hasSub()) {
                        subCollector.merge(newMtasDataBasic.subCollectorNextLevel,
                                increaseSourceNumber);
                    }
                }
                closeNewList();
            }
            else {
                throw new IOException("cannot merge " + collectorType);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataCollector#initNewList(int)
     */
    @Override
    public final void initNewList(int maxNumberOfTerms) throws IOException
    {
        super.initNewList(maxNumberOfTerms);
        initNewListBasic(maxNumberOfTerms);
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataCollector#initNewList(int, java.lang.String)
     */
    @Override
    public final void initNewList(int maxNumberOfTerms, String segmentName, int segmentNumber,
            String boundary)
        throws IOException
    {
        super.initNewList(maxNumberOfTerms, segmentName, segmentNumber, boundary);
        initNewListBasic(maxNumberOfTerms);
    }

    /**
     * Inits the new list basic.
     *
     * @param maxNumberOfTerms
     *            the max number of terms
     */
    private void initNewListBasic(int maxNumberOfTerms)
    {
        newBasicValueSumList = operations.createVector1(newSize);
        newBasicValueNList = new long[newSize];
    }

}
