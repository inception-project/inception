package mtas.search.spans;

import java.io.IOException;

import org.apache.lucene.queries.spans.SpanCollector;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.search.TwoPhaseIterator;

import mtas.search.spans.util.MtasSpans;

/**
 * Return just the end of hits.
 */
public class MtasSpanEndSpans
    extends MtasSpans
{

    /** The query. */
    private MtasSpanEndQuery query;

    /** The spans. */
    private Spans spans;

    /**
     * Instantiates a new mtas span end spans.
     *
     * @param query
     *            the query
     * @param spans
     *            the spans
     */
    public MtasSpanEndSpans(MtasSpanEndQuery query, Spans spans)
    {
        super();
        this.query = query;
        this.spans = spans;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#nextStartPosition()
     */
    @Override
    public int nextStartPosition() throws IOException
    {
        spans.nextStartPosition();
        return startPosition();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#startPosition()
     */
    @Override
    public int startPosition()
    {
        return (spans == null) ? -1 : spans.endPosition();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#endPosition()
     */
    @Override
    public int endPosition()
    {
        return (spans == null) ? -1 : spans.endPosition();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#width()
     */
    @Override
    public int width()
    {
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#collect(org.apache.lucene.search.spans
     * .SpanCollector)
     */
    @Override
    public void collect(SpanCollector collector) throws IOException
    {
        if (spans != null) {
            spans.collect(collector);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.DocIdSetIterator#docID()
     */
    @Override
    public int docID()
    {
        return (spans == null) ? NO_MORE_DOCS : spans.docID();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.DocIdSetIterator#nextDoc()
     */
    @Override
    public int nextDoc() throws IOException
    {
        return (spans == null) ? NO_MORE_DOCS : spans.nextDoc();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.DocIdSetIterator#advance(int)
     */
    @Override
    public int advance(int target) throws IOException
    {
        return (spans == null) ? NO_MORE_DOCS : spans.advance(target);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.DocIdSetIterator#cost()
     */
    @Override
    public long cost()
    {
        return (spans == null) ? 0 : spans.cost();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#asTwoPhaseIterator()
     */
    @Override
    public TwoPhaseIterator asTwoPhaseIterator()
    {
        if (spans == null || !query.twoPhaseIteratorAllowed()) {
            return null;
        }
        else {
            return spans.asTwoPhaseIterator();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#positionsCost()
     */
    @Override
    public float positionsCost()
    {
        return (spans == null) ? 0 : spans.positionsCost();
    }

}
