package mtas.codec.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.spatial.prefix.PrefixTreeStrategy;
import org.apache.lucene.util.BytesRef;
import org.locationtech.spatial4j.shape.Shape;

import mtas.analysis.token.MtasToken;
import mtas.analysis.token.MtasTokenString;
import mtas.codec.util.CodecSearchTree.MtasTreeHit;
import mtas.codec.util.collector.MtasDataCollector;
import mtas.codec.util.distance.Distance;
import mtas.parser.function.MtasFunctionParser;
import mtas.parser.function.ParseException;
import mtas.parser.function.util.MtasFunctionParserFunction;
import mtas.parser.function.util.MtasFunctionParserFunctionDefault;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Class CodecComponent.
 */

public class CodecComponent
{

    /**
     * Instantiates a new codec component.
     */
    private CodecComponent()
    {
    }

    /**
     * The Class ComponentFields.
     */
    public static class ComponentFields
    {

        /** The status. */
        public ComponentStatus status;

        /** The version. */
        public ComponentVersion version;

        /** The list. */
        public Map<String, ComponentField> list;

        /** The collection. */
        public List<ComponentCollection> collection;

        /** The do document. */
        public boolean doDocument;

        /** The do kwic. */
        public boolean doKwic;

        /** The do list. */
        public boolean doList;

        /** The do page. */
        public boolean doPage;

        /** The do page. */
        public boolean doIndex;

        /** The do heatmap. */
        public boolean doHeatmap;

        /** The do group. */
        public boolean doGroup;

        /** The do term vector. */
        public boolean doTermVector;

        /** The do stats. */
        public boolean doStats;

        /** The do stats spans. */
        public boolean doStatsSpans;

        /** The do stats positions. */
        public boolean doStatsPositions;

        /** The do stats tokens. */
        public boolean doStatsTokens;

        /** The do prefix. */
        public boolean doPrefix;

        /** The do facet. */
        public boolean doFacet;

        /** The do collection. */
        public boolean doCollection;

        /** The do status. */
        public boolean doStatus;

        /** The do version. */
        public boolean doVersion;

        /**
         * Instantiates a new component fields.
         */
        public ComponentFields()
        {
            status = null;
            list = new HashMap<>();
            collection = new ArrayList<>();
            doDocument = false;
            doKwic = false;
            doList = false;
            doPage = false;
            doIndex = false;
            doHeatmap = false;
            doGroup = false;
            doStats = false;
            doTermVector = false;
            doStatsSpans = false;
            doStatsPositions = false;
            doStatsTokens = false;
            doPrefix = false;
            doFacet = false;
            doCollection = false;
            doStatus = false;
            doVersion = false;
        }
    }

    /**
     * The Interface BasicComponent.
     */
    public abstract static interface BasicComponent
    {
    }

    /**
     * The Class ComponentField.
     */
    public static class ComponentField
        implements BasicComponent
    {

        /** The unique key field. */
        public String uniqueKeyField;

        /** The document list. */
        public List<ComponentDocument> documentList;

        /** The kwic list. */
        public List<ComponentKwic> kwicList;

        /** The list list. */
        public List<ComponentList> listList;

        /** The page list. */
        public List<ComponentPage> pageList;

        /** The index list. */
        public List<ComponentIndex> indexList;

        /** The heatmap list. */
        public List<ComponentHeatmap> heatmapList;

        /** The group list. */
        public List<ComponentGroup> groupList;

        /** The facet list. */
        public List<ComponentFacet> facetList;

        /** The term vector list. */
        public List<ComponentTermVector> termVectorList;

        /** The stats position list. */
        public List<ComponentPosition> statsPositionList;

        /** The stats token list. */
        public List<ComponentToken> statsTokenList;

        /** The stats span list. */
        public List<ComponentSpan> statsSpanList;

        /** The span query list. */
        public List<MtasSpanQuery> spanQueryList;

        /** The prefix. */
        public ComponentPrefix prefix;

        /**
         * Instantiates a new component field.
         *
         * @param uniqueKeyField
         *            the unique key field
         */
        public ComponentField(String uniqueKeyField)
        {
            this.uniqueKeyField = uniqueKeyField;
            // initialise
            documentList = new ArrayList<>();
            kwicList = new ArrayList<>();
            listList = new ArrayList<>();
            pageList = new ArrayList<>();
            indexList = new ArrayList<>();
            heatmapList = new ArrayList<>();
            groupList = new ArrayList<>();
            facetList = new ArrayList<>();
            termVectorList = new ArrayList<>();
            statsPositionList = new ArrayList<>();
            statsTokenList = new ArrayList<>();
            statsSpanList = new ArrayList<>();
            spanQueryList = new ArrayList<>();
            prefix = null;
        }
    }

    /**
     * The Class ComponentPrefix.
     */
    public static class ComponentPrefix
        implements BasicComponent
    {

        /** The key. */
        public String key;

        /** The single position list. */
        public SortedSet<String> singlePositionList;

        /** The multiple position list. */
        public SortedSet<String> multiplePositionList;

        /** The set position list. */
        public SortedSet<String> setPositionList;

        /** The intersecting list. */
        public SortedSet<String> intersectingList;

        /**
         * Instantiates a new component prefix.
         *
         * @param key
         *            the key
         */
        public ComponentPrefix(String key)
        {
            this.key = key;
            singlePositionList = new TreeSet<>();
            multiplePositionList = new TreeSet<>();
            setPositionList = new TreeSet<>();
            intersectingList = new TreeSet<>();
        }

        /**
         * Adds the single position.
         *
         * @param prefix
         *            the prefix
         */
        public void addSinglePosition(String prefix)
        {
            if (!prefix.trim().isEmpty() && !singlePositionList.contains(prefix)
                    && !multiplePositionList.contains(prefix)) {
                singlePositionList.add(prefix);
            }
        }

        /**
         * Adds the multiple position.
         *
         * @param prefix
         *            the prefix
         */
        public void addMultiplePosition(String prefix)
        {
            if (!prefix.trim().isEmpty()) {
                if (!singlePositionList.contains(prefix)) {
                    if (!multiplePositionList.contains(prefix)) {
                        multiplePositionList.add(prefix);
                    }
                }
                else {
                    singlePositionList.remove(prefix);
                    multiplePositionList.add(prefix);
                }
            }
        }

        /**
         * Adds the set position.
         *
         * @param prefix
         *            the prefix
         */
        public void addSetPosition(String prefix)
        {
            if (!prefix.trim().isEmpty()) {
                if (!singlePositionList.contains(prefix)) {
                    if (!setPositionList.contains(prefix)) {
                        setPositionList.add(prefix);
                    }
                }
                else {
                    singlePositionList.remove(prefix);
                    setPositionList.add(prefix);
                }
            }
        }

        /**
         * Adds the intersecting.
         *
         * @param prefix
         *            the prefix
         */
        public void addIntersecting(String prefix)
        {
            if (!prefix.trim().isEmpty()) {
                intersectingList.add(prefix);
            }
        }

    }

