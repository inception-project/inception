package mtas.search.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.queries.spans.SpanWithinQuery;
import mtas.search.spans.util.MtasMaximumExpandSpanQuery;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Class MtasSpanWithinQuery.
 */
public class MtasSpanWithinQuery extends MtasSpanQuery {

  /** The base query. */
  private SpanWithinQuery baseQuery;

  /** The small query. */
  private MtasSpanQuery smallQuery;

  /** The big query. */
  private MtasSpanQuery bigQuery;

  /** The left boundary big minimum. */
  private int leftBoundaryBigMinimum;

  /** The left boundary big maximum. */
  private int leftBoundaryBigMaximum;

  /** The right boundary big maximum. */
  private int rightBoundaryBigMaximum;

  /** The right boundary big minimum. */
  private int rightBoundaryBigMinimum;

  /** The auto adjust big query. */
  private boolean autoAdjustBigQuery;

  /** The field. */
  String field;

  /**
   * Instantiates a new mtas span within query.
   *
   * @param q1 the q 1
   * @param q2 the q 2
   */

  public MtasSpanWithinQuery(MtasSpanQuery q1, MtasSpanQuery q2) {
    this(q1, q2, 0, 0, 0, 0, true);
  }

  /**
   * Instantiates a new mtas span within query.
   *
   * @param q1 the q 1
   * @param q2 the q 2
   * @param leftMinimum the left minimum
   * @param leftMaximum the left maximum
   * @param rightMinimum the right minimum
   * @param rightMaximum the right maximum
   * @param adjustBigQuery the adjust big query
   */
  public MtasSpanWithinQuery(MtasSpanQuery q1, MtasSpanQuery q2,
      int leftMinimum, int leftMaximum, int rightMinimum, int rightMaximum,
      boolean adjustBigQuery) {
    super(null, null);
    bigQuery = q1;
    smallQuery = q2;
    leftBoundaryBigMinimum = leftMinimum;
    leftBoundaryBigMaximum = leftMaximum;
    rightBoundaryBigMinimum = rightMinimum;
    rightBoundaryBigMaximum = rightMaximum;
    autoAdjustBigQuery = adjustBigQuery;
    // recompute width
    Integer minimumWidth = null;
    Integer maximumWidth = null;
    if (bigQuery != null) {
      maximumWidth = bigQuery.getMaximumWidth();
      maximumWidth = (maximumWidth != null)
          ? maximumWidth + rightBoundaryBigMaximum + leftBoundaryBigMaximum
          : null;
    }
    if (smallQuery != null) {
      if (smallQuery.getMaximumWidth() != null && (maximumWidth == null
          || smallQuery.getMaximumWidth() < maximumWidth)) {
        maximumWidth = smallQuery.getMaximumWidth();
      }
      minimumWidth = smallQuery.getMinimumWidth();
    }
    setWidth(minimumWidth, maximumWidth);
    // compute field
    if (bigQuery != null && bigQuery.getField() != null) {
      field = bigQuery.getField();
    } else if (smallQuery != null && smallQuery.getField() != null) {
      field = smallQuery.getField();
    } else {
      field = null;
    }
    if (field != null) {
      baseQuery = new SpanWithinQuery(new MtasMaximumExpandSpanQuery(bigQuery,
          leftBoundaryBigMinimum, leftBoundaryBigMaximum,
          rightBoundaryBigMinimum, rightBoundaryBigMaximum), smallQuery);
    } else {
      baseQuery = null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.search.spans.util.MtasSpanQuery#rewrite(org.apache.lucene.index.
   * IndexReader)
   */
  @Override
  public MtasSpanQuery rewrite(IndexReader reader) throws IOException {
    MtasSpanQuery newBigQuery = bigQuery.rewrite(reader);
    MtasSpanQuery newSmallQuery = smallQuery.rewrite(reader);

    if (newBigQuery == null || newBigQuery instanceof MtasSpanMatchNoneQuery
        || newSmallQuery == null
        || newSmallQuery instanceof MtasSpanMatchNoneQuery) {
      return new MtasSpanMatchNoneQuery(field);
    }

    if (newSmallQuery.getMinimumWidth() != null
        && newBigQuery.getMaximumWidth() != null
        && newSmallQuery.getMinimumWidth() > (newBigQuery.getMaximumWidth()
            + leftBoundaryBigMaximum + rightBoundaryBigMaximum)) {
      return new MtasSpanMatchNoneQuery(field);
    }

    if (autoAdjustBigQuery) {
      if (newBigQuery instanceof MtasSpanRecurrenceQuery) {
        MtasSpanRecurrenceQuery recurrenceQuery = (MtasSpanRecurrenceQuery) newBigQuery;
        if (recurrenceQuery.getIgnoreQuery() == null
            && recurrenceQuery.getQuery() instanceof MtasSpanMatchAllQuery) {
          rightBoundaryBigMaximum += leftBoundaryBigMaximum
              + recurrenceQuery.getMaximumRecurrence();
          rightBoundaryBigMinimum += leftBoundaryBigMinimum
              + recurrenceQuery.getMinimumRecurrence();
          leftBoundaryBigMaximum = 0;
          leftBoundaryBigMinimum = 0;
          newBigQuery = new MtasSpanMatchAllQuery(field);
          // System.out.println("REPLACE WITH " + newBigQuery + " (["
          // + leftBoundaryMinimum + "," + leftBoundaryMaximum + "],["
          // + rightBoundaryMinimum + "," + rightBoundaryMaximum + "])");
          return new MtasSpanWithinQuery(newBigQuery, newSmallQuery,
              leftBoundaryBigMinimum, leftBoundaryBigMaximum,
              rightBoundaryBigMinimum, rightBoundaryBigMaximum,
              autoAdjustBigQuery).rewrite(reader);
        }
      } else if (newBigQuery instanceof MtasSpanMatchAllQuery) {
        if (leftBoundaryBigMaximum > 0) {
          rightBoundaryBigMaximum += leftBoundaryBigMaximum;
          rightBoundaryBigMinimum += leftBoundaryBigMinimum;
          leftBoundaryBigMaximum = 0;
          leftBoundaryBigMinimum = 0;
          // System.out.println("REPLACE WITH " + newBigQuery + " (["
          // + leftBoundaryMinimum + "," + leftBoundaryMaximum + "],["
          // + rightBoundaryMinimum + "," + rightBoundaryMaximum + "])");
          return new MtasSpanWithinQuery(newBigQuery, newSmallQuery,
              leftBoundaryBigMinimum, leftBoundaryBigMaximum,
              rightBoundaryBigMinimum, rightBoundaryBigMaximum,
              autoAdjustBigQuery).rewrite(reader);
        }
      } else if (newBigQuery instanceof MtasSpanSequenceQuery) {
        MtasSpanSequenceQuery sequenceQuery = (MtasSpanSequenceQuery) newBigQuery;
        if (sequenceQuery.getIgnoreQuery() == null) {
          List<MtasSpanSequenceItem> items = sequenceQuery.getItems();
          List<MtasSpanSequenceItem> newItems = new ArrayList<>();
          int newLeftBoundaryMinimum = 0;
          int newLeftBoundaryMaximum = 0;
          int newRightBoundaryMinimum = 0;
          int newRightBoundaryMaximum = 0;
          for (int i = 0; i < items.size(); i++) {
            // first item
            if (i == 0) {
              if (items.get(i).getQuery() instanceof MtasSpanMatchAllQuery) {
                newLeftBoundaryMaximum++;
                if (!items.get(i).isOptional()) {
                  newLeftBoundaryMinimum++;
                }
              } else if (items.get(i)
                  .getQuery() instanceof MtasSpanRecurrenceQuery) {
                MtasSpanRecurrenceQuery msrq = (MtasSpanRecurrenceQuery) items
                    .get(i).getQuery();
                if (msrq.getQuery() instanceof MtasSpanMatchAllQuery) {
                  newLeftBoundaryMaximum += msrq.getMaximumRecurrence();
                  if (!items.get(i).isOptional()) {
                    newLeftBoundaryMinimum += msrq.getMinimumRecurrence();
                  }
                } else {
                  newItems.add(items.get(i));
                }
              } else {
                newItems.add(items.get(i));
              }
              // last item
            } else if (i == (items.size() - 1)) {
              if (items.get(i).getQuery() instanceof MtasSpanMatchAllQuery) {
                newRightBoundaryMaximum++;
                if (!items.get(i).isOptional()) {
                  newRightBoundaryMinimum++;
                }
              } else if (items.get(i)
                  .getQuery() instanceof MtasSpanRecurrenceQuery) {
                MtasSpanRecurrenceQuery msrq = (MtasSpanRecurrenceQuery) items
                    .get(i).getQuery();
                if (msrq.getQuery() instanceof MtasSpanMatchAllQuery) {
                  newRightBoundaryMaximum += msrq.getMaximumRecurrence();
                  if (!items.get(i).isOptional()) {
                    newRightBoundaryMinimum += msrq.getMinimumRecurrence();
                  }
                } else {
                  newItems.add(items.get(i));
                }
              } else {
                newItems.add(items.get(i));
              }
              // other items
            } else {
              newItems.add(items.get(i));
            }
          }
          leftBoundaryBigMaximum += newLeftBoundaryMaximum;
          leftBoundaryBigMinimum += newLeftBoundaryMinimum;
          rightBoundaryBigMaximum += newRightBoundaryMaximum;
          rightBoundaryBigMinimum += newRightBoundaryMinimum;
          if (newItems.isEmpty()) {
            rightBoundaryBigMaximum = Math.max(0,
                rightBoundaryBigMaximum + leftBoundaryBigMaximum - 1);
            rightBoundaryBigMinimum = Math.max(0,
                rightBoundaryBigMinimum + leftBoundaryBigMinimum - 1);
            leftBoundaryBigMaximum = 0;
            leftBoundaryBigMinimum = 0;
            newItems.add(new MtasSpanSequenceItem(
                new MtasSpanMatchAllQuery(field), false));
          }
          if (!items.equals(newItems) || newLeftBoundaryMaximum > 0
              || newRightBoundaryMaximum > 0) {
            newBigQuery = (new MtasSpanSequenceQuery(newItems, null, null))
                .rewrite(reader);
            // System.out.println("REPLACE WITH " + newBigQuery + " (["
            // + leftBoundaryMinimum + "," + leftBoundaryMaximum + "],["
            // + rightBoundaryMinimum + "," + rightBoundaryMaximum + "])");
            return new MtasSpanWithinQuery(newBigQuery, newSmallQuery,
                leftBoundaryBigMinimum, leftBoundaryBigMaximum,
                rightBoundaryBigMinimum, rightBoundaryBigMaximum,
                autoAdjustBigQuery).rewrite(reader);
          }
        }
      }
    }

    if (!newBigQuery.equals(bigQuery) || !newSmallQuery.equals(smallQuery)) {
      return (new MtasSpanWithinQuery(newBigQuery, newSmallQuery,
          leftBoundaryBigMinimum, leftBoundaryBigMaximum,
          rightBoundaryBigMinimum, rightBoundaryBigMaximum, autoAdjustBigQuery))
              .rewrite(reader);
    } else if (newBigQuery.equals(newSmallQuery)) {
      return newBigQuery;
    } else {
      baseQuery = (SpanWithinQuery) baseQuery.rewrite(reader);
      return super.rewrite(reader);
    }
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
  public SpanWeight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost)
      throws IOException {
    return baseQuery.createWeight(searcher, scoreMode, boost);
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
    if (smallQuery != null) {
      buffer.append(smallQuery.toString(smallQuery.getField()));
    } else {
      buffer.append("null");
    }
    buffer.append(",");
    if (bigQuery != null) {
      buffer.append(bigQuery.toString(bigQuery.getField()));
    } else {
      buffer.append("null");
    }
    buffer.append(
        "],[" + leftBoundaryBigMinimum + "," + leftBoundaryBigMaximum + "],["
            + rightBoundaryBigMinimum + "," + rightBoundaryBigMaximum + "])");
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
    final MtasSpanWithinQuery that = (MtasSpanWithinQuery) obj;
    return baseQuery.equals(that.baseQuery)
        && leftBoundaryBigMinimum == that.leftBoundaryBigMinimum
        && leftBoundaryBigMaximum == that.leftBoundaryBigMaximum
        && rightBoundaryBigMinimum == that.rightBoundaryBigMinimum
        && rightBoundaryBigMaximum == that.rightBoundaryBigMaximum;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.Query#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.getClass().getSimpleName(), smallQuery, bigQuery, leftBoundaryBigMinimum, leftBoundaryBigMaximum, rightBoundaryBigMinimum, rightBoundaryBigMaximum,autoAdjustBigQuery);       
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.search.spans.util.MtasSpanQuery#disableTwoPhaseIterator()
   */
  @Override
  public void disableTwoPhaseIterator() {
    super.disableTwoPhaseIterator();
    bigQuery.disableTwoPhaseIterator();
    smallQuery.disableTwoPhaseIterator();
  }
  
  @Override
  public boolean isMatchAllPositionsQuery() {
    return false;
  }

@Override
public void visit(QueryVisitor aVisitor)
{
    baseQuery.visit(aVisitor);
}

}
