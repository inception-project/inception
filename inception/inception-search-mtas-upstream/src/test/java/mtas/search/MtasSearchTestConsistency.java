package mtas.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtas.codec.util.CodecInfo;
import mtas.codec.util.CodecSearchTree.MtasTreeHit;
import mtas.codec.util.CodecUtil;
import mtas.parser.cql.MtasCQLParser;
import mtas.parser.cql.ParseException;
import mtas.search.spans.util.MtasDisabledTwoPhaseIteratorSpanQuery;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Class MtasSearchTestConsistency.
 */
public class MtasSearchTestConsistency
{

    /** The log. */
    private static final Logger log = LoggerFactory.getLogger(MtasSearchTestConsistency.class);

    /** The Constant FIELD_ID. */
    private static final String FIELD_ID = "id";

    /** The Constant FIELD_TITLE. */
    private static final String FIELD_TITLE = "title";

    /** The Constant FIELD_CONTENT. */
    private static final String FIELD_CONTENT = "content";

    /** The directory. */
    private static Directory directory;

    /** The files. */
    private static HashMap<String, String> files;

    /** The docs. */
    private static ArrayList<Integer> docs;

    /**
     * Initialize.
     */
    @org.junit.BeforeClass
    public static void initialize()
    {
        try {
            Path dataPath = Paths.get("src" + File.separator + "test" + File.separator + "resources"
                    + File.separator + "data");
            // directory = FSDirectory.open(Paths.get("testindexMtas"));
            directory = new ByteBuffersDirectory();
            files = new HashMap<>();
            files.put("Een onaangenaam mens in de Haarlemmerhout", dataPath.resolve("resources")
                    .resolve("beets1.xml.gz").toAbsolutePath().toString());
            files.put("Een oude kennis", dataPath.resolve("resources").resolve("beets2.xml.gz")
                    .toAbsolutePath().toString());
            files.put("Varen en Rijden", dataPath.resolve("resources").resolve("beets3.xml.gz")
                    .toAbsolutePath().toString());
            createIndex(dataPath.resolve("conf").resolve("folia.xml").toAbsolutePath().toString(),
                    files);
            docs = getLiveDocs(DirectoryReader.open(directory));
        }
        catch (IOException e) {
            log.error("Error", e);
        }
    }

