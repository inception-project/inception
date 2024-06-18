package mtas.search.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.queries.spans.SpanCollector;
import org.apache.lucene.queries.spans.Spans;

import mtas.search.spans.util.MtasIgnoreItem;
import mtas.search.spans.util.MtasSpans;

/**
 * The Class MtasSpanRecurrenceSpans.
 */
public class MtasSpanRecurrenceSpans extends MtasSpans {

  /** The log. */
  private static final Logger log = LoggerFactory.getLogger(MtasSpanRecurrenceSpans.class);

  /** The query. */
  private MtasSpanRecurrenceQuery query;

  /** The spans. */
  private Spans spans;

  /** The ignore item. */
  private MtasIgnoreItem ignoreItem;

  /** The minimum recurrence. */
  int minimumRecurrence;

  /** The maximum recurrence. */
  int maximumRecurrence;

  /** The queue spans. */
  List<Match> queueSpans;

  /** The queue matches. */
  List<Match> queueMatches;

  /** The current match. */
  Match currentMatch;

  /** The no more positions. */
  boolean noMorePositions;

  /** The last start position. */
  int lastStartPosition; // startPosition of last retrieved span

  /** The last span. */
  boolean lastSpan; // last span for this document added to queue

  /**
   * Instantiates a new mtas span recurrence spans.
   *
   * @param query the query
   * @param spans the spans
   * @param minimumRecurrence the minimum recurrence
   * @param maximumRecurrence the maximum recurrence
   * @param ignoreSpans the ignore spans
   * @param maximumIgnoreLength the maximum ignore length
   */
  public MtasSpanRecurrenceSpans(MtasSpanRecurrenceQuery query, Spans spans,
      int minimumRecurrence, int maximumRecurrence, Spans ignoreSpans,
      Integer maximumIgnoreLength) {
    assert minimumRecurrence <= maximumRecurrence : "minimumRecurrence > maximumRecurrence";
    assert minimumRecurrence > 0 : "minimumRecurrence < 1 not supported";
    this.query = query;
    this.spans = spans;
    this.minimumRecurrence = minimumRecurrence;
    this.maximumRecurrence = maximumRecurrence;
    queueSpans = new ArrayList<>();
    queueMatches = new ArrayList<>();
    ignoreItem = new MtasIgnoreItem(ignoreSpans, maximumIgnoreLength);
    resetQueue();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#nextStartPosition()
   */
  @Override
  public int nextStartPosition() throws IOException {
    if (findMatches()) {
      currentMatch = queueMatches.get(0);
      queueMatches.remove(0);
      noMorePositions = false;
      return currentMatch.startPosition();
    } else {
      currentMatch = null;
      noMorePositions = true;
      return NO_MORE_POSITIONS;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#startPosition()
   */
  @Override
  public int startPosition() {
    if (currentMatch == null) {
      if (noMorePositions) {
        return NO_MORE_POSITIONS;
      } else {
        return -1;
      }
    } else {
      return currentMatch.startPosition();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#endPosition()
   */
  @Override
  public int endPosition() {
    if (currentMatch == null) {
      if (noMorePositions) {
        return NO_MORE_POSITIONS;
      } else {
        return -1;
      }
    } else {
      return currentMatch.endPosition();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#width()
   */
  @Override
  public int width() {
    return 1;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.lucene.search.spans.Spans#collect(org.apache.lucene.search.spans
   * .SpanCollector)
   */
  @Override
  public void collect(SpanCollector collector) throws IOException {
    spans.collect(collector);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.DocIdSetIterator#docID()
   */
  @Override
  public int docID() {
    return spans.docID();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.DocIdSetIterator#nextDoc()
   */
  @Override
  public int nextDoc() throws IOException {
    resetQueue();
    return (spans.nextDoc() == NO_MORE_DOCS) ? NO_MORE_DOCS : toMatchDoc();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.DocIdSetIterator#advance(int)
   */
  @Override
  public int advance(int target) throws IOException {
    resetQueue();
    return (spans.advance(target) == NO_MORE_DOCS) ? NO_MORE_DOCS
        : toMatchDoc();
  }

  /**
   * Reset queue.
   */
  void resetQueue() {
    queueSpans.clear();
    queueMatches.clear();
    lastStartPosition = 0;
    lastSpan = false;
    currentMatch = null;
    noMorePositions = false;
  }

  /**
   * To match doc.
   *
   * @return the int
   * @throws IOException Signals that an I/O exception has occurred.
   */
  int toMatchDoc() throws IOException {
    while (true) {
      if (findMatches()) {
        return docID();
      }
      resetQueue();
      if (spans.nextDoc() == NO_MORE_DOCS) {
        return NO_MORE_DOCS;
      }
    }
  }

  /**
   * Collect span.
   *
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  // try to get something in the queue of spans
  private boolean collectSpan() throws IOException {
    if (lastSpan) {
      return false;
    } else if (spans.nextStartPosition() == NO_MORE_POSITIONS) {
      lastSpan = true;
      return false;
    } else {
      queueSpans.add(new Match(spans.startPosition(), spans.endPosition()));
      lastStartPosition = spans.startPosition();
      return true;
    }
  }

  /**
   * Find matches.
   *
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private boolean findMatches() throws IOException {
    // check for something in queue of matches
    if (!queueMatches.isEmpty()) {
      return true;
    } else {
      ignoreItem.advanceToDoc(spans.docID());
      while (true) {
        // try to get something in queue of spans
        if (queueSpans.isEmpty() && !collectSpan()) {
          return false;
        }
        // try to get matches with first span in queue
        Match firstMatch = queueSpans.remove(0);
        // create a list of matches with same startPosition as firstMatch
        List<Match> matches = new ArrayList<>();
        matches.add(firstMatch);
        // matches.addAll(expandWithIgnoreItem(spans.docID(), firstMatch));
        // try to collect spans until lastStartPosition not equal to
        // startPosition of firstMatch
        while (!lastSpan && (lastStartPosition == firstMatch.startPosition())) {
          collectSpan();
        }
        while (!queueSpans.isEmpty() && (queueSpans.get(0)
            .startPosition() == firstMatch.startPosition())) {
          Match additionalMatch = queueSpans.remove(0);
          matches.add(additionalMatch);
          matches.addAll(expandWithIgnoreItem(spans.docID(), additionalMatch));
        }
        // construct all matches for this startPosition
        for (Match match : matches) {
          for (int n = (minimumRecurrence - 1); n <= (maximumRecurrence
              - 1); n++) {
            findMatches(match, n);
          }
        }
        // check for something in queue of matches
        if (!queueMatches.isEmpty()) {
          ignoreItem.removeBefore(spans.docID(),
              queueMatches.get(0).startPosition());
          return true;
        }
      }
    }
  }

  /**
   * Find matches.
   *
   * @param match the match
   * @param n the n
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void findMatches(Match match, int n) throws IOException {
    if (n > 0) {
      int largestMatchingEndPosition = match.endPosition();
      Set<Integer> list = ignoreItem.getFullEndPositionList(spans.docID(),
          match.endPosition());
      // try to find matches with existing queue
      if (!queueSpans.isEmpty()) {
        Match span;
        for (int i = 0; i < queueSpans.size(); i++) {
          span = queueSpans.get(i);
          if (match.endPosition() == span.startPosition()
              || (list != null && list.contains(span.startPosition()))) {
            findMatches(new Match(match.startPosition(), span.endPosition()),
                (n - 1));
            largestMatchingEndPosition = Math.max(largestMatchingEndPosition,
                span.endPosition());
          }
        }
      }
      // extend queue if necessary and possible
      while (!lastSpan && (largestMatchingEndPosition >= lastStartPosition)) {
        if (spans.nextStartPosition() == NO_MORE_POSITIONS) {
          lastSpan = true;
        } else {
          Match span = new Match(spans.startPosition(), spans.endPosition());
          queueSpans.add(span);
          lastStartPosition = spans.startPosition();
          // check if this provides new match
          if (match.endPosition() == span.startPosition()
              || (list != null && list.contains(span.startPosition()))) {
            findMatches(new Match(match.startPosition(), span.endPosition()),
                (n - 1));
            largestMatchingEndPosition = Math.max(largestMatchingEndPosition,
                span.endPosition());
          }
        }
      }
    } else {
      // only unique spans
      if (!queueMatches.contains(match)) {
        queueMatches.add(match);
      }
    }
  }

  /**
   * Expand with ignore item.
   *
   * @param docId the doc id
   * @param match the match
   * @return the list
   */
  private List<Match> expandWithIgnoreItem(int docId, Match match) {
    List<Match> list = new ArrayList<>();
    try {
      Set<Integer> ignoreList = ignoreItem.getFullEndPositionList(docId,
          match.endPosition);
      if (ignoreList != null) {
        for (Integer endPosition : ignoreList) {
          list.add(new Match(match.startPosition, endPosition));
        }
      }
    } catch (IOException e) {
      log.debug("Error", e);
    }
    return list;
  }

  /**
   * The Class Match.
   */
  private static class Match {

    /** The start position. */
    private int startPosition;

    /** The end position. */
    private int endPosition;

    /**
     * Instantiates a new match.
     *
     * @param startPosition the start position
     * @param endPosition the end position
     */
    Match(int startPosition, int endPosition) {
      this.startPosition = startPosition;
      this.endPosition = endPosition;
    }

    /**
     * Start position.
     *
     * @return the int
     */
    public int startPosition() {
      return startPosition;
    }

    /**
     * End position.
     *
     * @return the int
     */
    public int endPosition() {
      return endPosition;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
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
      final Match that = (Match) obj;
      return startPosition == that.startPosition
          && endPosition == that.endPosition;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      int h = this.getClass().getSimpleName().hashCode();
      h = (h * 5) ^ startPosition;
      h = (h * 7) ^ endPosition;
      return h;
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.DocIdSetIterator#cost()
   */
  @Override
  public long cost() {
    return (spans == null) ? 0 : spans.cost();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#positionsCost()
   */
  @Override
  public float positionsCost() {
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.search.spans.util.MtasSpans#asTwoPhaseIterator()
   */
  @Override
  public TwoPhaseIterator asTwoPhaseIterator() {
    if (spans == null || !query.twoPhaseIteratorAllowed()) {
      return null;
    } else {
      // TODO
      return null;
    }
  }

}
