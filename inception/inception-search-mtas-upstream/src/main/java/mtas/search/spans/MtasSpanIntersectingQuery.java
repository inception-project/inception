package mtas.search.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.queries.spans.Spans;
import mtas.search.spans.util.MtasSpanQuery;
import mtas.search.spans.util.MtasSpanWeight;
import mtas.search.spans.util.MtasSpans;

/**
 * The Class MtasSpanIntersectingQuery.
 */
public class MtasSpanIntersectingQuery extends MtasSpanQuery {

  /** The field. */
  private String field;

  /** The q 1. */
  private MtasSpanQuery q1;

  /** The q 2. */
  private MtasSpanQuery q2;

  /**
   * Instantiates a new mtas span intersecting query.
   *
   * @param q1 the q 1
   * @param q2 the q 2
   */
  public MtasSpanIntersectingQuery(MtasSpanQuery q1, MtasSpanQuery q2) {
    super(q1 != null ? q1.getMinimumWidth() : null,
        q1 != null ? q1.getMaximumWidth() : null);
    if (q1 != null && (field = q1.getField()) != null) {
      if (q2 != null && q2.getField() != null && !q2.getField().equals(field)) {
        throw new IllegalArgumentException("Clauses must have same field.");
      }
    } else if (q2 != null) {
      field = q2.getField();
    } else {
      field = null;
    }
    this.q1 = q1;
    this.q2 = q2;
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
   * @see
   * org.apache.lucene.search.spans.SpanQuery#createWeight(org.apache.lucene.
   * search.IndexSearcher, boolean)
   */
  @Override
  public MtasSpanWeight createWeight(IndexSearcher searcher,
      ScoreMode scoreMode, float boost) throws IOException {
    if (q1 == null || q2 == null) {
      return null;
    } else {
      MtasSpanIntersectingQueryWeight w1 = new MtasSpanIntersectingQueryWeight(
          q1.createWeight(searcher, scoreMode, boost));
      MtasSpanIntersectingQueryWeight w2 = new MtasSpanIntersectingQueryWeight(
          q2.createWeight(searcher, scoreMode, boost));
      // subWeights
      List<MtasSpanIntersectingQueryWeight> subWeights = new ArrayList<>();
      subWeights.add(w1);
      subWeights.add(w2);
      // return
      return new SpanIntersectingWeight(w1, w2, searcher,
          scoreMode.needsScores() ? getTermStates(subWeights) : null, boost);
    }
  }

