package mtas.search.spans.util;

import java.io.IOException;

import org.apache.lucene.queries.spans.SpanCollector;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.search.TwoPhaseIterator;

import mtas.codec.util.CodecInfo;
import mtas.codec.util.CodecInfo.IndexDoc;

/**
 * The Class MtasMaximumExpandSpans.
 */
public class MtasMaximumExpandSpans
    extends MtasSpans
{

    /** The sub spans. */
    Spans subSpans;

    /** The query. */
    MtasMaximumExpandSpanQuery query;

    /** The min position. */
    int minPosition;

    /** The max position. */
    int maxPosition;

    /** The field. */
    String field;

    /** The mtas codec info. */
    CodecInfo mtasCodecInfo;

    /** The start position. */
    int startPosition;

    /** The end position. */
    int endPosition;

    /** The called next start position. */
    private boolean calledNextStartPosition;

    /** The doc id. */
    int docId;

    /**
     * Instantiates a new mtas maximum expand spans.
     *
     * @param query
     *            the query
     * @param mtasCodecInfo
     *            the mtas codec info
     * @param field
     *            the field
     * @param subSpans
     *            the sub spans
     */
    public MtasMaximumExpandSpans(MtasMaximumExpandSpanQuery query, CodecInfo mtasCodecInfo,
            String field, Spans subSpans)
    {
        super();
        this.subSpans = subSpans;
        this.field = field;
        this.mtasCodecInfo = mtasCodecInfo;
        this.query = query;
        docId = -1;
        reset();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#nextStartPosition()
     */
    @Override
    public int nextStartPosition() throws IOException
    {
        if (docId == -1 || docId == NO_MORE_DOCS) {
            throw new IOException("no document");
        }
        else if (!calledNextStartPosition) {
            calledNextStartPosition = true;
            return startPosition;
            // compute next match
        }
        else {
            if (goToNextStartPosition()) {
                // match found
                return startPosition;
            }
            else {
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
    public int startPosition()
    {
        return startPosition;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#endPosition()
     */
    @Override
    public int endPosition()
    {
        return endPosition;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#width()
     */
    @Override
    public int width()
    {
        return endPosition - startPosition;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#collect(org.apache.lucene.search.
     * spans.SpanCollector)
     */
    @Override
    public void collect(SpanCollector collector) throws IOException
    {
        subSpans.collect(collector);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#positionsCost()
     */
    @Override
    public float positionsCost()
    {
        // return subSpans.positionsCost();
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.DocIdSetIterator#docID()
     */
    @Override
    public int docID()
    {
        return docId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.DocIdSetIterator#nextDoc()
     */
    @Override
    public int nextDoc() throws IOException
    {
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
    public int advance(int target) throws IOException
    {
        reset();
        if (docId == NO_MORE_DOCS) {
            return docId;
        }
        else if (target <= docId) {
            // should not happen
            docId = NO_MORE_DOCS;
            return docId;
        }
        else {
            docId = subSpans.advance(target);
            if (docId == NO_MORE_DOCS) {
                return docId;
            }
            else {
                IndexDoc doc = mtasCodecInfo.getDoc(field, docId);
                if (doc != null) {
                    minPosition = doc.minPosition;
                    maxPosition = doc.maxPosition;
                }
                else {
                    minPosition = NO_MORE_POSITIONS;
                    maxPosition = NO_MORE_POSITIONS;
                }
                if (goToNextStartPosition()) {
                    return docId;
                }
                else {
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
    public TwoPhaseIterator asTwoPhaseIterator()
    {
        if (!query.twoPhaseIteratorAllowed()) {
            return null;
        }
        else {
            TwoPhaseIterator originalTwoPhaseIterator = subSpans.asTwoPhaseIterator();
            if (originalTwoPhaseIterator != null) {
                return new TwoPhaseIterator(originalTwoPhaseIterator.approximation())
                {
                    @Override
                    public boolean matches() throws IOException
                    {
                        return originalTwoPhaseIterator.matches() && twoPhaseCurrentDocMatches();
                    }

                    @Override
                    public float matchCost()
                    {
                        return originalTwoPhaseIterator.matchCost();
                    }
                };
            }
            else {
                return new TwoPhaseIterator(subSpans)
                {

                    @Override
                    public boolean matches() throws IOException
                    {
                        return twoPhaseCurrentDocMatches();
                    }

                    @Override
                    public float matchCost()
                    {
                        return subSpans.positionsCost();
                    }
                };
            }
        }
    }

    /**
     * Two phase current doc matches.
     *
     * @return true, if successful
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private boolean twoPhaseCurrentDocMatches() throws IOException
    {
        if (docId != subSpans.docID()) {
            reset();
            docId = subSpans.docID();
            IndexDoc doc = mtasCodecInfo.getDoc(field, docId);
            if (doc != null) {
                minPosition = doc.minPosition;
                maxPosition = doc.maxPosition;
            }
            else {
                minPosition = NO_MORE_POSITIONS;
                maxPosition = NO_MORE_POSITIONS;
            }
        }
        if (docId == NO_MORE_DOCS) {
            return false;
        }
        else {
            return goToNextStartPosition();
        }
    }

    /**
     * Go to next doc.
     *
     * @return true, if successful
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private boolean goToNextDoc() throws IOException
    {
        reset();
        if (docId == NO_MORE_DOCS) {
            minPosition = NO_MORE_POSITIONS;
            maxPosition = NO_MORE_POSITIONS;
            return true;
        }
        else {
            docId = subSpans.nextDoc();
            if (docId == NO_MORE_DOCS) {
                minPosition = NO_MORE_POSITIONS;
                maxPosition = NO_MORE_POSITIONS;
                return true;
            }
            else {
                IndexDoc doc = mtasCodecInfo.getDoc(field, docId);
                if (doc != null) {
                    minPosition = doc.minPosition;
                    maxPosition = doc.maxPosition;
                }
                else {
                    minPosition = NO_MORE_POSITIONS;
                    maxPosition = NO_MORE_POSITIONS;
                }
                if (goToNextStartPosition()) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
    }

    /**
     * Go to next start position.
     *
     * @return true, if successful
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private boolean goToNextStartPosition() throws IOException
    {
        int basicStartPosition;
        int basicEndPosition;
        if (docId == -1 || docId == NO_MORE_DOCS) {
            throw new IOException("no document");
        }
        else {
            while ((basicStartPosition = subSpans.nextStartPosition()) != NO_MORE_POSITIONS) {
                basicEndPosition = subSpans.endPosition();
                startPosition = Math.max(minPosition, (basicStartPosition - query.maximumLeft));
                endPosition = Math.min(maxPosition + 1, (basicEndPosition + query.maximumRight));
                if (startPosition <= (basicStartPosition - query.minimumLeft)
                        && endPosition >= (basicEndPosition + query.minimumRight)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Reset.
     */
    private void reset()
    {
        calledNextStartPosition = false;
        minPosition = 0;
        maxPosition = 0;
        startPosition = -1;
        endPosition = -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.DocIdSetIterator#cost()
     */
    @Override
    public long cost()
    {
        return subSpans != null ? subSpans.cost() : 0;
    }
}
