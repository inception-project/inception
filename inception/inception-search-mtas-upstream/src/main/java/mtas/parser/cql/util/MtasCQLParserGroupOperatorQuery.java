package mtas.parser.cql.util;

import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;

import mtas.search.spans.MtasSpanOperatorQuery;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Class MtasCQLParserWordComparatorQuery.
 */
public class MtasCQLParserGroupOperatorQuery
    extends MtasSpanQuery
{

    /** The query. */
    MtasSpanQuery query;

    /**
     * Instantiates a new mtas CQL parser word position query.
     *
     * @param field
     *            the field
     * @param prefix
     *            the prefix
     * @param operator
     *            the operator
     * @param value
     *            the value
     */
    public MtasCQLParserGroupOperatorQuery(String field, String prefix, String operator, int value)
    {
        super(1, 1);
        query = new MtasSpanOperatorQuery(field, prefix, operator, value, false);
    }

    /**
     * Instantiates a new mtas CQL parser group operator query.
     *
     * @param field
     *            the field
     * @param prefix
     *            the prefix
     * @param operator
     *            the operator
     * @param value
     *            the value
     */
    public MtasCQLParserGroupOperatorQuery(String field, String prefix, String operator,
            String value)
    {
        super(1, 1);
        query = new MtasSpanOperatorQuery(field, prefix, operator, value, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanQuery#getField()
     */
    @Override
    public String getField()
    {
        return query.getField();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Query#rewrite(org.apache.lucene.index.IndexReader)
     */
    @Override
    public MtasSpanQuery rewrite(IndexReader reader) throws IOException
    {
        return query.rewrite(reader);
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
        return query.createWeight(searcher, scoreMode, boost);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Query#toString(java.lang.String)
     */
    @Override
    public String toString(String field)
    {
        return query.toString(field);
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
        final MtasCQLParserGroupOperatorQuery that = (MtasCQLParserGroupOperatorQuery) obj;
        return query.equals(that.query);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Query#hashCode()
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(this.getClass().getSimpleName(), query);
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
        query.disableTwoPhaseIterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.search.spans.util.MtasSpanQuery#isMatchAllPositionsQuery()
     */
    @Override
    public boolean isMatchAllPositionsQuery()
    {
        return false;
    }

    @Override
    public void visit(QueryVisitor aVisitor)
    {
        query.visit(aVisitor);
    }
}
