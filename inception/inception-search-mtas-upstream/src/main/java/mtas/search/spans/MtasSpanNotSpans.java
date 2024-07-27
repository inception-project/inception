package mtas.search.spans;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.queries.spans.SpanCollector;
import mtas.search.spans.MtasSpanNotQuery.MtasSpanNotQuerySpans;
import mtas.search.spans.util.MtasSpans;

/**
 * The Class MtasSpanNotSpans.
 */
public class MtasSpanNotSpans extends MtasSpans {

  /** The query. */
  private MtasSpanNotQuery query;

  /** The spans 1. */
  private MtasSpanNotQuerySpans spans1;

  /** The spans 2. */
  private MtasSpanNotQuerySpans spans2;

  /** The called next start position. */
  private boolean calledNextStartPosition;

  /** The last spans 2 start position. */
  private int lastSpans2StartPosition;

  /** The last spans 2 end position. */
  private int lastSpans2EndPosition;

  /** The last spans 2 end positions. */
  private Set<Integer> lastSpans2EndPositions;

  /** The next spans 2 start position. */
  private int nextSpans2StartPosition;

  /** The next spans 2 end position. */
  private int nextSpans2EndPosition;

  /** The doc id. */
  private int docId;

  /**
   * Instantiates a new mtas span not spans.
   *
   * @param query the query
   * @param spans1 the spans 1
   * @param spans2 the spans 2
   */
  public MtasSpanNotSpans(MtasSpanNotQuery query, MtasSpanNotQuerySpans spans1,
      MtasSpanNotQuerySpans spans2) {
    super();
    docId = -1;
    this.query = query;
    this.spans1 = spans1;
    this.spans2 = spans2;
    this.lastSpans2EndPositions = new HashSet<>();
    reset();
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
   * @see org.apache.lucene.search.spans.Spans#startPosition()
   */
  @Override
  public int startPosition() {
    if (calledNextStartPosition) {
      return spans1.spans.startPosition();
    } else {
      return -1;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#endPosition()
   */
  @Override
  public int endPosition() {
    if (calledNextStartPosition) {
      return spans1.spans.endPosition();
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
      return spans1.spans.endPosition() - spans1.spans.startPosition();
    } else {
      return 0;
    }
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
   * @see org.apache.lucene.search.spans.Spans#positionsCost()
   */
  @Override
  public float positionsCost() {
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
    } else if (target <= docId) {
      // should not happen
      docId = NO_MORE_DOCS;
      return docId;
    } else {
      docId = spans1.spans.advance(target);
      if (docId == NO_MORE_DOCS) {
        return docId;
      } else {
        int spans2DocId = spans2.spans.docID();
        if (spans2DocId < docId) {
          spans2DocId = spans2.spans.advance(docId);
        }
        if (docId != spans2DocId) {
          return spans1.spans.nextStartPosition() != NO_MORE_POSITIONS ? docId
              : NO_MORE_DOCS;
        } else if (goToNextStartPosition()) {
          return docId;
        } else {
          return nextDoc();
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#asTwoPhaseIterator()
   */
  @Override
  public TwoPhaseIterator asTwoPhaseIterator() {
    if (spans1 == null || spans2 == null || !query.twoPhaseIteratorAllowed()) {
      return null;
    } else {

      TwoPhaseIterator twoPhaseIterator1 = spans1.spans.asTwoPhaseIterator();
      if (twoPhaseIterator1 != null) {
        return new TwoPhaseIterator(twoPhaseIterator1.approximation()) {
          @Override
          public boolean matches() throws IOException {
            return twoPhaseIterator1.matches() && twoPhaseCurrentDocMatches();
          }

          @Override
          public float matchCost() {
            return twoPhaseIterator1.matchCost();
          }
        };
      } else {
        return new TwoPhaseIterator(spans1.spans) {
          @Override
          public boolean matches() throws IOException {
            return twoPhaseCurrentDocMatches();
          }

          @Override
          public float matchCost() {
            return spans1.spans.positionsCost();
          }
        };
      }
    }
  }

  /**
   * Two phase current doc matches.
   *
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private boolean twoPhaseCurrentDocMatches() throws IOException {
    if (docId != spans1.spans.docID()) {
      reset();
      docId = spans1.spans.docID();
    }
    if (docId == NO_MORE_DOCS) {
      return false;
    } else {
      int spans2DocId = spans2.spans.docID();
      if (spans2DocId < docId) {
        spans2DocId = spans2.spans.advance(docId);
      }
      if (docId != spans2DocId) {
        return spans1.spans.nextStartPosition() != NO_MORE_POSITIONS;
      } else {
        return goToNextStartPosition();
      }
    }
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
      docId = spans1.spans.nextDoc();
      if (docId == NO_MORE_DOCS) {
        return true;
      } else {
        int spans2DocId = spans2.spans.docID();
        if (spans2DocId < docId) {
          spans2DocId = spans2.spans.advance(docId);
        }
        if (docId != spans2DocId) {
          return spans1.spans.nextStartPosition() != NO_MORE_POSITIONS;
        } else if (goToNextStartPosition()) {
          return true;
        } else {
          reset();
          return false;
        }
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
    int nextSpans1EndPosition;
    while ((nextSpans1StartPosition = spans1.spans
        .nextStartPosition()) != NO_MORE_POSITIONS) {
      if (spans1.spans.docID() == spans2.spans.docID()) {
        // clean up
        if (nextSpans1StartPosition > lastSpans2StartPosition) {
          lastSpans2StartPosition = -1;
        }
        // fast check
        if (lastSpans2StartPosition == -1
            && nextSpans1StartPosition < nextSpans2StartPosition) {
          return true;
        }
        nextSpans1EndPosition = spans1.spans.endPosition();
        if (nextSpans1StartPosition == lastSpans2StartPosition) {
          // try to collect all lastSpans2Endpositions, and return true if not
          // contained
          if (collectAndCheckLastSpans(nextSpans1StartPosition,
              nextSpans1EndPosition)) {
            return true;
          } else {
            // continue
          }
        } else {
          // reset, assume lastSpans2StartPosition<nextSpans1StartPosition
          lastSpans2StartPosition = -1;
          // go to correct next
          while (nextSpans2StartPosition < nextSpans1StartPosition) {
            nextSpans2StartPosition = spans2.spans.nextStartPosition();
          }
          nextSpans2EndPosition = spans2.spans.endPosition();
          if (nextSpans1StartPosition == nextSpans2StartPosition) {
            // try to collect all lastSpans2Endpositions, and return true if not
            // contained
            if (collectAndCheckLastSpans(nextSpans1StartPosition,
                nextSpans1EndPosition)) {
              return true;
            } else {
              // continue
            }
          } else {
            return true;
          }
        }
      } else {
        return true;
      }
    }
    // no more positions
    return false;
  }

  /**
   * Collect and check last spans.
   *
   * @param nextSpans1StartPosition the next spans 1 start position
   * @param nextSpans1EndPosition the next spans 1 end position
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private boolean collectAndCheckLastSpans(int nextSpans1StartPosition,
      int nextSpans1EndPosition) throws IOException {
    // check next
    if (nextSpans1StartPosition == nextSpans2StartPosition
        && nextSpans1EndPosition == nextSpans2EndPosition) {
      return false;
    }
    // check last
    if (nextSpans1StartPosition == lastSpans2StartPosition
        && (nextSpans1EndPosition == lastSpans2EndPosition
            || lastSpans2EndPositions.contains(nextSpans1EndPosition))) {
      return false;
    }
    // collect
    if (nextSpans1StartPosition == nextSpans2StartPosition) {
      // reset
      if (nextSpans2StartPosition != lastSpans2StartPosition) {
        lastSpans2StartPosition = nextSpans2StartPosition;
        lastSpans2EndPosition = -1;
        lastSpans2EndPositions.clear();
      }
      while (nextSpans1StartPosition == nextSpans2StartPosition) {
        if (lastSpans2EndPosition > -1) {
          lastSpans2EndPositions.add(lastSpans2EndPosition);
        }
        lastSpans2EndPosition = nextSpans2EndPosition;
        nextSpans2StartPosition = spans2.spans.nextStartPosition();
        nextSpans2EndPosition = spans2.spans.endPosition();
        if (nextSpans1StartPosition == nextSpans2StartPosition
            && nextSpans1EndPosition == nextSpans2EndPosition) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Reset.
   */
  private void reset() {
    calledNextStartPosition = false;
    lastSpans2StartPosition = -1;
    lastSpans2EndPosition = -1;
    lastSpans2EndPositions.clear();
    nextSpans2StartPosition = -1;
    nextSpans2EndPosition = -1;
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

}
