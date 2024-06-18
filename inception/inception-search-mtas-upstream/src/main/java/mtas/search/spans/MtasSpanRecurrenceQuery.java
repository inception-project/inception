package mtas.search.spans;

import java.io.IOException;
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
 * The Class MtasSpanRecurrenceQuery.
 */
public class MtasSpanRecurrenceQuery extends MtasSpanQuery {

  /** The query. */
  private MtasSpanQuery query;

  /** The minimum recurrence. */
  private int minimumRecurrence;

  /** The maximum recurrence. */
  private int maximumRecurrence;

  /** The ignore query. */
  private MtasSpanQuery ignoreQuery;

  /** The maximum ignore length. */
  private Integer maximumIgnoreLength;

  /** The field. */
  private String field;

  /**
   * Instantiates a new mtas span recurrence query.
   *
   * @param query the query
   * @param minimumRecurrence the minimum recurrence
   * @param maximumRecurrence the maximum recurrence
   * @param ignoreQuery the ignore query
   * @param maximumIgnoreLength the maximum ignore length
   */
  public MtasSpanRecurrenceQuery(MtasSpanQuery query, int minimumRecurrence,
      int maximumRecurrence, MtasSpanQuery ignoreQuery,
      Integer maximumIgnoreLength) {
    super(null, null);
    field = query.getField();
    this.query = query;
    if (field != null && ignoreQuery != null) {
      if (ignoreQuery.getField() == null
          || field.equals(ignoreQuery.getField())) {
        this.ignoreQuery = ignoreQuery;
        if (maximumIgnoreLength == null) {
          this.maximumIgnoreLength = 1;
        } else {
          this.maximumIgnoreLength = maximumIgnoreLength;
        }
      } else {
        throw new IllegalArgumentException(
            "ignore must have same field as clauses");
      }
    } else {
      this.ignoreQuery = null;
      this.maximumIgnoreLength = null;
    }
    setRecurrence(minimumRecurrence, maximumRecurrence);
  }

  /**
   * Gets the query.
   *
   * @return the query
   */
  public MtasSpanQuery getQuery() {
    return query;
  }

  /**
   * Gets the ignore query.
   *
   * @return the ignore query
   */
  public MtasSpanQuery getIgnoreQuery() {
    return ignoreQuery;
  }

  /**
   * Gets the maximum ignore length.
   *
   * @return the maximum ignore length
   */
  public Integer getMaximumIgnoreLength() {
    return maximumIgnoreLength;
  }

  /**
   * Gets the minimum recurrence.
   *
   * @return the minimum recurrence
   */
  public int getMinimumRecurrence() {
    return minimumRecurrence;
  }

  /**
   * Gets the maximum recurrence.
   *
   * @return the maximum recurrence
   */
  public int getMaximumRecurrence() {
    return maximumRecurrence;
  }

