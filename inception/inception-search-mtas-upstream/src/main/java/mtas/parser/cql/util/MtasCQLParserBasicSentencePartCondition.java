package mtas.parser.cql.util;

import mtas.parser.cql.ParseException;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Class MtasCQLParserBasicSentencePartCondition.
 */
public abstract class MtasCQLParserBasicSentencePartCondition
{

    /** The minimum occurence. */
    protected int minimumOccurence;

    /** The maximum occurence. */
    protected int maximumOccurence;

    /** The optional. */
    protected boolean optional;

    /** The not. */
    protected boolean not;

    /**
     * Gets the query.
     *
     * @return the query
     * @throws ParseException
     *             the parse exception
     */
    public abstract MtasSpanQuery getQuery() throws ParseException;

    /**
     * Gets the minimum occurence.
     *
     * @return the minimum occurence
     */
    public int getMinimumOccurence()
    {
        return minimumOccurence;
    }

    /**
     * Gets the maximum occurence.
     *
     * @return the maximum occurence
     */
    public int getMaximumOccurence()
    {
        return maximumOccurence;
    }

    /**
     * Sets the occurence.
     *
     * @param min
     *            the min
     * @param max
     *            the max
     * @throws ParseException
     *             the parse exception
     */
    public void setOccurence(int min, int max) throws ParseException
    {
        if ((min < 0) || (min > max) || (max < 1)) {
            throw new ParseException("Illegal number {" + min + "," + max + "}");
        }
        if (min == 0) {
            optional = true;
        }
        minimumOccurence = Math.max(1, min);
        maximumOccurence = max;
    }

    /**
     * Checks if is optional.
     *
     * @return true, if is optional
     */
    public boolean isOptional()
    {
        return optional;
    }

    /**
     * Sets the optional.
     *
     * @param status
     *            the new optional
     */
    public void setOptional(boolean status)
    {
        optional = status;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return toString("", "");
    }

    /**
     * To string.
     *
     * @param firstIndent
     *            the first indent
     * @param indent
     *            the indent
     * @return the string
     */
    public String toString(String firstIndent, String indent)
    {
        String text = "";
        text += firstIndent + "PART";
        if (optional) {
            text += " OPTIONAL";
        }
        if ((minimumOccurence > 1) || (minimumOccurence != maximumOccurence)) {
            if (minimumOccurence != maximumOccurence) {
                text += " {" + minimumOccurence + "," + maximumOccurence + "}";
            }
            else {
                text += " {" + minimumOccurence + "}";
            }
        }
        try {
            text += "\n" + indent + "- Query: " + getQuery().toString(getQuery().getField());
        }
        catch (ParseException e) {
            text += "\n" + indent + "- Query: " + e.getMessage();
        }
        text += "\n";
        return text;
    }

}
