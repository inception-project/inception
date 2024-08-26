package mtas.search.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.lucene.queries.spans.SpanCollector;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.search.TwoPhaseIterator;

import mtas.search.spans.MtasSpanSequenceQuery.MtasSpanSequenceQuerySpans;
import mtas.search.spans.util.MtasIgnoreItem;
import mtas.search.spans.util.MtasSpans;

/**
 * The Class MtasSpanSequenceSpans.
 */
public class MtasSpanSequenceSpans
    extends MtasSpans
{

    /** The query. */
    private MtasSpanSequenceQuery query;

    /** The queue spans. */
    private List<QueueItem> queueSpans;

    /** The ignore item. */
    private MtasIgnoreItem ignoreItem;

    /** The queue matches. */
    private List<Match> queueMatches;

    /** The doc id. */
    private int docId;

    /** The current position. */
    private int currentPosition;

    /** The cost. */
    private long cost;

    /** The current match. */
    Match currentMatch;

    /**
     * Instantiates a new mtas span sequence spans.
     *
     * @param query
     *            the query
     * @param setSequenceSpans
     *            the set sequence spans
     * @param ignoreSpans
     *            the ignore spans
     * @param maximumIgnoreLength
     *            the maximum ignore length
     */
    public MtasSpanSequenceSpans(MtasSpanSequenceQuery query,
            List<MtasSpanSequenceQuerySpans> setSequenceSpans, Spans ignoreSpans,
            Integer maximumIgnoreLength)
    {
        super();
        docId = -1;
        this.query = query;
        queueSpans = new ArrayList<>();
        queueMatches = new ArrayList<>();
        for (MtasSpanSequenceQuerySpans sequenceSpans : setSequenceSpans) {
            queueSpans.add(new QueueItem(sequenceSpans));
        }
        ignoreItem = new MtasIgnoreItem(ignoreSpans, maximumIgnoreLength);
        resetQueue();
        computeCosts();
    }

    /**
     * Compute costs.
     */
    private void computeCosts()
    {
        cost = Long.MAX_VALUE;
        for (QueueItem item : queueSpans) {
            cost = Math.min(cost, item.sequenceSpans.spans.cost());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#nextStartPosition()
     */
    @Override
    public int nextStartPosition() throws IOException
    {
        if (findMatches()) {
            currentMatch = queueMatches.get(0);
            currentPosition = currentMatch.startPosition();
            queueMatches.remove(0);
            return currentMatch.startPosition();
        }
        else {
            currentMatch = new Match(NO_MORE_POSITIONS, NO_MORE_POSITIONS);
            currentPosition = NO_MORE_POSITIONS;
            return NO_MORE_POSITIONS;
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
        if (currentMatch == null) {
            return -1;
        }
        else {
            return currentMatch.startPosition();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#endPosition()
     */
    @Override
    public int endPosition()
    {
        if (currentMatch == null) {
            return -1;
        }
        else {
            return currentMatch.endPosition();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#width()
     */
    @Override
    public int width()
    {
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#collect(org.apache.lucene.search.spans
     * .SpanCollector)
     */
    @Override
    public void collect(SpanCollector collector) throws IOException
    {
        for (QueueItem item : queueSpans) {
            item.sequenceSpans.spans.collect(collector);
        }
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
        resetQueue();
        while (!goToNextDoc())
            ;
        return docId;
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
        if (docId == NO_MORE_DOCS) {
            return true;
        }
        else {
            // try to find docId with match for all items from sequence
            Integer spanDocId;
            Integer newDocId = null;
            Integer minOptionalDocId = null;
            boolean allItemsOptional = true;
            for (QueueItem item : queueSpans) {
                if (!item.sequenceSpans.optional) {
                    allItemsOptional = false;
                }
                if (!item.noMoreDocs) {
                    if (item.sequenceSpans.spans == null) {
                        spanDocId = NO_MORE_DOCS;
                    }
                    else if (newDocId == null) {
                        spanDocId = item.sequenceSpans.spans.nextDoc();
                    }
                    else {
                        if (!item.sequenceSpans.optional) {
                            spanDocId = item.sequenceSpans.spans.advance(newDocId);
                        }
                        else {
                            if (item.sequenceSpans.spans.docID() == -1
                                    || newDocId > item.sequenceSpans.spans.docID()) {
                                spanDocId = item.sequenceSpans.spans.advance(newDocId);
                            }
                            else {
                                spanDocId = item.sequenceSpans.spans.docID();
                            }
                        }
                    }
                    if (spanDocId.equals(NO_MORE_DOCS)) {
                        item.noMoreDocs = true;
                        if (!item.sequenceSpans.optional) {
                            // a not optional span has NO_MORE_DOCS: stop
                            docId = NO_MORE_DOCS;
                            return true;
                        }
                    }
                    else if (!spanDocId.equals(newDocId)) {
                        // last found spanDocId not equal to potential new docId
                        if (newDocId != null) {
                            if (!item.sequenceSpans.optional) {
                                // move also previous spans to at least spanDocId
                                advance(spanDocId);
                                return true;
                            }
                            // define potential new docId
                        }
                        else {
                            if (!item.sequenceSpans.optional) {
                                // previous optional span with lower docId
                                if ((minOptionalDocId != null) && (minOptionalDocId < spanDocId)) {
                                    advance(spanDocId);
                                    return true;
                                }
                                else {
                                    // use spanDocId as potential newDocId
                                    newDocId = spanDocId;
                                }
                            }
                            else {
                                // remember minimum docId optional spans
                                minOptionalDocId = (minOptionalDocId == null) ? spanDocId
                                        : Math.min(minOptionalDocId, spanDocId);
                            }
                        }
                    }
                }
            }
            // if all items are optional
            if (allItemsOptional && newDocId == null && minOptionalDocId != null) {
                newDocId = minOptionalDocId;
            }
            // nothing found
            if (newDocId == null) {
                docId = NO_MORE_DOCS;
                return true;
            }
            else {
                docId = newDocId;
                ignoreItem.advanceToDoc(docId);
                // try and glue together
                if (findMatches()) {
                    return true;
                    // no matches
                }
                else {
                    resetQueue();
                    return false;
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.DocIdSetIterator#advance(int)
     */
    @Override
    public int advance(int target) throws IOException
    {
        resetQueue();
        Integer newTarget = target;
        do {
            newTarget = advanceToDoc(newTarget);
        }
        while (newTarget != null);
        return docId;
    }

    /**
     * Advance to doc.
     *
     * @param target
     *            the target
     * @return the integer
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private Integer advanceToDoc(int target) throws IOException
    {
        if (docId == NO_MORE_DOCS || target <= docId) {
            return null;
        }
        else {
            Integer spanDocId;
            Integer newDocId = target;
            for (QueueItem item : queueSpans) {
                if (item.sequenceSpans.spans != null) {
                    if (item.sequenceSpans.spans.docID() < newDocId) {
                        spanDocId = item.sequenceSpans.spans.advance(newDocId);
                        if (spanDocId.equals(NO_MORE_DOCS)) {
                            item.noMoreDocs = true;
                            if (!item.sequenceSpans.optional) {
                                // a not optional span has NO_MORE_DOCS: stop
                                docId = NO_MORE_DOCS;
                                return null;
                            }
                        }
                        else {
                            if (!spanDocId.equals(newDocId) && !item.sequenceSpans.optional) {
                                // a not optional span has nothing for newDocId: stop
                                return spanDocId;
                            }
                        }
                    }
                    else if (item.sequenceSpans.spans.docID() != newDocId) {
                        spanDocId = item.sequenceSpans.spans.docID();
                        if (!item.sequenceSpans.optional) {
                            // a not optional span seems to have nothing for newDocId: stop
                            return spanDocId;
                        }
                    }
                }
            }
            // find match
            docId = newDocId;
            ignoreItem.advanceToDoc(docId);
            // try and glue together
            if (findMatches()) {
                return null;
                // no matches
            }
            else {
                resetQueue();
                // try next document
                return (newDocId + 1);
            }
        }
    }

    /**
     * Find matches.
     *
     * @return true, if successful
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private boolean findMatches() throws IOException
    {
        Boolean status = _findMatches();
        while (!(status || (currentPosition == NO_MORE_POSITIONS))) {
            status = _findMatches();
        }
        return status;
    }

    /**
     * Find matches.
     *
     * @return true, if successful
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private boolean _findMatches() throws IOException
    {
        // queue not empty
        if (!queueMatches.isEmpty()) {
            return true;
            // no more matches to be found
        }
        else if (currentPosition == NO_MORE_POSITIONS) {
            return false;
            // try to find matches
        }
        else {
            // subMatches: try to build matches while collecting
            Integer subMatchesStartPosition = null;
            Boolean subMatchesOptional = true;
            List<Match> subMatchesQueue = new ArrayList<>();
            // minimum startPosition previous, used to set lower boundary on
            // startPosition next
            Integer minStartPositionPrevious = null;
            // maximum endPosition previous, used to set upper boundary on
            // startPosition next
            Integer maxEndPositionPrevious = null;
            // other variables
            Integer minStartPositionNext = null;
            Integer minStartPosition = null;
            Integer minOptionalStartPosition = null;
            // adjusted minimum ignoreItem
            boolean adjustedMinimumIgnoreItem = false;
            // fill queue if necessary and possible
            for (int i = 0; i < queueSpans.size(); i++) {
                QueueItem item = queueSpans.get(i);
                // if span is optional, check docId
                if (!item.sequenceSpans.optional || (item.sequenceSpans.spans != null
                        && item.sequenceSpans.spans.docID() == docId)) {
                    // compute minimum startPosition until next non-optional item
                    // used as lower boundary on endPosition next
                    minStartPositionNext = null;
                    for (int j = (i + 1); j < queueSpans.size(); j++) {

                        // check for available lowestPosition
                        if (!queueSpans.get(j).sequenceSpans.optional
                                && queueSpans.get(j).lowestPosition != null) {
                            minStartPositionNext = (minStartPositionNext == null)
                                    ? queueSpans.get(j).lowestPosition
                                    : Math.min(minStartPositionNext,
                                            queueSpans.get(j).lowestPosition);
                            // computing restrictions not possible
                        }
                        else {
                            if (!queueSpans.get(j).sequenceSpans.optional) {
                                minStartPositionNext = null;
                            }
                            break;
                        }
                    }
                    // fill queue
                    if ((minStartPositionPrevious == null) || subMatchesOptional) {
                        fillQueue(item, null, maxEndPositionPrevious, minStartPositionNext);
                    }
                    else {
                        fillQueue(item, minStartPositionPrevious, maxEndPositionPrevious,
                                minStartPositionNext);
                    }
                    // try to adjust minimum ignoreItem
                    if (!adjustedMinimumIgnoreItem && !item.sequenceSpans.optional
                            && item.filledPosition) {
                        if (minOptionalStartPosition != null) {
                            ignoreItem.removeBefore(docId,
                                    Math.min(minOptionalStartPosition, item.lowestPosition));
                        }
                        else {
                            ignoreItem.removeBefore(docId, item.lowestPosition);
                        }
                        adjustedMinimumIgnoreItem = true;
                    }
                    // check for available positions
                    if (!item.sequenceSpans.optional && item.noMorePositions
                            && !item.filledPosition) {
                        currentPosition = NO_MORE_POSITIONS;
                        return false;
                    }
                    // build matches
                    subMatchesQueue = _glue(subMatchesQueue, subMatchesOptional, item);
                    // update subMatchesOptional
                    if (!item.sequenceSpans.optional) {
                        subMatchesOptional = false;
                    }
                    // check if matches are still achievable
                    if (!subMatchesOptional && subMatchesQueue.isEmpty()) {
                        // clean up previous queues
                        if (subMatchesStartPosition != null) {
                            int cleanStartPosition = subMatchesStartPosition;
                            for (int j = 0; j <= i; j++) {
                                queueSpans.get(j).del(cleanStartPosition);
                                if (!queueSpans.get(j).sequenceSpans.optional) {
                                    cleanStartPosition++;
                                }
                            }
                        }
                        return false;
                    }
                    // update subMatchesStartPosition
                    if (subMatchesQueue.isEmpty()) {
                        subMatchesStartPosition = null;
                    }
                    else {
                        subMatchesStartPosition = subMatchesQueue.get(0).startPosition;
                    }
                    // compute minimum startPosition for next span
                    if (item.lowestPosition != null) {
                        minStartPositionPrevious = (minStartPositionPrevious == null)
                                ? item.lowestPosition
                                : Math.min(minStartPositionPrevious, item.lowestPosition);
                    }
                    // for optional spans
                    if (item.sequenceSpans.optional) {
                        // update stats
                        if (item.lowestPosition != null) {
                            minOptionalStartPosition = (minOptionalStartPosition == null)
                                    ? item.lowestPosition
                                    : Math.min(minOptionalStartPosition, item.lowestPosition);
                        }
                        // for not optional spans
                    }
                    else {
                        // update stats, item.lowestPosition should be set
                        minStartPosition = (minStartPosition == null) ? item.lowestPosition
                                : Math.min(minStartPosition, item.lowestPosition);
                        // reset maximum endPosition for next span
                        maxEndPositionPrevious = null;
                    }
                    // compute maximum endPosition for next span
                    if (item.lowestPosition != null) {
                        for (Integer endPosition : item.queue.get(item.lowestPosition)) {
                            maxEndPositionPrevious = (maxEndPositionPrevious == null) ? endPosition
                                    : Math.max(maxEndPositionPrevious, endPosition);
                        }
                    }
                }
            }
            if (subMatchesQueue.isEmpty()) {
                // condition has only optional parts
                if (subMatchesOptional) {
                    // check for
                    boolean allFinished = true;
                    for (int i = 0; i < queueSpans.size(); i++) {
                        if (!queueSpans.get(i).noMorePositions) {
                            allFinished = false;
                            break;
                        }
                    }
                    if (allFinished) {
                        currentPosition = NO_MORE_POSITIONS;
                    }
                }
                return false;
            }
            else if ((minOptionalStartPosition != null)
                    && (minOptionalStartPosition < subMatchesStartPosition)) {
                for (int i = 0; i < queueSpans.size(); i++) {
                    if (!queueSpans.get(i).sequenceSpans.optional) {
                        break;
                    }
                    else {
                        queueSpans.get(i).del(minOptionalStartPosition);
                    }
                }
                return false;
            }
            else {
                for (int i = 0; i < queueSpans.size(); i++) {
                    queueSpans.get(i).del(subMatchesStartPosition);
                }
                for (Match m : subMatchesQueue) {
                    if (!queueMatches.contains(m)) {
                        queueMatches.add(m);
                    }
                }
                ignoreItem.removeBefore(docId, queueMatches.get(0).startPosition);
                return true;
            }
        }
    }

    /**
     * Glue.
     *
     * @param subMatchesQueue
     *            the sub matches queue
     * @param subMatchesOptional
     *            the sub matches optional
     * @param item
     *            the item
     * @return the list
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private List<Match> _glue(List<Match> subMatchesQueue, Boolean subMatchesOptional,
            QueueItem item)
        throws IOException
    {
        List<Match> newSubMatchesQueue = new ArrayList<>();
        // no previous queue, only use current item
        if (subMatchesQueue.isEmpty()) {
            if (item.filledPosition) {
                for (Integer endPosition : item.queue.get(item.lowestPosition)) {
                    Match m = new Match(item.lowestPosition, endPosition);
                    if (!newSubMatchesQueue.contains(m)) {
                        newSubMatchesQueue.add(m);
                    }
                }
            }
            return newSubMatchesQueue;
            // previous queue
        }
        else {
            // startposition from queue
            int startPosition = subMatchesQueue.get(0).startPosition;
            // previous queue optional, current item optional
            if (subMatchesOptional && item.sequenceSpans.optional) {
                // forget previous, because current has lower startposition
                if (item.filledPosition && item.lowestPosition < startPosition) {
                    for (Integer endPosition : item.queue.get(item.lowestPosition)) {
                        Match m = new Match(item.lowestPosition, endPosition);
                        if (!newSubMatchesQueue.contains(m)) {
                            newSubMatchesQueue.add(m);
                        }
                    }
                    // merge with previous
                }
                else if (item.filledPosition) {
                    if (item.lowestPosition.equals(startPosition)) {
                        for (Integer endPosition : item.queue.get(item.lowestPosition)) {
                            Match m = new Match(item.lowestPosition, endPosition);
                            if (!newSubMatchesQueue.contains(m)) {
                                newSubMatchesQueue.add(m);
                            }
                        }
                    }
                    newSubMatchesQueue.addAll(subMatchesQueue);
                    for (Match m : subMatchesQueue) {
                        if (item.queue.containsKey(m.endPosition)) {
                            for (Integer endPosition : item.queue.get(m.endPosition)) {
                                Match o = new Match(m.startPosition, endPosition);
                                if (!newSubMatchesQueue.contains(o)) {
                                    newSubMatchesQueue.add(o);
                                }
                            }
                        }
                    }
                    // no filled position
                }
                else {
                    newSubMatchesQueue.addAll(subMatchesQueue);
                }
                // previous queue optional, current item not optional
            }
            else if (subMatchesOptional && !item.sequenceSpans.optional) {
                assert item.filledPosition : "span not optional, should contain items";
                // forget previous
                if (item.lowestPosition < startPosition) {
                    for (Integer endPosition : item.queue.get(item.lowestPosition)) {
                        Match m = new Match(item.lowestPosition, endPosition);
                        if (!newSubMatchesQueue.contains(m)) {
                            newSubMatchesQueue.add(m);
                        }
                    }
                    // merge with previous
                }
                else {
                    if (item.lowestPosition.equals(startPosition)) {
                        for (Integer endPosition : item.queue.get(item.lowestPosition)) {
                            Match m = new Match(item.lowestPosition, endPosition);
                            if (!newSubMatchesQueue.contains(m)) {
                                newSubMatchesQueue.add(m);
                            }
                        }
                    }
                    for (Match m : subMatchesQueue) {
                        if (item.queue.containsKey(m.endPosition)) {
                            for (Integer endPosition : item.queue.get(m.endPosition)) {
                                Match o = new Match(m.startPosition, endPosition);
                                if (!newSubMatchesQueue.contains(o)) {
                                    newSubMatchesQueue.add(o);
                                }
                            }
                        }
                    }
                }
                // previous queue not optional, current item optional
            }
            else if (!subMatchesOptional && item.sequenceSpans.optional) {
                newSubMatchesQueue.addAll(subMatchesQueue);
                // merge with previous
                if (item.filledPosition) {
                    for (Match m : subMatchesQueue) {
                        if (item.queue.containsKey(m.endPosition)) {
                            for (Integer endPosition : item.queue.get(m.endPosition)) {
                                Match o = new Match(m.startPosition, endPosition);
                                if (!newSubMatchesQueue.contains(o)) {
                                    newSubMatchesQueue.add(o);
                                }
                            }
                        }
                    }
                }
                // previous queue not optional, current item not optional
            }
            else if (!subMatchesOptional && !item.sequenceSpans.optional && item.filledPosition) {
                for (Match m : subMatchesQueue) {
                    Set<Integer> ignoreList = ignoreItem.getFullEndPositionList(docId,
                            m.endPosition);
                    Integer[] checkList;
                    if (ignoreList == null) {
                        checkList = new Integer[] { m.endPosition };
                    }
                    else {
                        checkList = new Integer[1 + ignoreList.size()];
                        checkList = ignoreList.toArray(checkList);
                        checkList[ignoreList.size()] = m.endPosition;
                    }
                    for (Integer checkEndPosition : checkList) {
                        if (item.queue.containsKey(checkEndPosition)) {
                            for (Integer endPosition : item.queue.get(checkEndPosition)) {
                                Match o = new Match(m.startPosition, endPosition);
                                if (!newSubMatchesQueue.contains(o)) {
                                    newSubMatchesQueue.add(o);
                                }
                            }
                        }
                    }
                }
            }
        }
        return newSubMatchesQueue;
    }

    /**
     * Fill queue.
     *
     * @param item
     *            the item
     * @param minStartPosition
     *            the min start position
     * @param maxStartPosition
     *            the max start position
     * @param minEndPosition
     *            the min end position
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void fillQueue(QueueItem item, Integer minStartPosition, Integer maxStartPosition,
            Integer minEndPosition)
        throws IOException
    {
        int newStartPosition;
        int newEndPosition;
        Integer firstRetrievedPosition = null;
        // remove everything below minStartPosition
        if ((minStartPosition != null) && (item.lowestPosition != null)
                && (item.lowestPosition < minStartPosition)) {
            item.del((minStartPosition - 1));
        }
        // fill queue
        while (!item.noMorePositions) {
            boolean doNotCollectAnotherPosition;
            doNotCollectAnotherPosition = item.filledPosition && (minStartPosition == null)
                    && (maxStartPosition == null);
            doNotCollectAnotherPosition |= item.filledPosition && (maxStartPosition != null)
                    && (item.lastRetrievedPosition != null)
                    && (maxStartPosition < item.lastRetrievedPosition);
            if (doNotCollectAnotherPosition) {
                return;
            }
            else {
                // collect another full position
                firstRetrievedPosition = null;
                while (!item.noMorePositions) {
                    newStartPosition = item.sequenceSpans.spans.nextStartPosition();
                    if (newStartPosition == NO_MORE_POSITIONS) {
                        if (!item.queue.isEmpty()) {
                            item.filledPosition = true;
                            item.lastFilledPosition = item.lastRetrievedPosition;
                        }
                        item.noMorePositions = true;
                        return;
                    }
                    else if ((minStartPosition != null) && (newStartPosition < minStartPosition)) {
                        // do nothing
                    }
                    else {
                        newEndPosition = item.sequenceSpans.spans.endPosition();
                        if ((minEndPosition == null) || (newEndPosition >= minEndPosition
                                - ignoreItem.getMinStartPosition(docId, newEndPosition))) {
                            item.add(newStartPosition, newEndPosition);
                            if (firstRetrievedPosition == null) {
                                firstRetrievedPosition = newStartPosition;
                            }
                            else if (!firstRetrievedPosition.equals(newStartPosition)) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Reset queue.
     */
    void resetQueue()
    {
        currentPosition = -1;
        queueMatches.clear();
        for (QueueItem item : queueSpans) {
            item.reset();
        }
        currentMatch = null;
    }

    /**
     * The Class QueueItem.
     */
    private static class QueueItem
    {

        /** The no more docs. */
        private boolean noMoreDocs;

        /** The no more positions. */
        private boolean noMorePositions;

        /** The filled position. */
        private boolean filledPosition;

        /** The lowest position. */
        private Integer lowestPosition;

        /** The last filled position. */
        private Integer lastFilledPosition;

        /** The last retrieved position. */
        private Integer lastRetrievedPosition;

        /** The queue. */
        private HashMap<Integer, List<Integer>> queue;

        /** The sequence spans. */
        public MtasSpanSequenceQuerySpans sequenceSpans;

        /**
         * Instantiates a new queue item.
         *
         * @param sequenceSpans
         *            the sequence spans
         */
        QueueItem(MtasSpanSequenceQuerySpans sequenceSpans)
        {
            noMoreDocs = false;
            this.sequenceSpans = sequenceSpans;
            queue = new HashMap<>();
            reset();
        }

        /**
         * Reset.
         */
        public void reset()
        {
            noMorePositions = false;
            lowestPosition = null;
            lastFilledPosition = null;
            lastRetrievedPosition = null;
            filledPosition = false;
            queue.clear();
        }

        /**
         * Adds the.
         *
         * @param startPosition
         *            the start position
         * @param endPosition
         *            the end position
         */
        public void add(int startPosition, int endPosition)
        {
            if (!queue.keySet().contains(startPosition)) {
                if (!queue.isEmpty()) {
                    filledPosition = true;
                    lastFilledPosition = lastRetrievedPosition;
                }
                queue.put(startPosition, new ArrayList<Integer>());
            }
            queue.get(startPosition).add(endPosition);
            if ((lowestPosition == null) || (lowestPosition > startPosition)) {
                lowestPosition = startPosition;
            }
            lastRetrievedPosition = startPosition;
        }

        /**
         * Del.
         *
         * @param position
         *            the position
         */
        public void del(int position)
        {
            ArrayList<Integer> removePositions = new ArrayList<>();
            for (int p : queue.keySet()) {
                if (p <= position) {
                    removePositions.add(p);
                }
            }
            if (!removePositions.isEmpty()) {
                // positions.removeAll(removePositions);
                for (int p : removePositions) {
                    queue.remove(p);
                }
                if (queue.isEmpty()) {
                    lowestPosition = null;
                    lastFilledPosition = null;
                    filledPosition = false;
                }
                else {
                    lowestPosition = Collections.min(queue.keySet());
                    if (filledPosition && !queue.keySet().contains(lastFilledPosition)) {
                        lastFilledPosition = null;
                        filledPosition = false;
                    }
                }
            }
        }
    }

    /**
     * The Class Match.
     */
    private static class Match
    {

        /** The start position. */
        private int startPosition;

        /** The end position. */
        private int endPosition;

        /**
         * Instantiates a new match.
         *
         * @param startPosition
         *            the start position
         * @param endPosition
         *            the end position
         */
        Match(int startPosition, int endPosition)
        {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
        }

        /**
         * Start position.
         *
         * @return the int
         */
        public int startPosition()
        {
            return startPosition;
        }

        /**
         * End position.
         *
         * @return the int
         */
        public int endPosition()
        {
            return endPosition;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final Match that = (Match) obj;
            return startPosition == that.startPosition && endPosition == that.endPosition;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode()
        {
            int h = this.getClass().getSimpleName().hashCode();
            h = (h * 5) ^ startPosition;
            h = (h * 7) ^ endPosition;
            return h;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return "[" + startPosition + "," + endPosition + "]";
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.DocIdSetIterator#cost()
     */
    @Override
    public long cost()
    {
        return cost;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.spans.Spans#positionsCost()
     */
    @Override
    public float positionsCost()
    {
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.search.spans.util.MtasSpans#asTwoPhaseIterator()
     */
    @Override
    public TwoPhaseIterator asTwoPhaseIterator()
    {
        if (queueSpans == null || !query.twoPhaseIteratorAllowed()) {
            return null;
        }
        else {
            // TODO
            return null;
        }
    }

}
