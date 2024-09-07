package mtas.search.spans;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Class MtasSpanSequenceItem.
 */
public class MtasSpanSequenceItem
{

    /** The span query. */
    private MtasSpanQuery spanQuery;

    /** The optional. */
    private boolean optional;

    /**
     * Instantiates a new mtas span sequence item.
     *
     * @param spanQuery
     *            the span query
     * @param optional
     *            the optional
     */
    public MtasSpanSequenceItem(MtasSpanQuery spanQuery, boolean optional)
    {
        this.spanQuery = spanQuery;
        this.optional = optional;
    }

    /**
     * Gets the query.
     *
     * @return the query
     */
    public MtasSpanQuery getQuery()
    {
        return spanQuery;
    }

    /**
     * Sets the query.
     *
     * @param spanQuery
     *            the new query
     */
    public void setQuery(MtasSpanQuery spanQuery)
    {
        this.spanQuery = spanQuery;
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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof MtasSpanSequenceItem) {
            MtasSpanSequenceItem that = (MtasSpanSequenceItem) o;
            return spanQuery.equals(that.getQuery()) && (optional == that.isOptional());
        }
        else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        int h = this.getClass().getSimpleName().hashCode();
        h = (h * 3) ^ spanQuery.hashCode();
        h += (optional ? 1 : 0);
        return h;
    }

    /**
     * Rewrite.
     *
     * @param reader
     *            the reader
     * @return the mtas span sequence item
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public MtasSpanSequenceItem rewrite(IndexReader reader) throws IOException
    {
        MtasSpanQuery newSpanQuery = spanQuery.rewrite(reader);
        if (!newSpanQuery.equals(spanQuery)) {
            return new MtasSpanSequenceItem(newSpanQuery, optional);
        }
        else {
            return this;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "[" + spanQuery.toString() + " - " + (optional ? "OPTIONAL" : "NOT OPTIONAL") + "]";
    }

    /**
     * Merge.
     *
     * @param item1
     *            the item 1
     * @param item2
     *            the item 2
     * @param ignoreQuery
     *            the ignore query
     * @param maximumIgnoreLength
     *            the maximum ignore length
     * @return the mtas span sequence item
     */
    public static MtasSpanSequenceItem merge(MtasSpanSequenceItem item1, MtasSpanSequenceItem item2,
            MtasSpanQuery ignoreQuery, Integer maximumIgnoreLength)
    {
        if (item1 == null || item2 == null) {
            return null;
        }
        else {
            MtasSpanQuery q1 = item1.getQuery();
            MtasSpanQuery q2 = item2.getQuery();
            boolean optional = item1.optional && item2.optional;
            // first spanRecurrenceQuery
            if (q1 instanceof MtasSpanRecurrenceQuery) {
                MtasSpanRecurrenceQuery rq1 = (MtasSpanRecurrenceQuery) q1;
                // both spanRecurrenceQuery
                if (q2 instanceof MtasSpanRecurrenceQuery) {
                    MtasSpanRecurrenceQuery rq2 = (MtasSpanRecurrenceQuery) q2;
                    // equal query
                    if (rq1.getQuery().equals(rq2.getQuery())) {
                        // equal ignoreQuery settings
                        boolean checkCondition;
                        checkCondition = ignoreQuery != null && rq1.getIgnoreQuery() != null;
                        checkCondition = checkCondition ? ignoreQuery.equals(rq1.getIgnoreQuery())
                                : false;
                        checkCondition = checkCondition
                                ? maximumIgnoreLength.equals(rq1.getMaximumIgnoreLength())
                                : false;
                        checkCondition = checkCondition ? rq2.getIgnoreQuery() != null : false;
                        checkCondition = checkCondition ? ignoreQuery.equals(rq2.getIgnoreQuery())
                                : false;
                        checkCondition = checkCondition
                                ? maximumIgnoreLength.equals(rq2.getMaximumIgnoreLength())
                                : false;
                        if (checkCondition) {
                            // at least one optional
                            if (item1.optional || item2.optional) {
                                int minimum = Math.min(rq1.getMinimumRecurrence(),
                                        rq2.getMinimumRecurrence());
                                int maximum = rq1.getMaximumRecurrence()
                                        + rq2.getMaximumRecurrence();
                                // only if ranges match
                                if ((rq1.getMaximumRecurrence() + 1) >= rq2.getMinimumRecurrence()
                                        && (rq2.getMaximumRecurrence() + 1) >= rq1
                                                .getMinimumRecurrence()) {
                                    return new MtasSpanSequenceItem(
                                            new MtasSpanRecurrenceQuery(rq1.getQuery(), minimum,
                                                    maximum, ignoreQuery, maximumIgnoreLength),
                                            optional);
                                }
                                // not optional
                            }
                            else {
                                int minimum = rq1.getMinimumRecurrence()
                                        + rq2.getMinimumRecurrence();
                                int maximum = rq1.getMaximumRecurrence()
                                        + rq2.getMaximumRecurrence();
                                // only if ranges match
                                if ((rq1.getMaximumRecurrence() + 1) >= rq2.getMinimumRecurrence()
                                        && (rq2.getMaximumRecurrence() + 1) >= rq1
                                                .getMinimumRecurrence()) {
                                    return new MtasSpanSequenceItem(
                                            new MtasSpanRecurrenceQuery(rq1.getQuery(), minimum,
                                                    maximum, ignoreQuery, maximumIgnoreLength),
                                            optional);
                                }
                            }
                        }
                    }
                }
                else {
                    if (rq1.getQuery().equals(q2)) {
                        boolean checkCondition;
                        checkCondition = ignoreQuery != null;
                        checkCondition &= rq1.getIgnoreQuery() != null;
                        checkCondition &= ignoreQuery.equals(rq1.getIgnoreQuery());
                        checkCondition &= rq1.getMaximumIgnoreLength() != null;
                        checkCondition &= maximumIgnoreLength.equals(rq1.getMaximumIgnoreLength());
                        if (checkCondition) {
                            if (!optional) {
                                if (item1.optional) {
                                    if (rq1.getMinimumRecurrence() == 1) {
                                        return new MtasSpanSequenceItem(new MtasSpanRecurrenceQuery(
                                                q2, 1, rq1.getMaximumRecurrence() + 1, ignoreQuery,
                                                maximumIgnoreLength), false);
                                    }
                                }
                                else if (item2.optional) {
                                    return new MtasSpanSequenceItem(new MtasSpanRecurrenceQuery(q2,
                                            rq1.getMinimumRecurrence(),
                                            rq1.getMaximumRecurrence() + 1, ignoreQuery,
                                            maximumIgnoreLength), false);
                                }
                                else {
                                    return new MtasSpanSequenceItem(new MtasSpanRecurrenceQuery(q2,
                                            rq1.getMinimumRecurrence() + 1,
                                            rq1.getMaximumRecurrence() + 1, ignoreQuery,
                                            maximumIgnoreLength), false);
                                }
                            }
                            else {
                                if (rq1.getMinimumRecurrence() == 1) {
                                    return new MtasSpanSequenceItem(new MtasSpanRecurrenceQuery(q2,
                                            1, rq1.getMaximumRecurrence() + 1, ignoreQuery,
                                            maximumIgnoreLength), true);
                                }
                            }
                        }
                    }
                }
                // second spanRecurrenceQuery
            }
            else if (q2 instanceof MtasSpanRecurrenceQuery) {
                MtasSpanRecurrenceQuery rq2 = (MtasSpanRecurrenceQuery) q2;
                if (rq2.getQuery().equals(q1)) {
                    boolean checkCondition;
                    checkCondition = ignoreQuery != null;
                    checkCondition &= rq2.getIgnoreQuery() != null;
                    checkCondition &= ignoreQuery.equals(rq2.getIgnoreQuery());
                    checkCondition &= maximumIgnoreLength.equals(rq2.getMaximumIgnoreLength());
                    if (checkCondition) {
                        if (!optional) {
                            if (item1.optional) {
                                return new MtasSpanSequenceItem(new MtasSpanRecurrenceQuery(q1,
                                        rq2.getMinimumRecurrence(), rq2.getMaximumRecurrence() + 1,
                                        ignoreQuery, maximumIgnoreLength), false);
                            }
                            else if (item2.optional) {
                                if (rq2.getMinimumRecurrence() == 1) {
                                    return new MtasSpanSequenceItem(new MtasSpanRecurrenceQuery(q1,
                                            1, rq2.getMaximumRecurrence() + 1, ignoreQuery,
                                            maximumIgnoreLength), false);
                                }
                            }
                            else {
                                return new MtasSpanSequenceItem(new MtasSpanRecurrenceQuery(q1,
                                        rq2.getMinimumRecurrence() + 1,
                                        rq2.getMaximumRecurrence() + 1, ignoreQuery,
                                        maximumIgnoreLength), false);
                            }
                        }
                        else {
                            if (rq2.getMinimumRecurrence() == 1) {
                                return new MtasSpanSequenceItem(new MtasSpanRecurrenceQuery(q1, 1,
                                        rq2.getMaximumRecurrence() + 1, ignoreQuery,
                                        maximumIgnoreLength), true);
                            }
                        }
                    }
                }
                // both no spanRecurrenceQuery
            }
            else if (q1.equals(q2)) {
                // at least one optional
                if (item1.optional || item2.optional) {
                    return new MtasSpanSequenceItem(
                            new MtasSpanRecurrenceQuery(q1, 1, 2, ignoreQuery, maximumIgnoreLength),
                            optional);
                }
                else {
                    return new MtasSpanSequenceItem(
                            new MtasSpanRecurrenceQuery(q1, 2, 2, ignoreQuery, maximumIgnoreLength),
                            optional);
                }
            }
            return null;
        }
    }

}
