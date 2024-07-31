package mtas.search.spans;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.queries.spans.SpanQuery;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;

import mtas.search.spans.util.MtasSpanQuery;
import mtas.search.spans.util.MtasSpanWeight;
import mtas.search.spans.util.MtasSpans;

/**
 * Search for the end of a hit for the provided MtasSpanQuery.
 */
public class MtasSpanEndQuery extends MtasSpanQuery {

  /** The clause. */
  private MtasSpanQuery clause;

  /**
   * Instantiates a new Mtas span end query.
   *
   * @param query the query
   */
  public MtasSpanEndQuery(MtasSpanQuery query) {
    super(0, 0);
    clause = query;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.lucene.search.Query#rewrite(org.apache.lucene.index.IndexReader)
   */
  @Override
  public MtasSpanQuery rewrite(IndexReader reader) throws IOException {
    //rewrite the main query
    MtasSpanQuery newClause = clause.rewrite(reader);
    //if something changed, retry
    if (!newClause.equals(clause)) {
      return new MtasSpanEndQuery(newClause).rewrite(reader);
    //if main query has maximum width zero, just use this query instead
    } else if (newClause.getMaximumWidth() != null
        && newClause.getMaximumWidth() == 0) {
      return newClause;
    //otherwise continue as normal  
    } else {
      return super.rewrite(reader);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.lucene.search.spans.SpanTermQuery#toString(java.lang.String)
   */
  @Override
  public String toString(String field) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(this.getClass().getSimpleName() + "([");
    buffer.append(clause.toString(field));
    buffer.append("])");
    return buffer.toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.SpanQuery#getField()
   */
  @Override
  public String getField() {
    return clause.getField();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.lucene.search.spans.SpanQuery#createWeight(org.apache.lucene.
   * search.IndexSearcher, boolean)
   */
  @Override
  public MtasSpanWeight createWeight(IndexSearcher searcher,
      ScoreMode scoreMode, float boost) throws IOException {
    SpanWeight spanWeight = ((SpanQuery) searcher.rewrite(clause))
        .createWeight(searcher, scoreMode, boost);
    return new SpanTermWeight(spanWeight, searcher, boost);
  }

  /**
   * The Class SpanTermWeight.
   */
  public class SpanTermWeight extends MtasSpanWeight {

    /** The span weight. */
    SpanWeight spanWeight;

    /**
     * Instantiates a new span term weight.
     *
     * @param spanWeight the span weight
     * @param searcher the searcher
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public SpanTermWeight(SpanWeight spanWeight, IndexSearcher searcher, float boost)
        throws IOException {
      super(MtasSpanEndQuery.this, searcher, null, boost);
      this.spanWeight = spanWeight;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.lucene.search.spans.SpanWeight#extractTermContexts(java.util.
     * Map)
     */
    @Override
    public void extractTermStates(Map<Term, TermStates> contexts) {
      spanWeight.extractTermStates(contexts);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.lucene.search.spans.SpanWeight#getSpans(org.apache.lucene.
     * index.LeafReaderContext,
     * org.apache.lucene.search.spans.SpanWeight.Postings)
     */
    @Override
    public MtasSpans getSpans(LeafReaderContext context,
        Postings requiredPostings) throws IOException {
      return new MtasSpanEndSpans(MtasSpanEndQuery.this,
          spanWeight.getSpans(context, requiredPostings));
    }

//    @Override
//    public boolean isCacheable(LeafReaderContext arg0) {
//      return spanWeight.isCacheable(arg0);
//    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.Query#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }
    if (obj == null) {
        return false;
    }
    if (getClass() != obj.getClass()) {
        return false;
    }
    final MtasSpanEndQuery that = (MtasSpanEndQuery) obj;
    return clause.equals(that.clause);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.Query#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.getClass().getSimpleName(), clause);    
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.search.spans.util.MtasSpanQuery#disableTwoPhaseIterator()
   */
  @Override
  public void disableTwoPhaseIterator() {
    super.disableTwoPhaseIterator();
    clause.disableTwoPhaseIterator();
  }
  
  @Override
  public boolean isMatchAllPositionsQuery() {
    return false;
  }

  @Override
  public void visit(QueryVisitor aVisitor)
  {
      clause.visit(aVisitor);
  }
}
