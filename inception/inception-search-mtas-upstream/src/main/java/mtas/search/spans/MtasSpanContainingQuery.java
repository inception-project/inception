package mtas.search.spans;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.spans.SpanContainingQuery;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;

import mtas.search.spans.util.MtasSpanQuery;

/**
 * Search for a hit from a MtasSpanQuery (big) containing the hit from another MtasSpanQuery (small)
 */
public class MtasSpanContainingQuery
    extends MtasSpanQuery
{

    /** The base query. */
    private SpanContainingQuery baseQuery;

    /** The big query. */
    private MtasSpanQuery bigQuery;

    /** The small query. */
    private MtasSpanQuery smallQuery;

    /** The field. */
    private String field;

    /**
     * Instantiates a new MtasSpanContainingQuery.
     *
     * @param bigQuery
     *            the big query
     * @param smallQuery
     *            the small query
     */
    public MtasSpanContainingQuery(MtasSpanQuery bigQuery, MtasSpanQuery smallQuery)
    {
        // use minimum and maximum from the big query
        super(bigQuery != null ? bigQuery.getMinimumWidth() : null,
                bigQuery != null ? bigQuery.getMaximumWidth() : null);
        // adjust minimum if necessary from the small query
        if (smallQuery != null && smallQuery.getMinimumWidth() != null
                && (this.getMinimumWidth() == null
                        || this.getMinimumWidth() < smallQuery.getMinimumWidth())) {
            this.setWidth(smallQuery.getMinimumWidth(), this.getMaximumWidth());
        }
        // define queries and field
        this.bigQuery = bigQuery;
        this.smallQuery = smallQuery;
        if (bigQuery != null && bigQuery.getField() != null) {
            field = bigQuery.getField();
        }
        else if (smallQuery != null && smallQuery.getField() != null) {
            field = smallQuery.getField();
        }
        else {
            field = null;
        }
        // define base query if possible
        if (field != null && bigQuery != null && smallQuery != null) {
            if (bigQuery.getField() != null && smallQuery.getField() != null) {
                baseQuery = new SpanContainingQuery(bigQuery, smallQuery);
            }
            else {
                baseQuery = null;
            }
        }
        else {
            baseQuery = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanQuery#getField()
     */
    @Override
    public String getField()
    {
        return baseQuery.getField();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanQuery#createWeight(org.apache.lucene.
     * search.IndexSearcher, boolean)
     */
    @Override
    public SpanWeight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost)
        throws IOException
    {
        return baseQuery.createWeight(searcher, scoreMode, boost);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Query#toString(java.lang.String)
     */
    @Override
    public String toString(String field)
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.getClass().getSimpleName());
        buffer.append("(");
        buffer.append(bigQuery != null ? bigQuery.toString(field) : "null");
        buffer.append(", ");
        buffer.append(smallQuery != null ? smallQuery.toString(field) : "null");
        buffer.append(")");
        return buffer.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.search.spans.util.MtasSpanQuery#rewrite(org.apache.lucene.index. IndexReader)
     */
    @Override
    public MtasSpanQuery rewrite(IndexReader reader) throws IOException
    {
        // rewrite big and small
        MtasSpanQuery newBigQuery = bigQuery.rewrite(reader);
        MtasSpanQuery newSmallQuery = smallQuery.rewrite(reader);
        // check if query became trivial
        if (newBigQuery == null || newBigQuery instanceof MtasSpanMatchNoneQuery
                || newSmallQuery == null || newSmallQuery instanceof MtasSpanMatchNoneQuery) {
            return new MtasSpanMatchNoneQuery(field);
        }
        // really new queries
        if (!newBigQuery.equals(bigQuery) || !newSmallQuery.equals(smallQuery)) {
            return new MtasSpanContainingQuery(newBigQuery, newSmallQuery).rewrite(reader);
            // if equal, then just the big one
        }
        else if (newBigQuery.equals(newSmallQuery)) {
            return newBigQuery;
            // without a baseQuery, everything stops
        }
        else if (baseQuery == null) {
            return new MtasSpanMatchNoneQuery(field);
            // no easy way, just continue
        }
        else {
            baseQuery = (SpanContainingQuery) baseQuery.rewrite(reader);
            return super.rewrite(reader);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Query#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MtasSpanContainingQuery that = (MtasSpanContainingQuery) obj;
        return baseQuery.equals(that.baseQuery);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Query#hashCode()
     */
    @Override
    public int hashCode()
    {
        return baseQuery.hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.search.spans.util.MtasSpanQuery#disableTwoPhaseIterator()
     */
    @Override
    public void disableTwoPhaseIterator()
    {
        super.disableTwoPhaseIterator();
        bigQuery.disableTwoPhaseIterator();
        smallQuery.disableTwoPhaseIterator();
    }

    @Override
    public boolean isMatchAllPositionsQuery()
    {
        return false;
    }

    @Override
    public void visit(QueryVisitor aVisitor)
    {
        baseQuery.visit(aVisitor);
    }

}