    /**
     * Basic search equals.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchEquals() throws IOException
    {
        IndexReader indexReader = DirectoryReader.open(directory);
        testNumberOfHits(indexReader, FIELD_CONTENT, Arrays.asList("[t==\"de\"]"),
                Arrays.asList("[t=\"de\"]"));
        testNumberOfHits(indexReader, FIELD_CONTENT, Arrays.asList("[t==\".\"]"),
                Arrays.asList("[t=\"\\.\"]"));
        testNumberOfHits(indexReader, FIELD_CONTENT, Arrays.asList("[t=1]"),
                Arrays.asList("[t=\"1\"]"));
        testNumberOfHits(indexReader, FIELD_CONTENT, Arrays.asList("[t=1]"),
                Arrays.asList("[t==\"1\"]"));
    }

    /**
     * Basic search comparator.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchNumbers() throws IOException
    {
        IndexReader indexReader = DirectoryReader.open(directory);
        int value1 = 34;
        int value2 = 10;
        List valuesLess = new ArrayList<String>();
        List valuesLessOrEqual = new ArrayList<String>();
        List valuesEqual = new ArrayList<String>();
        for (int i = 0; i < value1; i++) {
            valuesLess.add("[t=\"" + Integer.toString(i) + "\"]");
            valuesLessOrEqual.add("[t=\"" + Integer.toString(i) + "\"]");
        }
        valuesLessOrEqual.add("[t=\"" + Integer.toString(value1) + "\"]");
        valuesEqual.add("[t=\"" + Integer.toString(value1) + "\"]");
        List valuesInterval = new ArrayList<String>();
        List valuesIntervalInclusive = new ArrayList<String>();
        for (int i = value2 + 1; i < value1; i++) {
            valuesInterval.add("[t=\"" + Integer.toString(i) + "\"]");
            valuesIntervalInclusive.add("[t=\"" + Integer.toString(i) + "\"]");
        }
        valuesIntervalInclusive.add("[t=\"" + Integer.toString(value1) + "\"]");
        valuesIntervalInclusive.add("[t=\"" + Integer.toString(value2) + "\"]");
        testNumberOfHits(indexReader, FIELD_CONTENT,
                Arrays.asList("[t<" + Integer.toString(value1) + "]"), valuesLess);
        testNumberOfHits(indexReader, FIELD_CONTENT,
                Arrays.asList("[t<=" + Integer.toString(value1) + "]"), valuesLessOrEqual);
        testNumberOfHits(indexReader, FIELD_CONTENT,
                Arrays.asList("[t=" + Integer.toString(value1) + "]"), valuesEqual);
        testNumberOfHits(indexReader, FIELD_CONTENT, Arrays.asList(
                "[t<" + Integer.toString(value1) + " & t>" + Integer.toString(value2) + "]"),
                valuesInterval);
        testNumberOfHits(indexReader, FIELD_CONTENT, Arrays.asList(
                "[t<=" + Integer.toString(value1) + " & t>=" + Integer.toString(value2) + "]"),
                valuesIntervalInclusive);
        indexReader.close();
    }

    /**
     * Basic search number of words.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchNumberOfWords() throws IOException
    {
        IndexReader indexReader = DirectoryReader.open(directory);
        testNumberOfHits(indexReader, FIELD_CONTENT, Arrays.asList("[]"),
                Arrays.asList("[][]", "[#0]"));
        indexReader.close();
    }

    /**
     * Basic search start sentence 1.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchStartSentence1() throws IOException
    {
        IndexReader indexReader = DirectoryReader.open(directory);
        testNumberOfHits(indexReader, FIELD_CONTENT, Arrays.asList("<s/>"), Arrays.asList("<s>[]"));
        indexReader.close();
    }

    /**
     * Basic search start sentence 2.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchStartSentence2() throws IOException
    {
        IndexReader indexReader = DirectoryReader.open(directory);
        testNumberOfHits(indexReader, FIELD_CONTENT, Arrays.asList("[]</s><s>[]", "[#0]"),
                Arrays.asList("<s>[]"));
        indexReader.close();
    }

    /**
     * Basic search intersecting 1.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchIntersecting1() throws IOException
    {
        IndexReader indexReader = DirectoryReader.open(directory);
        testNumberOfHits(indexReader, FIELD_CONTENT, Arrays.asList("<s/>"), Arrays
                .asList("<s/> intersecting [pos=\"ADJ\"]", "<s/> !intersecting [pos=\"ADJ\"]"));
        indexReader.close();
    }

    /**
     * Basic search intersecting 2.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchIntersecting2() throws IOException
    {
        String cql = "([pos=\"N\"][]) intersecting ([pos=\"N\"][])";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql, null, null,
                null, true);
        assertFalse("Intersecting: " + cql + " has no hits (" + queryResult1.hits + ")",
                queryResult1.hits == 0);
        assertEquals("Intersecting: - twoPhaseIterator", queryResult1.hits,
                queryResult1disabled.hits);
    }

    /**
     * Basic search intersecting 3.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchIntersecting3() throws IOException
    {
        String cql = "([]</s>) intersecting (<s>[])";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql, null, null,
                null, true);
        assertTrue("Intersecting: " + cql + " has hits (" + queryResult1.hits + ")",
                queryResult1.hits == 0);
        assertEquals("Intersecting: - twoPhaseIterator", queryResult1.hits,
                queryResult1disabled.hits);
    }

    /**
     * Basic search ignore.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchIgnore() throws IOException
    {
        int ignoreNumber = 10;
        String cql1 = "[pos=\"LID\"][pos=\"ADJ\"]{0," + ignoreNumber + "}[pos=\"N\"]";
        String cql2 = "[pos=\"LID\"][pos=\"N\"]";
        String cql2ignore = "[pos=\"ADJ\"]";
        // get total number of nouns
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql1, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql1, null, null,
                null, true);
        MtasSpanQuery ignore;
        try {
            ignore = createQuery(FIELD_CONTENT, cql2ignore, null, null, false);
        }
        catch (ParseException e) {
            throw new IOException("Parse Exception", e);
        }
        QueryResult queryResult2 = doQuery(indexReader, FIELD_CONTENT, cql2, ignore, ignoreNumber,
                null, false);
        QueryResult queryResult2disabled = doQuery(indexReader, FIELD_CONTENT, cql2, ignore,
                ignoreNumber, null, true);
        assertEquals("Article followed by Noun ignoring Adjectives", queryResult1.hits,
                queryResult2.hits);
        assertEquals("Article followed by Noun ignoring Adjectives - disabled twoPhaseIterator",
                queryResult1disabled.hits, queryResult2disabled.hits);
        assertEquals("Ignore: twoPhaseIterator", queryResult1.hits, queryResult1disabled.hits);
        indexReader.close();
    }

    /**
     * Basic search sequence.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchSequence() throws IOException
    {
        String cql1 = "[pos=\"N\"][]{2,3}[pos=\"LID\"]";
        String cql2 = "[][pos=\"N\"][]{2,3}[pos=\"LID\"][]";
        String cql3 = "[]{0,3}[pos=\"N\"][]{2,3}[pos=\"LID\"][]{0,2}";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql1, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql1, null, null,
                null, true);
        QueryResult queryResult2 = doQuery(indexReader, FIELD_CONTENT, cql2, null, null, null,
                false);
        QueryResult queryResult2disabled = doQuery(indexReader, FIELD_CONTENT, cql2, null, null,
                null, true);
        QueryResult queryResult3 = doQuery(indexReader, FIELD_CONTENT, cql3, null, null, null,
                false);
        QueryResult queryResult3disabled = doQuery(indexReader, FIELD_CONTENT, cql3, null, null,
                null, true);
        assertTrue("Sequences: not #" + cql1 + " (" + queryResult1.hits + ") >= #" + cql2 + " ("
                + queryResult2.hits + ")", queryResult1.hits >= queryResult2.hits);
        assertTrue(
                "Sequences: not #" + cql1 + " (" + queryResult1.hits + ") >= #" + cql2 + " ("
                        + queryResult2.hits + ") - disabled twoPhaseIterator",
                queryResult1disabled.hits >= queryResult2disabled.hits);
        assertTrue("Sequences: not #" + cql1 + " (" + queryResult1.hits + ") >= #" + cql3 + " ("
                + queryResult3.hits + ")", queryResult1.hits <= queryResult3.hits);
        assertTrue(
                "Sequences: not #" + cql1 + " (" + queryResult1.hits + ") >= #" + cql3 + " ("
                        + queryResult3.hits + ") - disabled twoPhaseIterator",
                queryResult1disabled.hits <= queryResult3disabled.hits);
        assertEquals("Sequences: twoPhaseIterator 1", queryResult1.hits, queryResult1disabled.hits);
        assertEquals("Sequences: twoPhaseIterator 2", queryResult2.hits, queryResult2disabled.hits);
        assertEquals("Sequences: twoPhaseIterator 3", queryResult3.hits, queryResult3disabled.hits);
        indexReader.close();
    }

    /**
     * Basic search within 1.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchWithin1() throws IOException
    {
        IndexReader indexReader = DirectoryReader.open(directory);
        testNumberOfHits(indexReader, FIELD_CONTENT, Arrays.asList("[]"),
                Arrays.asList("[] within <s/>"));
        indexReader.close();
    }

    /**
     * Basic search within 2.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchWithin2() throws IOException
    {
        String cql1 = "[pos=\"N\"][][pos=\"LID\"]";
        String cql2 = "[pos=\"N\"][pos=\"N\"][pos=\"LID\"]";
        String cql3 = "[pos=\"N\"] within [pos=\"N\"][][pos=\"LID\"]";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql1, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql1, null, null,
                null, true);
        QueryResult queryResult2 = doQuery(indexReader, FIELD_CONTENT, cql2, null, null, null,
                false);
        QueryResult queryResult2disabled = doQuery(indexReader, FIELD_CONTENT, cql2, null, null,
                null, true);
        QueryResult queryResult3 = doQuery(indexReader, FIELD_CONTENT, cql3, null, null, null,
                false);
        QueryResult queryResult3disabled = doQuery(indexReader, FIELD_CONTENT, cql3, null, null,
                null, true);
        assertFalse("Within: " + cql3 + " has no hits (" + queryResult3.hits + ")",
                queryResult3.hits == 0);
        assertEquals("Within: " + cql3, queryResult3.hits,
                (long) queryResult1.hits + queryResult2.hits);
        assertEquals("Within: " + cql3 + " - disabled twoPhaseIterator", queryResult3.hits,
                (long) queryResult1.hits + queryResult2.hits);
        assertEquals("Within: twoPhaseIterator 1", queryResult1.hits, queryResult1disabled.hits);
        assertEquals("Within: twoPhaseIterator 2", queryResult2.hits, queryResult2disabled.hits);
        assertEquals("Within: twoPhaseIterator 3", queryResult3.hits, queryResult3disabled.hits);
        indexReader.close();
    }

    /**
     * Basic search within 3.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchWithin3() throws IOException
    {
        String cql1 = "[pos=\"N\"][][pos=\"LID\"][]{2}";
        String cql2 = "[pos=\"N\"][pos=\"N\"][pos=\"LID\"][]{2}";
        String cql3 = "[pos=\"N\"] within []{0,2}[pos=\"N\"][][pos=\"LID\"][]{2,3}";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql1, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql1, null, null,
                null, true);
        QueryResult queryResult2 = doQuery(indexReader, FIELD_CONTENT, cql2, null, null, null,
                false);
        QueryResult queryResult2disabled = doQuery(indexReader, FIELD_CONTENT, cql2, null, null,
                null, true);
        QueryResult queryResult3 = doQuery(indexReader, FIELD_CONTENT, cql3, null, null, null,
                false);
        QueryResult queryResult3disabled = doQuery(indexReader, FIELD_CONTENT, cql3, null, null,
                null, true);
        assertFalse("Within: " + cql3 + " has no hits (" + queryResult3.hits + ")",
                queryResult3.hits == 0);
        assertTrue(
                "Within: " + cql3 + " not " + queryResult3.hits + " >= " + queryResult1.hits + " + "
                        + queryResult2.hits,
                queryResult3.hits >= queryResult1.hits + queryResult2.hits);
        assertTrue(
                "Within: " + cql3 + " not " + queryResult3.hits + " >= " + queryResult1.hits + " + "
                        + queryResult2.hits + " - disabled twoPhaseIterator",
                queryResult3.hits >= queryResult1.hits + queryResult2.hits);
        assertEquals("Within: twoPhaseIterator 1", queryResult1.hits, queryResult1disabled.hits);
        assertEquals("Within: twoPhaseIterator 2", queryResult2.hits, queryResult2disabled.hits);
        assertEquals("Within: twoPhaseIterator 3", queryResult3.hits, queryResult3disabled.hits);
        indexReader.close();
    }

    /**
     * Basic search within 4.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchWithin4() throws IOException
    {
        String cql = "((<s>) !within ([]{0,5}[pos=\"N\"])) within ([]{0,5}[pos=\"N\"])";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult = doQuery(indexReader, FIELD_CONTENT, cql, null, null, null, false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql, null, null,
                null, true);
        assertTrue("Within: " + cql + " has hits (" + queryResult.hits + ")",
                queryResult.hits == 0);
        assertEquals("Within: - twoPhaseIterator", queryResult.hits, queryResult1disabled.hits);
    }

    /**
     * Basic search within 5.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchWithin5() throws IOException
    {
        String cql = "([pos=\"N\"][]) within ([pos=\"N\"][])";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql, null, null,
                null, true);
        assertFalse("Within: " + cql + " has no hits (" + queryResult1.hits + ")",
                queryResult1.hits == 0);
        assertEquals("Within: - twoPhaseIterator", queryResult1.hits, queryResult1disabled.hits);
    }

    /**
     * Basic search containing 1.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchContaining1() throws IOException
    {
        IndexReader indexReader = DirectoryReader.open(directory);
        testNumberOfHits(indexReader, FIELD_CONTENT, Arrays.asList("<s/>"),
                Arrays.asList("<s/> containing [pos=\"ADJ\"]", "<s/> !containing [pos=\"ADJ\"]"));
        indexReader.close();
    }

    /**
     * Basic search containing 2.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchContaining2() throws IOException
    {
        String cql1 = "[pos=\"N\"][][pos=\"LID\"]";
        String cql2 = "[pos=\"N\"][pos=\"N\"][pos=\"LID\"]";
        String cql3 = "[pos=\"N\"][][pos=\"LID\"] containing [pos=\"N\"][pos=\"LID\"]";
        String cql4 = "[pos=\"N\"][][pos=\"LID\"] !containing [pos=\"N\"][pos=\"LID\"]";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql1, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql1, null, null,
                null, true);
        QueryResult queryResult2 = doQuery(indexReader, FIELD_CONTENT, cql2, null, null, null,
                false);
        QueryResult queryResult2disabled = doQuery(indexReader, FIELD_CONTENT, cql2, null, null,
                null, true);
        QueryResult queryResult3 = doQuery(indexReader, FIELD_CONTENT, cql3, null, null, null,
                false);
        QueryResult queryResult3disabled = doQuery(indexReader, FIELD_CONTENT, cql3, null, null,
                null, true);
        QueryResult queryResult4 = doQuery(indexReader, FIELD_CONTENT, cql4, null, null, null,
                false);
        QueryResult queryResult4disabled = doQuery(indexReader, FIELD_CONTENT, cql4, null, null,
                null, true);
        assertEquals("Containing: " + cql3, queryResult3.hits, queryResult2.hits);
        assertEquals("Containing: " + cql3 + " disabled twoPhaseIterator",
                queryResult3disabled.hits, queryResult2disabled.hits);
        assertEquals("Containing: " + cql4, queryResult4.hits,
                (long) queryResult1.hits - queryResult2.hits);
        assertEquals("Containing: " + cql4 + " disabled twoPhaseIterator",
                queryResult4disabled.hits,
                (long) queryResult1disabled.hits - queryResult2disabled.hits);
        assertEquals("Containing: twoPhaseIterator 1", queryResult1.hits,
                queryResult1disabled.hits);
        assertEquals("Containing: twoPhaseIterator 2", queryResult2.hits,
                queryResult2disabled.hits);
        assertEquals("Containing: twoPhaseIterator 3", queryResult3.hits,
                queryResult3disabled.hits);
        assertEquals("Containing: twoPhaseIterator 4", queryResult4.hits,
                queryResult4disabled.hits);
        indexReader.close();
    }

    /**
     * Basic search containing 3.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchContaining3() throws IOException
    {
        String cql = "(([]{0,5}[pos=\"N\"]) !containing (<s>)) containing (<s>)";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql, null, null,
                null, true);
        assertTrue("Containing: " + cql + " has hits (" + queryResult1.hits + ")",
                queryResult1.hits == 0);
        assertEquals("Containing: twoPhaseIterator", queryResult1.hits, queryResult1disabled.hits);
    }

    /**
     * Basic search containing 4.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchContaining4() throws IOException
    {
        String cql = "([pos=\"N\"][]) containing ([pos=\"N\"][])";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql, null, null,
                null, true);
        assertFalse("Containing: " + cql + " has no hits (" + queryResult1.hits + ")",
                queryResult1.hits == 0);
        assertEquals("Containing: - twoPhaseIterator", queryResult1.hits,
                queryResult1disabled.hits);
    }

    /**
     * Basic search followed by 1.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchFollowedBy1() throws IOException
    {
        String cql1 = "[pos=\"LID\"] followedby []?[pos=\"ADJ\"]";
        String cql2 = "[pos=\"LID\"][]?[pos=\"ADJ\"]";
        String cql3 = "[pos=\"LID\"][pos=\"ADJ\"][pos=\"ADJ\"]";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql1, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql1, null, null,
                null, true);
        QueryResult queryResult2 = doQuery(indexReader, FIELD_CONTENT, cql2, null, null, null,
                false);
        QueryResult queryResult2disabled = doQuery(indexReader, FIELD_CONTENT, cql2, null, null,
                null, true);
        QueryResult queryResult3 = doQuery(indexReader, FIELD_CONTENT, cql3, null, null, null,
                false);
        QueryResult queryResult3disabled = doQuery(indexReader, FIELD_CONTENT, cql3, null, null,
                null, true);
        assertEquals("Article followed by Adjective", queryResult1.hits,
                (long) queryResult2.hits - queryResult3.hits);
        assertEquals("Article followed by Adjective - disabled twoPhaseIterator", queryResult1.hits,
                (long) queryResult2disabled.hits - queryResult3disabled.hits);
        assertEquals("FollowedBy: twoPhaseIterator 1", queryResult1.hits,
                queryResult1disabled.hits);
        assertEquals("FollowedBy: twoPhaseIterator 2", queryResult2.hits,
                queryResult2disabled.hits);
        indexReader.close();
    }

    /**
     * Basic search followed by 2.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchFollowedBy2() throws IOException
    {
        String cql1 = "[pos=\"LID\"] followedby []?[pos=\"ADJ\"]";
        String cql2 = "[pos=\"LID\"][]?[pos=\"ADJ\"]";
        String cql3 = "[pos=\"LID\"][pos=\"ADJ\"][pos=\"ADJ\"]";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql1, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql1, null, null,
                null, true);
        QueryResult queryResult2 = doQuery(indexReader, FIELD_CONTENT, cql2, null, null, null,
                false);
        QueryResult queryResult2disabled = doQuery(indexReader, FIELD_CONTENT, cql2, null, null,
                null, true);
        QueryResult queryResult3 = doQuery(indexReader, FIELD_CONTENT, cql3, null, null, null,
                false);
        QueryResult queryResult3disabled = doQuery(indexReader, FIELD_CONTENT, cql3, null, null,
                null, true);
        assertEquals("Article followed by Adjective", queryResult1.hits,
                (long) queryResult2.hits - queryResult3.hits);
        assertEquals("Article followed by Adjective - disabled twoPhaseIterator",
                queryResult1disabled.hits,
                (long) queryResult2disabled.hits - queryResult3disabled.hits);
        assertEquals("FollowedBy: twoPhaseIterator 1", queryResult1.hits,
                queryResult1disabled.hits);
        assertEquals("FollowedBy: twoPhaseIterator 2", queryResult2.hits,
                queryResult2disabled.hits);
        indexReader.close();
    }

    /**
     * Basic search preceded by 1.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchPrecededBy1() throws IOException
    {
        String cql1 = "[pos=\"ADJ\"] precededby [pos=\"LID\"][]?";
        String cql2 = "[pos=\"LID\"][]?[pos=\"ADJ\"]";
        String cql3 = "[pos=\"LID\"][pos=\"LID\"][pos=\"ADJ\"]";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql1, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql1, null, null,
                null, true);
        QueryResult queryResult2 = doQuery(indexReader, FIELD_CONTENT, cql2, null, null, null,
                false);
        QueryResult queryResult2disabled = doQuery(indexReader, FIELD_CONTENT, cql2, null, null,
                null, true);
        QueryResult queryResult3 = doQuery(indexReader, FIELD_CONTENT, cql3, null, null, null,
                false);
        QueryResult queryResult3disabled = doQuery(indexReader, FIELD_CONTENT, cql3, null, null,
                null, true);
        assertEquals("Adjective preceded by Article", queryResult1.hits,
                (long) queryResult2.hits - queryResult3.hits);
        assertEquals("Adjective preceded by Article - disabled twoPhaseIterator",
                queryResult1disabled.hits,
                (long) queryResult2disabled.hits - queryResult3disabled.hits);
        assertEquals("PrecededBy: twoPhaseIterator 1", queryResult1.hits,
                queryResult1disabled.hits);
        assertEquals("PrecededBy: twoPhaseIterator 2", queryResult2.hits,
                queryResult2disabled.hits);
        assertEquals("PrecededBy: twoPhaseIterator 3", queryResult3.hits,
                queryResult3disabled.hits);
        indexReader.close();
    }

    /**
     * Basic search preceded by 2.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchPrecededBy2() throws IOException
    {
        String cql1 = "[]?[pos=\"ADJ\"] precededby [pos=\"LID\"]";
        String cql2 = "[pos=\"LID\"][]?[pos=\"ADJ\"]";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql1, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql1, null, null,
                null, true);
        QueryResult queryResult2 = doQuery(indexReader, FIELD_CONTENT, cql2, null, null, null,
                false);
        QueryResult queryResult2disabled = doQuery(indexReader, FIELD_CONTENT, cql2, null, null,
                null, true);
        assertEquals("Adjective preceded by Article", queryResult1.hits, queryResult2.hits);
        assertEquals("Adjective preceded by Article - disabled twoPhaseIterator",
                queryResult1disabled.hits, queryResult2disabled.hits);
        assertEquals("PrecededBy: twoPhaseIterator 1", queryResult1.hits,
                queryResult1disabled.hits);
        assertEquals("PrecededBy: twoPhaseIterator 2", queryResult2.hits,
                queryResult2disabled.hits);
        indexReader.close();
    }

    /**
     * Basic search fully aligned with 1.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchFullyAlignedWith1() throws IOException
    {
        String cql1 = "[pos=\"N\"]";
        String cql2 = "[] fullyalignedwith [pos=\"N\"]";
        String cql3 = "[pos=\"N\"]{2}";
        String cql4 = "[pos=\"N\"]{1} fullyalignedwith [pos=\"N\"]{2}";
        String cql5 = "[pos=\"N\"]{2} fullyalignedwith [pos=\"N\"]{2}";
        String cql6 = "[pos=\"N\"]{0,3} fullyalignedwith [pos=\"N\"]{2}";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql1, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql1, null, null,
                null, true);
        QueryResult queryResult2 = doQuery(indexReader, FIELD_CONTENT, cql2, null, null, null,
                false);
        QueryResult queryResult2disabled = doQuery(indexReader, FIELD_CONTENT, cql2, null, null,
                null, true);
        QueryResult queryResult3 = doQuery(indexReader, FIELD_CONTENT, cql3, null, null, null,
                false);
        QueryResult queryResult3disabled = doQuery(indexReader, FIELD_CONTENT, cql3, null, null,
                null, true);
        QueryResult queryResult4 = doQuery(indexReader, FIELD_CONTENT, cql4, null, null, null,
                false);
        QueryResult queryResult4disabled = doQuery(indexReader, FIELD_CONTENT, cql4, null, null,
                null, true);
        QueryResult queryResult5 = doQuery(indexReader, FIELD_CONTENT, cql5, null, null, null,
                false);
        QueryResult queryResult5disabled = doQuery(indexReader, FIELD_CONTENT, cql5, null, null,
                null, true);
        QueryResult queryResult6 = doQuery(indexReader, FIELD_CONTENT, cql6, null, null, null,
                false);
        QueryResult queryResult6disabled = doQuery(indexReader, FIELD_CONTENT, cql6, null, null,
                null, true);
        assertEquals("Fully Aligned With (1)", queryResult1.hits, queryResult2.hits);
        assertEquals("Fully Aligned With (1) - disable twoPhaseIterator", queryResult1disabled.hits,
                queryResult2disabled.hits);
        assertTrue("Fully Aligned With (2): was " + queryResult4.hits, queryResult4.hits == 0);
        assertTrue(
                "Fully Aligned With (2): was" + queryResult4.hits + " - disable twoPhaseIterator",
                queryResult4disabled.hits == 0);
        assertEquals("Fully Aligned With (3)", queryResult3.hits, queryResult5.hits);
        assertEquals("Fully Aligned With (3) - disable twoPhaseIterator", queryResult3disabled.hits,
                queryResult5disabled.hits);
        assertEquals("Fully Aligned With (4)", queryResult3.hits, queryResult6.hits);
        assertEquals("Fully Aligned With (4) - disable twoPhaseIterator", queryResult3disabled.hits,
                queryResult6disabled.hits);
        assertEquals("FullyAlignedWith: twoPhaseIterator 1", queryResult1.hits,
                queryResult1disabled.hits);
        assertEquals("FullyAlignedWith: twoPhaseIterator 2", queryResult2.hits,
                queryResult2disabled.hits);
        assertEquals("FullyAlignedWith: twoPhaseIterator 3", queryResult3.hits,
                queryResult3disabled.hits);
        assertEquals("FullyAlignedWith: twoPhaseIterator 4", queryResult4.hits,
                queryResult4disabled.hits);
        assertEquals("FullyAlignedWith: twoPhaseIterator 5", queryResult5.hits,
                queryResult5disabled.hits);
        assertEquals("FullyAlignedWith: twoPhaseIterator 6", queryResult6.hits,
                queryResult6disabled.hits);
        indexReader.close();
    }

    /**
     * Basic search fully aligned with 2.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @org.junit.Test
    public void basicSearchFullyAlignedWith2() throws IOException
    {
        String cql = "([pos=\"N\"][]) fullyalignedwith ([pos=\"N\"][])";
        // get total number
        IndexReader indexReader = DirectoryReader.open(directory);
        QueryResult queryResult1 = doQuery(indexReader, FIELD_CONTENT, cql, null, null, null,
                false);
        QueryResult queryResult1disabled = doQuery(indexReader, FIELD_CONTENT, cql, null, null,
                null, true);
        assertFalse("Fullyalignedwith: " + cql + " has no hits (" + queryResult1.hits + ")",
                queryResult1.hits == 0);
        assertEquals("Fullyalignedwith: - twoPhaseIterator", queryResult1.hits,
                queryResult1disabled.hits);
    }

    /**
     * Creates the index.
     *
     * @param configFile
     *            the config file
     * @param files
     *            the files
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static void createIndex(String configFile, HashMap<String, String> files)
        throws IOException
    {
        // analyzer
        Map<String, String> paramsCharFilterMtas = new HashMap<>();
        paramsCharFilterMtas.put("type", "file");
        Map<String, String> paramsTokenizer = new HashMap<>();
        paramsTokenizer.put("configFile", configFile);
        Analyzer mtasAnalyzer = CustomAnalyzer.builder(Paths.get("docker").toAbsolutePath())
                .addCharFilter("mtas", paramsCharFilterMtas).withTokenizer("mtas", paramsTokenizer)
                .build();
        Map<String, Analyzer> analyzerPerField = new HashMap<>();
        analyzerPerField.put(FIELD_CONTENT, mtasAnalyzer);
        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(),
                analyzerPerField);
        // indexwriter
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setUseCompoundFile(false);
        config.setCodec(Codec.forName("MtasCodec"));
        IndexWriter w = new IndexWriter(directory, config);
        // delete
        w.deleteAll();
        // add
        int counter = 0;
        for (Entry<String, String> entry : files.entrySet()) {
            addDoc(w, counter, entry.getKey(), entry.getValue());
            if (counter == 0) {
                w.commit();
            }
            else {
                addDoc(w, counter, entry.getKey(), entry.getValue());
                addDoc(w, counter, "deletable", entry.getValue());
                w.commit();
                w.deleteDocuments(new Term(FIELD_ID, Integer.toString(counter)));
                w.deleteDocuments(new Term(FIELD_TITLE, "deletable"));
                addDoc(w, counter, entry.getKey(), entry.getValue());
            }
            counter++;
        }
        w.commit();
        // finish
        w.close();
    }

    /**
     * Adds the doc.
     *
     * @param w
     *            the w
     * @param id
     *            the id
     * @param title
     *            the title
     * @param file
     *            the file
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static void addDoc(IndexWriter w, Integer id, String title, String file)
        throws IOException
    {
        try {
            Document doc = new Document();
            doc.add(new StringField(FIELD_ID, id.toString(), Field.Store.YES));
            doc.add(new StringField(FIELD_TITLE, title, Field.Store.YES));
            doc.add(new TextField(FIELD_CONTENT, file, Field.Store.YES));
            w.addDocument(doc);
        }
        catch (Exception e) {
            log.error("Couldn't add " + title + " (" + file + ")", e);
        }
    }

    /**
     * Gets the live docs.
     *
     * @param indexReader
     *            the index reader
     * @return the live docs
     */
    private static ArrayList<Integer> getLiveDocs(IndexReader indexReader)
    {
        ArrayList<Integer> list = new ArrayList<>();
        ListIterator<LeafReaderContext> iterator = indexReader.leaves().listIterator();
        while (iterator.hasNext()) {
            LeafReaderContext lrc = iterator.next();
            SegmentReader r = (SegmentReader) lrc.reader();
            for (int docId = 0; docId < r.maxDoc(); docId++) {
                if (r.numDocs() == r.maxDoc() || r.getLiveDocs().get(docId)) {
                    list.add(lrc.docBase + docId);
                }
            }
        }
        return list;
    }

