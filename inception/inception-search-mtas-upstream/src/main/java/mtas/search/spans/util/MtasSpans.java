package mtas.search.spans.util;

import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.queries.spans.Spans;

/**
 * The Class MtasSpans.
 */
abstract public class MtasSpans extends Spans {

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#asTwoPhaseIterator()
   */
  @Override
  public abstract TwoPhaseIterator asTwoPhaseIterator();

}
