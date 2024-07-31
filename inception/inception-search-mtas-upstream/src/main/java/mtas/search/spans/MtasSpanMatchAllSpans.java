package mtas.search.spans;

import java.io.IOException;

import mtas.codec.util.CodecInfo;
import mtas.codec.util.CodecInfo.IndexDoc;
import mtas.search.spans.util.MtasSpans;

import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.queries.spans.SpanCollector;

/**
 * The Class MtasSpanMatchAllSpans.
 */
public class MtasSpanMatchAllSpans extends MtasSpans {

  /** The query. */
  private MtasSpanMatchAllQuery query;

  /** The field. */
  private String field;

  /** The min position. */
  private int minPosition;

  /** The max position. */
  private int maxPosition;

  /** The current start position. */
  private int currentStartPosition;

  /** The current end position. */
  private int currentEndPosition;

  /** The doc id. */
  private int docId;

  /** The mtas codec info. */
  private CodecInfo mtasCodecInfo;

  /**
   * Instantiates a new mtas span match all spans.
   *
   * @param query the query
   * @param mtasCodecInfo the mtas codec info
   * @param field the field
   */
  public MtasSpanMatchAllSpans(MtasSpanMatchAllQuery query,
      CodecInfo mtasCodecInfo, String field) {
    super();
    this.query = query;
    this.mtasCodecInfo = mtasCodecInfo;
    this.field = field;
    minPosition = NO_MORE_POSITIONS;
    maxPosition = NO_MORE_POSITIONS;
    currentStartPosition = NO_MORE_POSITIONS;
    currentEndPosition = NO_MORE_POSITIONS;
    docId = -1;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#nextStartPosition()
   */
  @Override
  public int nextStartPosition() throws IOException {
    if (currentStartPosition < minPosition) {
      currentStartPosition = minPosition;
      currentEndPosition = currentStartPosition + 1;
    } else {
      currentStartPosition++;
      currentEndPosition = currentStartPosition + 1;
      if (currentStartPosition > maxPosition) {
        currentStartPosition = NO_MORE_POSITIONS;
        currentEndPosition = NO_MORE_POSITIONS;
      }
    }
    return currentStartPosition;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#startPosition()
   */
  @Override
  public int startPosition() {
    return currentStartPosition;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#endPosition()
   */
  @Override
  public int endPosition() {
    return currentEndPosition;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#width()
   */
  @Override
  public int width() {
    return 0;
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
    // do nothing
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
    IndexDoc indexDoc = mtasCodecInfo.getNextDoc(field, docId);
    if (indexDoc != null) {
      docId = indexDoc.docId;
      minPosition = indexDoc.minPosition;
      maxPosition = indexDoc.maxPosition;
      currentStartPosition = -1;
      currentEndPosition = -1;
    } else {
      docId = NO_MORE_DOCS;
      minPosition = NO_MORE_POSITIONS;
      maxPosition = NO_MORE_POSITIONS;
      currentStartPosition = NO_MORE_POSITIONS;
      currentEndPosition = NO_MORE_POSITIONS;
    }
    return docId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.DocIdSetIterator#advance(int)
   */
  @Override
  public int advance(int target) throws IOException {
    IndexDoc indexDoc = mtasCodecInfo.getNextDoc(field, (target - 1));
    if (indexDoc != null) {
      docId = indexDoc.docId;
      minPosition = indexDoc.minPosition;
      maxPosition = indexDoc.maxPosition;
      currentStartPosition = -1;
      currentEndPosition = -1;
    } else {
      docId = NO_MORE_DOCS;
      minPosition = NO_MORE_POSITIONS;
      maxPosition = NO_MORE_POSITIONS;
      currentStartPosition = NO_MORE_POSITIONS;
      currentEndPosition = NO_MORE_POSITIONS;
    }
    return docId;
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
   * @see org.apache.lucene.search.spans.Spans#positionsCost()
   */
  @Override
  public float positionsCost() {
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.Spans#asTwoPhaseIterator()
   */
  @Override
  public TwoPhaseIterator asTwoPhaseIterator() {
    if (!query.twoPhaseIteratorAllowed()) {
      return null;
    } else {
      // TODO
      return null;
    }
  }

}
