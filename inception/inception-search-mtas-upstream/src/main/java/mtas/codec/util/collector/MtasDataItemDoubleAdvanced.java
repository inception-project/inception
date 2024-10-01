package mtas.codec.util.collector;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import mtas.codec.util.CodecUtil;

/**
 * The Class MtasDataItemDoubleAdvanced.
 */
public class MtasDataItemDoubleAdvanced
    extends MtasDataItemAdvanced<Double, Double>
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new mtas data item double advanced.
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
     * @param sourceNumber
     *            the source number
     */
    public MtasDataItemDoubleAdvanced(Double valueSum, Double valueSumOfLogs,
            Double valueSumOfSquares, Double valueMin, Double valueMax, long valueN,
            MtasDataCollector<?, ?> sub, Set<String> statsItems, String sortType,
            String sortDirection, int errorNumber, Map<String, Integer> errorList, int sourceNumber)
    {
        super(valueSum, valueSumOfLogs, valueSumOfSquares, valueMin, valueMax, valueN, sub,
                statsItems, sortType, sortDirection, errorNumber, errorList,
                new MtasDataDoubleOperations(), sourceNumber);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public int compareTo(MtasDataItem<Double, Double> o)
    {
        int compare = 0;
        if (o instanceof MtasDataItemDoubleAdvanced) {
            MtasDataItemDoubleAdvanced to = (MtasDataItemDoubleAdvanced) o;
            MtasDataItemNumberComparator c1 = getComparableValue();
            MtasDataItemNumberComparator c2 = to.getComparableValue();
            compare = (c1 != null && c2 != null) ? c1.compareTo(c2.getValue()) : 0;
        }
        return sortDirection.equals(CodecUtil.SORT_DESC) ? -1 * compare : compare;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.collector.MtasDataItem#getCompareValue1()
     */
    @Override
    public MtasDataItemNumberComparator<Double> getCompareValue1()
    {
        switch (sortType) {
        case CodecUtil.STATS_TYPE_SUM:
            return new MtasDataItemNumberComparator<>(valueSum, sortDirection);
        case CodecUtil.STATS_TYPE_MAX:
            return new MtasDataItemNumberComparator<>(valueMax, sortDirection);
        case CodecUtil.STATS_TYPE_MIN:
            return new MtasDataItemNumberComparator<>(valueMin, sortDirection);
        case CodecUtil.STATS_TYPE_SUMSQ:
            return new MtasDataItemNumberComparator<>(valueSumOfSquares, sortDirection);
        default:
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.collector.MtasDataItem#getCompareValue2()
     */
    public MtasDataItemNumberComparator<Double> getCompareValue2()
    {
        switch (sortType) {
        case CodecUtil.STATS_TYPE_SUMOFLOGS:
            return new MtasDataItemNumberComparator<>(valueSumOfLogs, sortDirection);
        case CodecUtil.STATS_TYPE_MEAN:
        case CodecUtil.STATS_TYPE_GEOMETRICMEAN:
        case CodecUtil.STATS_TYPE_STANDARDDEVIATION:
        case CodecUtil.STATS_TYPE_VARIANCE:
        case CodecUtil.STATS_TYPE_POPULATIONVARIANCE:
        case CodecUtil.STATS_TYPE_QUADRATICMEAN:
            return new MtasDataItemNumberComparator<>(getValue(sortType), sortDirection);
        default:
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return this.getClass().getSimpleName() + "[" + valueSum + "," + valueN + "]";
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MtasDataItemDoubleAdvanced that = (MtasDataItemDoubleAdvanced) obj;
        MtasDataItemNumberComparator<?> c1 = getComparableValue();
        MtasDataItemNumberComparator<?> c2 = that.getComparableValue();
        return (c1 != null && c2 != null && c1.equals(c2));
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(this.getClass().getSimpleName(), getComparableValue());
    }

}
