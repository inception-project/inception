package mtas.codec.util.collector;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;

import mtas.codec.util.CodecUtil;
import mtas.codec.util.DataCollector;

/**
 * The Class MtasDataFull.
 *
 * @param <T1>
 *            the generic type
 * @param <T2>
 *            the generic type
 */
abstract class MtasDataFull<T1 extends Number & Comparable<T1>, T2 extends Number & Comparable<T2>>
    extends MtasDataCollector<T1, T2>
    implements Serializable
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The full value list. */
    protected T1[][] fullValueList = null;

    /** The new full value list. */
    protected T1[][] newFullValueList = null;

    /** The operations. */
    protected MtasDataOperations<T1, T2> operations;

    /**
     * Instantiates a new mtas data full.
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
    public MtasDataFull(String collectorType, String dataType, SortedSet<String> statsItems,
            String sortType, String sortDirection, Integer start, Integer number,
            String[] subCollectorTypes, String[] subDataTypes, String[] subStatsTypes,
            SortedSet<String>[] subStatsItems, String[] subSortTypes, String[] subSortDirections,
            Integer[] subStart, Integer[] subNumber, MtasDataOperations<T1, T2> operations,
            String segmentRegistration, String boundary)
        throws IOException
    {
        super(collectorType, dataType, CodecUtil.STATS_FULL, statsItems, sortType, sortDirection,
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
            newFullValueList[newPosition] = operations.createVector1(0);
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
        T1[][] tmpNewFullValueList = newFullValueList;
        newFullValueList = operations.createMatrix1(newSize);
        newPosition = tmpNewPosition;
        System.arraycopy(tmpNewFullValueList, 0, newFullValueList, 0, tmpOldSize);
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
            T1[][] fullValueListCopy = fullValueList.clone();
            size = 0;
            for (int i = 0; i < sizeCopy; i++) {
                if (segmentKeys.contains(keyListCopy[i])) {
                    keyList[size] = keyListCopy[i];
                    fullValueList[size] = fullValueListCopy[i];
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
            T1[][] fullValueListCopy = fullValueList.clone();
            for (int i = 0; i < fullValueListCopy.length; i++) {
                if (fullValueListCopy[i] != null) {
                    fullValueListCopy[i] = fullValueListCopy[i].clone();
                }
            }
            keyList = new String[keys.size()];
            errorNumber = new int[keys.size()];
            errorList = new HashMap[keys.size()];
            sourceNumberList = new int[keys.size()];
            fullValueList = operations.createMatrix1(keys.size());
            size = 0;
            for (int i = 0; i < sizeCopy; i++) {
                if (keys.contains(keyListCopy[i])) {
                    keyList[size] = keyListCopy[i];
                    errorNumber[size] = errorNumberCopy[i];
                    errorList[size] = errorListCopy[i];
                    sourceNumberList[size] = sourceNumberListCopy[i];
                    fullValueList[size] = fullValueListCopy[i];
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
        newFullValueList[newPosition] = fullValueList[position];
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataCollector#copyFromNew()
     */
    @Override
    protected void copyFromNew()
    {
        fullValueList = newFullValueList;
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
            if (currentExisting) {
                T1[] tmpList = operations
                        .createVector1(newFullValueList[newPosition].length + number);
                System.arraycopy(newFullValueList[newPosition], 0, tmpList, 0,
                        newFullValueList[newPosition].length);
                System.arraycopy(values, 0, tmpList, newFullValueList[newPosition].length, number);
                newFullValueList[newPosition] = tmpList;
            }
            else {
                if (number < values.length) {
                    T1[] tmpList = operations.createVector1(number);
                    System.arraycopy(values, 0, tmpList, 0, number);
                    newFullValueList[newPosition] = tmpList;
                }
                else {
                    newFullValueList[newPosition] = values;
                }
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
        T1[][] originalFullValueList = fullValueList.clone();
        fullValueList = operations.createMatrix1(mapping.length);
        for (int i = 0; i < mapping.length; i++) {
            for (int j = 0; j < mapping[i].length; j++) {
                if (j == 0) {
                    setValue(i, originalFullValueList[mapping[i][j]],
                            originalFullValueList[mapping[i][j]].length, false);
                }
                else {
                    setValue(i, originalFullValueList[mapping[i][j]],
                            originalFullValueList[mapping[i][j]].length, true);
                }
            }
        }
        fullValueList = newFullValueList;
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
                || !(newDataCollector instanceof MtasDataFull)) {
            throw new IOException("cannot merge different dataCollectors");
        }
        else {
            segmentRegistration = null;
            @SuppressWarnings("unchecked")
            MtasDataFull<T1, T2> newMtasDataFull = (MtasDataFull<T1, T2>) newDataCollector;
            closeNewList();
            initNewList(newMtasDataFull.getSize());
            newDataCollector.mergedInto = this;
            if (collectorType.equals(DataCollector.COLLECTOR_TYPE_LIST)) {
                for (int i = 0; i < newMtasDataFull.getSize(); i++) {
                    if (newMtasDataFull.fullValueList[i].length > 0) {
                        MtasDataCollector<?, ?> subCollector = getSubCollector(
                                newMtasDataFull.keyList[i], increaseSourceNumber);
                        setError(newCurrentPosition, newMtasDataFull.errorNumber[i],
                                newMtasDataFull.errorList[i], newCurrentExisting);
                        setValue(newCurrentPosition, newMtasDataFull.fullValueList[i],
                                newMtasDataFull.fullValueList[i].length, newCurrentExisting);
                        if (hasSub() && newMtasDataFull.hasSub()) {
                            // single key implies exactly one subCollector if hasSub
                            subCollector.merge(newMtasDataFull.subCollectorListNextLevel[i],
                                    increaseSourceNumber);
                        }
                    }
                }
            }
            else if (collectorType.equals(DataCollector.COLLECTOR_TYPE_DATA)) {
                if (newMtasDataFull.getSize() > 0) {
                    MtasDataCollector<?, ?> subCollector = getSubCollector(increaseSourceNumber);
                    setError(newCurrentPosition, newMtasDataFull.errorNumber[0],
                            newMtasDataFull.errorList[0], newCurrentExisting);
                    setValue(newCurrentPosition, newMtasDataFull.fullValueList[0],
                            newMtasDataFull.fullValueList[0].length, newCurrentExisting);
                    if (hasSub() && newMtasDataFull.hasSub()) {
                        subCollector.merge(newMtasDataFull.subCollectorNextLevel,
                                increaseSourceNumber);
                    }
                }
            }
            else {
                throw new IOException("cannot merge " + collectorType);
            }
            closeNewList();
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
        newFullValueList = operations.createMatrix1(newSize);
    }

}
