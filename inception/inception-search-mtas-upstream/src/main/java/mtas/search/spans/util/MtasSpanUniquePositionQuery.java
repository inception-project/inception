package mtas.search.spans.util;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.queries.spans.Spans;

/**
 * The Class MtasSpanUniquePositionQuery.
 */
public class MtasSpanUniquePositionQuery extends MtasSpanQuery {

  /** The clause. */
  private MtasSpanQuery clause;

  /** The field. */
  private String field;

  /**
   * Instantiates a new mtas span unique position query.
   *
   * @param clause the clause
   */
  public MtasSpanUniquePositionQuery(MtasSpanQuery clause) {
    super(clause.getMinimumWidth(), clause.getMaximumWidth());
    field = clause.getField();
    this.clause = clause;
  }

  /**
   * Gets the clause.
   *
   * @return the clause
   */
  public MtasSpanQuery getClause() {
    return clause;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.SpanQuery#getField()
   */
  @Override
  public String getField() {
    return field;
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
    final MtasSpanUniquePositionQuery that = (MtasSpanUniquePositionQuery) obj;
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
   * @see org.apache.lucene.search.Query#toString(java.lang.String)
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
   * @see mtas.search.spans.util.MtasSpanQuery#rewrite(org.apache.lucene.index.
   * IndexReader)
   */
  @Override
  public MtasSpanQuery rewrite(IndexReader reader) throws IOException {
    MtasSpanQuery newClause = clause.rewrite(reader);
    if (!newClause.equals(clause)) {
      return new MtasSpanUniquePositionQuery(newClause).rewrite(reader);
    } else {
      return super.rewrite(reader);
    }
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
    SpanWeight subWeight = clause.createWeight(searcher, ScoreMode.COMPLETE_NO_SCORES, boost);
    return new SpanUniquePositionWeight(subWeight, searcher,
        scoreMode.needsScores() ? getTermStates(subWeight) : null, boost);
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
    return clause.isMatchAllPositionsQuery();
  }

  /**
   * The Class SpanUniquePositionWeight.
   */
  public class SpanUniquePositionWeight extends MtasSpanWeight {

    /** The sub weight. */
    final SpanWeight subWeight;

    /**
     * Instantiates a new span unique position weight.
     *
     * @param subWeight the sub weight
     * @param searcher the searcher
     * @param terms the terms
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public SpanUniquePositionWeight(SpanWeight subWeight,
        IndexSearcher searcher, Map<Term, TermStates> terms, float boost)
        throws IOException {
      super(MtasSpanUniquePositionQuery.this, searcher, terms, boost);
      this.subWeight = subWeight;
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
      subWeight.extractTermStates(contexts);
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
      Spans subSpan = subWeight.getSpans(context, requiredPostings);
      if (subSpan == null) {
        return null;
      } else {
        return new MtasSpanUniquePositionSpans(MtasSpanUniquePositionQuery.this,
            subSpan);
      }
    }
    
//    @Override
//    public boolean isCacheable(LeafReaderContext arg0) {
//      return subWeight.isCacheable(arg0);
//    }

  }

  @Override
  public void visit(QueryVisitor aVisitor)
  {
      clause.visit(aVisitor);    
  }

}
