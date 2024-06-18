package mtas.search.spans;

import java.io.IOException;
import java.util.HashSet;

import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.queries.spans.SpanCollector;
import mtas.search.spans.MtasSpanPrecededByQuery.MtasSpanPrecededByQuerySpans;
import mtas.search.spans.util.MtasSpans;

/**
 * The Class MtasSpanPrecededBySpans.
 */
public class MtasSpanPrecededBySpans extends MtasSpans {

  /** The query. */
  private MtasSpanPrecededByQuery query;

  /** The spans 1. */
  private MtasSpanPrecededByQuerySpans spans1;

  /** The spans 2. */
  private MtasSpanPrecededByQuerySpans spans2;

  /** The last spans 2 start position. */
  private int lastSpans2StartPosition;

  /** The last spans 2 end position. */
  private int lastSpans2EndPosition;

  /** The maximum spans 2 end position. */
  private int maximumSpans2EndPosition;

  /** The previous spans 2 end positions. */
  private HashSet<Integer> previousSpans2EndPositions;

  /** The called next start position. */
  private boolean calledNextStartPosition;

  /** The no more positions. */
  private boolean noMorePositions;

  /** The doc id. */
  private int docId;

  /**
   * Instantiates a new mtas span preceded by spans.
   *
   * @param query the query
   * @param spans1 the spans 1
   * @param spans2 the spans 2
   */
  public MtasSpanPrecededBySpans(MtasSpanPrecededByQuery query,
      MtasSpanPrecededByQuerySpans spans1,
      MtasSpanPrecededByQuerySpans spans2) {
    super();
    docId = -1;
    this.query = query;
    this.spans1 = spans1;
    this.spans2 = spans2;
    previousSpans2EndPositions = new HashSet<>();
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
    spans1.spans.collect(collector);
    spans2.spans.collect(collector);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#endPosition()
   */
  @Override
  public int endPosition() {
    if (calledNextStartPosition) {
      return noMorePositions ? NO_MORE_POSITIONS : spans1.spans.endPosition();
    } else {
      return -1;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#nextStartPosition()
   */
  @Override
  public int nextStartPosition() throws IOException {
    // no document
    if (docId == -1 || docId == NO_MORE_DOCS) {
      throw new IOException("no document");
      // finished
    } else if (noMorePositions) {
      return NO_MORE_POSITIONS;
      // littleSpans already at start match, because of check for matching
      // document
    } else if (!calledNextStartPosition) {
      calledNextStartPosition = true;
      return spans1.spans.startPosition();
      // compute next match
    } else {
      if (goToNextStartPosition()) {
        // match found
        return spans1.spans.startPosition();
      } else {
        // no more matches: document finished
        return NO_MORE_POSITIONS;
      }
    }
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
   * @see org.apache.lucene.search.spans.Spans#startPosition()
   */
  @Override
  public int startPosition() {
    if (calledNextStartPosition) {
      return noMorePositions ? NO_MORE_POSITIONS : spans1.spans.startPosition();
    } else {
      return -1;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#width()
   */
  @Override
  public int width() {
    if (calledNextStartPosition) {
      return noMorePositions ? 0
          : spans1.spans.endPosition() - spans1.spans.startPosition();
    } else {
      return 0;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.DocIdSetIterator#advance(int)
   */
  @Override
  public int advance(int target) throws IOException {
    reset();
    if (docId == NO_MORE_DOCS) {
      return docId;
    } else if (target < docId) {
      // should not happen
      docId = NO_MORE_DOCS;
      return docId;
    } else {
      // advance 1
      int spans1DocId = spans1.spans.docID();
      int newTarget = target;
      if (spans1DocId < newTarget) {
        spans1DocId = spans1.spans.advance(target);
        if (spans1DocId == NO_MORE_DOCS) {
          docId = NO_MORE_DOCS;
          return docId;
        }
        newTarget = Math.max(newTarget, spans1DocId);
      }
      int spans2DocId = spans2.spans.docID();
      // advance 2
      if (spans2DocId < newTarget) {
        spans2DocId = spans2.spans.advance(newTarget);
        if (spans2DocId == NO_MORE_DOCS) {
          docId = NO_MORE_DOCS;
          return docId;
        }
      }
      // check equal docId, otherwise next
      if (spans1DocId == spans2DocId) {
        docId = spans1DocId;
        // check match
        if (goToNextStartPosition()) {
          return docId;
        } else {
          return nextDoc();
        }
      } else {
        return nextDoc();
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.DocIdSetIterator#cost()
   */
  @Override
  public long cost() {
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.DocIdSetIterator#docID()
   */
  @Override
  public int docID() {
    return docId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.DocIdSetIterator#nextDoc()
   */
  @Override
  public int nextDoc() throws IOException {
    reset();
    while (!goToNextDoc())
      ;
    return docId;
  }

  /**
   * Go to next doc.
   *
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private boolean goToNextDoc() throws IOException {
    if (docId == NO_MORE_DOCS) {
      return true;
    } else {
      int spans1DocId = spans1.spans.nextDoc();
      int spans2DocId = spans2.spans.docID();
      docId = Math.max(spans1DocId, spans2DocId);
      while (spans1DocId != spans2DocId && docId != NO_MORE_DOCS) {
        if (spans1DocId < spans2DocId) {
          spans1DocId = spans1.spans.advance(spans2DocId);
          docId = spans1DocId;
        } else {
          spans2DocId = spans2.spans.advance(spans1DocId);
          docId = spans2DocId;
        }
      }
      if (docId != NO_MORE_DOCS && !goToNextStartPosition()) {
        reset();
        return false;
      } else {
        return true;
      }
    }
  }

  /**
   * Go to next start position.
   *
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private boolean goToNextStartPosition() throws IOException {
    int nextSpans1StartPosition;
    while ((nextSpans1StartPosition = spans1.spans
        .nextStartPosition()) != NO_MORE_POSITIONS) {
      if (nextSpans1StartPosition == lastSpans2EndPosition) {
        return true;
      } else {
        // clean up
        if (maximumSpans2EndPosition < nextSpans1StartPosition) {
          previousSpans2EndPositions.clear();
          maximumSpans2EndPosition = -1;
        } else if (previousSpans2EndPositions
            .contains(nextSpans1StartPosition)) {
          return true;
        }
        // try to find match
        while (lastSpans2StartPosition < nextSpans1StartPosition) {
          if (lastSpans2StartPosition != NO_MORE_POSITIONS) {
            lastSpans2StartPosition = spans2.spans.nextStartPosition();
          }
          if (lastSpans2StartPosition == NO_MORE_POSITIONS) {
            if (previousSpans2EndPositions.isEmpty()) {
              noMorePositions = true;
              return false;
            }
          } else {
            lastSpans2EndPosition = spans2.spans.endPosition();
            if (lastSpans2EndPosition >= nextSpans1StartPosition) {
              previousSpans2EndPositions.add(lastSpans2EndPosition);
              maximumSpans2EndPosition = Math.max(maximumSpans2EndPosition,
                  lastSpans2EndPosition);
            }
            if (nextSpans1StartPosition == lastSpans2EndPosition) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Reset.
   */
  private void reset() {
    calledNextStartPosition = false;
    noMorePositions = false;
    lastSpans2StartPosition = -1;
    lastSpans2EndPosition = -1;
    maximumSpans2EndPosition = -1;
    previousSpans2EndPositions.clear();
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.search.spans.util.MtasSpans#asTwoPhaseIterator()
   */
  @Override
  public TwoPhaseIterator asTwoPhaseIterator() {
    if (spans1 == null || spans2 == null || !query.twoPhaseIteratorAllowed()) {
      return null;
    } else {
      // TODO
      return null;
    }
  }

}
