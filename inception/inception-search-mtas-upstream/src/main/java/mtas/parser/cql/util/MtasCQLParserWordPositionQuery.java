package mtas.parser.cql.util;

import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;

import mtas.search.spans.MtasSpanPositionQuery;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Class MtasCQLParserWordPositionQuery.
 */
public class MtasCQLParserWordPositionQuery
    extends MtasSpanQuery
{

    /** The query. */
    MtasSpanQuery query;

    /** The term. */
    Term term;

    /**
     * Instantiates a new mtas CQL parser word position query.
     *
     * @param field
     *            the field
     * @param position
     *            the position
     */
    public MtasCQLParserWordPositionQuery(String field, int position)
    {
        super(1, 1);
        term = new Term(field);
        query = new MtasSpanPositionQuery(field, position);
    }

    /**
     * Instantiates a new mtas CQL parser word position query.
     *
     * @param field
     *            the field
     * @param start
     *            the start
     * @param end
     *            the end
     */
    public MtasCQLParserWordPositionQuery(String field, int start, int end)
    {
        super(1, 1);
        term = new Term(field);
        query = new MtasSpanPositionQuery(field, start, end);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanQuery#getField()
     */
    @Override
    public String getField()
    {
        return term.field();
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
        return query.toString(term.field());
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
        final MtasCQLParserWordPositionQuery that = (MtasCQLParserWordPositionQuery) obj;
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
        return Objects.hash(this.getClass().getSimpleName(), term, query);
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
