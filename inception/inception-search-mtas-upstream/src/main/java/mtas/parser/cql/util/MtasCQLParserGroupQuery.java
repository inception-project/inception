package mtas.parser.cql.util;

import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;

import mtas.analysis.token.MtasToken;
import mtas.search.spans.MtasSpanOperatorQuery;
import mtas.search.spans.MtasSpanPrefixQuery;
import mtas.search.spans.MtasSpanRegexpQuery;
import mtas.search.spans.MtasSpanTermQuery;
import mtas.search.spans.MtasSpanWildcardQuery;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Class MtasCQLParserGroupQuery.
 */
public class MtasCQLParserGroupQuery
    extends MtasSpanQuery
{

    /** The query. */
    MtasSpanQuery query;

    /** The term. */
    Term term;

    /** The Constant MTAS_CQL_TERM_QUERY. */
    public static final String MTAS_CQL_TERM_QUERY = "term";

    /** The Constant MTAS_CQL_REGEXP_QUERY. */
    public static final String MTAS_CQL_REGEXP_QUERY = "regexp";

    /** The Constant MTAS_CQL_WILDCARD_QUERY. */
    public static final String MTAS_CQL_WILDCARD_QUERY = "wildcard";

    /**
     * Instantiates a new mtas CQL parser group query.
     *
     * @param field
     *            the field
     * @param prefix
     *            the prefix
     */
    public MtasCQLParserGroupQuery(String field, String prefix)
    {
        super(null, null);
        term = new Term(field, prefix + MtasToken.DELIMITER);
        query = new MtasSpanPrefixQuery(term, false);
    }

    /**
     * Instantiates a new mtas CQL parser group query.
     *
     * @param field
     *            the field
     * @param prefix
     *            the prefix
     * @param value
     *            the value
     */
    public MtasCQLParserGroupQuery(String field, String prefix, String value)
    {
        this(field, prefix, value, MTAS_CQL_REGEXP_QUERY);
    }

    /**
     * Instantiates a new mtas CQL parser group query.
     *
     * @param field
     *            the field
     * @param prefix
     *            the prefix
     * @param value
     *            the value
     * @param type
     *            the type
     */
    public MtasCQLParserGroupQuery(String field, String prefix, String value, String type)
    {
        super(null, null);
        if (value == null || value.trim().isEmpty()) {
            term = new Term(field, prefix + MtasToken.DELIMITER);
            query = new MtasSpanPrefixQuery(term, false);
        }
        else if (type == null) {
            term = new Term(field, prefix + MtasToken.DELIMITER + value);
            query = new MtasSpanTermQuery(term, false);
        }
        else {
            String termBase = prefix + MtasToken.DELIMITER + value;
            switch (type) {
            case MTAS_CQL_REGEXP_QUERY:
                term = new Term(field, termBase + "\u0000*");
                query = new MtasSpanRegexpQuery(term, false);
                break;
            case MTAS_CQL_WILDCARD_QUERY:
                term = new Term(field, termBase);
                query = new MtasSpanWildcardQuery(term, false);
                break;
            case MTAS_CQL_TERM_QUERY:
            default:
                term = new Term(field, "\"" + termBase.replace("\"", "\"\\\"\"") + "\"\u0000*");
                query = new MtasSpanOperatorQuery(field, prefix,
                        MtasSpanOperatorQuery.MTAS_OPERATOR_DEQUAL, value, false);
                break;
            }
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
        final MtasCQLParserGroupQuery that = (MtasCQLParserGroupQuery) obj;
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
