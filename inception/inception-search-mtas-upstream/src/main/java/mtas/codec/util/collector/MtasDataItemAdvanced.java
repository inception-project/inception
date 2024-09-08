package mtas.codec.util.collector;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mtas.codec.util.CodecUtil;

/**
 * The Class MtasDataItemAdvanced.
 *
 * @param <T1>
 *            the generic type
 * @param <T2>
 *            the generic type
 */
abstract class MtasDataItemAdvanced<T1 extends Number & Comparable<T1>, T2 extends Number & Comparable<T2>>
    extends MtasDataItem<T1, T2>
    implements Serializable
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The value sum. */
    protected T1 valueSum;

    /** The value sum of logs. */
    protected T2 valueSumOfLogs;

    /** The value sum of squares. */
    protected T1 valueSumOfSquares;

    /** The value min. */
    protected T1 valueMin;

    /** The value max. */
    protected T1 valueMax;

    /** The value N. */
    protected Long valueN;

    /** The operations. */
    protected MtasDataOperations<T1, T2> operations;

    /**
     * Instantiates a new mtas data item advanced.
     *
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
     * @param operations
     *            the operations
     * @param sourceNumber
     *            the source number
     */
    public MtasDataItemAdvanced(T1 valueSum, T2 valueSumOfLogs, T1 valueSumOfSquares, T1 valueMin,
            T1 valueMax, Long valueN, MtasDataCollector<?, ?> sub, Set<String> statsItems,
            String sortType, String sortDirection, int errorNumber, Map<String, Integer> errorList,
            MtasDataOperations<T1, T2> operations, int sourceNumber)
    {
        super(sub, statsItems, sortType, sortDirection, errorNumber, errorList, sourceNumber);
        this.valueSum = valueSum;
        this.valueSumOfLogs = valueSumOfLogs;
        this.valueSumOfSquares = valueSumOfSquares;
        this.valueMin = valueMin;
        this.valueMax = valueMax;
        this.valueN = valueN;
        this.operations = operations;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataItem#add(mtas.codec.util.
     * DataCollector.MtasDataItem)
     */
    @Override
    public void add(MtasDataItem<T1, T2> newItem) throws IOException
    {
        if (newItem instanceof MtasDataItemAdvanced) {
            MtasDataItemAdvanced<T1, T2> newTypedItem = (MtasDataItemAdvanced<T1, T2>) newItem;
            valueSum = operations.add11(valueSum, newTypedItem.valueSum);
            valueSumOfLogs = operations.add22(valueSumOfLogs, newTypedItem.valueSumOfLogs);
            valueSumOfSquares = operations.add11(valueSumOfSquares, newTypedItem.valueSumOfSquares);
            valueMin = operations.min11(valueMin, newTypedItem.valueMin);
            valueMax = operations.max11(valueMax, newTypedItem.valueMax);
            valueN += newTypedItem.valueN;
            recomputeComparableSortValue = true;
        }
        else {
            throw new IOException("can only add MtasDataItemAdvanced");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataItem#rewrite()
     */
    @Override
    public Map<String, Object> rewrite(boolean showDebugInfo) throws IOException
    {
        Map<String, Object> response = new HashMap<>();
        for (String statsItem : getStatsItems()) {
            if (statsItem.equals(CodecUtil.STATS_TYPE_SUM)) {
                response.put(statsItem, valueSum);
            }
            else if (statsItem.equals(CodecUtil.STATS_TYPE_N)) {
                response.put(statsItem, valueN);
            }
            else if (statsItem.equals(CodecUtil.STATS_TYPE_MAX)) {
                response.put(statsItem, valueMax);
            }
            else if (statsItem.equals(CodecUtil.STATS_TYPE_MIN)) {
                response.put(statsItem, valueMin);
            }
            else if (statsItem.equals(CodecUtil.STATS_TYPE_SUMSQ)) {
                response.put(statsItem, valueSumOfSquares);
            }
            else if (statsItem.equals(CodecUtil.STATS_TYPE_SUMOFLOGS)) {
                response.put(statsItem, valueSumOfLogs);
            }
            else if (statsItem.equals(CodecUtil.STATS_TYPE_MEAN)) {
                response.put(statsItem, getValue(statsItem));
            }
            else if (statsItem.equals(CodecUtil.STATS_TYPE_GEOMETRICMEAN)) {
                response.put(statsItem, getValue(statsItem));
            }
            else if (statsItem.equals(CodecUtil.STATS_TYPE_STANDARDDEVIATION)) {
                response.put(statsItem, getValue(statsItem));
            }
            else if (statsItem.equals(CodecUtil.STATS_TYPE_VARIANCE)) {
                response.put(statsItem, getValue(statsItem));
            }
            else if (statsItem.equals(CodecUtil.STATS_TYPE_POPULATIONVARIANCE)) {
                response.put(statsItem, getValue(statsItem));
            }
            else if (statsItem.equals(CodecUtil.STATS_TYPE_QUADRATICMEAN)) {
                response.put(statsItem, getValue(statsItem));
            }
            else {
                response.put(statsItem, null);
            }
        }
        if (errorNumber > 0) {
            Map<String, Object> errorResponse = new HashMap<>();
            for (Entry<String, Integer> entry : getErrorList().entrySet()) {
                errorResponse.put(entry.getKey(), entry.getValue());
            }
            response.put("errorNumber", errorNumber);
            response.put("errorList", errorResponse);
        }
        if (showDebugInfo) {
            response.put("sourceNumber", sourceNumber);
            response.put("stats", "advanced");
        }
        return response;
    }

    /**
     * Gets the value.
     *
     * @param statsType
     *            the stats type
     * @return the value
     */
    protected T2 getValue(String statsType)
    {
        if (statsType.equals(CodecUtil.STATS_TYPE_MEAN)) {
            return operations.divide1(valueSum, valueN);
        }
        else if (statsType.equals(CodecUtil.STATS_TYPE_GEOMETRICMEAN)) {
            return operations.exp2(operations.divide2(valueSumOfLogs, valueN));
        }
        else if (statsType.equals(CodecUtil.STATS_TYPE_STANDARDDEVIATION)) {
            return operations.sqrt2(operations.divide2(
                    operations.subtract12(valueSumOfSquares,
                            operations.divide1(operations.product11(valueSum, valueSum), valueN)),
                    (valueN - 1)));
        }
        else if (statsType.equals(CodecUtil.STATS_TYPE_VARIANCE)) {
            return operations.divide2(
                    operations.subtract12(valueSumOfSquares,
                            operations.divide1(operations.product11(valueSum, valueSum), valueN)),
                    (valueN - 1));
        }
        else if (statsType.equals(CodecUtil.STATS_TYPE_POPULATIONVARIANCE)) {
            return operations.divide2(
                    operations.subtract12(valueSumOfSquares,
                            operations.divide1(operations.product11(valueSum, valueSum), valueN)),
                    valueN);
        }
        else if (statsType.equals(CodecUtil.STATS_TYPE_QUADRATICMEAN)) {
            return operations.sqrt2(operations.divide1(valueSumOfSquares, valueN));
        }
        else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.collector.MtasDataItem#getCompareValueType()
     */
    @Override
    public int getCompareValueType() throws IOException
    {
        switch (sortType) {
        case CodecUtil.STATS_TYPE_N:
            return 0;
        case CodecUtil.STATS_TYPE_SUM:
            return 1;
        case CodecUtil.STATS_TYPE_MAX:
            return 1;
        case CodecUtil.STATS_TYPE_MIN:
            return 1;
        case CodecUtil.STATS_TYPE_SUMSQ:
            return 1;
        case CodecUtil.STATS_TYPE_SUMOFLOGS:
            return 2;
        case CodecUtil.STATS_TYPE_MEAN:
            return 2;
        case CodecUtil.STATS_TYPE_GEOMETRICMEAN:
            return 2;
        case CodecUtil.STATS_TYPE_STANDARDDEVIATION:
            return 2;
        case CodecUtil.STATS_TYPE_VARIANCE:
            return 2;
        case CodecUtil.STATS_TYPE_POPULATIONVARIANCE:
            return 2;
        case CodecUtil.STATS_TYPE_QUADRATICMEAN:
            return 2;
        default:
            throw new IOException("sortType " + sortType + " not supported");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.collector.MtasDataItem#getCompareValue0()
     */
    @Override
    public final MtasDataItemNumberComparator<Long> getCompareValue0()
    {
        switch (sortType) {
        case CodecUtil.STATS_TYPE_N:
            return new MtasDataItemNumberComparator<Long>(valueN, sortDirection);
        default:
            return null;
        }
    }

}
