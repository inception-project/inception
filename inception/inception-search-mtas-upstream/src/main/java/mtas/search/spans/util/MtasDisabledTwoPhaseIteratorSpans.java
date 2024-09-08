package mtas.search.spans.util;

import java.io.IOException;

import org.apache.lucene.queries.spans.SpanCollector;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.search.TwoPhaseIterator;

/**
 * The Class MtasDisabledTwoPhaseIteratorSpans.
 */
public class MtasDisabledTwoPhaseIteratorSpans
    extends MtasSpans
{

    /** The sub spans. */
    private Spans subSpans;

    /**
     * Instantiates a new mtas disabled two phase iterator spans.
     *
     * @param subSpans
     *            the sub spans
     */
    public MtasDisabledTwoPhaseIteratorSpans(Spans subSpans)
    {
        super();
        this.subSpans = subSpans;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#nextStartPosition()
     */
    @Override
    public int nextStartPosition() throws IOException
    {
        return subSpans.nextStartPosition();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#startPosition()
     */
    @Override
    public int startPosition()
    {
        return subSpans.startPosition();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#endPosition()
     */
    @Override
    public int endPosition()
    {
        return subSpans.endPosition();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#width()
     */
    @Override
    public int width()
    {
        return subSpans.width();
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
        subSpans.collect(collector);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#positionsCost()
     */
    @Override
    public float positionsCost()
    {
        return subSpans.positionsCost();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.DocIdSetIterator#docID()
     */
    @Override
    public int docID()
    {
        return subSpans.docID();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.DocIdSetIterator#nextDoc()
     */
    @Override
    public int nextDoc() throws IOException
    {
        return subSpans.nextDoc();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.DocIdSetIterator#advance(int)
     */
    @Override
    public int advance(int target) throws IOException
    {
        return subSpans.advance(target);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#asTwoPhaseIterator()
     */
    @Override
    public TwoPhaseIterator asTwoPhaseIterator()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.DocIdSetIterator#cost()
     */
    @Override
    public long cost()
    {
        return subSpans.cost();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.getClass().getSimpleName() + "([");
        buffer.append(subSpans != null ? subSpans.toString() : "null");
        buffer.append("])");
        return buffer.toString();
    }

}
