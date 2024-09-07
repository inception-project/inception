package mtas.search.spans.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queries.spans.FilterSpans;
import org.apache.lucene.queries.spans.SpanTermQuery;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;

import mtas.analysis.token.MtasToken;
import mtas.codec.util.CodecUtil;

/**
 * The Class MtasExtendedSpanTermQuery.
 */
public class MtasExtendedSpanTermQuery
    extends SpanTermQuery
{

    /** The prefix. */
    private String prefix;

    /** The value. */
    private String value;

    /** The single position. */
    private boolean singlePosition;

    /** The local term. */
    private Term localTerm;
    private SpanTermQuery query;

    /**
     * Instantiates a new mtas extended span term query.
     *
     * @param term
     *            the term
     */
    public MtasExtendedSpanTermQuery(Term term)
    {
        this(term, true);
    }

    /**
     * Instantiates a new mtas extended span term query.
     *
     * @param term
     *            the term
     * @param singlePosition
     *            the single position
     */
    private MtasExtendedSpanTermQuery(Term term, boolean singlePosition)
    {
        this(new SpanTermQuery(term), singlePosition);
    }

    /**
     * Instantiates a new mtas extended span term query.
     *
     * @param query
     *            the query
     * @param singlePosition
     *            the single position
     */
    public MtasExtendedSpanTermQuery(SpanTermQuery query, boolean singlePosition)
    {
        super(query.getTerm());
        this.query = query;
        localTerm = query.getTerm();
        this.singlePosition = singlePosition;
        int i = localTerm.text().indexOf(MtasToken.DELIMITER);
        if (i >= 0) {
            prefix = localTerm.text().substring(0, i);
            value = localTerm.text().substring((i + MtasToken.DELIMITER.length()));
            value = (value.length() > 0) ? value : null;
        }
        else {
            prefix = localTerm.text();
            value = null;
        }
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
        final TermStates context;
        if (termStates == null) {
            context = TermStates.build(searcher, localTerm, true);
        }
        else {
            context = termStates;
        }
        return new SpanTermWeight(context, searcher,
                scoreMode.needsScores() ? Collections.singletonMap(localTerm, context) : null,
                boost);
    }

    /**
     * The Class SpanTermWeight.
     */
    public class SpanTermWeight
        extends SpanWeight
    {

        /** The Constant METHOD_GET_DELEGATE. */
        private static final String METHOD_GET_DELEGATE = "getDelegate";

        /** The term states. */
        final TermStates termStates;

        /**
         * Instantiates a new span term weight.
         *
         * @param searcher
         *            the searcher
         * @param terms
         *            the terms
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public SpanTermWeight(TermStates termStates, IndexSearcher searcher,
                Map<Term, TermStates> terms, float boost)
            throws IOException
        {
            super(MtasExtendedSpanTermQuery.this, searcher, terms, boost);
            this.termStates = termStates;
            assert termStates != null : "TermStates must not be null";
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.spans.SpanWeight#extractTermContexts(java.util .Map)
         */
        @Override
        public void extractTermStates(Map<Term, TermStates> contexts)
        {
            contexts.put(localTerm, termStates);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.spans.SpanWeight#getSpans(org.apache.lucene.
         * index.LeafReaderContext, org.apache.lucene.search.spans.SpanWeight.Postings)
         */
        @Override
        public Spans getSpans(final LeafReaderContext context, Postings requiredPostings)
            throws IOException
        {
            final TermState state = termStates.get(context);
            if (state == null) { // term is not present in that reader
                assert context.reader().docFreq(
                        localTerm) == 0 : "no termstate found but term exists in reader term="
                                + localTerm;
                return null;
            }

            final Terms terms = context.reader().terms(localTerm.field());
            if (terms == null) {
                return null;
            }
            if (!terms.hasPositions()) {
                throw new IllegalStateException("field \"" + localTerm.field()
                        + "\" was indexed without position data; cannot run SpanTermQuery (term="
                        + localTerm.text() + ")");
            }

            final TermsEnum termsEnum = terms.iterator();
            termsEnum.seekExact(localTerm.bytes(), state);

            final PostingsEnum postings;
            Spans matchSpans;

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

                FieldInfo fieldInfo = r.getFieldInfos().fieldInfo(field);

                if (CodecUtil.isSinglePositionPrefix(fieldInfo, prefix)) {
                    postings = termsEnum.postings(null, requiredPostings.getRequiredPostings());
                    matchSpans = new MtasExtendedTermSpans(postings, localTerm, true);
                }
                else {
                    postings = termsEnum.postings(null,
                            requiredPostings.atLeast(Postings.PAYLOADS).getRequiredPostings());
                    matchSpans = new MtasExtendedTermSpans(postings, localTerm, false);
                }
                if (singlePosition) {
                    return new FilterSpans(matchSpans)
                    {
                        @Override
                        protected AcceptStatus accept(Spans candidate) throws IOException
                        {
                            assert candidate.startPosition() != candidate.endPosition();
                            if ((candidate.endPosition() - candidate.startPosition()) == 1) {
                                return AcceptStatus.YES;
                            }
                            else {
                                return AcceptStatus.NO;
                            }
                        }
                    };
                }
                else {
                    return matchSpans;
                }
            }
            catch (Exception e) {
                // e.printStackTrace();
                throw new IOException("Can't get reader: " + e.getMessage(), e);
            }

        }

        @Override
        public boolean isCacheable(LeafReaderContext arg0)
        {
            return false;
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
            buffer.append(field + ":" + prefix);
        }
        else {
            buffer.append(field + ":" + prefix + "=" + value);
        }
        buffer.append("])");
        return buffer.toString();
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
        MtasExtendedSpanTermQuery other = (MtasExtendedSpanTermQuery) obj;
        return other.localTerm.equals(localTerm) && (other.singlePosition == singlePosition);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.SpanTermQuery#hashCode()
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(this.getClass().getSimpleName(), localTerm, singlePosition);
    }

    @Override
    public void visit(QueryVisitor aVisitor)
    {
        query.visit(aVisitor);
    }
}
