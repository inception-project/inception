package mtas.search.spans;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafSimScorer;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;

import mtas.search.similarities.MtasSimScorer;
import mtas.search.spans.util.MtasSpanQuery;
import mtas.search.spans.util.MtasSpanWeight;
import mtas.search.spans.util.MtasSpans;

/**
 * The Class MtasSpanMatchNoneQuery.
 */
public class MtasSpanMatchNoneQuery
    extends MtasSpanQuery
{

    /** The field. */
    private String field;

    /**
     * Instantiates a new mtas span match none query.
     *
     * @param field
     *            the field
     */
    public MtasSpanMatchNoneQuery(String field)
    {
        super(null, null);
        this.field = field;
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
     * @see org.apache.lucene.search.spans.SpanQuery#createWeight(org.apache.lucene.
     * search.IndexSearcher, boolean)
     */
    @Override
    public MtasSpanWeight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost)
        throws IOException
    {
        return new SpanNoneWeight(searcher, null, boost);
    }

    /**
     * The Class SpanNoneWeight.
     */
    protected class SpanNoneWeight
        extends MtasSpanWeight
    {

        /** The Constant METHOD_GET_DELEGATE. */
        private static final String METHOD_GET_DELEGATE = "getDelegate";

        /**
         * Instantiates a new span none weight.
         *
         * @param searcher
         *            the searcher
         * @param termContexts
         *            the term contexts
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public SpanNoneWeight(IndexSearcher searcher, Map<Term, TermStates> termContexts,
                float boost)
            throws IOException
        {
            super(MtasSpanMatchNoneQuery.this, searcher, termContexts, boost);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.spans.SpanWeight#extractTermContexts(java.util. Map)
         */
        @Override
        public void extractTermStates(Map<Term, TermStates> contexts)
        {
            // don't do anything
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
            try {
                // get leafreader
                LeafReader r = context.reader();
                // get delegate
                Boolean hasMethod = true;
                while (hasMethod) {
                    hasMethod = false;
                    Method[] methods = r.getClass().getMethods();
                    for (Method m : methods) {
                        if (m.getName().equals(METHOD_GET_DELEGATE)) {
                            hasMethod = true;
                            r = (LeafReader) m.invoke(r, (Object[]) null);
                            break;
                        }
                    }
                }
                // get MtasFieldsProducer using terms
                return new MtasSpanMatchNoneSpans(MtasSpanMatchNoneQuery.this);
            }
            catch (InvocationTargetException | IllegalAccessException e) {
                throw new IOException("Can't get reader", e);
            }

        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.spans.SpanWeight#getSimScorer(org.apache.lucene.
         * index.LeafReaderContext)
         */
        @Override
        public LeafSimScorer getSimScorer(LeafReaderContext context) throws IOException
        {
            return new LeafSimScorer(new MtasSimScorer(), context.reader(), field, true);
        }

        // @Override
        // public boolean isCacheable(LeafReaderContext arg0) {
        // return true;
        // }

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
        buffer.append(this.getClass().getSimpleName() + "([])");
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
        final MtasSpanMatchNoneQuery that = (MtasSpanMatchNoneQuery) obj;
        if (field == null) {
            return that.field == null;
        }
        else {
            return field.equals(that.field);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Query#hashCode()
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(this.getClass().getSimpleName(), field);
    }

    @Override
    public boolean isMatchAllPositionsQuery()
    {
        return false;
    }

    @Override
    public void visit(QueryVisitor aVisitor)
    {
        // don't do anything
    }

}