  /**
   * Gets the term contexts.
   *
   * @param items the items
   * @return the term contexts
   */
  protected Map<Term, TermStates> getTermStates(
      List<MtasSpanIntersectingQueryWeight> items) {
    List<SpanWeight> weights = new ArrayList<>();
    for (MtasSpanIntersectingQueryWeight item : items) {
      weights.add(item.spanWeight);
    }
    return getTermStates(weights);
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
    if (q1 != null) {
      buffer.append(q1.toString(q1.getField()));
    } else {
      buffer.append("null");
    }
    buffer.append(",");
    if (q2 != null) {
      buffer.append(q2.toString(q2.getField()));
    } else {
      buffer.append("null");
    }
    buffer.append("])");
    return buffer.toString();
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
    final MtasSpanIntersectingQuery other = (MtasSpanIntersectingQuery) obj;
    return q1.equals(other.q1) && q2.equals(other.q2);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.Query#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.getClass().getSimpleName(), q1, q2);   
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.search.spans.util.MtasSpanQuery#rewrite(org.apache.lucene.index.
   * IndexReader)
   */
  @Override
  public MtasSpanQuery rewrite(IndexReader reader) throws IOException {
    MtasSpanQuery newQ1 = (MtasSpanQuery) q1.rewrite(reader);
    MtasSpanQuery newQ2 = (MtasSpanQuery) q2.rewrite(reader);
    if (!newQ1.equals(q1) || !newQ2.equals(q2)) {
      return new MtasSpanIntersectingQuery(newQ1, newQ2).rewrite(reader);
    } else if (newQ1.equals(newQ2)) {
      return newQ1;
    } else {

      boolean returnNone;
      returnNone = newQ1.getMaximumWidth() != null
          && newQ1.getMaximumWidth() == 0;
      returnNone |= newQ2.getMaximumWidth() != null
          && newQ2.getMaximumWidth() == 0;
      if (returnNone) {
        return new MtasSpanMatchNoneQuery(this.getField());
      } else {
        return super.rewrite(reader);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.search.spans.util.MtasSpanQuery#disableTwoPhaseIterator()
   */
  @Override
  public void disableTwoPhaseIterator() {
    super.disableTwoPhaseIterator();
    q1.disableTwoPhaseIterator();
    q2.disableTwoPhaseIterator();
  }

  /**
   * The Class SpanIntersectingWeight.
   */
  protected class SpanIntersectingWeight extends MtasSpanWeight {

    /** The w 1. */
    MtasSpanIntersectingQueryWeight w1;

    /** The w 2. */
    MtasSpanIntersectingQueryWeight w2;

    /**
     * Instantiates a new span intersecting weight.
     *
     * @param w1 the w 1
     * @param w2 the w 2
     * @param searcher the searcher
     * @param terms the terms
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public SpanIntersectingWeight(MtasSpanIntersectingQueryWeight w1,
        MtasSpanIntersectingQueryWeight w2, IndexSearcher searcher,
        Map<Term, TermStates> terms, float boost) throws IOException {
      super(MtasSpanIntersectingQuery.this, searcher, terms, boost);
      this.w1 = w1;
      this.w2 = w2;
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
      w1.spanWeight.extractTermStates(contexts);
      w2.spanWeight.extractTermStates(contexts);
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
      Terms terms = context.reader().terms(field);
      if (terms == null) {
        return null; // field does not exist
      }
      MtasSpanIntersectingQuerySpans s1 = new MtasSpanIntersectingQuerySpans(
          MtasSpanIntersectingQuery.this,
          w1.spanWeight.getSpans(context, requiredPostings));
      MtasSpanIntersectingQuerySpans s2 = new MtasSpanIntersectingQuerySpans(
          MtasSpanIntersectingQuery.this,
          w2.spanWeight.getSpans(context, requiredPostings));
      return new MtasSpanIntersectingSpans(MtasSpanIntersectingQuery.this, s1,
          s2);
    }

//    @Override
//    public boolean isCacheable(LeafReaderContext arg0) {
//      return w1.spanWeight.isCacheable(arg0) && w2.spanWeight.isCacheable(arg0);
//    }

  }

  /**
   * The Class MtasSpanIntersectingQuerySpans.
   */
  protected static class MtasSpanIntersectingQuerySpans {

    /** The spans. */
    public Spans spans;

    /**
     * Instantiates a new mtas span intersecting query spans.
     *
     * @param query the query
     * @param spans the spans
     */
    public MtasSpanIntersectingQuerySpans(MtasSpanIntersectingQuery query,
        Spans spans) {
      this.spans = spans != null ? spans : new MtasSpanMatchNoneSpans(query);
    }

  }

  /**
   * The Class MtasSpanIntersectingQueryWeight.
   */
  private static class MtasSpanIntersectingQueryWeight {

    /** The span weight. */
    public SpanWeight spanWeight;

    /**
     * Instantiates a new mtas span intersecting query weight.
     *
     * @param spanWeight the span weight
     */
    public MtasSpanIntersectingQueryWeight(SpanWeight spanWeight) {
      this.spanWeight = spanWeight;
    }
  }
  
  @Override
  public boolean isMatchAllPositionsQuery() {
    return false;
  }

@Override
public void visit(QueryVisitor aVisitor)
{
    q1.visit(aVisitor);
    q2.visit(aVisitor);
}

}
