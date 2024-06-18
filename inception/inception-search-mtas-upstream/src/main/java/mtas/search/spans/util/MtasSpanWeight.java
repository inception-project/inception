package mtas.search.spans.util;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafSimScorer;
import org.apache.lucene.queries.spans.SpanQuery;
import org.apache.lucene.queries.spans.SpanScorer;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.queries.spans.SpanWeight.Postings;

/**
 * The Class MtasSpanWeight.
 */
public abstract class MtasSpanWeight extends SpanWeight {

  /**
   * Instantiates a new mtas span weight.
   *
   * @param query
   *          the query
   * @param searcher
   *          the searcher
   * @param termContexts
   *          the term contexts
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public MtasSpanWeight(SpanQuery query, IndexSearcher searcher,
      Map<Term, TermStates> termContexts, float boost) throws IOException {
    super(query, searcher, termContexts, boost);
  }

  // abstract public MtasSpans getSpans(LeafReaderContext context,
  // Postings requiredPostings) throws IOException;

  @Override
  public boolean isCacheable(LeafReaderContext arg0) {
    return false;
  }
  
  @Override
  public SpanScorer scorer(LeafReaderContext context) throws IOException {
    //SpanScorer spanScorer = super.scorer(context);
    
    final Spans spans = getSpans(context, Postings.POSITIONS);
    if (spans == null) {
      return null;
    }
    final LeafSimScorer docScorer = getSimScorer(context);
    return new MtasSpanScorer(this, spans, docScorer);
  }
  
}

