package mtas.search.spans.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.queries.spans.SpanCollector;
import org.apache.lucene.queries.spans.Spans;

/**
 * The Class MtasSpanUniquePositionSpans.
 */
public class MtasSpanUniquePositionSpans extends MtasSpans {

  /** The query. */
  private MtasSpanUniquePositionQuery query;

  /** The spans. */
  private Spans spans;

  /** The queue spans. */
  private List<Match> queueSpans;

  /** The queue matches. */
  private List<Match> queueMatches;

  /** The current match. */
  private Match currentMatch;

  /** The last start position. */
  private int lastStartPosition; // startPosition of last retrieved span

  /** The last span. */
  private boolean lastSpan; // last span for this document added to queue

  /** The no more positions. */
  private boolean noMorePositions;

  /**
   * Instantiates a new mtas span unique position spans.
   *
   * @param query the query
   * @param spans the spans
   */
  public MtasSpanUniquePositionSpans(MtasSpanUniquePositionQuery query,
      Spans spans) {
    super();
    this.query = query;
    this.spans = spans;
    queueSpans = new ArrayList<>();
    queueMatches = new ArrayList<>();
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
    // return (currentMatch.endPosition() - currentMatch.startPosition());
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
    noMorePositions = false;
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
    noMorePositions = false;
    return (spans.advance(target) == NO_MORE_DOCS) ? NO_MORE_DOCS
        : toMatchDoc();
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
      return spans.asTwoPhaseIterator();
    }
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
      while (true) {
        // try to get something in queue of spans
        if (queueSpans.isEmpty() && !collectSpan()) {
          return false;
        }
        // try to get matches with first span in queue
        Match firstMatch = queueSpans.get(0);
        queueSpans.remove(0);
        // create a list of matches with same startposition as firstMatch
        List<Match> matches = new ArrayList<>();
        matches.add(firstMatch);
        // try to collect spans until lastStartPosition not equal to
        // startposition of firstMatch
        while (!lastSpan && (lastStartPosition == firstMatch.startPosition())) {
          collectSpan();
        }
        while (!queueSpans.isEmpty() && (queueSpans.get(0)
            .startPosition() == firstMatch.startPosition())) {
          matches.add(queueSpans.get(0));
          queueSpans.remove(0);
        }
        // construct all matches for this startposition
        for (Match match : matches) {
          // only unique spans
          if (!queueMatches.contains(match)) {
            queueMatches.add(match);
          }
        }
        // check for something in queue of matches
        if (!queueMatches.isEmpty()) {
          return true;
        }
      }
    }
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
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
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
    return (spans == null) ? 0 : spans.positionsCost();
  }

}
