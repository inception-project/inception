package mtas.codec.util;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Class Status.
 */
public class Status
{

    /** The Constant TYPE_SEGMENT. */
    public static final String TYPE_SEGMENT = "segment";

    /** The Constant KEY_NUMBER. */
    public static final String KEY_NUMBER = "number";

    /** The Constant KEY_NUMBEROFDOCUMENTS. */
    public static final String KEY_NUMBEROFDOCUMENTS = "numberOfDocuments";

    /** The number segments finished. */
    public volatile Integer numberSegmentsFinished = null;

    /** The number segments total. */
    public volatile Integer numberSegmentsTotal = null;

    /** The sub number segments total. */
    public volatile Integer subNumberSegmentsTotal = null;

    /** The sub number segments finished total. */
    public volatile Integer subNumberSegmentsFinishedTotal = null;

    /** The sub number segments finished. */
    public volatile Map<String, Integer> subNumberSegmentsFinished = new ConcurrentHashMap<>();

    /** The number documents found. */
    public volatile Long numberDocumentsFound = null;

    /** The number documents finished. */
    public volatile Long numberDocumentsFinished = null;

    /** The number documents total. */
    public volatile Long numberDocumentsTotal = null;

    /** The sub number documents total. */
    public volatile Long subNumberDocumentsTotal = null;

    /** The sub number documents finished total. */
    public volatile Long subNumberDocumentsFinishedTotal = null;

    /** The sub number documents finished. */
    public volatile Map<String, Long> subNumberDocumentsFinished = new ConcurrentHashMap<>();

    /**
     * Inits the.
     *
     * @param numberOfDocuments
     *            the number of documents
     * @param numberOfSegments
     *            the number of segments
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void init(long numberOfDocuments, int numberOfSegments) throws IOException
    {
        if (numberDocumentsTotal == null) {
            numberDocumentsTotal = numberOfDocuments;
        }
        else if (numberDocumentsTotal != numberOfDocuments) {
            throw new IOException("conflict number of documents: " + numberDocumentsTotal + " / "
                    + numberOfDocuments);
        }
        if (numberSegmentsTotal == null) {
            numberSegmentsTotal = numberOfSegments;
        }
        else if (numberSegmentsTotal != numberOfSegments) {
            throw new IOException("conflict number of segments: " + numberSegmentsTotal + " / "
                    + numberOfSegments);
        }
        numberDocumentsFinished = (numberDocumentsFinished == null) ? Long.valueOf(0)
                : numberDocumentsFinished;
        if (numberSegmentsFinished == null) {
            numberSegmentsFinished = 0;
        }
        subNumberDocumentsTotal = numberDocumentsTotal * subNumberDocumentsFinished.size();
        subNumberDocumentsFinishedTotal = (subNumberDocumentsFinishedTotal == null)
                ? Long.valueOf(0)
                : subNumberDocumentsFinishedTotal;
        subNumberSegmentsTotal = numberOfSegments * subNumberSegmentsFinished.size();
        if (subNumberSegmentsFinishedTotal == null) {
            subNumberSegmentsFinishedTotal = 0;
        }
    }

    /**
     * Adds the subs.
     *
     * @param subItems
     *            the sub items
     */
    public void addSubs(Set<String> subItems)
    {
        for (String subItem : subItems) {
            addSub(subItem);
        }
    }

    /**
     * Adds the sub.
     *
     * @param subItem
     *            the sub item
     */
    public void addSub(String subItem)
    {
        if (!subNumberSegmentsFinished.containsKey(subItem)) {
            subNumberSegmentsFinished.put(subItem, 0);
            if (numberSegmentsTotal != null) {
                subNumberSegmentsTotal += numberSegmentsTotal;
            }
        }
        if (!subNumberDocumentsFinished.containsKey(subItem)) {
            subNumberDocumentsFinished.put(subItem, Long.valueOf(0));
            if (numberDocumentsTotal != null) {
                subNumberDocumentsTotal += numberDocumentsTotal;
            }
        }
    }

}
