package mtas.search.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.index.Terms;
import org.apache.lucene.queries.spans.SpanNotQuery;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;

import mtas.search.spans.util.MtasSpanQuery;
import mtas.search.spans.util.MtasSpanWeight;
import mtas.search.spans.util.MtasSpans;

/**
 * The Class MtasSpanNotQuery.
 */
public class MtasSpanNotQuery
    extends MtasSpanQuery
{

    /** The field. */
    private String field;

    /** The base query. */
    private SpanNotQuery baseQuery;

    /** The q 1. */
    private MtasSpanQuery q1;

    /** The q 2. */
    private MtasSpanQuery q2;

    /**
     * Instantiates a new mtas span not query.
     *
     * @param q1
     *            the q 1
     * @param q2
     *            the q 2
     */
    public MtasSpanNotQuery(MtasSpanQuery q1, MtasSpanQuery q2)
    {
        super(q1 != null ? q1.getMinimumWidth() : null, q2 != null ? q2.getMaximumWidth() : null);
        if (q1 != null && (field = q1.getField()) != null) {
            if (q2 != null && q2.getField() != null && !q2.getField().equals(field)) {
                throw new IllegalArgumentException("Clauses must have same field.");
            }
        }
        else if (q2 != null) {
            field = q2.getField();
        }
        else {
            field = null;
        }
        this.q1 = q1;
        this.q2 = q2;
        baseQuery = new SpanNotQuery(q1, q2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanQuery#getField()
     */
    @Override
    public String getField()
    {
        return field;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.search.spans.util.MtasSpanQuery#createWeight(org.apache.lucene.search.
     * IndexSearcher, boolean)
     */
    @Override
    public MtasSpanWeight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost)
        throws IOException
    {
        // return baseQuery.createWeight(searcher, needsScores);
        if (q1 == null || q2 == null) {
            return null;
        }
        else {
            MtasSpanNotQueryWeight w1 = new MtasSpanNotQueryWeight(
                    q1.createWeight(searcher, scoreMode, boost));
            MtasSpanNotQueryWeight w2 = new MtasSpanNotQueryWeight(
                    q2.createWeight(searcher, scoreMode, boost));
            // subWeights
            List<MtasSpanNotQueryWeight> subWeights = new ArrayList<>();
            subWeights.add(w1);
            subWeights.add(w2);
            // return
            return new SpanNotWeight(w1, w2, searcher,
                    scoreMode.needsScores() ? getTermStates(subWeights) : null, boost);
        }
    }

    /**
     * Gets the term contexts.
     *
     * @param items
     *            the items
     * @return the term contexts
     */
    protected Map<Term, TermStates> getTermStates(List<MtasSpanNotQueryWeight> items)
    {
        List<SpanWeight> weights = new ArrayList<>();
        for (MtasSpanNotQueryWeight item : items) {
            weights.add(item.spanWeight);
        }
        return getTermStates(weights);
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.search.spans.util.MtasSpanQuery#rewrite(org.apache.lucene.index. IndexReader)
     */
    @Override
    public MtasSpanQuery rewrite(IndexReader reader) throws IOException
    {
        MtasSpanQuery newQ1 = (MtasSpanQuery) q1.rewrite(reader);
        MtasSpanQuery newQ2 = (MtasSpanQuery) q2.rewrite(reader);
        if (!newQ1.equals(q1) || !newQ2.equals(q2)) {
            return new MtasSpanNotQuery(newQ1, newQ2).rewrite(reader);
        }
        else {
            baseQuery = (SpanNotQuery) baseQuery.rewrite(reader);
            return super.rewrite(reader);
        }
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
        buffer.append(this.getClass().getSimpleName() + "([");
        if (q1 != null) {
            buffer.append(q1.toString(q1.getField()));
        }
        else {
            buffer.append("null");
        }
        buffer.append(",");
        if (q2 != null) {
            buffer.append(q2.toString(q2.getField()));
        }
        else {
            buffer.append("null");
        }
        buffer.append("])");
        return buffer.toString();
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
        final MtasSpanNotQuery that = (MtasSpanNotQuery) obj;
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
        q1.disableTwoPhaseIterator();
        q2.disableTwoPhaseIterator();
    }

    /**
     * The Class SpanNotWeight.
     */
    protected class SpanNotWeight
        extends MtasSpanWeight
    {

        /** The w 1. */
        MtasSpanNotQueryWeight w1;

        /** The w 2. */
        MtasSpanNotQueryWeight w2;

        /**
         * Instantiates a new span not weight.
         *
         * @param w1
         *            the w 1
         * @param w2
         *            the w 2
         * @param searcher
         *            the searcher
         * @param termContexts
         *            the term contexts
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public SpanNotWeight(MtasSpanNotQueryWeight w1, MtasSpanNotQueryWeight w2,
                IndexSearcher searcher, Map<Term, TermStates> termContexts, float boost)
            throws IOException
        {
            super(MtasSpanNotQuery.this, searcher, termContexts, boost);
            this.w1 = w1;
            this.w2 = w2;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.spans.SpanWeight#extractTermContexts(java.util. Map)
         */
        @Override
        public void extractTermStates(Map<Term, TermStates> contexts)
        {
            w1.spanWeight.extractTermStates(contexts);
            w2.spanWeight.extractTermStates(contexts);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.spans.SpanWeight#getSpans(org.apache.lucene.
         * index.LeafReaderContext, org.apache.lucene.search.spans.SpanWeight.Postings)
         */
        @Override
        public MtasSpans getSpans(LeafReaderContext context, Postings requiredPostings)
            throws IOException
        {
            Terms terms = context.reader().terms(field);
            if (terms == null) {
                return null; // field does not exist
            }
            MtasSpanNotQuerySpans s1 = new MtasSpanNotQuerySpans(MtasSpanNotQuery.this,
                    w1.spanWeight.getSpans(context, requiredPostings));
            MtasSpanNotQuerySpans s2 = new MtasSpanNotQuerySpans(MtasSpanNotQuery.this,
                    w2.spanWeight.getSpans(context, requiredPostings));
            return new MtasSpanNotSpans(MtasSpanNotQuery.this, s1, s2);
        }

        // @Override
        // public boolean isCacheable(LeafReaderContext arg0) {
        // return w1.spanWeight.isCacheable(arg0) && w2.spanWeight.isCacheable(arg0);
        // }

    }

    /**
     * The Class MtasSpanNotQuerySpans.
     */
    protected static class MtasSpanNotQuerySpans
    {

        /** The spans. */
        public Spans spans;

        /**
         * Instantiates a new mtas span not query spans.
         *
         * @param query
         *            the query
         * @param spans
         *            the spans
         */
        public MtasSpanNotQuerySpans(MtasSpanNotQuery query, Spans spans)
        {
            this.spans = spans != null ? spans : new MtasSpanMatchNoneSpans(query);
        }

    }

    /**
     * The Class MtasSpanNotQueryWeight.
     */
    private static class MtasSpanNotQueryWeight
    {

        /** The span weight. */
        public SpanWeight spanWeight;

        /**
         * Instantiates a new mtas span not query weight.
         *
         * @param spanWeight
         *            the span weight
         */
        public MtasSpanNotQueryWeight(SpanWeight spanWeight)
        {
            this.spanWeight = spanWeight;
        }
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
