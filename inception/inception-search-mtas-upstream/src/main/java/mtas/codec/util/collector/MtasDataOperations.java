package mtas.codec.util.collector;

/**
 * The Interface MtasDataOperations.
 *
 * @param <T1>
 *            the generic type
 * @param <T2>
 *            the generic type
 */
abstract interface MtasDataOperations<T1 extends Number, T2 extends Number>
{

    /**
     * Product 11.
     *
     * @param arg1
     *            the arg 1
     * @param arg2
     *            the arg 2
     * @return the t1
     */
    public T1 product11(T1 arg1, T1 arg2);

    /**
     * Adds the 11.
     *
     * @param arg1
     *            the arg 1
     * @param arg2
     *            the arg 2
     * @return the t1
     */
    public T1 add11(T1 arg1, T1 arg2);

    /**
     * Adds the 22.
     *
     * @param arg1
     *            the arg 1
     * @param arg2
     *            the arg 2
     * @return the t2
     */
    public T2 add22(T2 arg1, T2 arg2);

    /**
     * Subtract 12.
     *
     * @param arg1
     *            the arg 1
     * @param arg2
     *            the arg 2
     * @return the t2
     */
    public T2 subtract12(T1 arg1, T2 arg2);

    /**
     * Divide 1.
     *
     * @param arg1
     *            the arg 1
     * @param arg2
     *            the arg 2
     * @return the t2
     */
    public T2 divide1(T1 arg1, long arg2);

    /**
     * Divide 2.
     *
     * @param arg1
     *            the arg 1
     * @param arg2
     *            the arg 2
     * @return the t2
     */
    public T2 divide2(T2 arg1, long arg2);

    /**
     * Exp 2.
     *
     * @param arg1
     *            the arg 1
     * @return the t2
     */
    public T2 exp2(T2 arg1);

    /**
     * Sqrt 2.
     *
     * @param arg1
     *            the arg 1
     * @return the t2
     */
    public T2 sqrt2(T2 arg1);

    /**
     * Log 1.
     *
     * @param arg1
     *            the arg 1
     * @return the t2
     */
    public T2 log1(T1 arg1);

    /**
     * Min 11.
     *
     * @param arg1
     *            the arg 1
     * @param arg2
     *            the arg 2
     * @return the t1
     */
    public T1 min11(T1 arg1, T1 arg2);

    /**
     * Max 11.
     *
     * @param arg1
     *            the arg 1
     * @param arg2
     *            the arg 2
     * @return the t1
     */
    public T1 max11(T1 arg1, T1 arg2);

    /**
     * Creates the vector 1.
     *
     * @param length
     *            the length
     * @return the t 1 []
     */
    public T1[] createVector1(int length);

    /**
     * Creates the vector 2.
     *
     * @param length
     *            the length
     * @return the t 2 []
     */
    public T2[] createVector2(int length);

    /**
     * Creates the matrix 1.
     *
     * @param length
     *            the length
     * @return the t 1 [][]
     */
    public T1[][] createMatrix1(int length);

    /**
     * Gets the zero 1.
     *
     * @return the zero 1
     */
    public T1 getZero1();

    /**
     * Gets the zero 2.
     *
     * @return the zero 2
     */
    public T2 getZero2();

}