    /**
     * The Class ComponentDocument.
     */
    public static class ComponentDocument
        implements BasicComponent
    {

        /** The key. */
        public String key;

        /** The prefix. */
        public String prefix;

        /** The regexp. */
        public String regexp;

        /** The ignore regexp. */
        public String ignoreRegexp;

        /** The list. */
        public Set<String> list;

        /** The ignore list. */
        public Set<String> ignoreList;

        /** The list regexp. */
        public boolean listRegexp;

        /** The list expand. */
        public boolean listExpand;

        /** The ignore list regexp. */
        public boolean ignoreListRegexp;

        /** The list expand number. */
        public int listExpandNumber;

        /** The data type. */
        public String dataType;

        /** The stats type. */
        public String statsType;

        /** The stats items. */
        public SortedSet<String> statsItems;

        /** The list number. */
        public int listNumber;

        /** The unique key. */
        public Map<Integer, String> uniqueKey;

        /** The stats data. */
        public Map<Integer, MtasDataCollector<?, ?>> statsData;

        /** The stats list. */
        public Map<Integer, MtasDataCollector<?, ?>> statsList;

        /**
         * Instantiates a new component document.
         *
         * @param key
         *            the key
         * @param prefix
         *            the prefix
         * @param statsType
         *            the stats type
         * @param regexp
         *            the regexp
         * @param list
         *            the list
         * @param listNumber
         *            the list number
         * @param listRegexp
         *            the list regexp
         * @param listExpand
         *            the list expand
         * @param listExpandNumber
         *            the list expand number
         * @param ignoreRegexp
         *            the ignore regexp
         * @param ignoreList
         *            the ignore list
         * @param ignoreListRegexp
         *            the ignore list regexp
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public ComponentDocument(String key, String prefix, String statsType, String regexp,
                String[] list, int listNumber, Boolean listRegexp, Boolean listExpand,
                int listExpandNumber, String ignoreRegexp, String[] ignoreList,
                Boolean ignoreListRegexp)
            throws IOException
        {
            this.key = key;
            this.prefix = prefix;
            this.regexp = regexp;
            if (list != null && list.length > 0) {
                this.list = new HashSet<>(Arrays.asList(list));
                this.listRegexp = listRegexp != null ? listRegexp : false;
                this.listExpand = (listExpand != null && listExpandNumber > 0) ? listExpand : false;
                if (this.listExpand) {
                    this.listExpandNumber = listExpandNumber;
                }
                else {
                    this.listExpandNumber = 0;
                }
            }
            else {
                this.list = null;
                this.listRegexp = false;
                this.listExpand = false;
                this.listExpandNumber = 0;
            }
            this.ignoreRegexp = ignoreRegexp;
            if (ignoreList != null && ignoreList.length > 0) {
                this.ignoreList = new HashSet<>(Arrays.asList(ignoreList));
                this.ignoreListRegexp = ignoreListRegexp != null ? ignoreListRegexp : false;
            }
            else {
                this.ignoreList = null;
                this.ignoreListRegexp = false;
            }
            this.listNumber = listNumber;
            uniqueKey = new HashMap<>();
            dataType = CodecUtil.DATA_TYPE_LONG;
            statsItems = CodecUtil.createStatsItems(statsType);
            this.statsType = CodecUtil.createStatsType(statsItems, null, null);
            this.statsData = new HashMap<>();
            if (this.listNumber > 0) {
                this.statsList = new HashMap<>();
            }
            else {
                this.statsList = null;
            }
        }
    }

    /**
     * The Class ComponentKwic.
     */
    public static class ComponentKwic
        implements BasicComponent
    {

        /** The query. */
        public MtasSpanQuery query;

        /** The key. */
        public String key;

        /** The tokens. */
        public Map<Integer, List<KwicToken>> tokens;

        /** The hits. */
        public Map<Integer, List<KwicHit>> hits;

        /** The unique key. */
        public Map<Integer, String> uniqueKey;

        /** The sub total. */
        public Map<Integer, Integer> subTotal;

        /** The min position. */
        public Map<Integer, Integer> minPosition;

        /** The max position. */
        public Map<Integer, Integer> maxPosition;

        /** The prefixes. */
        public List<String> prefixes;

        /** The left. */
        public int left;

        /** The right. */
        public int right;

        /** The start. */
        public int start;

        /** The number. */
        public Integer number;

        /** The page start. */
        public Integer pageStart;

        /** The page end. */
        public Integer pageEnd;

        /** The output. */
        public String output;

        /** The Constant KWIC_OUTPUT_TOKEN. */
        public static final String KWIC_OUTPUT_TOKEN = "token";

        /** The Constant KWIC_OUTPUT_HIT. */
        public static final String KWIC_OUTPUT_HIT = "hit";

        /**
         * Instantiates a new component kwic.
         *
         * @param query
         *            the query
         * @param key
         *            the key
         * @param prefixes
         *            the prefixes
         * @param number
         *            the number
         * @param start
         *            the start
         * @param pageStart
         *            the page start
         * @param pageEnd
         *            the page end
         * @param left
         *            the left
         * @param right
         *            the right
         * @param output
         *            the output
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public ComponentKwic(MtasSpanQuery query, String key, String prefixes, Integer number,
                int start, Integer pageStart, Integer pageEnd, int left, int right, String output)
            throws IOException
        {
            this.query = query;
            this.key = key;
            this.left = (left > 0) ? left : 0;
            this.right = (right > 0) ? right : 0;
            this.start = (start > 0) ? start : 0;
            this.number = (number != null && number >= 0) ? number : null;
            this.pageStart = (pageStart != null && pageEnd != null) ? pageStart : null;
            this.pageEnd = (pageStart != null && pageEnd != null) ? pageEnd : null;
            this.output = output;
            tokens = new HashMap<>();
            hits = new HashMap<>();
            uniqueKey = new HashMap<>();
            subTotal = new HashMap<>();
            minPosition = new HashMap<>();
            maxPosition = new HashMap<>();
            this.prefixes = new ArrayList<>();
            if ((prefixes != null) && (prefixes.trim().length() > 0)) {
                List<String> l = Arrays.asList(prefixes.split(Pattern.quote(",")));
                for (String ls : l) {
                    if (ls.trim().length() > 0) {
                        this.prefixes.add(ls.trim());
                    }
                }
            }
            if (this.output == null) {
                if (!this.prefixes.isEmpty()) {
                    this.output = ComponentKwic.KWIC_OUTPUT_HIT;
                }
                else {
                    this.output = ComponentKwic.KWIC_OUTPUT_TOKEN;
                }
            }
            else if (!this.output.equals(ComponentKwic.KWIC_OUTPUT_HIT)
                    && !this.output.equals(ComponentKwic.KWIC_OUTPUT_TOKEN)) {
                throw new IOException("unrecognized output '" + this.output + "'");
            }
        }
    }

    /**
     * The Class ComponentList.
     */
    public static class ComponentList
        implements BasicComponent
    {

        /** The span query. */
        public MtasSpanQuery spanQuery;

        /** The field. */
        public String field;

        /** The query value. */
        public String queryValue;

        /** The query type. */
        public String queryType;

        /** The query prefix. */
        public String queryPrefix;

        /** The query ignore. */
        public String queryIgnore;

        /** The query maximum ignore length. */
        public String queryMaximumIgnoreLength;

        /** The key. */
        public String key;

        /** The query variables. */
        public Map<String, String[]> queryVariables;

        /** The tokens. */
        public List<ListToken> tokens;

        /** The hits. */
        public List<ListHit> hits;

        /** The unique key. */
        public Map<Integer, String> uniqueKey;

        /** The sub total. */
        public Map<Integer, Integer> subTotal;

        /** The min position. */
        public Map<Integer, Integer> minPosition;

        /** The max position. */
        public Map<Integer, Integer> maxPosition;

        /** The prefixes. */
        public List<String> prefixes;

        /** The field values. */
        public Map<Integer, Map<String, Object>> fieldValues;

        /** The field names. */
        public List<String> fieldNames;

        /** The left. */
        public int left;

        /** The right. */
        public int right;

        /** The total. */
        public int total;

        /** The position. */
        public int position;

        /** The start. */
        public int start;

        /** The number. */
        public int number;

        /** The field list. */
        public String fieldList;

        /** The prefix. */
        public String prefix;

        /** The output. */
        public String output;

        /** The Constant LIST_OUTPUT_TOKEN. */
        public static final String LIST_OUTPUT_TOKEN = "token";

        /** The Constant LIST_OUTPUT_HIT. */
        public static final String LIST_OUTPUT_HIT = "hit";

        /**
         * Instantiates a new component list.
         *
         * @param spanQuery
         *            the span query
         * @param field
         *            the field
         * @param queryValue
         *            the query value
         * @param queryType
         *            the query type
         * @param queryPrefix
         *            the query prefix
         * @param queryVariables
         *            the query variables
         * @param queryIgnore
         *            the query ignore
         * @param queryMaximumIgnoreLength
         *            the query maximum ignore length
         * @param key
         *            the key
         * @param fieldList
         *            the field list
         * @param prefix
         *            the prefix
         * @param start
         *            the start
         * @param number
         *            the number
         * @param left
         *            the left
         * @param right
         *            the right
         * @param output
         *            the output
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public ComponentList(MtasSpanQuery spanQuery, String field, String queryValue,
                String queryType, String queryPrefix, Map<String, String[]> queryVariables,
                String queryIgnore, String queryMaximumIgnoreLength, String key, String fieldList,
                String prefix, int start, int number, int left, int right, String output)
            throws IOException
        {
            this.spanQuery = spanQuery;
            this.field = field;
            this.queryValue = queryValue;
            this.queryType = queryType;
            this.queryPrefix = queryPrefix;
            this.queryIgnore = queryIgnore;
            this.queryMaximumIgnoreLength = queryMaximumIgnoreLength;
            this.queryVariables = queryVariables;
            this.key = key;
            this.fieldList = fieldList;
            this.left = left;
            this.right = right;
            this.start = start;
            this.number = number;
            this.output = output;
            this.prefix = prefix;
            total = 0;
            position = 0;
            tokens = new ArrayList<>();
            hits = new ArrayList<>();
            uniqueKey = new HashMap<>();
            subTotal = new HashMap<>();
            minPosition = new HashMap<>();
            maxPosition = new HashMap<>();
            this.prefixes = new ArrayList<>();
            if ((prefix != null) && (prefix.trim().length() > 0)) {
                List<String> l = Arrays.asList(prefix.split(Pattern.quote(",")));
                for (String ls : l) {
                    if (ls.trim().length() > 0) {
                        this.prefixes.add(ls.trim());
                    }
                }
            }
            fieldValues = new HashMap<>();
            fieldNames = new ArrayList<>();
            if ((fieldList != null) && (fieldList.trim().length() > 0)) {
                List<String> l = Arrays.asList(fieldList.split(Pattern.quote(",")));
                for (String ls : l) {
                    if (ls.trim().length() > 0) {
                        this.fieldNames.add(ls.trim());
                    }
                }
            }
            // check output
            if (this.output == null) {
                if (!this.prefixes.isEmpty()) {
                    this.output = ComponentList.LIST_OUTPUT_HIT;
                }
                else {
                    this.output = ComponentList.LIST_OUTPUT_TOKEN;
                }
            }
            else if (!this.output.equals(ComponentList.LIST_OUTPUT_HIT)
                    && !this.output.equals(ComponentList.LIST_OUTPUT_TOKEN)) {
                throw new IOException("unrecognized output '" + this.output + "'");
            }
        }
    }

    /**
     * The Class ComponentIndex.
     */
    public static class ComponentIndex
        implements BasicComponent
    {

        /** The query. */
        public MtasSpanQuery query;

        /** The key. */
        public String key;

        /** The unique key. */
        public Map<Integer, String> uniqueKey;

        /** The min position. */
        public Map<Integer, Integer> minPosition;

        /** The max position. */
        public Map<Integer, Integer> maxPosition;

        /** The block size. */
        public Integer blockSize;

        /** The block number. */
        public Integer blockNumber;

        /** The block number. */
        public MtasSpanQuery blockQuery;

        /** The match. */
        public String match;

        /** The list prefix. */
        public List<String> listPrefixes;

        /** The list number. */
        public Integer listNumber;

        /** The list sort. */
        public String listSort;

        /** The index items. */
        public Map<Integer, List<IndexItem>> indexItems;

        /** The Constant DEFAULT_INDEX_BLOCK_NUMBER. */
        public static final Integer DEFAULT_INDEX_BLOCK_NUMBER = 10;

        /** The Constant INDEX_LIST_SORT_INDEX. */
        public static final String INDEX_LIST_SORT_INDEX = "index";

        /** The Constant INDEX_LIST_SORT_COUNT. */
        public static final String INDEX_LIST_SORT_COUNT = "count";

        /** The Constant INDEX_LIST_SORT_TFIBF. */
        public static final String INDEX_LIST_SORT_TFIBF = "tfibf";

        /**
         * Instantiates a new component index.
         *
         * @param query
         *            the query
         * @param key
         *            the key
         * @param blockSize
         *            the block size
         * @param blockNumber
         *            the block number
         * @param blockQuery
         *            the block query
         * @param match
         *            the match
         * @param listPrefix
         *            the list prefix
         * @param listNumber
         *            the list number
         * @param listSort
         *            the list sort
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public ComponentIndex(MtasSpanQuery query, String key, int blockSize, int blockNumber,
                MtasSpanQuery blockQuery, String match, String listPrefix, Integer listNumber,
                String listSort)
            throws IOException
        {
            uniqueKey = new HashMap<>();
            minPosition = new HashMap<>();
            maxPosition = new HashMap<>();
            this.query = query;
            this.key = key;
            // set and check block settings
            this.blockSize = Math.abs(blockSize);
            this.blockSize = (this.blockSize > 0) ? this.blockSize : null;
            this.blockNumber = Math.abs(blockNumber);
            this.blockNumber = (this.blockNumber > 0) ? this.blockNumber : null;
            this.blockQuery = blockQuery;
            this.indexItems = new HashMap<>();
            if (this.blockSize == null && this.blockNumber == null && this.blockQuery == null) {
                this.blockNumber = DEFAULT_INDEX_BLOCK_NUMBER;
            }
            else if (this.blockSize != null
                    && (this.blockNumber != null || this.blockQuery != null)) {
                throw new IOException(
                        "invalid block specification, mixture of blockTypes not allowed");
            }
            else if (this.blockNumber != null
                    && (this.blockSize != null || this.blockQuery != null)) {
                throw new IOException(
                        "invalid block specification, mixture of blockTypes not allowed");
            }
            else if (this.blockQuery != null
                    && (this.blockSize != null || this.blockNumber != null)) {
                throw new IOException(
                        "invalid block specification, mixture of blockTypes not allowed");
            }
            // set and check match
            this.match = match;
            if (this.match == null) {
                this.match = CodecCollector.MATCH_INTERSECT;
            }
            else if (!(this.match.equals(CodecCollector.MATCH_INTERSECT)
                    || this.match.equals(CodecCollector.MATCH_START)
                    || this.match.equals(CodecCollector.MATCH_COMPLETE))) {
                throw new IOException("unknown match specification for index");
            }
            // set and check list
            this.listPrefixes = new ArrayList<>();
            if ((listPrefix != null) && (!listPrefix.trim().isEmpty())) {
                List<String> l = Arrays.asList(listPrefix.split(Pattern.quote(",")));
                for (String ls : l) {
                    if (!ls.trim().isEmpty()) {
                        this.listPrefixes.add(ls.trim());
                    }
                }
            }
            if (!listPrefixes.isEmpty()) {
                this.listNumber = (listNumber != null && listNumber > 0) ? listNumber : null;
                this.listSort = listSort;
                if (this.listSort == null) {
                    this.listSort = INDEX_LIST_SORT_INDEX;
                }
                else if (!this.listSort.equals(INDEX_LIST_SORT_INDEX)
                        && !this.listSort.equals(INDEX_LIST_SORT_COUNT)
                        && !this.listSort.equals(INDEX_LIST_SORT_TFIBF)) {
                    throw new IOException("unknown list sort specification for index");
                }
            }
            else {
                this.listNumber = null;
                this.listSort = null;
            }
        }
    }

    /**
     * The Class ComponentPage.
     */
    public static class ComponentPage
        implements BasicComponent
    {
        /** The key. */
        public String key;

        /** The unique key. */
        public Map<Integer, String> uniqueKey;

        /** The min position. */
        public Map<Integer, Integer> minPosition;

        /** The max position. */
        public Map<Integer, Integer> maxPosition;

        /** The word list. */
        public Map<Integer, Map<Integer, PageWordData>> wordList;

        /** The range list. */
        public Map<Integer, Map<Integer, PageRangeData>> rangeList;

        /** The set list. */
        public Map<Integer, Map<Integer, PageSetData>> setList;

        /** The prefixes. */
        public List<String> prefixes;

        /** The start. */
        public int start;

        /** The end. */
        public int end;

        /**
         * Instantiates a new component list.
         *
         * @param field
         *            the field
         * @param key
         *            the key
         * @param prefix
         *            the prefix
         * @param start
         *            the start
         * @param end
         *            the end
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public ComponentPage(String field, String key, String prefix, int start, int end)
            throws IOException
        {
            this.key = key;
            this.start = start;
            this.end = end;
            uniqueKey = new HashMap<>();
            minPosition = new HashMap<>();
            maxPosition = new HashMap<>();
            wordList = new HashMap();
            rangeList = new HashMap();
            setList = new HashMap();
            this.prefixes = new ArrayList<>();
            if ((prefix != null) && (prefix.trim().length() > 0)) {
                List<String> l = Arrays.asList(prefix.split(Pattern.quote(",")));
                for (String ls : l) {
                    if (ls.trim().length() > 0) {
                        this.prefixes.add(ls.trim());
                    }
                }
            }
        }

    }

    /**
     * The Class ComponentHeatmap.
     */
    public static class ComponentHeatmap
        implements BasicComponent
    {

        /** The key. */
        public String key;

        /** The queries. */
        public MtasSpanQuery[] queries;

        /** The strategy. */
        public PrefixTreeStrategy strategy;

        /** The bounds shape. */
        public Shape boundsShape;

        /** The grid level. */
        public Integer gridLevel;

        /** The max cells. */
        public int maxCells;

        /** The data type. */
        public String dataType;

        /** The stats type. */
        public String statsType;

        /** The stats items. */
        public SortedSet<String> statsItems;

        /** The minimum long. */
        public Long minimumLong;

        /** The maximum long. */
        public Long maximumLong;

        /** The parser. */
        public MtasFunctionParserFunction parser;

        /** The Constant DEFAULT_MAX_CELLS. */
        private static final int DEFAULT_MAX_CELLS = 100000;

        /**
         * Instantiates a new component heatmap.
         *
         * @param key
         *            the key
         * @param queries
         *            the queries
         * @param minimumDouble
         *            the minimum double
         * @param maximumDouble
         *            the maximum double
         * @param type
         *            the type
         * @param functionKey
         *            the function key
         * @param functionExpression
         *            the function expression
         * @param functionType
         *            the function type
         * @param strategy
         *            the strategy
         * @param boundsShape
         *            the bounds shape
         * @param gridLevel
         *            the grid level
         * @param maxCells
         *            the max cells
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         * @throws ParseException
         *             the parse exception
         */
        public ComponentHeatmap(String key, MtasSpanQuery[] queries, Double minimumDouble,
                Double maximumDouble, String type, String[] functionKey,
                String[] functionExpression, String[] functionType, PrefixTreeStrategy strategy,
                Shape boundsShape, Integer gridLevel, Integer maxCells)
            throws IOException, ParseException
        {
            this.key = key;
            this.queries = (MtasSpanQuery[]) queries.clone();
            this.strategy = strategy;
            this.boundsShape = boundsShape;
            this.gridLevel = gridLevel;
            this.maxCells = maxCells == null ? DEFAULT_MAX_CELLS : maxCells;
            this.parser = new MtasFunctionParserFunctionDefault(queries.length);
            dataType = parser.getType();
            statsItems = CodecUtil.createStatsItems(type);
            statsType = CodecUtil.createStatsType(this.statsItems, null, parser);
            if (minimumDouble != null) {
                this.minimumLong = minimumDouble.longValue();
            }
            else {
                this.minimumLong = null;
            }
            if (maximumDouble != null) {
                this.maximumLong = maximumDouble.longValue();
            }
            else {
                this.maximumLong = null;
            }
        }

    }

    /**
     * The Class ComponentGroup.
     */
    public static class ComponentGroup
        implements BasicComponent
    {

        /** The span query. */
        public MtasSpanQuery spanQuery;

        /** The data type. */
        public String dataType;

        /** The stats type. */
        public String statsType;

        /** The sort type. */
        public String sortType;

        /** The sort direction. */
        public String sortDirection;

        /** The stats items. */
        public SortedSet<String> statsItems;

        /** The start. */
        public Integer start;

        /** The number. */
        public Integer number;

        /** The key. */
        public String key;

        /** The data collector. */
        public MtasDataCollector<?, ?> dataCollector;

        /** The prefixes. */
        ArrayList<String> prefixes;

        /** The hit inside. */
        HashSet<String> hitInside;

        /** The hit inside left. */
        HashSet<String>[] hitInsideLeft;

        /** The hit inside right. */
        HashSet<String>[] hitInsideRight;

        /** The hit left. */
        HashSet<String>[] hitLeft;

        /** The hit right. */
        HashSet<String>[] hitRight;

        /** The left. */
        HashSet<String>[] left;

        /** The right. */
        HashSet<String>[] right;

        /**
         * Instantiates a new component group.
         *
         * @param spanQuery
         *            the span query
         * @param key
         *            the key
         * @param number
         *            the number
         * @param start
         *            the start
         * @param groupingHitInsidePrefixes
         *            the grouping hit inside prefixes
         * @param groupingHitInsideLeftPosition
         *            the grouping hit inside left position
         * @param groupingHitInsideLeftPrefixes
         *            the grouping hit inside left prefixes
         * @param groupingHitInsideRightPosition
         *            the grouping hit inside right position
         * @param groupingHitInsideRightPrefixes
         *            the grouping hit inside right prefixes
         * @param groupingHitLeftPosition
         *            the grouping hit left position
         * @param groupingHitLeftPrefixes
         *            the grouping hit left prefixes
         * @param groupingHitRightPosition
         *            the grouping hit right position
         * @param groupingHitRightPrefixes
         *            the grouping hit right prefixes
         * @param groupingLeftPosition
         *            the grouping left position
         * @param groupingLeftPrefixes
         *            the grouping left prefixes
         * @param groupingRightPosition
         *            the grouping right position
         * @param groupingRightPrefixes
         *            the grouping right prefixes
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public ComponentGroup(MtasSpanQuery spanQuery, String key, int number, int start,
                String groupingHitInsidePrefixes, String[] groupingHitInsideLeftPosition,
                String[] groupingHitInsideLeftPrefixes, String[] groupingHitInsideRightPosition,
                String[] groupingHitInsideRightPrefixes, String[] groupingHitLeftPosition,
                String[] groupingHitLeftPrefixes, String[] groupingHitRightPosition,
                String[] groupingHitRightPrefixes, String[] groupingLeftPosition,
                String[] groupingLeftPrefixes, String[] groupingRightPosition,
                String[] groupingRightPrefixes)
            throws IOException
        {
            this.spanQuery = spanQuery;
            this.key = key;
            this.dataType = CodecUtil.DATA_TYPE_LONG;
            this.sortType = CodecUtil.STATS_TYPE_SUM;
            this.sortDirection = CodecUtil.SORT_DESC;
            this.statsItems = CodecUtil.createStatsItems("n,sum,mean");
            this.statsType = CodecUtil.createStatsType(this.statsItems, this.sortType, null);
            this.start = start;
            this.number = number;
            HashSet<String> tmpPrefixes = new HashSet<>();
            // analyze grouping condition
            if (groupingHitInsidePrefixes != null) {
                hitInside = new HashSet<>();
                String[] tmpList = groupingHitInsidePrefixes.split(",");
                for (String tmpItem : tmpList) {
                    if (!tmpItem.trim().isEmpty()) {
                        hitInside.add(tmpItem.trim());
                    }
                }
                tmpPrefixes.addAll(hitInside);
            }
            else {
                hitInside = null;
            }
            hitInsideLeft = createPositionedPrefixes(tmpPrefixes, groupingHitInsideLeftPosition,
                    groupingHitInsideLeftPrefixes);
            hitInsideRight = createPositionedPrefixes(tmpPrefixes, groupingHitInsideRightPosition,
                    groupingHitInsideRightPrefixes);
            hitLeft = createPositionedPrefixes(tmpPrefixes, groupingHitLeftPosition,
                    groupingHitLeftPrefixes);
            hitRight = createPositionedPrefixes(tmpPrefixes, groupingHitRightPosition,
                    groupingHitRightPrefixes);
            left = createPositionedPrefixes(tmpPrefixes, groupingLeftPosition,
                    groupingLeftPrefixes);
            right = createPositionedPrefixes(tmpPrefixes, groupingRightPosition,
                    groupingRightPrefixes);
            prefixes = new ArrayList<>(tmpPrefixes);
            // datacollector
            dataCollector = DataCollector.getCollector(DataCollector.COLLECTOR_TYPE_LIST,
                    this.dataType, this.statsType, this.statsItems, this.sortType,
                    this.sortDirection, this.start, this.number, null, null);
        }

        /**
         * Creates the positioned prefixes.
         *
         * @param prefixList
         *            the prefix list
         * @param position
         *            the position
         * @param prefixes
         *            the prefixes
         * @return the hash set[]
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        private static HashSet<String>[] createPositionedPrefixes(HashSet<String> prefixList,
                String[] position, String[] prefixes)
            throws IOException
        {
            Pattern p = Pattern.compile("^([0-9]+)(\\-([0-9]+))?$");
            Matcher m;
            if (position == null && prefixes == null) {
                return null;
            }
            else if (prefixes == null || position == null || position.length != prefixes.length) {
                throw new IOException("incorrect position/prefixes");
            }
            else if (position.length == 0) {
                return null;
            }
            else {
                // analyze positions
                int[][] tmpPosition = new int[position.length][];
                int maxPosition = -1;
                for (int i = 0; i < position.length; i++) {
                    m = p.matcher(position[i]);
                    if (m.find()) {
                        if (m.group(3) == null) {
                            int start = Integer.parseInt(m.group(1));
                            tmpPosition[i] = new int[] { start };
                            maxPosition = Math.max(maxPosition, start);
                        }
                        else {
                            int start = Integer.parseInt(m.group(1));
                            int end = Integer.parseInt(m.group(3));
                            if (start > end) {
                                throw new IOException("incorrect position " + position[i]);
                            }
                            else {
                                tmpPosition[i] = new int[end - start + 1];
                                for (int t = start; t <= end; t++) {
                                    tmpPosition[i][t - start] = t;
                                }
                                maxPosition = Math.max(maxPosition, end);
                            }
                        }
                    }
                    else {
                        throw new IOException("incorrect position " + position[i]);
                    }
                }
                @SuppressWarnings("unchecked")
                HashSet<String>[] result = new HashSet[maxPosition + 1];
                Arrays.fill(result, null);
                List<String> tmpPrefixList;
                String[] tmpList;
                for (int i = 0; i < tmpPosition.length; i++) {
                    tmpList = prefixes[i].split(",");
                    tmpPrefixList = new ArrayList<>();
                    for (String tmpItem : tmpList) {
                        if (!tmpItem.trim().isEmpty()) {
                            tmpPrefixList.add(tmpItem.trim());
                        }
                    }
                    if (tmpPrefixList.isEmpty()) {
                        throw new IOException("incorrect prefixes " + prefixes[i]);
                    }
                    for (int t = 0; t < tmpPosition[i].length; t++) {
                        if (result[tmpPosition[i][t]] == null) {
                            result[tmpPosition[i][t]] = new HashSet<>();
                        }
                        result[tmpPosition[i][t]].addAll(tmpPrefixList);
                    }
                    prefixList.addAll(tmpPrefixList);
                }
                return result;
            }
        }

    }

    /**
     * The Class ComponentFacet.
     */
    public static class ComponentFacet
        implements BasicComponent
    {

        /** The span queries. */
        public MtasSpanQuery[] spanQueries;

        /** The base fields. */
        public String[] baseFields;

        /** The base field types. */
        public String[] baseFieldTypes;

        /** The base types. */
        public String[] baseTypes;

        /** The base sort types. */
        public String[] baseSortTypes;

        /** The base sort directions. */
        public String[] baseSortDirections;

        /** The base range sizes. */
        public Double[] baseRangeSizes;

        /** The base range bases. */
        public Double[] baseRangeBases;

        /** The base collector types. */
        public String[] baseCollectorTypes;

        /** The base data types. */
        public String[] baseDataTypes;

        /** The base stats types. */
        public String[] baseStatsTypes;

        /** The base stats items. */
        public SortedSet<String>[] baseStatsItems;

        /** The key. */
        public String key;

        /** The data collector. */
        public MtasDataCollector<?, ?> dataCollector;

        /** The base function list. */
        public HashMap<MtasDataCollector<?, ?>, SubComponentFunction[]>[] baseFunctionList;

        /** The base numbers. */
        public Integer[] baseNumbers;

        /** The base minimum longs. */
        public Long[] baseMinimumLongs;

        /** The base maximum longs. */
        public Long[] baseMaximumLongs;

        /** The base parsers. */
        public MtasFunctionParserFunction[] baseParsers;

        /** The base function keys. */
        public String[][] baseFunctionKeys;

        /** The base function expressions. */
        public String[][] baseFunctionExpressions;

        /** The base function types. */
        public String[][] baseFunctionTypes;

        /** The base function parser functions. */
        public MtasFunctionParserFunction[][] baseFunctionParserFunctions;

        /** The Constant TYPE_STRING. */
        public static final String TYPE_STRING = "string";

        /** The Constant TYPE_POINTFIELD_WITHOUT_DOCVALUES. */
        public static final String TYPE_POINTFIELD_WITHOUT_DOCVALUES = "pointfield_without_docvalues";

        /**
         * Instantiates a new component facet.
         *
         * @param spanQueries
         *            the span queries
         * @param field
         *            the field
         * @param key
         *            the key
         * @param baseFields
         *            the base fields
         * @param baseFieldTypes
         *            the base field types
         * @param baseTypes
         *            the base types
         * @param baseRangeSizes
         *            the base range sizes
         * @param baseRangeBases
         *            the base range bases
         * @param baseSortTypes
         *            the base sort types
         * @param baseSortDirections
         *            the base sort directions
         * @param baseNumbers
         *            the base numbers
         * @param baseMinimumDoubles
         *            the base minimum doubles
         * @param baseMaximumDoubles
         *            the base maximum doubles
         * @param baseFunctionKeys
         *            the base function keys
         * @param baseFunctionExpressions
         *            the base function expressions
         * @param baseFunctionTypes
         *            the base function types
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         * @throws ParseException
         *             the parse exception
         */
        @SuppressWarnings("unchecked")
        public ComponentFacet(MtasSpanQuery[] spanQueries, String field, String key,
                String[] baseFields, String[] baseFieldTypes, String[] baseTypes,
                Double[] baseRangeSizes, Double[] baseRangeBases, String[] baseSortTypes,
                String[] baseSortDirections, Integer[] baseNumbers, Double[] baseMinimumDoubles,
                Double[] baseMaximumDoubles, String[][] baseFunctionKeys,
                String[][] baseFunctionExpressions, String[][] baseFunctionTypes)
            throws IOException, ParseException
        {
            this.spanQueries = (MtasSpanQuery[]) spanQueries.clone();
            this.key = key;
            this.baseFields = (String[]) baseFields.clone();
            this.baseFieldTypes = (String[]) baseFieldTypes.clone();
            this.baseTypes = (String[]) baseTypes.clone();
            this.baseRangeSizes = (Double[]) baseRangeSizes.clone();
            this.baseRangeBases = (Double[]) baseRangeBases.clone();
            this.baseSortTypes = (String[]) baseSortTypes.clone();
            this.baseSortDirections = (String[]) baseSortDirections.clone();
            this.baseNumbers = (Integer[]) baseNumbers.clone();
            // compute types
            this.baseMinimumLongs = new Long[baseFields.length];
            this.baseMaximumLongs = new Long[baseFields.length];
            this.baseCollectorTypes = new String[baseFields.length];
            this.baseStatsItems = new SortedSet[baseFields.length];
            this.baseStatsTypes = new String[baseFields.length];
            this.baseDataTypes = new String[baseFields.length];
            this.baseParsers = new MtasFunctionParserFunction[baseFields.length];
            this.baseFunctionList = new HashMap[baseFields.length];
            this.baseFunctionParserFunctions = new MtasFunctionParserFunction[baseFields.length][];
            for (int i = 0; i < baseFields.length; i++) {
                if (baseMinimumDoubles[i] != null) {
                    this.baseMinimumLongs[i] = baseMinimumDoubles[i].longValue();
                }
                else {
                    this.baseMinimumLongs[i] = null;
                }
                if (baseMaximumDoubles[i] != null) {
                    this.baseMaximumLongs[i] = baseMaximumDoubles[i].longValue();
                }
                else {
                    this.baseMaximumLongs[i] = null;
                }
                baseDataTypes[i] = CodecUtil.DATA_TYPE_LONG;
                baseFunctionList[i] = new HashMap<>();
                baseFunctionParserFunctions[i] = null;
                baseParsers[i] = new MtasFunctionParserFunctionDefault(this.spanQueries.length);
                if (this.baseSortDirections[i] == null) {
                    this.baseSortDirections[i] = CodecUtil.SORT_ASC;
                }
                else if (!this.baseSortDirections[i].equals(CodecUtil.SORT_ASC)
                        && !this.baseSortDirections[i].equals(CodecUtil.SORT_DESC)) {
                    throw new IOException(
                            "unrecognized sortDirection " + this.baseSortDirections[i]);
                }
                if (this.baseSortTypes[i] == null) {
                    this.baseSortTypes[i] = CodecUtil.SORT_TERM;
                }
                else if (!this.baseSortTypes[i].equals(CodecUtil.SORT_TERM)
                        && !CodecUtil.isStatsType(this.baseSortTypes[i])) {
                    throw new IOException("unrecognized sortType " + this.baseSortTypes[i]);
                }
                this.baseCollectorTypes[i] = DataCollector.COLLECTOR_TYPE_LIST;
                this.baseStatsItems[i] = CodecUtil.createStatsItems(this.baseTypes[i]);
                this.baseStatsTypes[i] = CodecUtil.createStatsType(baseStatsItems[i],
                        this.baseSortTypes[i], new MtasFunctionParserFunctionDefault(1));
            }
            boolean doFunctions;
            doFunctions = baseFunctionKeys != null && baseFunctionExpressions != null
                    && baseFunctionTypes != null;
            doFunctions = doFunctions ? baseFunctionKeys.length == baseFields.length : false;
            doFunctions = doFunctions ? baseFunctionTypes.length == baseFields.length : false;
            if (doFunctions) {
                this.baseFunctionKeys = new String[baseFields.length][];
                this.baseFunctionExpressions = new String[baseFields.length][];
                this.baseFunctionTypes = new String[baseFields.length][];
                for (int i = 0; i < baseFields.length; i++) {
                    if (baseFunctionKeys[i].length == baseFunctionExpressions[i].length
                            && baseFunctionKeys[i].length == baseFunctionTypes[i].length) {
                        this.baseFunctionKeys[i] = new String[baseFunctionKeys[i].length];
                        this.baseFunctionExpressions[i] = new String[baseFunctionExpressions[i].length];
                        this.baseFunctionTypes[i] = new String[baseFunctionTypes[i].length];
                        baseFunctionParserFunctions[i] = new MtasFunctionParserFunction[baseFunctionExpressions[i].length];
                        for (int j = 0; j < baseFunctionKeys[i].length; j++) {
                            this.baseFunctionKeys[i][j] = baseFunctionKeys[i][j];
                            this.baseFunctionExpressions[i][j] = baseFunctionExpressions[i][j];
                            this.baseFunctionTypes[i][j] = baseFunctionTypes[i][j];
                            baseFunctionParserFunctions[i][j] = new MtasFunctionParser(
                                    new BufferedReader(
                                            new StringReader(baseFunctionExpressions[i][j])))
                                                    .parse();
                        }
                    }
                    else {
                        this.baseFunctionKeys[i] = new String[0];
                        this.baseFunctionExpressions[i] = new String[0];
                        this.baseFunctionTypes[i] = new String[0];
                        baseFunctionParserFunctions[i] = new MtasFunctionParserFunction[0];
                    }
                }
            }
            if (baseFields.length > 0) {
                if (baseFields.length == 1) {
                    dataCollector = DataCollector.getCollector(this.baseCollectorTypes[0],
                            this.baseDataTypes[0], this.baseStatsTypes[0], this.baseStatsItems[0],
                            this.baseSortTypes[0], this.baseSortDirections[0], 0,
                            this.baseNumbers[0], null, null);
                }
                else {
                    String[] subBaseCollectorTypes = Arrays.copyOfRange(baseCollectorTypes, 1,
                            baseDataTypes.length);
                    String[] subBaseDataTypes = Arrays.copyOfRange(baseDataTypes, 1,
                            baseDataTypes.length);
                    String[] subBaseStatsTypes = Arrays.copyOfRange(baseStatsTypes, 1,
                            baseStatsTypes.length);
                    SortedSet<String>[] subBaseStatsItems = Arrays.copyOfRange(baseStatsItems, 1,
                            baseStatsItems.length);
                    String[] subBaseSortTypes = Arrays.copyOfRange(baseSortTypes, 1,
                            baseSortTypes.length);
                    String[] subBaseSortDirections = Arrays.copyOfRange(baseSortDirections, 1,
                            baseSortDirections.length);
                    Integer[] subNumbers = Arrays.copyOfRange(baseNumbers, 1, baseNumbers.length);
                    Integer[] subStarts = Arrays.stream(new int[subNumbers.length]).boxed()
                            .toArray(Integer[]::new);

                    dataCollector = DataCollector.getCollector(this.baseCollectorTypes[0],
                            this.baseDataTypes[0], this.baseStatsTypes[0], this.baseStatsItems[0],
                            this.baseSortTypes[0], this.baseSortDirections[0], 0,
                            this.baseNumbers[0], subBaseCollectorTypes, subBaseDataTypes,
                            subBaseStatsTypes, subBaseStatsItems, subBaseSortTypes,
                            subBaseSortDirections, subStarts, subNumbers, null, null);
                }
            }
            else {
                throw new IOException("no baseFields");
            }
        }

        /**
         * Function sum rule.
         *
         * @return true, if successful
         */
        public boolean functionSumRule()
        {
            if (baseFunctionParserFunctions != null) {
                for (int i = 0; i < baseFields.length; i++) {
                    for (MtasFunctionParserFunction function : baseFunctionParserFunctions[i]) {
                        if (!function.sumRule()) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        /**
         * Function need positions.
         *
         * @return true, if successful
         */
        public boolean functionNeedPositions()
        {
            if (baseFunctionParserFunctions != null) {
                for (int i = 0; i < baseFields.length; i++) {
                    for (MtasFunctionParserFunction function : baseFunctionParserFunctions[i]) {
                        if (function.needPositions()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /**
         * Base parser sum rule.
         *
         * @return true, if successful
         */
        public boolean baseParserSumRule()
        {
            for (int i = 0; i < baseFields.length; i++) {
                if (!baseParsers[i].sumRule()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Base parser need positions.
         *
         * @return true, if successful
         */
        public boolean baseParserNeedPositions()
        {
            for (int i = 0; i < baseFields.length; i++) {
                if (baseParsers[i].needPositions()) {
                    return true;
                }
            }
            return false;
        }

    }

    /**
     * The Class ComponentTermVector.
     */
    public static class ComponentTermVector
        implements BasicComponent
    {

        /** The key. */
        public String key;

        /** The prefix. */
        public String prefix;

        /** The distances. */
        public List<SubComponentDistance> distances;

        /** The regexp. */
        public String regexp;

        /** The ignore regexp. */
        public String ignoreRegexp;

        /** The boundary. */
        public String boundary;

        /** The full. */
        public boolean full;

        /** The list. */
        public Set<String> list;

        /** The ignore list. */
        public Set<String> ignoreList;

        /** The list regexp. */
        public boolean listRegexp;

        /** The ignore list regexp. */
        public boolean ignoreListRegexp;

        /** The functions. */
        public List<SubComponentFunction> functions;

        /** The number. */
        public int number;

        /** The start value. */
        public BytesRef startValue;

        /** The sub component function. */
        public SubComponentFunction subComponentFunction;

        /** The boundary registration. */
        public boolean boundaryRegistration;

        /** The sort type. */
        public String sortType;

        /** The sort direction. */
        public String sortDirection;

        /**
         * Instantiates a new component term vector.
         *
         * @param key
         *            the key
         * @param prefix
         *            the prefix
         * @param distanceKey
         *            the distance key
         * @param distanceType
         *            the distance type
         * @param distanceBase
         *            the distance base
         * @param distanceParameter
         *            the distance parameter
         * @param distanceMinimum
         *            the distance minimum
         * @param distanceMaximum
         *            the distance maximum
         * @param regexp
         *            the regexp
         * @param full
         *            the full
         * @param type
         *            the type
         * @param sortType
         *            the sort type
         * @param sortDirection
         *            the sort direction
         * @param startValue
         *            the start value
         * @param number
         *            the number
         * @param functionKey
         *            the function key
         * @param functionExpression
         *            the function expression
         * @param functionType
         *            the function type
         * @param boundary
         *            the boundary
         * @param list
         *            the list
         * @param listRegexp
         *            the list regexp
         * @param ignoreRegexp
         *            the ignore regexp
         * @param ignoreList
         *            the ignore list
         * @param ignoreListRegexp
         *            the ignore list regexp
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         * @throws ParseException
         *             the parse exception
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ComponentTermVector(String key, String prefix, String[] distanceKey,
                String[] distanceType, String[] distanceBase, Map[] distanceParameter,
                String[] distanceMinimum, String[] distanceMaximum, String regexp, Boolean full,
                String type, String sortType, String sortDirection, String startValue, int number,
                String[] functionKey, String[] functionExpression, String[] functionType,
                String boundary, String[] list, Boolean listRegexp, String ignoreRegexp,
                String[] ignoreList, Boolean ignoreListRegexp)
            throws IOException, ParseException
        {
            this.key = key;
            this.prefix = prefix;
            distances = new ArrayList<>();
            if (distanceKey != null && distanceType != null && distanceBase != null
                    && distanceParameter != null && distanceMaximum != null) {
                if (distanceKey.length == distanceType.length
                        && distanceKey.length == distanceBase.length
                        && distanceKey.length == distanceParameter.length
                        && distanceKey.length == distanceMaximum.length) {
                    for (int i = 0; i < distanceKey.length; i++) {
                        SubComponentDistance item = new SubComponentDistance(distanceKey[i],
                                distanceType[i], this.prefix, distanceBase[i], distanceParameter[i],
                                distanceMinimum[i], distanceMaximum[i]);
                        distances.add(item);
                    }
                }
            }
            this.regexp = regexp;
            this.full = (full != null && full) ? true : false;
            if (sortType == null) {
                this.sortType = CodecUtil.SORT_TERM;
            }
            else {
                this.sortType = sortType;
            }
            if (sortDirection == null) {
                if (this.sortType.equals(CodecUtil.SORT_TERM)) {
                    this.sortDirection = CodecUtil.SORT_ASC;
                }
                else {
                    this.sortDirection = CodecUtil.SORT_DESC;
                }
            }
            else {
                this.sortDirection = sortDirection;
            }
            if (list != null && list.length > 0) {
                this.list = new HashSet(Arrays.asList(list));
                this.listRegexp = listRegexp != null ? listRegexp : false;
                this.boundary = null;
                this.number = Integer.MAX_VALUE;
                if (!this.full) {
                    this.sortType = CodecUtil.SORT_TERM;
                    this.sortDirection = CodecUtil.SORT_ASC;
                }
            }
            else {
                this.list = null;
                this.listRegexp = false;
                this.startValue = (startValue != null)
                        ? new BytesRef(prefix + MtasToken.DELIMITER + startValue)
                        : null;
                if (boundary == null) {
                    this.boundary = null;
                    if (number < -1) {
                        throw new IOException("number should not be " + number);
                    }
                    else if (number >= 0) {
                        this.number = number;
                    }
                    else {
                        if (!full) {
                            throw new IOException(
                                    "number " + number + " only supported for full termvector");
                        }
                        else {
                            this.number = Integer.MAX_VALUE;
                        }
                    }
                }
                else {
                    this.boundary = boundary;
                    this.number = Integer.MAX_VALUE;
                }
            }
            this.ignoreRegexp = ignoreRegexp;
            if (ignoreList != null && ignoreList.length > 0) {
                this.ignoreList = new HashSet(Arrays.asList(ignoreList));
                this.ignoreListRegexp = ignoreListRegexp != null ? ignoreListRegexp : false;
            }
            else {
                this.ignoreList = null;
                this.ignoreListRegexp = false;
            }
            functions = new ArrayList<>();
            if (functionKey != null && functionExpression != null && functionType != null) {
                if (functionKey.length == functionExpression.length
                        && functionKey.length == functionType.length) {
                    for (int i = 0; i < functionKey.length; i++) {
                        functions.add(new SubComponentFunction(DataCollector.COLLECTOR_TYPE_LIST,
                                functionKey[i], functionExpression[i], functionType[i]));
                    }
                }
            }
            if (!this.sortType.equals(CodecUtil.SORT_TERM)
                    && !CodecUtil.isStatsType(this.sortType)) {
                throw new IOException("unknown sortType '" + this.sortType + "'");
            }
            else if (!full && !this.sortType.equals(CodecUtil.SORT_TERM)) {
                if (!(this.sortType.equals(CodecUtil.STATS_TYPE_SUM)
                        || this.sortType.equals(CodecUtil.STATS_TYPE_N))) {
                    throw new IOException(
                            "sortType '" + this.sortType + "' only supported with full termVector");
                }
            }
            if (!this.sortType.equals(CodecUtil.SORT_TERM)) {
                if (startValue != null) {
                    throw new IOException("startValue '" + startValue
                            + "' only supported with termVector sorted on " + CodecUtil.SORT_TERM);
                }
            }
            if (!this.sortDirection.equals(CodecUtil.SORT_ASC)
                    && !this.sortDirection.equals(CodecUtil.SORT_DESC)) {
                throw new IOException("unrecognized sortDirection '" + this.sortDirection + "'");
            }
            boundaryRegistration = this.boundary != null;
            String segmentRegistration = null;
            if (this.full) {
                this.boundary = null;
                segmentRegistration = null;
            }
            else if (this.boundary != null) {
                if (this.sortDirection.equals(CodecUtil.SORT_ASC)) {
                    segmentRegistration = MtasDataCollector.SEGMENT_BOUNDARY_ASC;
                }
                else if (this.sortDirection.equals(CodecUtil.SORT_DESC)) {
                    segmentRegistration = MtasDataCollector.SEGMENT_BOUNDARY_DESC;
                }
            }
            else if (!this.sortType.equals(CodecUtil.SORT_TERM)) {
                if (this.sortDirection.equals(CodecUtil.SORT_ASC)) {
                    segmentRegistration = MtasDataCollector.SEGMENT_SORT_ASC;
                }
                else if (this.sortDirection.equals(CodecUtil.SORT_DESC)) {
                    segmentRegistration = MtasDataCollector.SEGMENT_SORT_DESC;
                }
            }
            // create main subComponentFunction
            this.subComponentFunction = new SubComponentFunction(DataCollector.COLLECTOR_TYPE_LIST,
                    key, type, new MtasFunctionParserFunctionDefault(1), this.sortType,
                    this.sortDirection, 0, this.number, segmentRegistration, boundary);
        }

        /**
         * Function sum rule.
         *
         * @return true, if successful
         */
        public boolean functionSumRule()
        {
            if (functions != null) {
                for (SubComponentFunction function : functions) {
                    if (!function.parserFunction.sumRule()) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * Function need positions.
         *
         * @return true, if successful
         */
        public boolean functionNeedPositions()
        {
            if (functions != null) {
                for (SubComponentFunction function : functions) {
                    if (function.parserFunction.needPositions()) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    /**
     * The Class ComponentStatus.
     */
    public static class ComponentStatus
        implements BasicComponent
    {

        /** The handler. */
        public String handler;

        /** The name. */
        public String name;

        /** The key. */
        public String key;

        /** The number of documents. */
        public Integer numberOfDocuments;

        /** The number of segments. */
        public Integer numberOfSegments;

        /** The get mtas handler. */
        public boolean getMtasHandler;

        /** The get number of documents. */
        public boolean getNumberOfDocuments;

        /** The get number of segments. */
        public boolean getNumberOfSegments;

        /**
         * Instantiates a new component status.
         *
         * @param name
         *            the name
         * @param key
         *            the key
         * @param getMtasHandler
         *            the get mtas handler
         * @param getNumberOfDocuments
         *            the get number of documents
         * @param getNumberOfSegments
         *            the get number of segments
         */
        public ComponentStatus(String name, String key, boolean getMtasHandler,
                boolean getNumberOfDocuments, boolean getNumberOfSegments)
        {
            this.name = Objects.requireNonNull(name, "no name");
            this.key = key;
            this.getMtasHandler = getMtasHandler;
            this.getNumberOfDocuments = getNumberOfDocuments;
            this.getNumberOfSegments = getNumberOfSegments;
            handler = null;
            numberOfDocuments = null;
            numberOfSegments = null;
        }

    }

    /**
     * The Class ComponentVersion.
     */
    public static class ComponentVersion
        implements BasicComponent
    {

        /**
         * Instantiates a new component version.
         */
        public ComponentVersion()
        {

        }
    }

    /**
     * The Interface ComponentStats.
     */
    public abstract static interface ComponentStats
        extends BasicComponent
    {
    }

    /**
     * The Class ComponentSpan.
     */
    public static class ComponentSpan
        implements ComponentStats
    {

        /** The queries. */
        public MtasSpanQuery[] queries;

        /** The key. */
        public String key;

        /** The data type. */
        public String dataType;

        /** The stats type. */
        public String statsType;

        /** The stats items. */
        public SortedSet<String> statsItems;

        /** The minimum long. */
        public Long minimumLong;

        /** The maximum long. */
        public Long maximumLong;

        /** The data collector. */
        public MtasDataCollector<?, ?> dataCollector;

        /** The functions. */
        public List<SubComponentFunction> functions;

        /** The parser. */
        public MtasFunctionParserFunction parser;

        /**
         * Instantiates a new component span.
         *
         * @param queries
         *            the queries
         * @param key
         *            the key
         * @param minimumDouble
         *            the minimum double
         * @param maximumDouble
         *            the maximum double
         * @param type
         *            the type
         * @param functionKey
         *            the function key
         * @param functionExpression
         *            the function expression
         * @param functionType
         *            the function type
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         * @throws ParseException
         *             the parse exception
         */
        public ComponentSpan(MtasSpanQuery[] queries, String key, Double minimumDouble,
                Double maximumDouble, String type, String[] functionKey,
                String[] functionExpression, String[] functionType)
            throws IOException, ParseException
        {
            this.queries = (MtasSpanQuery[]) queries.clone();
            this.key = key;
            functions = new ArrayList<>();
            if (functionKey != null && functionExpression != null && functionType != null) {
                if (functionKey.length == functionExpression.length
                        && functionKey.length == functionType.length) {
                    for (int i = 0; i < functionKey.length; i++) {
                        functions.add(new SubComponentFunction(DataCollector.COLLECTOR_TYPE_DATA,
                                functionKey[i], functionExpression[i], functionType[i]));
                    }
                }
            }
            parser = new MtasFunctionParserFunctionDefault(queries.length);
            dataType = parser.getType();
            statsItems = CodecUtil.createStatsItems(type);
            statsType = CodecUtil.createStatsType(this.statsItems, null, parser);
            if (minimumDouble != null) {
                this.minimumLong = minimumDouble.longValue();
            }
            else {
                this.minimumLong = null;
            }
            if (maximumDouble != null) {
                this.maximumLong = maximumDouble.longValue();
            }
            else {
                this.maximumLong = null;
            }
            dataCollector = DataCollector.getCollector(DataCollector.COLLECTOR_TYPE_DATA, dataType,
                    this.statsType, this.statsItems, null, null, null, null, null, null);
        }

        /**
         * Function sum rule.
         *
         * @return true, if successful
         */
        public boolean functionSumRule()
        {
            if (functions != null) {
                for (SubComponentFunction function : functions) {
                    if (!function.parserFunction.sumRule()) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * Function basic.
         *
         * @return true, if successful
         */
        public boolean functionBasic()
        {
            if (functions != null) {
                for (SubComponentFunction function : functions) {
                    if (!function.statsType.equals(CodecUtil.STATS_BASIC)) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * Function need positions.
         *
         * @return true, if successful
         */
        public boolean functionNeedPositions()
        {
            if (functions != null) {
                for (SubComponentFunction function : functions) {
                    if (function.parserFunction.needPositions()) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Function need arguments.
         *
         * @return the sets the
         */
        public Set<Integer> functionNeedArguments()
        {
            Set<Integer> list = new HashSet<>();
            if (functions != null) {
                for (SubComponentFunction function : functions) {
                    list.addAll(function.parserFunction.needArgument());
                }
            }
            return list;
        }

    }

    /**
     * The Class ComponentPosition.
     */
    public static class ComponentPosition
        implements ComponentStats
    {

        /** The key. */
        public String key;

        /** The data type. */
        public String dataType;

        /** The stats type. */
        public String statsType;

        /** The stats items. */
        public SortedSet<String> statsItems;

        /** The minimum long. */
        public Long minimumLong;

        /** The maximum long. */
        public Long maximumLong;

        /** The data collector. */
        public MtasDataCollector<?, ?> dataCollector;

        /**
         * Instantiates a new component position.
         *
         * @param key
         *            the key
         * @param minimumDouble
         *            the minimum double
         * @param maximumDouble
         *            the maximum double
         * @param statsType
         *            the stats type
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         * @throws ParseException
         *             the parse exception
         */
        public ComponentPosition(String key, Double minimumDouble, Double maximumDouble,
                String statsType)
            throws IOException, ParseException
        {
            this.key = key;
            dataType = CodecUtil.DATA_TYPE_LONG;
            this.statsItems = CodecUtil.createStatsItems(statsType);
            this.statsType = CodecUtil.createStatsType(this.statsItems, null, null);
            if (minimumDouble != null) {
                this.minimumLong = minimumDouble.longValue();
            }
            else {
                this.minimumLong = null;
            }
            if (maximumDouble != null) {
                this.maximumLong = maximumDouble.longValue();
            }
            else {
                this.maximumLong = null;
            }
            dataCollector = DataCollector.getCollector(DataCollector.COLLECTOR_TYPE_DATA, dataType,
                    this.statsType, this.statsItems, null, null, null, null, null, null);
        }
    }

    /**
     * The Class ComponentToken.
     */
    public static class ComponentToken
        implements ComponentStats
    {

        /** The key. */
        public String key;

        /** The data type. */
        public String dataType;

        /** The stats type. */
        public String statsType;

        /** The stats items. */
        public SortedSet<String> statsItems;

        /** The minimum long. */
        public Long minimumLong;

        /** The maximum long. */
        public Long maximumLong;

        /** The data collector. */
        public MtasDataCollector<?, ?> dataCollector;

        /**
         * Instantiates a new component token.
         *
         * @param key
         *            the key
         * @param minimumDouble
         *            the minimum double
         * @param maximumDouble
         *            the maximum double
         * @param statsType
         *            the stats type
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         * @throws ParseException
         *             the parse exception
         */
        public ComponentToken(String key, Double minimumDouble, Double maximumDouble,
                String statsType)
            throws IOException, ParseException
        {
            this.key = key;
            dataType = CodecUtil.DATA_TYPE_LONG;
            this.statsItems = CodecUtil.createStatsItems(statsType);
            this.statsType = CodecUtil.createStatsType(this.statsItems, null, null);
            if (minimumDouble != null) {
                this.minimumLong = minimumDouble.longValue();
            }
            else {
                this.minimumLong = null;
            }
            if (maximumDouble != null) {
                this.maximumLong = maximumDouble.longValue();
            }
            else {
                this.maximumLong = null;
            }
            dataCollector = DataCollector.getCollector(DataCollector.COLLECTOR_TYPE_DATA, dataType,
                    this.statsType, this.statsItems, null, null, null, null, null, null);
        }
    }

    /**
     * The Class ComponentCollection.
     */
    public static class ComponentCollection
        implements BasicComponent
    {

        /** The Constant ACTION_CREATE. */
        public static final String ACTION_CREATE = "create";

        /** The Constant ACTION_CHECK. */
        public static final String ACTION_CHECK = "check";

        /** The Constant ACTION_LIST. */
        public static final String ACTION_LIST = "list";

        /** The Constant ACTION_POST. */
        public static final String ACTION_POST = "post";

        /** The Constant ACTION_IMPORT. */
        public static final String ACTION_IMPORT = "import";

        /** The Constant ACTION_DELETE. */
        public static final String ACTION_DELETE = "delete";

        /** The Constant ACTION_EMPTY. */
        public static final String ACTION_EMPTY = "empty";

        /** The Constant ACTION_GET. */
        public static final String ACTION_GET = "get";

        /** The key. */
        public String key;

        /** The version. */
        public String version;

        /** The original version. */
        public String originalVersion;

        /** The id. */
        public String id;

        /** The action. */
        private String action;

        /** The fields. */
        private Set<String> fields;

        /** The values. */
        private HashSet<String> values;

        /**
         * Instantiates a new component collection.
         *
         * @param key
         *            the key
         * @param action
         *            the action
         */
        public ComponentCollection(String key, String action)
        {
            this.key = key;
            this.action = action;
            this.version = null;
            this.originalVersion = null;
            values = new HashSet<>();
        }

        /**
         * Sets the list variables.
         *
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public void setListVariables() throws IOException
        {
            if (action.equals(ACTION_LIST)) {
                // do nothing
            }
            else {
                throw new IOException("not allowed with action " + action);
            }
        }

        /**
         * Sets the create variables.
         *
         * @param id
         *            the id
         * @param fields
         *            the fields
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public void setCreateVariables(String id, Set<String> fields) throws IOException
        {
            if (action.equals(ACTION_CREATE)) {
                this.id = id;
                this.fields = fields;
            }
            else {
                throw new IOException("not allowed with action " + action);
            }
        }

        /**
         * Sets the check variables.
         *
         * @param id
         *            the new check variables
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public void setCheckVariables(String id) throws IOException
        {
            if (action.equals(ACTION_CHECK)) {
                this.id = id;
            }
            else {
                throw new IOException("not allowed with action " + action);
            }
        }

        /**
         * Sets the gets the variables.
         *
         * @param id
         *            the new gets the variables
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public void setGetVariables(String id) throws IOException
        {
            if (action.equals(ACTION_GET)) {
                this.id = id;
            }
            else {
                throw new IOException("not allowed with action " + action);
            }
        }

        /**
         * Sets the post variables.
         *
         * @param id
         *            the id
         * @param values
         *            the values
         * @param originalVersion
         *            the original version
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public void setPostVariables(String id, HashSet<String> values, String originalVersion)
            throws IOException
        {
            if (action.equals(ACTION_POST)) {
                this.id = id;
                this.values = values;
                this.originalVersion = originalVersion;
            }
            else {
                throw new IOException("not allowed with action " + action);
            }
        }

        private String toString(InputStreamReader in) throws IOException
        {
            char[] arr = new char[8 * 1024];
            StringBuilder buffer = new StringBuilder();
            int numCharsRead;
            while ((numCharsRead = in.read(arr, 0, arr.length)) != -1) {
                buffer.append(arr, 0, numCharsRead);
            }
            in.close();
            return (buffer.toString());
        }

        /**
         * Sets the delete variables.
         *
         * @param id
         *            the new delete variables
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public void setDeleteVariables(String id) throws IOException
        {
            if (action.equals(ACTION_DELETE)) {
                this.id = id;
            }
            else {
                throw new IOException("not allowed with action " + action);
            }
        }

        /**
         * Action.
         *
         * @return the string
         */
        public String action()
        {
            return action;
        }

        /**
         * Original version.
         *
         * @return the string
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public String originalVersion() throws IOException
        {
            if (action.equals(ACTION_POST)) {
                return originalVersion;
            }
            else {
                throw new IOException("unexpected call for " + action);
            }
        }

        /**
         * Values.
         *
         * @return the hash set
         */
        public HashSet<String> values()
        {
            return values;
        }

        /**
         * Fields.
         *
         * @return the sets the
         */
        public Set<String> fields()
        {
            return fields;
        }

        /**
         * Adds the value.
         *
         * @param value
         *            the value
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public void addValue(String value) throws IOException
        {
            if (action.equals(ACTION_CREATE)) {
                if (version == null) {
                    values.add(value);
                }
                else {
                    throw new IOException("version already set");
                }
            }
            else {
                throw new IOException("not allowed for action '" + action + "'");
            }
        }

    }

    /**
     * The Class SubComponentDistance.
     */
    public static class SubComponentDistance
        implements Serializable
    {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        /** The key. */
        public String key;

        /** The type. */
        public String type;

        /** The base. */
        public String base;

        /** The prefix. */
        public String prefix;

        /** The minimum. */
        public Double minimum;

        /** The maximum. */
        public Double maximum;

        /** The parameters. */
        public Map<String, String> parameters;

        /** The distance. */
        public transient Distance distance = null;

        /** The Constant NAME_LEVENSHTEIN. */
        private static final String NAME_LEVENSHTEIN = "levenshtein";

        /** The Constant NAME_DAMERAULEVENSHTEIN. */
        private static final String NAME_DAMERAULEVENSHTEIN = "damerau-levenshtein";

        /** The Constant NAME_MORSE. */
        private static final String NAME_MORSE = "morse";

        /**
         * Instantiates a new sub component distance.
         *
         * @param key
         *            the key
         * @param type
         *            the type
         * @param prefix
         *            the prefix
         * @param base
         *            the base
         * @param parameters
         *            the parameters
         * @param minimum
         *            the minimum
         * @param maximum
         *            the maximum
         */
        public SubComponentDistance(String key, String type, String prefix, String base,
                Map<String, String> parameters, String minimum, String maximum)
        {
            this.key = key;
            this.prefix = prefix;
            this.type = type;
            this.base = base;
            this.parameters = parameters;
            this.minimum = minimum != null ? Double.parseDouble(minimum) : null;
            this.maximum = maximum != null ? Double.parseDouble(maximum) : null;
        }

        /**
         * Gets the distance.
         *
         * @return the distance
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public Distance getDistance() throws IOException
        {
            if (distance == null) {
                if (type != null) {
                    try {
                        Constructor<Distance> constructor = (Constructor<Distance>) Class.forName(
                                "mtas.codec.util.distance." + createClassName(type) + "Distance")
                                .getConstructor(String.class, String.class, Double.class,
                                        Double.class, Map.class);
                        distance = constructor.newInstance(prefix, base, minimum, maximum,
                                parameters);
                    }
                    catch (ClassNotFoundException | NoSuchMethodException | SecurityException
                            | InstantiationException | IllegalAccessException
                            | IllegalArgumentException | InvocationTargetException e) {
                        throw new IllegalStateException(e);
                    }
                    // distance = new MorseDistance(prefix, base, maximum, parameters);
                }
                else {
                    throw new IOException("unrecognized distance " + type);
                }
            }
            return distance;
        }

        /**
         * Creates the class name.
         *
         * @param type
         *            the type
         * @return the string
         */
        private String createClassName(String type)
        {
            final char DASH = '-';
            Objects.requireNonNull(type, "Type is obligatory");
            final StringBuilder output = new StringBuilder(type.length());
            boolean lastCharacterWasDash = true;
            boolean thisCharacterWasDash;
            for (final char currentCharacter : type.toCharArray()) {
                thisCharacterWasDash = (currentCharacter == DASH);
                if (!thisCharacterWasDash) {
                    if (lastCharacterWasDash) {
                        output.append(Character.toTitleCase(currentCharacter));
                    }
                    else {
                        output.append(currentCharacter);
                    }
                }
                lastCharacterWasDash = thisCharacterWasDash;
            }
            return output.toString();
        }

    }

    /**
     * The Class SubComponentFunction.
     */
    public static class SubComponentFunction
    {

        /** The key. */
        public String key;

        /** The expression. */
        public String expression;

        /** The type. */
        public String type;

        /** The parser function. */
        public MtasFunctionParserFunction parserFunction;

        /** The stats type. */
        public String statsType;

        /** The data type. */
        public String dataType;

        /** The sort type. */
        public String sortType;

        /** The sort direction. */
        public String sortDirection;

        /** The stats items. */
        public SortedSet<String> statsItems;

        /** The data collector. */
        public MtasDataCollector<?, ?> dataCollector;

        /**
         * Instantiates a new sub component function.
         *
         * @param collectorType
         *            the collector type
         * @param key
         *            the key
         * @param type
         *            the type
         * @param parserFunction
         *            the parser function
         * @param sortType
         *            the sort type
         * @param sortDirection
         *            the sort direction
         * @param start
         *            the start
         * @param number
         *            the number
         * @param segmentRegistration
         *            the segment registration
         * @param boundary
         *            the boundary
         * @throws ParseException
         *             the parse exception
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public SubComponentFunction(String collectorType, String key, String type,
                MtasFunctionParserFunction parserFunction, String sortType, String sortDirection,
                Integer start, Integer number, String segmentRegistration, String boundary)
            throws ParseException, IOException
        {
            this.key = key;
            this.expression = null;
            this.type = type;
            this.parserFunction = parserFunction;
            this.sortType = sortType;
            this.sortDirection = sortDirection;
            this.dataType = parserFunction.getType();
            this.statsItems = CodecUtil.createStatsItems(this.type);
            this.statsType = CodecUtil.createStatsType(statsItems, sortType, parserFunction);
            if (collectorType.equals(DataCollector.COLLECTOR_TYPE_LIST)) {
                dataCollector = DataCollector.getCollector(DataCollector.COLLECTOR_TYPE_LIST,
                        dataType, statsType, statsItems, sortType, sortDirection, start, number,
                        null, null, null, null, null, null, null, null, segmentRegistration,
                        boundary);
            }
            else if (collectorType.equals(DataCollector.COLLECTOR_TYPE_DATA)) {
                dataCollector = DataCollector.getCollector(DataCollector.COLLECTOR_TYPE_DATA,
                        dataType, statsType, statsItems, sortType, sortDirection, start, number,
                        segmentRegistration, boundary);
            }
        }

        /**
         * Instantiates a new sub component function.
         *
         * @param collectorType
         *            the collector type
         * @param key
         *            the key
         * @param expression
         *            the expression
         * @param type
         *            the type
         * @throws ParseException
         *             the parse exception
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public SubComponentFunction(String collectorType, String key, String expression,
                String type)
            throws ParseException, IOException
        {
            this.key = key;
            this.expression = expression;
            this.type = type;
            this.sortType = null;
            this.sortDirection = null;
            parserFunction = new MtasFunctionParser(
                    new BufferedReader(new StringReader(this.expression))).parse();
            dataType = parserFunction.getType();
            statsItems = CodecUtil.createStatsItems(this.type);
            statsType = CodecUtil.createStatsType(statsItems, null, parserFunction);
            if (collectorType.equals(DataCollector.COLLECTOR_TYPE_LIST)) {
                dataCollector = DataCollector.getCollector(DataCollector.COLLECTOR_TYPE_LIST,
                        dataType, statsType, statsItems, sortType, sortDirection, 0,
                        Integer.MAX_VALUE, null, null);
            }
            else if (collectorType.equals(DataCollector.COLLECTOR_TYPE_DATA)) {
                dataCollector = DataCollector.getCollector(DataCollector.COLLECTOR_TYPE_DATA,
                        dataType, statsType, statsItems, sortType, sortDirection, null, null, null,
                        null);
            }
        }
    }

    /**
     * The Class KwicToken.
     */
    public static class KwicToken
    {

        /** The start position. */
        public int startPosition;

        /** The end position. */
        public int endPosition;

        /** The tokens. */
        public List<MtasTokenString> tokens;

        /**
         * Instantiates a new kwic token.
         *
         * @param match
         *            the match
         * @param tokens
         *            the tokens
         */
        public KwicToken(Match match, List<MtasTokenString> tokens)
        {
            startPosition = match.startPosition;
            endPosition = match.endPosition - 1;
            this.tokens = tokens;
        }

    }

    /**
     * The Class KwicHit.
     */
    public static class KwicHit
    {

        /** The start position. */
        public int startPosition;

        /** The end position. */
        public int endPosition;

        /** The hits. */
        public Map<Integer, List<String>> hits;

        /**
         * Instantiates a new kwic hit.
         *
         * @param match
         *            the match
         * @param hits
         *            the hits
         */
        public KwicHit(Match match, Map<Integer, List<String>> hits)
        {
            startPosition = match.startPosition;
            endPosition = match.endPosition - 1;
            this.hits = hits;
        }
    }

    /**
     * The Class GroupHit.
     */
    public static class GroupHit
    {

        /** The hash. */
        private int hash;

        /** The hash left. */
        private int hashLeft;

        /** The hash hit. */
        private int hashHit;

        /** The hash right. */
        private int hashRight;

        /** The key. */
        private String key;

        /** The key left. */
        private String keyLeft;

        /** The key hit. */
        private String keyHit;

        /** The key right. */
        private String keyRight;

        /** The data hit. */
        public List<String>[] dataHit;

        /** The data left. */
        public List<String>[] dataLeft;

        /** The data right. */
        public List<String>[] dataRight;

        /** The missing hit. */
        public Set<String>[] missingHit;

        /** The missing left. */
        public Set<String>[] missingLeft;

        /** The missing right. */
        public Set<String>[] missingRight;

        /** The unknown hit. */
        public Set<String>[] unknownHit;

        /** The unknown left. */
        public Set<String>[] unknownLeft;

        /** The unknown right. */
        public Set<String>[] unknownRight;

        /** The Constant KEY_START. */
        public static final String KEY_START = MtasToken.DELIMITER + "grouphit"
                + MtasToken.DELIMITER;

        /**
         * Sort.
         *
         * @param data
         *            the data
         * @return the list
         */
        private List<MtasTreeHit<String>> sort(List<MtasTreeHit<String>> data)
        {
            Collections.sort(data, new Comparator<MtasTreeHit<String>>()
            {
                @Override
                public int compare(MtasTreeHit<String> hit1, MtasTreeHit<String> hit2)
                {
                    int compare = Integer.compare(hit1.additionalId, hit2.additionalId);
                    compare = (compare == 0) ? Long.compare(hit1.additionalRef, hit2.additionalRef)
                            : compare;
                    return compare;
                }
            });
            return data;
        }

        /**
         * Instantiates a new group hit.
         *
         * @param list
         *            the list
         * @param start
         *            the start
         * @param end
         *            the end
         * @param hitStart
         *            the hit start
         * @param hitEnd
         *            the hit end
         * @param group
         *            the group
         * @param knownPrefixes
         *            the known prefixes
         * @throws UnsupportedEncodingException
         *             the unsupported encoding exception
         */
        @SuppressWarnings("unchecked")
        public GroupHit(List<MtasTreeHit<String>> list, int start, int end, int hitStart,
                int hitEnd, ComponentGroup group, Set<String> knownPrefixes)
            throws UnsupportedEncodingException
        {
            // compute dimensions
            int leftRangeStart = start;
            int leftRangeEnd = Math.min(end, hitStart - 1);
            int leftRangeLength = Math.max(0, 1 + leftRangeEnd - leftRangeStart);
            int hitLength = 1 + hitEnd - hitStart;
            int rightRangeStart = Math.max(start, hitEnd + 1);
            int rightRangeEnd = end;
            int rightRangeLength = Math.max(0, 1 + rightRangeEnd - rightRangeStart);
            // create initial arrays
            if (leftRangeLength > 0) {
                keyLeft = "";
                dataLeft = (ArrayList<String>[]) new ArrayList[leftRangeLength];
                missingLeft = (HashSet<String>[]) new HashSet[leftRangeLength];
                unknownLeft = (HashSet<String>[]) new HashSet[leftRangeLength];
                for (int p = 0; p < leftRangeLength; p++) {
                    dataLeft[p] = new ArrayList<>();
                    missingLeft[p] = new HashSet<>();
                    unknownLeft[p] = new HashSet<>();
                }
            }
            else {
                keyLeft = null;
                dataLeft = null;
                missingLeft = null;
                unknownLeft = null;
            }
            if (hitLength > 0) {
                keyHit = "";
                dataHit = (ArrayList<String>[]) new ArrayList[hitLength];
                missingHit = (HashSet<String>[]) new HashSet[hitLength];
                unknownHit = (HashSet<String>[]) new HashSet[hitLength];
                for (int p = 0; p < hitLength; p++) {
                    dataHit[p] = new ArrayList<>();
                    missingHit[p] = new HashSet<>();
                    unknownHit[p] = new HashSet<>();
                }
            }
            else {
                keyHit = null;
                dataHit = null;
                missingHit = null;
                unknownHit = null;
            }
            if (rightRangeLength > 0) {
                keyRight = "";
                dataRight = (ArrayList<String>[]) new ArrayList[rightRangeLength];
                missingRight = (HashSet<String>[]) new HashSet[rightRangeLength];
                unknownRight = (HashSet<String>[]) new HashSet[rightRangeLength];
                for (int p = 0; p < rightRangeLength; p++) {
                    dataRight[p] = new ArrayList<>();
                    missingRight[p] = new HashSet<>();
                    unknownRight[p] = new HashSet<>();
                }
            }
            else {
                keyRight = null;
                dataRight = null;
                missingRight = null;
                unknownRight = null;
            }

            // construct missing sets
            if (group.hitInside != null) {
                for (int p = hitStart; p <= hitEnd; p++) {
                    missingHit[p - hitStart].addAll(group.hitInside);
                }
            }
            if (group.hitInsideLeft != null) {
                for (int p = hitStart; p <= Math.min(hitEnd,
                        hitStart + group.hitInsideLeft.length - 1); p++) {
                    if (group.hitInsideLeft[p - hitStart] != null) {
                        missingHit[p - hitStart].addAll(group.hitInsideLeft[p - hitStart]);
                    }
                }
            }
            if (group.hitLeft != null) {
                for (int p = hitStart; p <= Math.min(hitEnd,
                        hitStart + group.hitLeft.length - 1); p++) {
                    if (group.hitLeft[p - hitStart] != null) {
                        missingHit[p - hitStart].addAll(group.hitLeft[p - hitStart]);
                    }
                }
            }
            if (group.hitInsideRight != null) {
                for (int p = Math.max(hitStart,
                        hitEnd - group.hitInsideRight.length + 1); p <= hitEnd; p++) {
                    if (group.hitInsideRight[hitEnd - p] != null) {
                        missingHit[p - hitStart].addAll(group.hitInsideRight[hitEnd - p]);
                    }
                }
            }
            if (group.hitRight != null) {
                for (int p = hitStart; p <= Math.min(hitEnd,
                        hitStart + group.hitRight.length - 1); p++) {
                    if (group.hitRight[p - hitStart] != null) {
                        missingHit[p - hitStart].addAll(group.hitRight[p - hitStart]);
                    }
                }
            }
            if (group.left != null) {
                for (int p = 0; p < Math.min(leftRangeLength, group.left.length); p++) {
                    if (group.left[p] != null) {
                        missingLeft[p].addAll(group.left[p]);
                    }
                }
            }
            if (group.hitRight != null) {
                for (int p = 0; p < Math.min(leftRangeLength,
                        group.hitRight.length - dataHit.length); p++) {
                    if (group.hitRight[p + dataHit.length] != null) {
                        missingLeft[p].addAll(group.hitRight[p + dataHit.length]);
                    }
                }
            }
            if (group.right != null) {
                for (int p = 0; p < Math.min(rightRangeLength, group.right.length); p++) {
                    if (group.right[p] != null) {
                        missingRight[p].addAll(group.right[p]);
                    }
                }
            }
            if (group.hitLeft != null) {
                for (int p = 0; p < Math.min(rightRangeLength,
                        group.hitLeft.length - dataHit.length); p++) {
                    if (group.hitLeft[p + dataHit.length] != null) {
                        missingRight[p].addAll(group.hitLeft[p + dataHit.length]);
                    }
                }
            }

            // fill arrays and update missing administration
            List<MtasTreeHit<String>> sortedList = sort(list);
            for (MtasTreeHit<String> hit : sortedList) {
                // inside hit
                if (group.hitInside != null && hit.idData != null
                        && group.hitInside.contains(hit.idData)) {
                    for (int p = Math.max(hitStart, hit.startPosition); p <= Math.min(hitEnd,
                            hit.endPosition); p++) {
                        dataHit[p - hitStart].add(hit.refData);
                        missingHit[p - hitStart].remove(MtasToken.getPrefixFromValue(hit.refData));
                    }
                }
                else if ((group.hitInsideLeft != null || group.hitLeft != null
                        || group.hitInsideRight != null || group.hitRight != null)
                        && hit.idData != null) {
                    for (int p = Math.max(hitStart, hit.startPosition); p <= Math.min(hitEnd,
                            hit.endPosition); p++) {
                        int pHitLeft = p - hitStart;
                        int pHitRight = hitEnd - p;
                        if (group.hitInsideLeft != null
                                && pHitLeft <= (group.hitInsideLeft.length - 1)
                                && group.hitInsideLeft[pHitLeft] != null
                                && group.hitInsideLeft[pHitLeft].contains(hit.idData)) {
                            // keyHit += hit.refData;
                            dataHit[p - hitStart].add(hit.refData);
                            missingHit[p - hitStart]
                                    .remove(MtasToken.getPrefixFromValue(hit.refData));
                        }
                        else if (group.hitLeft != null && pHitLeft <= (group.hitLeft.length - 1)
                                && group.hitLeft[pHitLeft] != null
                                && group.hitLeft[pHitLeft].contains(hit.idData)) {
                            // keyHit += hit.refData;
                            dataHit[p - hitStart].add(hit.refData);
                            missingHit[p - hitStart]
                                    .remove(MtasToken.getPrefixFromValue(hit.refData));
                        }
                        else if (group.hitInsideRight != null
                                && pHitRight <= (group.hitInsideRight.length - 1)
                                && group.hitInsideRight[pHitRight] != null
                                && group.hitInsideRight[pHitRight].contains(hit.idData)) {
                            dataHit[p - hitStart].add(hit.refData);
                            missingHit[p - hitStart]
                                    .remove(MtasToken.getPrefixFromValue(hit.refData));
                        }
                        else if (group.hitRight != null && pHitRight <= (group.hitRight.length - 1)
                                && group.hitRight[pHitRight] != null
                                && group.hitRight[pHitRight].contains(hit.idData)) {
                            dataHit[p - hitStart].add(hit.refData);
                            missingHit[p - hitStart]
                                    .remove(MtasToken.getPrefixFromValue(hit.refData));
                        }
                    }
                }
                // left
                if (hit.startPosition < hitStart) {
                    if ((group.left != null || (group.hitRight != null
                            && group.hitRight.length > (1 + hitEnd - hitStart)))
                            && hit.idData != null) {
                        for (int p = Math.min(hit.endPosition,
                                hitStart - 1); p >= hit.startPosition; p--) {
                            int pLeft = hitStart - 1 - p;
                            int pHitRight = hitEnd - p;
                            if (group.left != null && pLeft <= (group.left.length - 1)
                                    && group.left[pLeft] != null
                                    && group.left[pLeft].contains(hit.idData)) {
                                dataLeft[hitStart - 1 - p].add(hit.refData);
                                missingLeft[hitStart - 1 - p]
                                        .remove(MtasToken.getPrefixFromValue(hit.refData));
                            }
                            else if (group.hitRight != null
                                    && pHitRight <= (group.hitRight.length - 1)
                                    && group.hitRight[pHitRight] != null
                                    && group.hitRight[pHitRight].contains(hit.idData)) {
                                dataLeft[hitStart - 1 - p].add(hit.refData);
                                missingLeft[hitStart - 1 - p]
                                        .remove(MtasToken.getPrefixFromValue(hit.refData));
                            }
                        }
                    }
                }
                // right
                if (hit.endPosition > hitEnd) {
                    if ((group.right != null || (group.hitLeft != null
                            && group.hitLeft.length > (1 + hitEnd - hitStart)))
                            && hit.idData != null) {
                        for (int p = Math.max(hit.startPosition,
                                hitEnd + 1); p <= hit.endPosition; p++) {
                            int pRight = p - hitEnd - 1;
                            int pHitLeft = p - hitStart;
                            if (group.right != null && pRight <= (group.right.length - 1)
                                    && group.right[pRight] != null
                                    && group.right[pRight].contains(hit.idData)) {
                                dataRight[p - rightRangeStart].add(hit.refData);
                                missingRight[p - rightRangeStart]
                                        .remove(MtasToken.getPrefixFromValue(hit.refData));
                            }
                            else if (group.hitLeft != null && pHitLeft <= (group.hitLeft.length - 1)
                                    && group.hitLeft[pHitLeft] != null
                                    && group.hitLeft[pHitLeft].contains(hit.idData)) {
                                dataRight[p - rightRangeStart].add(hit.refData);
                                missingRight[p - rightRangeStart]
                                        .remove(MtasToken.getPrefixFromValue(hit.refData));
                            }
                        }
                    }
                }
            }
            // register unknown
            if (missingLeft != null) {
                for (int i = 0; i < missingLeft.length; i++) {
                    for (String prefix : missingLeft[i]) {
                        if (!knownPrefixes.contains(prefix)) {
                            unknownLeft[i].add(prefix);
                        }
                    }
                }
            }
            if (missingHit != null) {
                for (int i = 0; i < missingHit.length; i++) {
                    for (String prefix : missingHit[i]) {
                        if (!knownPrefixes.contains(prefix)) {
                            unknownHit[i].add(prefix);
                        }
                    }
                }
            }
            if (missingRight != null) {
                for (int i = 0; i < missingRight.length; i++) {
                    for (String prefix : missingRight[i]) {
                        if (!knownPrefixes.contains(prefix)) {
                            unknownRight[i].add(prefix);
                        }
                    }
                }
            }
            // construct keys
            keyLeft = dataToString(dataLeft, missingLeft, true);
            keyHit = dataToString(dataHit, missingHit, false);
            keyRight = dataToString(dataRight, missingRight, false);
            key = KEY_START;
            if (keyLeft != null) {
                key += keyLeft;
                hashLeft = keyLeft.hashCode();
            }
            else {
                hashLeft = 1;
            }
            key += "|";
            if (keyHit != null) {
                key += keyHit;
                hashHit = keyHit.hashCode();
            }
            else {
                hashHit = 1;
            }
            key += "|";
            if (keyRight != null) {
                key += keyRight;
                hashRight = keyRight.hashCode();
            }
            else {
                hashRight = 1;
            }
            // compute hash
            hash = hashHit * (hashLeft ^ 3) * (hashRight ^ 5);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode()
        {
            return hash;
        }

        /**
         * Data equals.
         *
         * @param d1
         *            the d 1
         * @param d2
         *            the d 2
         * @return true, if successful
         */
        private boolean dataEquals(List<String>[] d1, List<String>[] d2)
        {
            List<String> a1;
            List<String> a2;
            if (d1 == null && d2 == null) {
                return true;
            }
            else if (d1 == null || d2 == null) {
                return false;
            }
            else {
                if (d1.length == d2.length) {
                    for (int i = 0; i < d1.length; i++) {
                        a1 = d1[i];
                        a2 = d2[i];
                        if (a1 != null && a2 != null && a1.size() == a2.size()) {
                            for (int j = 0; j < a1.size(); j++) {
                                if (!a1.get(j).equals(a2.get(j))) {
                                    return false;
                                }
                            }
                        }
                        else {
                            return false;
                        }
                    }
                    return true;
                }
                else {
                    return false;
                }
            }
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
            GroupHit other = (GroupHit) obj;
            if (hashCode() != other.hashCode()) {
                return false;
            }
            if (!dataEquals(dataHit, other.dataHit)) {
                return false;
            }
            if (!dataEquals(dataLeft, other.dataLeft)) {
                return false;
            }
            if (!dataEquals(dataRight, other.dataRight)) {
                return false;
            }
            return true;
        }

        /**
         * Data to string.
         *
         * @param data
         *            the data
         * @param missing
         *            the missing
         * @param reverse
         *            the reverse
         * @return the string
         * @throws UnsupportedEncodingException
         *             the unsupported encoding exception
         */
        private String dataToString(List<String>[] data, Set<String>[] missing, boolean reverse)
            throws UnsupportedEncodingException
        {
            StringBuilder text = null;
            Encoder encoder = Base64.getEncoder();
            String prefix;
            String postfix;
            List<String> dataItem;
            Set<String> missingItem;
            if (data != null && missing != null && data.length == missing.length) {
                for (int i = 0; i < data.length; i++) {
                    if (reverse) {
                        dataItem = data[(data.length - i - 1)];
                        missingItem = missing[(data.length - i - 1)];
                    }
                    else {
                        dataItem = data[i];
                        missingItem = missing[i];
                    }
                    if (i > 0) {
                        text.append(",");
                    }
                    else {
                        text = new StringBuilder();
                    }
                    for (int j = 0; j < dataItem.size(); j++) {
                        if (j > 0) {
                            text.append("&");
                        }
                        prefix = MtasToken.getPrefixFromValue(dataItem.get(j));
                        postfix = MtasToken.getPostfixFromValue(dataItem.get(j));
                        text.append(
                                encoder.encodeToString(prefix.getBytes(StandardCharsets.UTF_8)));
                        if (!postfix.isEmpty()) {
                            text.append(".");
                            text.append(encoder
                                    .encodeToString(postfix.getBytes(StandardCharsets.UTF_8)));
                        }
                    }
                    if (missingItem != null) {
                        String[] tmpMissing = missingItem.toArray(new String[missingItem.size()]);
                        for (int j = 0; j < tmpMissing.length; j++) {
                            if (j > 0 || !dataItem.isEmpty()) {
                                text.append("&");
                            }
                            text.append(encoder.encodeToString(
                                    ("!" + tmpMissing[j]).getBytes(StandardCharsets.UTF_8)));
                        }
                    }
                }
            }
            return text != null ? text.toString() : null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return key;
        }

        /**
         * Key to sub sub object.
         *
         * @param key
         *            the key
         * @param newKey
         *            the new key
         * @return the map[]
         */
        private static Map<String, String>[] keyToSubSubObject(String key, StringBuilder newKey)
        {
            if (!key.isEmpty()) {
                newKey.append(" [");
                String prefix;
                String postfix;
                String[] parts = key.split(Pattern.quote("&"));
                Map<String, String>[] result = new HashMap[parts.length];
                Pattern pattern = Pattern.compile("^([^\\.]*)\\.([^\\.]*)$");
                Decoder decoder = Base64.getDecoder();
                Matcher matcher;
                StringBuilder tmpNewKey = null;
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].isEmpty()) {
                        result[i] = null;
                    }
                    else {
                        HashMap<String, String> subResult = new HashMap<>();
                        matcher = pattern.matcher(parts[i]);
                        if (tmpNewKey != null) {
                            tmpNewKey.append(" & ");
                        }
                        else {
                            tmpNewKey = new StringBuilder();
                        }
                        if (matcher.matches()) {
                            prefix = new String(
                                    decoder.decode(
                                            matcher.group(1).getBytes(StandardCharsets.UTF_8)),
                                    StandardCharsets.UTF_8);
                            postfix = new String(
                                    decoder.decode(
                                            matcher.group(2).getBytes(StandardCharsets.UTF_8)),
                                    StandardCharsets.UTF_8);
                            tmpNewKey.append(prefix.replace("=", "\\="));
                            tmpNewKey.append("=\"" + postfix.replace("\"", "\\\"") + "\"");
                            subResult.put("prefix", prefix);
                            subResult.put("value", postfix);
                        }
                        else {
                            prefix = new String(
                                    decoder.decode(parts[i].getBytes(StandardCharsets.UTF_8)),
                                    StandardCharsets.UTF_8);
                            tmpNewKey.append(prefix.replace("=", "\\="));
                            if (prefix.startsWith("!")) {
                                subResult.put("missing", prefix.substring(1));
                            }
                            else {
                                subResult.put("prefix", prefix);
                            }
                        }
                        result[i] = subResult;
                    }
                }
                if (tmpNewKey != null) {
                    newKey.append(tmpNewKey);
                }
                newKey.append("]");
                return result;
            }
            else {
                newKey.append(" []");
                return null;
            }
        }

        /**
         * Key to sub object.
         *
         * @param key
         *            the key
         * @param newKey
         *            the new key
         * @return the map
         */
        private static Map<Integer, Map<String, String>[]> keyToSubObject(String key,
                StringBuilder newKey)
        {
            Map<Integer, Map<String, String>[]> result = new HashMap<>();
            if (key == null || key.trim().isEmpty()) {
                return null;
            }
            else {
                String[] parts = key.split(Pattern.quote(","), -1);
                if (parts.length > 0) {
                    for (int i = 0; i < parts.length; i++) {
                        result.put(i, keyToSubSubObject(parts[i].trim(), newKey));
                    }
                    return result;
                }
                else {
                    return null;
                }
            }
        }

        /**
         * Key to object.
         *
         * @param key
         *            the key
         * @param newKey
         *            the new key
         * @return the map
         */
        public static Map<String, Map<Integer, Map<String, String>[]>> keyToObject(String key,
                StringBuilder newKey)
        {
            if (key.startsWith(KEY_START)) {
                String content = key.substring(KEY_START.length());
                StringBuilder keyLeft = new StringBuilder("");
                StringBuilder keyHit = new StringBuilder("");
                StringBuilder keyRight = new StringBuilder("");
                Map<String, Map<Integer, Map<String, String>[]>> result = new HashMap<>();
                Map<Integer, Map<String, String>[]> resultLeft = null;
                Map<Integer, Map<String, String>[]> resultHit = null;
                Map<Integer, Map<String, String>[]> resultRight = null;
                String[] parts = content.split(Pattern.quote("|"), -1);
                if (parts.length == 3) {
                    resultLeft = keyToSubObject(parts[0].trim(), keyLeft);
                    resultHit = keyToSubObject(parts[1].trim(), keyHit);
                    resultRight = keyToSubObject(parts[2].trim(), keyRight);
                }
                else if (parts.length == 1) {
                    resultHit = keyToSubObject(parts[0].trim(), keyHit);
                }
                if (resultLeft != null) {
                    result.put("left", resultLeft);
                }
                result.put("hit", resultHit);
                if (resultRight != null) {
                    result.put("right", resultRight);
                }
                newKey.append(keyLeft);
                newKey.append(" |");
                newKey.append(keyHit);
                newKey.append(" |");
                newKey.append(keyRight);
                return result;
            }
            else {
                return null;
            }
        }

    }

    /**
     * The Class ListToken.
     */
    public static class ListToken
    {

        /** The doc id. */
        public Integer docId;

        /** The doc position. */
        public Integer docPosition;

        /** The start position. */
        public int startPosition;

        /** The end position. */
        public int endPosition;

        /** The tokens. */
        public List<MtasTokenString> tokens;

        /**
         * Instantiates a new list token.
         *
         * @param docId
         *            the doc id
         * @param docPosition
         *            the doc position
         * @param match
         *            the match
         * @param tokens
         *            the tokens
         */
        public ListToken(Integer docId, Integer docPosition, Match match,
                List<MtasTokenString> tokens)
        {
            this.docId = docId;
            this.docPosition = docPosition;
            startPosition = match.startPosition;
            endPosition = match.endPosition - 1;
            this.tokens = tokens;
        }
    }

    /**
     * The Class ListHit.
     */
    public static class ListHit
    {

        /** The doc id. */
        public Integer docId;

        /** The doc position. */
        public Integer docPosition;

        /** The start position. */
        public int startPosition;

        /** The end position. */
        public int endPosition;

        /** The hits. */
        public Map<Integer, List<String>> hits;

        /**
         * Instantiates a new list hit.
         *
         * @param docId
         *            the doc id
         * @param docPosition
         *            the doc position
         * @param match
         *            the match
         * @param hits
         *            the hits
         */
        public ListHit(Integer docId, Integer docPosition, Match match,
                Map<Integer, List<String>> hits)
        {
            this.docId = docId;
            this.docPosition = docPosition;
            startPosition = match.startPosition;
            endPosition = match.endPosition - 1;
            this.hits = hits;
        }
    }

    /**
     * The Class PageWordData.
     */
    public static class PageWordData
    {

        /** The words. */
        public List<PageWord> words;

        /**
         * Adds the.
         *
         * @param token
         *            the token
         */
        public void add(MtasTokenString token)
        {
            words.add(new PageWord(token));
        }

        /**
         * Instantiates a new page word data.
         */
        public PageWordData()
        {
            words = new ArrayList<>();
        }
    }

    /**
     * The Class PageWord.
     */
    public static class PageWord
    {

        /** The id. */
        public int id;

        /** The prefix. */
        public String prefix;

        /** The postfix. */
        public String postfix;

        /** The parent id. */
        public Integer parentId;

        /**
         * Instantiates a new page word.
         *
         * @param token
         *            the token
         */
        public PageWord(MtasTokenString token)
        {
            id = token.getId();
            prefix = token.getPrefix();
            postfix = token.getPostfix();
            parentId = token.getParentId();
        }

    }

    /**
     * The Class PageRangeData.
     */
    public static class PageRangeData
    {

        /** The ranges. */
        public List<PageRange> ranges;

        /**
         * Adds the.
         *
         * @param token
         *            the token
         */
        public void add(MtasTokenString token)
        {
            ranges.add(new PageRange(token));
        }

        /**
         * Instantiates a new page range data.
         */
        public PageRangeData()
        {
            ranges = new ArrayList<>();
        }
    }

    /**
     * The Class PageRange.
     */
    public static class PageRange
    {

        /** The id. */
        public int id;

        /** The start. */
        public int start;

        /** The end. */
        public int end;

        /** The prefix. */
        public String prefix;

        /** The postfix. */
        public String postfix;

        /** The parent id. */
        public Integer parentId;

        /**
         * Instantiates a new page range.
         *
         * @param token
         *            the token
         */
        public PageRange(MtasTokenString token)
        {
            id = token.getId();
            start = token.getPositionStart();
            end = token.getPositionEnd();
            prefix = token.getPrefix();
            postfix = token.getPostfix();
            parentId = token.getParentId();
        }

    }

    /**
     * The Class PageSetData.
     */
    public static class PageSetData
    {

        /** The sets. */
        public List<PageSet> sets;

        /**
         * Adds the.
         *
         * @param token
         *            the token
         */
        public void add(MtasTokenString token)
        {
            sets.add(new PageSet(token));
        }

        /**
         * Instantiates a new page set data.
         */
        public PageSetData()
        {
            sets = new ArrayList<>();
        }
    }

    /**
     * The Class PageSet.
     */
    public static class PageSet
    {

        /** The id. */
        public int id;

        /** The positions. */
        public int[] positions;

        /** The prefix. */
        public String prefix;

        /** The postfix. */
        public String postfix;

        /** The parent id. */
        public Integer parentId;

        /**
         * Instantiates a new page set.
         *
         * @param token
         *            the token
         */
        public PageSet(MtasTokenString token)
        {
            id = token.getId();
            positions = token.getPositions();
            prefix = token.getPrefix();
            postfix = token.getPostfix();
            parentId = token.getParentId();
        }

    }

    /**
     * The Class IndexItem.
     */
    public static class IndexItem
    {

        /** The start position. */
        public int startPosition;

        /** The end position. */
        public int endPosition;

        /** The name. */
        public String name;

        /** The number. */
        public int number;

        /** The list. */
        public Map<List<Map<String, Set<String>>>, Integer> list;

        /**
         * Instantiates a new index item.
         *
         * @param startPosition
         *            the start position
         * @param endPosition
         *            the end position
         * @param name
         *            the name
         */
        public IndexItem(int startPosition, int endPosition, String name)
        {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.name = name;
            this.number = 0;
            this.list = new HashMap<List<Map<String, Set<String>>>, Integer>();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return (name == null ? startPosition + "-" + endPosition : name) + ":" + number;
        }

    }

    /**
     * The Class Match.
     */
    public static class Match
    {

        /** The start position. */
        public int startPosition;

        /** The end position. */
        public int endPosition;

        /**
         * Instantiates a new match.
         *
         * @param startPosition
         *            the start position
         * @param endPosition
         *            the end position
         */
        public Match(int startPosition, int endPosition)
        {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
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
            return Objects.hash(this.getClass().getSimpleName(), startPosition, endPosition);
        }

    }

}
