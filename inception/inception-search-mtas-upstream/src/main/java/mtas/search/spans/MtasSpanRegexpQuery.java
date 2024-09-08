package mtas.search.spans;

import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.queries.spans.SpanOrQuery;
import org.apache.lucene.queries.spans.SpanQuery;
import org.apache.lucene.queries.spans.SpanTermQuery;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.ScoreMode;

import mtas.analysis.token.MtasToken;
import mtas.codec.util.CodecUtil;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Class MtasSpanRegexpQuery.
 */
public class MtasSpanRegexpQuery
    extends MtasSpanQuery
{

    /** The Constant MTAS_REGEXP_EXPAND_BOUNDARY. */
    private static final int MTAS_REGEXP_EXPAND_BOUNDARY = 1000000;

    /** The prefix. */
    private String prefix;

    /** The value. */
    private String value;

    /** The single position. */
    private boolean singlePosition;

    /** The term. */
    private Term term;

    /** The query. */
    private SpanMultiTermQueryWrapper<RegexpQuery> query;

    /**
     * Instantiates a new mtas span regexp query.
     *
     * @param term
     *            the term
     */
    public MtasSpanRegexpQuery(Term term)
    {
        this(term, true);
    }

    /**
     * Instantiates a new mtas span regexp query.
     *
     * @param term
     *            the term
     * @param singlePosition
     *            the single position
     */
    public MtasSpanRegexpQuery(Term term, boolean singlePosition)
    {
        super(singlePosition ? 1 : null, singlePosition ? 1 : null);
        RegexpQuery req = new RegexpQuery(term);
        query = new SpanMultiTermQueryWrapper<>(req);
        this.term = term;
        this.singlePosition = singlePosition;
        int i = term.text().indexOf(MtasToken.DELIMITER);
        if (i >= 0) {
            prefix = term.text().substring(0, i);
            value = term.text().substring((i + MtasToken.DELIMITER.length()));
            value = (value.length() > 0) ? value : null;
        }
        else {
            prefix = term.text();
            value = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Query#rewrite(org.apache.lucene.index.IndexReader)
     */
    @Override
    public MtasSpanQuery rewrite(IndexReader reader) throws IOException
    {
        Query q = query.rewrite(reader);
        if (q instanceof SpanOrQuery) {
            SpanQuery[] clauses = ((SpanOrQuery) q).getClauses();
            if (clauses.length > MTAS_REGEXP_EXPAND_BOUNDARY) {
                // forward index solution ?
                throw new IOException("Regexp \"" + CodecUtil.termValue(term.text())
                        + "\" expands to " + clauses.length + " terms, too many (boundary "
                        + MTAS_REGEXP_EXPAND_BOUNDARY + ")!");
            }
            MtasSpanQuery[] newClauses = new MtasSpanQuery[clauses.length];
            for (int i = 0; i < clauses.length; i++) {
                if (clauses[i] instanceof SpanTermQuery) {
                    newClauses[i] = new MtasSpanTermQuery((SpanTermQuery) clauses[i],
                            singlePosition).rewrite(reader);
                }
                else {
                    throw new IOException("no SpanTermQuery after rewrite");
                }
            }
            return new MtasSpanOrQuery(newClauses).rewrite(reader);
        }
        else {
            throw new IOException("no SpanOrQuery after rewrite");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanTermQuery#toString(java.lang.String)
     */
    @Override
    public String toString(String field)
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.getClass().getSimpleName() + "([");
        if (value == null) {
            buffer.append(this.query.getField() + ":" + prefix);
        }
        else {
            buffer.append(this.query.getField() + ":" + prefix + "="
                    + value.replaceAll("\u0000\\*$", ""));
        }
        buffer.append("])");
        return buffer.toString();
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
     * @see org.apache.lucene.search.spans.SpanQuery#createWeight(org.apache.lucene.
     * search.IndexSearcher, boolean)
     */
    @Override
    public SpanWeight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost)
        throws IOException
    {
        return ((SpanQuery) searcher.rewrite(query)).createWeight(searcher, scoreMode, boost);
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
        MtasSpanRegexpQuery that = (MtasSpanRegexpQuery) obj;
        return term.equals(that.term) && singlePosition == that.singlePosition;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Query#hashCode()
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(this.getClass().getSimpleName(), term, singlePosition);
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
