package mtas.search.spans;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtas.codec.util.CodecInfo;
import mtas.search.spans.util.MtasSpanQuery;
import mtas.search.spans.util.MtasSpanWeight;
import mtas.search.spans.util.MtasSpans;

/**
 * The Class MtasSpanMatchAllQuery.
 */
public class MtasSpanMatchAllQuery
    extends MtasSpanQuery
{

    /** The log. */
    private static final Logger log = LoggerFactory.getLogger(MtasSpanMatchAllQuery.class);

    /** The field. */
    private String field;

    /**
     * Instantiates a new mtas span match all query.
     *
     * @param field
     *            the field
     */
    public MtasSpanMatchAllQuery(String field)
    {
        super(1, 1);
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
        // keep things simple
        return new SpanAllWeight(searcher, null, boost);
    }

    /**
     * The Class SpanAllWeight.
     */
    protected class SpanAllWeight
        extends MtasSpanWeight
    {

        /** The Constant METHOD_GET_DELEGATE. */
        private static final String METHOD_GET_DELEGATE = "getDelegate";

        /** The Constant METHOD_GET_POSTINGS_READER. */
        private static final String METHOD_GET_POSTINGS_READER = "getPostingsReader";

        /** The searcher. */
        IndexSearcher searcher;

        /**
         * Instantiates a new span all weight.
         *
         * @param searcher
         *            the searcher
         * @param termContexts
         *            the term contexts
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public SpanAllWeight(IndexSearcher searcher, Map<Term, TermStates> termContexts,
                float boost)
            throws IOException
        {
            super(MtasSpanMatchAllQuery.this, searcher, termContexts, boost);
            this.searcher = searcher;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.spans.SpanWeight#extractTermContexts(java.util. Map)
         */
        @Override
        public void extractTermStates(Map<Term, TermStates> contexts)
        {
            Term term = new Term(field);
            if (!contexts.containsKey(term)) {
                try {
                    contexts.put(term, TermStates.build(searcher, term, true));
                }
                catch (IOException e) {
                    log.debug("Error", e);
                    // fail
                }
            }
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
                // get fieldsproducer
                Method fpm = r.getClass().getMethod(METHOD_GET_POSTINGS_READER, (Class<?>[]) null);
                FieldsProducer fp = (FieldsProducer) fpm.invoke(r, (Object[]) null);
                // get MtasFieldsProducer using terms
                Terms t = fp.terms(field);
                if (t == null) {
                    return new MtasSpanMatchNoneSpans(MtasSpanMatchAllQuery.this);
                }
                else {
                    CodecInfo mtasCodecInfo = CodecInfo.getCodecInfoFromTerms(t);
                    return new MtasSpanMatchAllSpans(MtasSpanMatchAllQuery.this, mtasCodecInfo,
                            field);
                }
            }
            catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                throw new IOException("Can't get reader", e);
            }

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
        final MtasSpanMatchAllQuery that = (MtasSpanMatchAllQuery) obj;
        return field.equals(that.field);
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
        return true;
    }

    @Override
    public void visit(QueryVisitor aVisitor)
    {
        // don't do anything
    }
}
