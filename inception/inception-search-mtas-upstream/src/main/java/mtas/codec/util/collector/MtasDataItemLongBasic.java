package mtas.codec.util.collector;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import mtas.codec.util.CodecUtil;

/**
 * The Class MtasDataItemLongBasic.
 */
class MtasDataItemLongBasic
    extends MtasDataItemBasic<Long, Double>
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new mtas data item long basic.
     *
     * @param valueSum
     *            the value sum
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
    public MtasDataItemLongBasic(Long valueSum, long valueN, MtasDataCollector<?, ?> sub,
            Set<String> statsItems, String sortType, String sortDirection, int errorNumber,
            Map<String, Integer> errorList, int sourceNumber)
    {
        super(valueSum, valueN, sub, statsItems, sortType, sortDirection, errorNumber, errorList,
                new MtasDataLongOperations(), sourceNumber);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public int compareTo(MtasDataItem<Long, Double> o)
    {
        int compare = 0;
        if (o instanceof MtasDataItemLongBasic) {
            MtasDataItemLongBasic to = (MtasDataItemLongBasic) o;
            MtasDataItemNumberComparator c1 = getComparableValue();
            MtasDataItemNumberComparator c2 = to.getComparableValue();
            compare = (c1 != null && c2 != null) ? c1.compareTo(c2.getValue()) : 0;
        }
        return sortDirection.equals(CodecUtil.SORT_DESC) ? -1 * compare : compare;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.collector.MtasDataItem#getCompareValue()
     */
    @Override
    public MtasDataItemNumberComparator<Long> getCompareValue1()
    {
        switch (sortType) {
        case CodecUtil.STATS_TYPE_N:
            return new MtasDataItemNumberComparator<Long>(valueN, sortDirection);
        case CodecUtil.STATS_TYPE_SUM:
            return new MtasDataItemNumberComparator<Long>(valueSum, sortDirection);
        default:
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.collector.MtasDataItem#getCompareValue2()
     */
    @Override
    public MtasDataItemNumberComparator<Double> getCompareValue2()
    {
        switch (sortType) {
        case CodecUtil.STATS_TYPE_MEAN:
            return new MtasDataItemNumberComparator<Double>(getValue(sortType), sortDirection);
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
        MtasDataItemLongBasic that = (MtasDataItemLongBasic) obj;
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
