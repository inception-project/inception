package mtas.analysis.token;

/**
 * The Class MtasOffset.
 */
public class MtasOffset
{

    /** The mtas offset start. */
    private int mtasOffsetStart;

    /** The mtas offset end. */
    private int mtasOffsetEnd;

    /**
     * Instantiates a new mtas offset.
     *
     * @param start
     *            the start
     * @param end
     *            the end
     */
    public MtasOffset(int start, int end)
    {
        mtasOffsetStart = start;
        mtasOffsetEnd = end;
    }

    /**
     * Adds the.
     *
     * @param start
     *            the start
     * @param end
     *            the end
     */
    public void add(int start, int end)
    {
        mtasOffsetStart = Math.min(mtasOffsetStart, start);
        mtasOffsetEnd = Math.max(mtasOffsetEnd, end);
    }

    /**
     * Gets the start.
     *
     * @return the start
     */
    public int getStart()
    {
        return mtasOffsetStart;
    }

    /**
     * Gets the end.
     *
     * @return the end
     */
    public int getEnd()
    {
        return mtasOffsetEnd;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "[" + mtasOffsetStart + "-" + mtasOffsetEnd + "]";
    }

}
