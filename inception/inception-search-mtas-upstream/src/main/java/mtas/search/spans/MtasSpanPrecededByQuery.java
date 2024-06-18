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
 * The Class MtasSpanPrecededByQuery.
 */
public class MtasSpanPrecededByQuery extends MtasSpanQuery {

  /** The field. */
  private String field;

  /** The q 1. */
  private MtasSpanQuery q1;

  /** The q 2. */
  private MtasSpanQuery q2;

  /**
   * Instantiates a new mtas span preceded by query.
   *
   * @param q1 the q 1
   * @param q2 the q 2
   */
  public MtasSpanPrecededByQuery(MtasSpanQuery q1, MtasSpanQuery q2) {
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
   * mtas.search.spans.util.MtasSpanQuery#createWeight(org.apache.lucene.search.
   * IndexSearcher, boolean)
   */
  @Override
  public MtasSpanWeight createWeight(IndexSearcher searcher,
      ScoreMode scoreMode, float boost) throws IOException {
    if (q1 == null || q2 == null) {
      return null;
    } else {
      MtasSpanPrecededByQueryWeight w1 = new MtasSpanPrecededByQueryWeight(
          q1.createWeight(searcher, scoreMode, boost));
      MtasSpanPrecededByQueryWeight w2 = new MtasSpanPrecededByQueryWeight(
          q2.createWeight(searcher, scoreMode, boost));
      // subWeights
      List<MtasSpanPrecededByQueryWeight> subWeights = new ArrayList<>();
      subWeights.add(w1);
      subWeights.add(w2);
      // return
      return new SpanPrecededByWeight(w1, w2, searcher,
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
      List<MtasSpanPrecededByQueryWeight> items) {
    List<SpanWeight> weights = new ArrayList<>();
    for (MtasSpanPrecededByQueryWeight item : items) {
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
    final MtasSpanPrecededByQuery other = (MtasSpanPrecededByQuery) obj;
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
    if (newQ1 == null || newQ1 instanceof MtasSpanMatchNoneQuery
        || newQ2 == null || newQ2 instanceof MtasSpanMatchNoneQuery) {
      return new MtasSpanMatchNoneQuery(field);
    } else if (!newQ1.equals(q1) || !newQ2.equals(q2)) {
      return new MtasSpanPrecededByQuery(newQ1, newQ2).rewrite(reader);
    } else {
      return super.rewrite(reader);
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
   * The Class SpanPrecededByWeight.
   */
  protected class SpanPrecededByWeight extends MtasSpanWeight {

    /** The w 1. */
    MtasSpanPrecededByQueryWeight w1;

    /** The w 2. */
    MtasSpanPrecededByQueryWeight w2;

    /**
     * Instantiates a new span preceded by weight.
     *
     * @param w1 the w 1
     * @param w2 the w 2
     * @param searcher the searcher
     * @param terms the terms
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public SpanPrecededByWeight(MtasSpanPrecededByQueryWeight w1,
        MtasSpanPrecededByQueryWeight w2, IndexSearcher searcher,
        Map<Term, TermStates> terms, float boost) throws IOException {
      super(MtasSpanPrecededByQuery.this, searcher, terms, boost);
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
      MtasSpanPrecededByQuerySpans s1 = new MtasSpanPrecededByQuerySpans(
          MtasSpanPrecededByQuery.this,
          w1.spanWeight.getSpans(context, requiredPostings));
      MtasSpanPrecededByQuerySpans s2 = new MtasSpanPrecededByQuerySpans(
          MtasSpanPrecededByQuery.this,
          w2.spanWeight.getSpans(context, requiredPostings));
      return new MtasSpanPrecededBySpans(MtasSpanPrecededByQuery.this, s1, s2);
    }

//    @Override
//    public boolean isCacheable(LeafReaderContext arg0) {
//      return w1.spanWeight.isCacheable(arg0) && w2.spanWeight.isCacheable(arg0);
//    }

  }

  /**
   * The Class MtasSpanPrecededByQuerySpans.
   */
  protected static class MtasSpanPrecededByQuerySpans {

    /** The spans. */
    public Spans spans;

    /**
     * Instantiates a new mtas span preceded by query spans.
     *
     * @param query the query
     * @param spans the spans
     */
    public MtasSpanPrecededByQuerySpans(MtasSpanPrecededByQuery query,
        Spans spans) {
      this.spans = spans != null ? spans : new MtasSpanMatchNoneSpans(query);
    }

  }

  /**
   * The Class MtasSpanPrecededByQueryWeight.
   */
  private static class MtasSpanPrecededByQueryWeight {

    /** The span weight. */
    public SpanWeight spanWeight;

    /**
     * Instantiates a new mtas span preceded by query weight.
     *
     * @param spanWeight the span weight
     */
    public MtasSpanPrecededByQueryWeight(SpanWeight spanWeight) {
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
