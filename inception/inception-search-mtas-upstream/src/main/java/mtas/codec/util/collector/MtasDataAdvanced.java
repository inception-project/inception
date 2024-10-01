package mtas.codec.util.collector;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;

import mtas.codec.util.CodecUtil;
import mtas.codec.util.DataCollector;

/**
 * The Class MtasDataAdvanced.
 *
 * @param <T1>
 *            the generic type
 * @param <T2>
 *            the generic type
 */
abstract class MtasDataAdvanced<T1 extends Number & Comparable<T1>, T2 extends Number & Comparable<T2>>
    extends MtasDataCollector<T1, T2>
    implements Serializable
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The advanced value sum list. */
    protected T1[] advancedValueSumList = null;

    /** The new advanced value sum list. */
    protected T1[] newAdvancedValueSumList = null;

    /** The advanced value max list. */
    protected T1[] advancedValueMaxList = null;

    /** The new advanced value max list. */
    protected T1[] newAdvancedValueMaxList = null;

    /** The advanced value min list. */
    protected T1[] advancedValueMinList = null;

    /** The new advanced value min list. */
    protected T1[] newAdvancedValueMinList = null;

    /** The advanced value sum of squares list. */
    protected T1[] advancedValueSumOfSquaresList = null;

    /** The new advanced value sum of squares list. */
    protected T1[] newAdvancedValueSumOfSquaresList = null;

    /** The advanced value sum of logs list. */
    protected T2[] advancedValueSumOfLogsList = null;

    /** The new advanced value sum of logs list. */
    protected T2[] newAdvancedValueSumOfLogsList = null;

    /** The advanced value N list. */
    protected long[] advancedValueNList = null;

    /** The new advanced value N list. */
    protected long[] newAdvancedValueNList = null;

    /** The operations. */
    protected MtasDataOperations<T1, T2> operations;

    /**
     * Instantiates a new mtas data advanced.
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
    public MtasDataAdvanced(String collectorType, String dataType, SortedSet<String> statsItems,
            String sortType, String sortDirection, Integer start, Integer number,
            String[] subCollectorTypes, String[] subDataTypes, String[] subStatsTypes,
            SortedSet<String>[] subStatsItems, String[] subSortTypes, String[] subSortDirections,
            Integer[] subStart, Integer[] subNumber, MtasDataOperations<T1, T2> operations,
            String segmentRegistration, String boundary)
        throws IOException
    {
        super(collectorType, dataType, CodecUtil.STATS_ADVANCED, statsItems, sortType,
                sortDirection, start, number, subCollectorTypes, subDataTypes, subStatsTypes,
                subStatsItems, subSortTypes, subSortDirections, subStart, subNumber,
                segmentRegistration, boundary);
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
            newAdvancedValueSumList[newPosition] = operations.getZero1();
            newAdvancedValueSumOfLogsList[newPosition] = operations.getZero2();
            newAdvancedValueSumOfSquaresList[newPosition] = operations.getZero1();
            newAdvancedValueMinList[newPosition] = operations.getZero1();
            newAdvancedValueMaxList[newPosition] = operations.getZero1();
            newAdvancedValueNList[newPosition] = 0;
        }
        newErrorNumber[newPosition] += number;
        if (newErrorList[newPosition].containsKey(error)) {
            newErrorList[newPosition].put(error, newErrorList[newPosition].get(error) + number);
        }
        else {
            newErrorList[newPosition].put(error, number);
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
        T1[] tmpNewAdvancedValueSumList = newAdvancedValueSumList;
        T2[] tmpNewAdvancedValueSumOfLogsList = newAdvancedValueSumOfLogsList;
        T1[] tmpNewAdvancedValueSumOfSquaresList = newAdvancedValueSumOfSquaresList;
        T1[] tmpNewAdvancedValueMinList = newAdvancedValueMinList;
        T1[] tmpNewAdvancedValueMaxList = newAdvancedValueMaxList;
        long[] tmpNewAdvancedValueNList = newAdvancedValueNList;
        newAdvancedValueSumList = operations.createVector1(newSize);
        newAdvancedValueSumOfLogsList = operations.createVector2(newSize);
        newAdvancedValueSumOfSquaresList = operations.createVector1(newSize);
        newAdvancedValueMinList = operations.createVector1(newSize);
        newAdvancedValueMaxList = operations.createVector1(newSize);
        newAdvancedValueNList = new long[newSize];
        newPosition = tmpNewPosition;
        System.arraycopy(tmpNewAdvancedValueSumList, 0, newAdvancedValueSumList, 0, tmpOldSize);
        System.arraycopy(tmpNewAdvancedValueSumOfLogsList, 0, newAdvancedValueSumOfLogsList, 0,
                tmpOldSize);
        System.arraycopy(tmpNewAdvancedValueSumOfSquaresList, 0, newAdvancedValueSumOfSquaresList,
                0, tmpOldSize);
        System.arraycopy(tmpNewAdvancedValueMinList, 0, newAdvancedValueMinList, 0, tmpOldSize);
        System.arraycopy(tmpNewAdvancedValueMaxList, 0, newAdvancedValueMaxList, 0, tmpOldSize);
        System.arraycopy(tmpNewAdvancedValueNList, 0, newAdvancedValueNList, 0, tmpOldSize);
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
            T1[] advancedValueSumListCopy = advancedValueSumList.clone();
            T1[] advancedValueMaxListCopy = advancedValueMaxList.clone();
            T1[] advancedValueMinListCopy = advancedValueMinList.clone();
            T1[] advancedValueSumOfSquaresListCopy = advancedValueSumOfSquaresList.clone();
            T2[] advancedValueSumOfLogsListCopy = advancedValueSumOfLogsList.clone();
            long[] advancedValueNListCopy = advancedValueNList.clone();
            size = 0;
            for (int i = 0; i < sizeCopy; i++) {
                if (segmentKeys.contains(keyListCopy[i])) {
                    keyList[size] = keyListCopy[i];
                    advancedValueSumList[size] = advancedValueSumListCopy[i];
                    advancedValueMaxList[size] = advancedValueMaxListCopy[i];
                    advancedValueMinList[size] = advancedValueMinListCopy[i];
                    advancedValueSumOfSquaresList[size] = advancedValueSumOfSquaresListCopy[i];
                    advancedValueSumOfLogsList[size] = advancedValueSumOfLogsListCopy[i];
                    advancedValueNList[size] = advancedValueNListCopy[i];
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
            T1[] advancedValueSumListCopy = advancedValueSumList.clone();
            T1[] advancedValueMaxListCopy = advancedValueMaxList.clone();
            T1[] advancedValueMinListCopy = advancedValueMinList.clone();
            T1[] advancedValueSumOfSquaresListCopy = advancedValueSumOfSquaresList.clone();
            T2[] advancedValueSumOfLogsListCopy = advancedValueSumOfLogsList.clone();
            long[] advancedValueNListCopy = advancedValueNList.clone();
            keyList = new String[keys.size()];
            errorNumber = new int[keys.size()];
            errorList = new HashMap[keys.size()];
            sourceNumberList = new int[keys.size()];
            advancedValueSumList = operations.createVector1(keys.size());
            advancedValueMaxList = operations.createVector1(keys.size());
            advancedValueMinList = operations.createVector1(keys.size());
            advancedValueSumOfSquaresList = operations.createVector1(keys.size());
            advancedValueSumOfLogsList = operations.createVector2(keys.size());
            advancedValueNList = new long[keys.size()];
            size = 0;
            for (int i = 0; i < sizeCopy; i++) {
                if (keys.contains(keyListCopy[i])) {
                    keyList[size] = keyListCopy[i];
                    errorNumber[size] = errorNumberCopy[i];
                    errorList[size] = errorListCopy[i];
                    sourceNumberList[size] = sourceNumberListCopy[i];
                    advancedValueSumList[size] = advancedValueSumListCopy[i];
                    advancedValueMaxList[size] = advancedValueMaxListCopy[i];
                    advancedValueMinList[size] = advancedValueMinListCopy[i];
                    advancedValueSumOfSquaresList[size] = advancedValueSumOfSquaresListCopy[i];
                    advancedValueSumOfLogsList[size] = advancedValueSumOfLogsListCopy[i];
                    advancedValueNList[size] = advancedValueNListCopy[i];
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
        newAdvancedValueSumList[newPosition] = advancedValueSumList[position];
        newAdvancedValueSumOfLogsList[newPosition] = advancedValueSumOfLogsList[position];
        newAdvancedValueSumOfSquaresList[newPosition] = advancedValueSumOfSquaresList[position];
        newAdvancedValueMinList[newPosition] = advancedValueMinList[position];
        newAdvancedValueMaxList[newPosition] = advancedValueMaxList[position];
        newAdvancedValueNList[newPosition] = advancedValueNList[position];
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataCollector#copyFromNew()
     */
    @Override
    protected void copyFromNew()
    {
        advancedValueSumList = newAdvancedValueSumList;
        advancedValueSumOfLogsList = newAdvancedValueSumOfLogsList;
        advancedValueSumOfSquaresList = newAdvancedValueSumOfSquaresList;
        advancedValueMinList = newAdvancedValueMinList;
        advancedValueMaxList = newAdvancedValueMaxList;
        advancedValueNList = newAdvancedValueNList;
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
            T2 valueSumOfLogs = null;
            T1 valueSumOfSquares = null;
            T1 valueMin = null;
            T1 valueMax = null;
            for (int i = 0; i < number; i++) {
                valueSum = (i == 0) ? values[i] : operations.add11(valueSum, values[i]);
                valueSumOfLogs = (i == 0) ? operations.log1(values[i])
                        : operations.add22(valueSumOfLogs, operations.log1(values[i]));
                valueSumOfSquares = (i == 0) ? operations.product11(values[i], values[i])
                        : operations.add11(valueSumOfSquares,
                                operations.product11(values[i], values[i]));
                valueMin = (i == 0) ? values[i] : operations.min11(valueMin, values[i]);
                valueMax = (i == 0) ? values[i] : operations.max11(valueMax, values[i]);
            }
            setValue(newPosition, valueSum, valueSumOfLogs, valueSumOfSquares, valueMin, valueMax,
                    number, currentExisting);
        }
    }

    /**
     * Sets the value.
     *
     * @param newPosition
     *            the new position
     * @param valueSum
     *            the value sum
     * @param valueSumOfLogs
     *            the value sum of logs
     * @param valueSumOfSquares
     *            the value sum of squares
     * @param valueMin
     *            the value min
     * @param valueMax
     *            the value max
     * @param valueN
     *            the value N
     * @param currentExisting
     *            the current existing
     */
    private void setValue(int newPosition, T1 valueSum, T2 valueSumOfLogs, T1 valueSumOfSquares,
            T1 valueMin, T1 valueMax, long valueN, boolean currentExisting)
    {
        if (valueN > 0) {
            if (currentExisting) {
                newAdvancedValueSumList[newPosition] = operations
                        .add11(newAdvancedValueSumList[newPosition], valueSum);
                newAdvancedValueSumOfLogsList[newPosition] = operations
                        .add22(newAdvancedValueSumOfLogsList[newPosition], valueSumOfLogs);
                newAdvancedValueSumOfSquaresList[newPosition] = operations
                        .add11(newAdvancedValueSumOfSquaresList[newPosition], valueSumOfSquares);
                newAdvancedValueMinList[newPosition] = operations
                        .min11(newAdvancedValueMinList[newPosition], valueMin);
                newAdvancedValueMaxList[newPosition] = operations
                        .max11(newAdvancedValueMaxList[newPosition], valueMax);
                newAdvancedValueNList[newPosition] += valueN;
            }
            else {
                newAdvancedValueSumList[newPosition] = valueSum;
                newAdvancedValueSumOfLogsList[newPosition] = valueSumOfLogs;
                newAdvancedValueSumOfSquaresList[newPosition] = valueSumOfSquares;
                newAdvancedValueMinList[newPosition] = valueMin;
                newAdvancedValueMaxList[newPosition] = valueMax;
                newAdvancedValueNList[newPosition] = valueN;
            }
        }
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
        T1[] originalAdvancedValueSumList = advancedValueSumList.clone();
        T2[] originalAdvancedValueSumOfLogsList = advancedValueSumOfLogsList.clone();
        T1[] originalAdvancedValueSumOfSquaresList = advancedValueSumOfSquaresList.clone();
        T1[] originalAdvancedValueMinList = advancedValueMinList.clone();
        T1[] originalAdvancedValueMaxList = advancedValueMaxList.clone();
        long[] originalAdvancedValueNList = advancedValueNList.clone();
        advancedValueSumList = operations.createVector1(mapping.length);
        advancedValueSumOfLogsList = operations.createVector2(mapping.length);
        advancedValueSumOfSquaresList = operations.createVector1(mapping.length);
        advancedValueMinList = operations.createVector1(mapping.length);
        advancedValueMaxList = operations.createVector1(mapping.length);
        advancedValueNList = new long[mapping.length];
        for (int i = 0; i < mapping.length; i++) {
            for (int j = 0; j < mapping[i].length; j++) {
                if (j == 0) {
                    setValue(i, originalAdvancedValueSumList[mapping[i][j]],
                            originalAdvancedValueSumOfLogsList[mapping[i][j]],
                            originalAdvancedValueSumOfSquaresList[mapping[i][j]],
                            originalAdvancedValueMinList[mapping[i][j]],
                            originalAdvancedValueMaxList[mapping[i][j]],
                            originalAdvancedValueNList[mapping[i][j]], false);
                }
                else {
                    setValue(i, originalAdvancedValueSumList[mapping[i][j]],
                            originalAdvancedValueSumOfLogsList[mapping[i][j]],
                            originalAdvancedValueSumOfSquaresList[mapping[i][j]],
                            originalAdvancedValueMinList[mapping[i][j]],
                            originalAdvancedValueMaxList[mapping[i][j]],
                            originalAdvancedValueNList[mapping[i][j]], true);
                }
            }
        }
        advancedValueSumList = newAdvancedValueSumList;
        advancedValueSumOfLogsList = newAdvancedValueSumOfLogsList;
        advancedValueSumOfSquaresList = newAdvancedValueSumOfSquaresList;
        advancedValueMinList = newAdvancedValueMinList;
        advancedValueMaxList = newAdvancedValueMaxList;
        advancedValueNList = newAdvancedValueNList;
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
                || !(newDataCollector instanceof MtasDataAdvanced)) {
            throw new IOException("cannot merge different dataCollectors");
        }
        else {
            segmentRegistration = null;
            @SuppressWarnings("unchecked")
            MtasDataAdvanced<T1, T2> newMtasDataAdvanced = (MtasDataAdvanced<T1, T2>) newDataCollector;
            newMtasDataAdvanced.closeNewList();
            initNewList(newMtasDataAdvanced.getSize());
            newDataCollector.mergedInto = this;
            if (collectorType.equals(DataCollector.COLLECTOR_TYPE_LIST)) {
                for (int i = 0; i < newMtasDataAdvanced.getSize(); i++) {
                    MtasDataCollector<?, ?> subCollector = getSubCollector(
                            newMtasDataAdvanced.keyList[i], increaseSourceNumber);
                    setError(newCurrentPosition, newMtasDataAdvanced.errorNumber[i],
                            newMtasDataAdvanced.errorList[i], newCurrentExisting);
                    setValue(newCurrentPosition, newMtasDataAdvanced.advancedValueSumList[i],
                            newMtasDataAdvanced.advancedValueSumOfLogsList[i],
                            newMtasDataAdvanced.advancedValueSumOfSquaresList[i],
                            newMtasDataAdvanced.advancedValueMinList[i],
                            newMtasDataAdvanced.advancedValueMaxList[i],
                            newMtasDataAdvanced.advancedValueNList[i], newCurrentExisting);
                    if (hasSub() && newMtasDataAdvanced.hasSub()) {
                        subCollector.merge(newMtasDataAdvanced.subCollectorListNextLevel[i],
                                increaseSourceNumber);
                    }
                }
                closeNewList();
            }
            else if (collectorType.equals(DataCollector.COLLECTOR_TYPE_DATA)) {
                if (newMtasDataAdvanced.getSize() > 0) {
                    MtasDataCollector<?, ?> subCollector = getSubCollector(increaseSourceNumber);
                    setError(newCurrentPosition, newMtasDataAdvanced.errorNumber[0],
                            newMtasDataAdvanced.errorList[0], newCurrentExisting);
                    setValue(newCurrentPosition, newMtasDataAdvanced.advancedValueSumList[0],
                            newMtasDataAdvanced.advancedValueSumOfLogsList[0],
                            newMtasDataAdvanced.advancedValueSumOfSquaresList[0],
                            newMtasDataAdvanced.advancedValueMinList[0],
                            newMtasDataAdvanced.advancedValueMaxList[0],
                            newMtasDataAdvanced.advancedValueNList[0], newCurrentExisting);
                    if (hasSub() && newMtasDataAdvanced.hasSub()) {
                        subCollector.merge(newMtasDataAdvanced.subCollectorNextLevel,
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
        initNewListBasic();
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
        initNewListBasic();
    }

    /**
     * Inits the new list basic.
     */
    private void initNewListBasic()
    {
        newAdvancedValueSumList = operations.createVector1(newSize);
        newAdvancedValueSumOfLogsList = operations.createVector2(newSize);
        newAdvancedValueSumOfSquaresList = operations.createVector1(newSize);
        newAdvancedValueMinList = operations.createVector1(newSize);
        newAdvancedValueMaxList = operations.createVector1(newSize);
        newAdvancedValueNList = new long[newSize];
    }

}