  /**
   * Sets the recurrence.
   *
   * @param minimumRecurrence the minimum recurrence
   * @param maximumRecurrence the maximum recurrence
   */
  public void setRecurrence(int minimumRecurrence, int maximumRecurrence) {
    if (minimumRecurrence > maximumRecurrence) {
      throw new IllegalArgumentException(
          "minimumRecurrence > maximumRecurrence");
    } else if (minimumRecurrence < 1) {
      throw new IllegalArgumentException("minimumRecurrence < 1 not supported");
    } else if (query == null) {
      throw new IllegalArgumentException("no clause");
    }
    this.minimumRecurrence = minimumRecurrence;
    this.maximumRecurrence = maximumRecurrence;
    // set minimum/maximum
    Integer minimum = null;
    Integer maximum = null;
    if (query.getMinimumWidth() != null) {
      minimum = minimumRecurrence * query.getMinimumWidth();
    }
    if (query.getMaximumWidth() != null) {
      maximum = maximumRecurrence * query.getMaximumWidth();
      if (ignoreQuery != null && maximumIgnoreLength != null) {
        if (ignoreQuery.getMaximumWidth() != null) {
          maximum += (maximumRecurrence - 1) * maximumIgnoreLength
              * ignoreQuery.getMaximumWidth();
        } else {
          maximum = null;
        }
      }
    }
    setWidth(minimum, maximum);
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
   * org.apache.lucene.search.Query#rewrite(org.apache.lucene.index.IndexReader)
   */
  @Override
  public MtasSpanQuery rewrite(IndexReader reader) throws IOException {
    MtasSpanQuery newQuery = query.rewrite(reader);
    if (maximumRecurrence == 1) {
      return newQuery;
    } else {
      MtasSpanQuery newIgnoreQuery = (ignoreQuery != null)
          ? ignoreQuery.rewrite(reader) : null;
      if (newQuery instanceof MtasSpanRecurrenceQuery) {
        // for now too difficult, possibly merge later
      }
      if (!newQuery.equals(query)
          || (newIgnoreQuery != null && !newIgnoreQuery.equals(ignoreQuery))) {
        return new MtasSpanRecurrenceQuery(newQuery, minimumRecurrence,
            maximumRecurrence, newIgnoreQuery, maximumIgnoreLength)
                .rewrite(reader);
      } else {
        return super.rewrite(reader);
      }
    }
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
    buffer.append(query.toString(query.getField()));
    buffer.append("," + minimumRecurrence + "," + maximumRecurrence);
    buffer.append(", ");
    buffer.append(ignoreQuery);
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
    final MtasSpanRecurrenceQuery other = (MtasSpanRecurrenceQuery) obj;
    boolean result;
    result = query.equals(other.query);
    result &= minimumRecurrence == other.minimumRecurrence;
    result &= maximumRecurrence == other.maximumRecurrence;
    if (result) {
      boolean subResult;
      subResult = ignoreQuery == null && other.ignoreQuery == null;
      subResult |= ignoreQuery != null && other.ignoreQuery != null
          && ignoreQuery.equals(other.ignoreQuery);
      return subResult;
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.Query#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.getClass().getSimpleName(), query, minimumRecurrence, maximumRecurrence);   
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
    SpanWeight subWeight = query.createWeight(searcher, ScoreMode.COMPLETE_NO_SCORES, boost);
    SpanWeight ignoreWeight = null;
    if (ignoreQuery != null) {
      ignoreWeight = ignoreQuery.createWeight(searcher, ScoreMode.COMPLETE_NO_SCORES, boost);
    }
    return new SpanRecurrenceWeight(subWeight, ignoreWeight,
        maximumIgnoreLength, searcher,
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
    query.disableTwoPhaseIterator();
    if (ignoreQuery != null) {
      ignoreQuery.disableTwoPhaseIterator();
    }
  }

  /**
   * The Class SpanRecurrenceWeight.
   */
  protected class SpanRecurrenceWeight extends MtasSpanWeight {

    /** The sub weight. */
    final SpanWeight subWeight;

    /** The ignore weight. */
    final SpanWeight ignoreWeight;

    /** The maximum ignore length. */
    final Integer maximumIgnoreLength;

    /**
     * Instantiates a new span recurrence weight.
     *
     * @param subWeight the sub weight
     * @param ignoreWeight the ignore weight
     * @param maximumIgnoreLength the maximum ignore length
     * @param searcher the searcher
     * @param terms the terms
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public SpanRecurrenceWeight(SpanWeight subWeight, SpanWeight ignoreWeight,
        Integer maximumIgnoreLength, IndexSearcher searcher,
        Map<Term, TermStates> terms, float boost) throws IOException {
      super(MtasSpanRecurrenceQuery.this, searcher, terms, boost);
      this.subWeight = subWeight;
      this.ignoreWeight = ignoreWeight;
      this.maximumIgnoreLength = maximumIgnoreLength;
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
      if (field == null) {
        return null;
      } else {
        Terms terms = context.reader().terms(field);
        if (terms == null) {
          return null; // field does not exist
        }
        Spans subSpans = subWeight.getSpans(context, requiredPostings);
        if (subSpans == null) {
          return null;
        } else {
          Spans ignoreSpans = null;
          if (ignoreWeight != null) {
            ignoreSpans = ignoreWeight.getSpans(context, requiredPostings);
          }
          return new MtasSpanRecurrenceSpans(MtasSpanRecurrenceQuery.this,
              subSpans, minimumRecurrence, maximumRecurrence, ignoreSpans,
              maximumIgnoreLength);
        }
      }
    }

//    @Override
//    public boolean isCacheable(LeafReaderContext arg0) {
//      return subWeight.isCacheable(arg0) && (ignoreWeight==null || ignoreWeight.isCacheable(arg0));
//    }

  }
  
  @Override
  public boolean isMatchAllPositionsQuery() {
    return false;
  }

@Override
public void visit(QueryVisitor aVisitor)
{
    query.visit(aVisitor);
    // FIXME REC-2024-03-12 Should ignoreQuery be visited here as well?
}

}
