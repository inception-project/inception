package mtas.search.spans.util;

import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.search.TwoPhaseIterator;

/**
 * The Class MtasSpans.
 */
abstract public class MtasSpans
    extends Spans
{

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#asTwoPhaseIterator()
     */
    @Override
    public abstract TwoPhaseIterator asTwoPhaseIterator();

}
