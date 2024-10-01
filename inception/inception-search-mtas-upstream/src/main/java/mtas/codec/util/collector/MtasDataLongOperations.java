package mtas.codec.util.collector;

import java.io.Serializable;

/**
 * The Class MtasDataLongOperations.
 */
class MtasDataLongOperations
    implements MtasDataOperations<Long, Double>, Serializable
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#product11(java.lang. Number,
     * java.lang.Number)
     */
    @Override
    public Long product11(Long arg1, Long arg2)
    {
        if (arg1 == null || arg2 == null) {
            return null;
        }
        else {
            return arg1 * arg2;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#add11(java.lang.Number,
     * java.lang.Number)
     */
    @Override
    public Long add11(Long arg1, Long arg2)
    {
        if (arg1 == null || arg2 == null) {
            return null;
        }
        else {
            return arg1 + arg2;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#add22(java.lang.Number,
     * java.lang.Number)
     */
    @Override
    public Double add22(Double arg1, Double arg2)
    {
        if (arg1 == null || arg2 == null) {
            return Double.NaN;
        }
        else {
            return arg1 + arg2;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#subtract12(java.lang. Number,
     * java.lang.Number)
     */
    @Override
    public Double subtract12(Long arg1, Double arg2)
    {
        if (arg1 == null || arg2 == null) {
            return Double.NaN;
        }
        else {
            return arg1.doubleValue() - arg2;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#divide1(java.lang. Number, long)
     */
    @Override
    public Double divide1(Long arg1, long arg2)
    {
        if (arg1 == null) {
            return Double.NaN;
        }
        else {
            return arg1 / (double) arg2;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#divide2(java.lang. Number, long)
     */
    @Override
    public Double divide2(Double arg1, long arg2)
    {
        if (arg1 == null) {
            return Double.NaN;
        }
        else {
            return arg1 / arg2;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#min11(java.lang.Number,
     * java.lang.Number)
     */
    @Override
    public Long min11(Long arg1, Long arg2)
    {
        if (arg1 == null || arg2 == null) {
            return null;
        }
        else {
            return Math.min(arg1, arg2);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#max11(java.lang.Number,
     * java.lang.Number)
     */
    @Override
    public Long max11(Long arg1, Long arg2)
    {
        if (arg1 == null || arg2 == null) {
            return null;
        }
        else {
            return Math.max(arg1, arg2);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#exp2(java.lang.Number)
     */
    @Override
    public Double exp2(Double arg1)
    {
        if (arg1 == null) {
            return Double.NaN;
        }
        else {
            return Math.exp(arg1);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#sqrt2(java.lang.Number)
     */
    @Override
    public Double sqrt2(Double arg1)
    {
        if (arg1 == null) {
            return Double.NaN;
        }
        else {
            return Math.sqrt(arg1);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#log1(java.lang.Number)
     */
    @Override
    public Double log1(Long arg1)
    {
        if (arg1 == null) {
            return Double.NaN;
        }
        else {
            return Math.log(arg1);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#createVector1(int)
     */
    @Override
    public Long[] createVector1(int length)
    {
        return new Long[length];
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#createVector2(int)
     */
    @Override
    public Double[] createVector2(int length)
    {
        return new Double[length];
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#createMatrix1(int)
     */
    @Override
    public Long[][] createMatrix1(int length)
    {
        return new Long[length][];
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#getZero1()
     */
    @Override
    public Long getZero1()
    {
        return Long.valueOf(0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.DataCollector.MtasDataOperations#getZero2()
     */
    @Override
    public Double getZero2()
    {
        return Double.valueOf(0);
    }

}