    /**
     * Creates the query.
     *
     * @param field
     *            the field
     * @param cql
     *            the cql
     * @param ignore
     *            the ignore
     * @param maximumIgnoreLength
     *            the maximum ignore length
     * @param disableTwoPhaseIterator
     *            the disable two phase iterator
     * @return the mtas span query
     * @throws ParseException
     *             the parse exception
     */
    private MtasSpanQuery createQuery(String field, String cql, MtasSpanQuery ignore,
            Integer maximumIgnoreLength, boolean disableTwoPhaseIterator)
        throws ParseException
    {
        Reader reader = new BufferedReader(new StringReader(cql));
        MtasCQLParser p = new MtasCQLParser(reader);
        MtasSpanQuery q = p.parse(field, null, null, ignore, maximumIgnoreLength);
        if (disableTwoPhaseIterator) {
            return new MtasDisabledTwoPhaseIteratorSpanQuery(q);
        }
        else {
            return q;
        }
    }

    /**
     * Do query.
     *
     * @param indexReader
     *            the index reader
     * @param field
     *            the field
     * @param cql
     *            the cql
     * @param ignore
     *            the ignore
     * @param maximumIgnoreLength
     *            the maximum ignore length
     * @param prefixes
     *            the prefixes
     * @param disableTwoPhaseIterator
     *            the disable two phase iterator
     * @return the query result
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private QueryResult doQuery(IndexReader indexReader, String field, String cql,
            MtasSpanQuery ignore, Integer maximumIgnoreLength, ArrayList<String> prefixes,
            boolean disableTwoPhaseIterator)
        throws IOException
    {
        QueryResult queryResult = new QueryResult();
        try {
            MtasSpanQuery q = createQuery(field, cql, ignore, maximumIgnoreLength,
                    disableTwoPhaseIterator);
            queryResult = doQuery(indexReader, field, q, prefixes);
        }
        catch (mtas.parser.cql.ParseException e) {
            log.error("Error", e);
        }
        return queryResult;
    }

    /**
     * Do query.
     *
     * @param indexReader
     *            the index reader
     * @param field
     *            the field
     * @param q
     *            the q
     * @param prefixes
     *            the prefixes
     * @return the query result
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private QueryResult doQuery(IndexReader indexReader, String field, MtasSpanQuery q,
            ArrayList<String> prefixes)
        throws IOException
    {
        QueryResult queryResult = new QueryResult();
        ListIterator<LeafReaderContext> iterator = indexReader.leaves().listIterator();
        IndexSearcher searcher = new IndexSearcher(indexReader);
        final float boost = 0;
        SpanWeight spanweight = q.rewrite(indexReader).createWeight(searcher,
                ScoreMode.COMPLETE_NO_SCORES, boost);

        while (iterator.hasNext()) {
            LeafReaderContext lrc = iterator.next();
            Spans spans = spanweight.getSpans(lrc, SpanWeight.Postings.POSITIONS);
            SegmentReader r = (SegmentReader) lrc.reader();
            Terms t = r.terms(field);
            CodecInfo mtasCodecInfo = CodecInfo.getCodecInfoFromTerms(t);
            if (spans != null) {
                while (spans.nextDoc() != Spans.NO_MORE_DOCS) {
                    if (r.numDocs() == r.maxDoc() || r.getLiveDocs().get(spans.docID())) {
                        queryResult.docs++;
                        while (spans.nextStartPosition() != Spans.NO_MORE_POSITIONS) {
                            queryResult.hits++;
                            if (prefixes != null && !prefixes.isEmpty()) {
                                List<MtasTreeHit<String>> terms = mtasCodecInfo
                                        .getPositionedTermsByPrefixesAndPositionRange(field,
                                                spans.docID(), prefixes, spans.startPosition(),
                                                (spans.endPosition() - 1));
                                for (MtasTreeHit<String> term : terms) {
                                    queryResult.resultList.add(new QueryHit(
                                            lrc.docBase + spans.docID(), term.startPosition,
                                            term.endPosition, CodecUtil.termPrefix(term.data),
                                            CodecUtil.termValue(term.data)));
                                }
                            }
                        }
                    }
                }
            }
        }
        return queryResult;
    }

    /**
     * Test number of hits.
     *
     * @param indexReader
     *            the index reader
     * @param field
     *            the field
     * @param cqls1
     *            the cqls 1
     * @param cqls2
     *            the cqls 2
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void testNumberOfHits(IndexReader indexReader, String field, List<String> cqls1,
            List<String> cqls2)
        throws IOException
    {
        int sum1 = 0;
        int sum2 = 0;
        int sum3 = 0;
        int sum4 = 0;
        QueryResult queryResult;
        for (String cql1 : cqls1) {
            queryResult = doQuery(indexReader, field, cql1, null, null, null, false);
            sum1 += queryResult.hits;
            queryResult = doQuery(indexReader, field, cql1, null, null, null, true);
            sum2 += queryResult.hits;
        }
        for (String cql2 : cqls2) {
            queryResult = doQuery(indexReader, field, cql2, null, null, null, false);
            sum3 += queryResult.hits;
            queryResult = doQuery(indexReader, field, cql2, null, null, null, true);
            sum4 += queryResult.hits;
        }
        assertEquals("twoPhaseIterator enabled for " + cqls1 + " (" + sum1 + ") and " + cqls2 + " ("
                + sum3 + ")", sum1, sum3);
        assertEquals("twoPhaseIterator disabled for " + cqls1 + " (" + sum2 + ") and " + cqls2
                + " (" + sum4 + ")", sum2, sum4);
        assertEquals("twoPhaseIterator for " + cqls1 + " (" + sum1 + " / " + sum2 + ")", sum1,
                sum2);
    }

    /**
     * The Class QueryResult.
     */
    private static class QueryResult
    {

        /** The docs. */
        public int docs;

        /** The hits. */
        public int hits;

        /** The result list. */
        public List<QueryHit> resultList;

        /**
         * Instantiates a new query result.
         */
        public QueryResult()
        {
            docs = 0;
            hits = 0;
            resultList = new ArrayList<>();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            StringBuilder buffer = new StringBuilder();
            buffer.append(docs + " document(s), ");
            buffer.append(hits + " hit(s)");
            return buffer.toString();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj)
        {

            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            QueryResult other = (QueryResult) obj;
            return other.hits == hits && other.docs == docs;
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
            h = (h * 5) ^ docs;
            h = (h * 7) ^ hits;
            return h;
        }

    }

    /**
     * The Class QueryHit.
     */
    private static class QueryHit
    {

        /**
         * Instantiates a new query hit.
         *
         * @param docId
         *            the doc id
         * @param startPosition
         *            the start position
         * @param endPosition
         *            the end position
         * @param prefix
         *            the prefix
         * @param value
         *            the value
         */
        protected QueryHit(int docId, int startPosition, int endPosition, String prefix,
                String value)
        {
        }
    }

}