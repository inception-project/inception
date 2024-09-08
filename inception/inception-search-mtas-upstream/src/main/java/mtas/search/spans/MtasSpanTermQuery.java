package mtas.search.spans;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.spans.SpanTermQuery;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;

import mtas.search.spans.util.MtasExtendedSpanTermQuery;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Class MtasSpanTermQuery.
 */
public class MtasSpanTermQuery
    extends MtasSpanQuery
{

    /** The base query. */
    private MtasExtendedSpanTermQuery baseQuery;

    /**
     * Instantiates a new mtas span term query.
     *
     * @param term
     *            the term
     */
    public MtasSpanTermQuery(Term term)
    {
        this(term, true);
    }

    /**
     * Instantiates a new mtas span term query.
     *
     * @param term
     *            the term
     * @param singlePosition
     *            the single position
     */
    public MtasSpanTermQuery(Term term, boolean singlePosition)
    {
        this(new SpanTermQuery(term), true);
    }

    /**
     * Instantiates a new mtas span term query.
     *
     * @param query
     *            the query
     * @param singlePosition
     *            the single position
     */
    public MtasSpanTermQuery(SpanTermQuery query, boolean singlePosition)
    {
        super(singlePosition ? 1 : null, singlePosition ? 1 : null);
        baseQuery = new MtasExtendedSpanTermQuery(query, singlePosition);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanTermQuery#createWeight(org.apache.lucene
     * .search.IndexSearcher, boolean)
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
     * @see mtas.search.spans.util.MtasSpanQuery#rewrite(org.apache.lucene.index. IndexReader)
     */
    @Override
    public MtasSpanQuery rewrite(IndexReader reader) throws IOException
    {
        baseQuery = (MtasExtendedSpanTermQuery) baseQuery.rewrite(reader);
        return super.rewrite(reader);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanTermQuery#toString(java.lang.String)
     */
    @Override
    public String toString(String field)
    {
        return baseQuery.toString(field);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanTermQuery#equals(java.lang.Object)
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
        MtasSpanTermQuery other = (MtasSpanTermQuery) obj;
        return baseQuery.equals(other.baseQuery);
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
     * @see org.apache.lucene.search.Query#hashCode()
     */
    @Override
    public int hashCode()
    {
        return baseQuery.hashCode();
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
