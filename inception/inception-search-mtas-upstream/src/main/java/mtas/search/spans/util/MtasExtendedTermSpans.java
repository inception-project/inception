package mtas.search.spans.util;

import java.io.IOException;
import java.util.Collection;

import mtas.analysis.token.MtasPosition;
import mtas.codec.payload.MtasPayloadDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.payloads.PayloadSpanCollector;
import org.apache.lucene.queries.spans.TermSpans;

/**
 * The Class MtasExtendedTermSpans.
 */
public class MtasExtendedTermSpans extends TermSpans {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(MtasExtendedTermSpans.class);

  /** The mtas position. */
  protected MtasPosition mtasPosition = null;

  /** The assume single position. */
  private boolean assumeSinglePosition;

  /** The payload span collector. */
  private PayloadSpanCollector payloadSpanCollector;

  /**
   * Instantiates a new mtas extended term spans.
   *
   * @param postings the postings
   * @param term the term
   */
  public MtasExtendedTermSpans(PostingsEnum postings, Term term) {
    this(postings, term, false);
  }

  /**
   * Instantiates a new mtas extended term spans.
   *
   * @param postings the postings
   * @param term the term
   * @param assumeSinglePosition the assume single position
   */
  public MtasExtendedTermSpans(PostingsEnum postings, Term term,
      boolean assumeSinglePosition) {
    super(null, postings, term, 1);
    payloadSpanCollector = new PayloadSpanCollector();
    this.assumeSinglePosition = assumeSinglePosition;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.TermSpans#endPosition()
   */
  @Override
  public int endPosition() {
    if (assumeSinglePosition) {
      return super.endPosition();
    } else {
      int status = super.endPosition();
      if (status >= 0) {
        try {
          processEncodedPayload();
          return (status != NO_MORE_POSITIONS) ? (mtasPosition.getEnd() + 1)
              : NO_MORE_POSITIONS;
        } catch (IOException e) {
          log.debug("Error", e);
          return NO_MORE_POSITIONS;
        }
      } else {
        return status;
      }
    }
  }

  /**
   * Gets the positions.
   *
   * @return the positions
   */
  public int[] getPositions() {
    int[] list;
    if (assumeSinglePosition) {
      list = new int[1];
      list[0] = super.startPosition();
      return list;
    } else {
      try {
        processEncodedPayload();
        list = mtasPosition.getPositions();
        if (list != null) {
          return mtasPosition.getPositions();
        }
      } catch (IOException e) {
        log.debug("Error", e);
        // do nothing
      }
      int start = super.startPosition();
      int end = super.endPosition();
      list = new int[end - start];
      for (int i = start; i < end; i++) {
        list[i - start] = i;
    }
      return list;
    }
  }

  /**
   * Process encoded payload.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void processEncodedPayload() throws IOException {
    if (!readPayload) {
      payloadSpanCollector.reset();
      collect(payloadSpanCollector);
      Collection<byte[]> originalPayloadCollection = payloadSpanCollector
          .getPayloads();
      if (originalPayloadCollection.iterator().hasNext()) {
        byte[] payload = originalPayloadCollection.iterator().next();
        if (payload == null) {
          throw new IOException("no payload");
        }
        MtasPayloadDecoder payloadDecoder = new MtasPayloadDecoder();
        payloadDecoder.init(startPosition(), payload);
        mtasPosition = payloadDecoder.getMtasPosition();
      } else {
        throw new IOException("no payload");
      }
    }
  }

}
