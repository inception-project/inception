package mtas.analysis.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtas.analysis.token.MtasToken;
import mtas.analysis.token.MtasTokenCollection;
import mtas.analysis.token.MtasTokenIdFactory;
import mtas.analysis.token.MtasTokenString;
import mtas.analysis.util.MtasConfigException;
import mtas.analysis.util.MtasConfiguration;
import mtas.analysis.util.MtasParserException;
import mtas.analysis.util.MtasPennTreebankReader;

/**
 * The Class MtasPennTreebankParser.
 */
/**
 * @author matthijs
 *
 */
public class MtasPennTreebankParser
    extends MtasParser
{

    /** The Constant log. */
    private static final Logger log = LoggerFactory.getLogger(MtasPennTreebankParser.class);

    /** The Constant PENNTREEBANK_IGNORE. */
    private static final String PENNTREEBANK_IGNORE = "ignore";

    /** The Constant PENNTREEBANK_NODE. */
    private static final String PENNTREEBANK_NODE = "node";

    /** The Constant PENNTREEBANK_NODE_NAME. */
    private static final String PENNTREEBANK_NODE_NAME = "name";

    /** The Constant NODE_CODE. */
    private static final String NODE_CODE = "CODE";

    /** The Constant NODE_CODE_PREFIX. */
    private static final String NODE_CODE_PREFIX = "$";

    /** The Constant STRING_SPLITTER. */
    private static final String STRING_SPLITTER = "_";

    /** The ignore nodes. */
    private Set<String> ignoreNodes = new HashSet<>();

    /**
     * Instantiates a new mtas penn treebank parser.
     *
     * @param config
     *            the config
     */
    public MtasPennTreebankParser(MtasConfiguration config)
    {
        super(config);
        try {
            initParser();
            // System.out.print(printConfig());
        }
        catch (MtasConfigException e) {
            log.error("Error", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.analysis.parser.MtasParser#initParser()
     */
    @Override
    protected void initParser() throws MtasConfigException
    {
        super.initParser();
        if (config != null) {
            for (int i = 0; i < config.children.size(); i++) {
                MtasConfiguration current = config.children.get(i);
                if (current.name.equals(PENNTREEBANK_IGNORE)) {
                    for (int j = 0; j < current.children.size(); j++) {
                        if (current.children.get(j).name.equals(PENNTREEBANK_NODE)) {
                            String nameVariable = current.children.get(j).attributes
                                    .get(PENNTREEBANK_NODE_NAME);
                            if (!nameVariable.isEmpty()) {
                                ignoreNodes.add(nameVariable);
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.analysis.parser.MtasParser#createTokenCollection(java.io.Reader)
     */
    @Override
    public MtasTokenCollection createTokenCollection(Reader reader)
        throws MtasParserException, MtasConfigException
    {
        tokenCollection = new MtasTokenCollection();
        MtasTokenIdFactory mtasTokenIdFactory = new MtasTokenIdFactory();
        List<Level> levels = new ArrayList<>();
        // Map<String,MtasToken> referencesNode = new HashMap<>();
        // Map<String,List<MtasToken>> referencesNullElement = new HashMap<>();
        try {
            MtasPennTreebankReader treebankReader = new MtasPennTreebankReader(reader);
            // variables main administration
            int event = treebankReader.getEventType();
            int position = 0;
            boolean ignore = false;
            Level level = null;
            // variables for code
            List<Integer> codePositions = new ArrayList<>();
            Integer codeOffsetStart = null;
            Integer codeOffsetEnd = null;
            // variables for string
            String stringValue = null;
            int stringOffsetStart;
            int stringOffsetEnd;
            // loop
            while (true) {
                switch (event) {
                case MtasPennTreebankReader.EVENT_STARTBRACKET:
                    if (level != null && level.code) {
                        throw new MtasParserException("unexpected start bracket for " + NODE_CODE);
                    }
                    else {
                        level = new Level();
                        level.ignore = ignore;
                        level.realOffsetStart = treebankReader.getPosition() + 1;
                        levels.add(level);
                    }
                    break;
                case MtasPennTreebankReader.EVENT_ENDBRACKET:
                    Objects.requireNonNull(level, "no level while ending bracket");
                    level.realOffsetEnd = treebankReader.getPosition() - 1;
                    Level parentLevel = levels.size() > 1 ? levels.get(levels.size() - 2) : null;
                    createNodeMappings(mtasTokenIdFactory, level, parentLevel);
                    // remove level
                    if (parentLevel != null) {
                        if (level.positionStart != null && level.positionEnd != null) {
                            parentLevel.addPositionRange(level.positionStart, level.positionEnd);
                        }
                        parentLevel.offsetStart = parentLevel.offsetStart == null
                                ? level.offsetStart
                                : parentLevel.offsetStart;
                        parentLevel.offsetEnd = level.offsetEnd == null ? parentLevel.offsetEnd
                                : level.offsetEnd;
                        levels.remove(levels.size() - 1);
                        level = parentLevel;
                        ignore = level.ignore;
                    }
                    else {
                        levels.clear();
                        level = null;
                        ignore = false;
                        // referencesNode.clear();
                        // referencesNullElement.clear();
                    }
                    break;
                case MtasPennTreebankReader.EVENT_NODE:
                    Objects.requireNonNull(level, "no level while handling node");
                    // register node with level
                    level.node = treebankReader.getString();
                    if (ignoreNodes.contains(level.node)) {
                        ignore = true;
                        level.ignore = true;
                    }
                    if (level.node.equals(NODE_CODE)) {
                        level.code = true;
                        if (!treebankReader.next() || (event = treebankReader
                                .getEventType()) != MtasPennTreebankReader.EVENT_STRING) {
                            throw new MtasParserException("expected string for " + NODE_CODE);
                        }
                        else if (!level.ignore) {
                            stringValue = treebankReader.getString();
                            stringOffsetStart = treebankReader.getPosition();
                            stringOffsetEnd = stringOffsetStart + stringValue.length();
                            if (!codePositions.isEmpty()) {
                                createCodeMappings(mtasTokenIdFactory, level, stringValue,
                                        codeOffsetStart, codeOffsetEnd, stringOffsetStart,
                                        stringOffsetEnd, codePositions);
                            }
                            else {
                                log.error("CODE without codePositions for " + stringValue);
                            }
                            codePositions.clear();
                            codeOffsetStart = null;
                            codeOffsetEnd = null;
                        }
                    }
                    break;
                case MtasPennTreebankReader.EVENT_STRING:
                    Objects.requireNonNull(level, "no level while handling string");
                    if (level.code) {
                        throw new MtasParserException("unexpected string for " + NODE_CODE);
                    }
                    else if (!level.ignore) {
                        stringValue = treebankReader.getString();
                        stringOffsetStart = treebankReader.getPosition();
                        stringOffsetEnd = stringOffsetStart + stringValue.length();
                        if (level.offsetStart == null) {
                            level.offsetStart = stringOffsetStart;
                        }
                        level.offsetEnd = stringOffsetEnd;
                        if (stringValue.startsWith(NODE_CODE_PREFIX)) {
                            codePositions.add(position);
                            stringValue = stringValue.substring(NODE_CODE_PREFIX.length(),
                                    stringValue.length());
                            if (codeOffsetStart == null) {
                                codeOffsetStart = stringOffsetStart;
                            }
                            codeOffsetEnd = stringOffsetEnd;
                        }
                        // register position
                        level.addPosition(position);
                        // create mappings
                        createStringMappings(mtasTokenIdFactory, level, stringValue,
                                stringOffsetStart, stringOffsetEnd, position);
                        // increase position
                        position++;
                    }
                    break;
                default:
                    break;
                }
                if (!treebankReader.next()) {
                    break;
                }
                else {
                    event = treebankReader.getEventType();
                }
            }
        }
        catch (IOException e) {
            log.debug("Error", e);
            throw new MtasParserException("No valid Penn Treebank syntax: " + e.getMessage());
        }
        // final check
        tokenCollection.check(autorepair, makeunique);
        return tokenCollection;

    }

    /**
     * Creates the code mappings.
     *
     * @param mtasTokenIdFactory
     *            the mtas token id factory
     * @param level
     *            the level
     * @param stringValue
     *            the string value
     * @param offsetStart
     *            the offset start
     * @param offsetEnd
     *            the offset end
     * @param realOffsetStart
     *            the real offset start
     * @param realOffsetEnd
     *            the real offset end
     * @param codePositions
     *            the code positions
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void createCodeMappings(MtasTokenIdFactory mtasTokenIdFactory, Level level,
            String stringValue, int offsetStart, int offsetEnd, int realOffsetStart,
            int realOffsetEnd, List<Integer> codePositions)
        throws IOException
    {
        String[] stringValues = MtasPennTreebankReader.createStrings(stringValue,
                Pattern.quote(STRING_SPLITTER));
        MtasToken token = new MtasTokenString(mtasTokenIdFactory.createTokenId(), level.node,
                filterString(stringValues[0].trim()));
        token.setOffset(offsetStart, offsetEnd);
        token.setRealOffset(realOffsetStart, realOffsetEnd);
        token.addPositions(codePositions.stream().mapToInt(i -> i).toArray());
        tokenCollection.add(token);
        level.tokens.add(token);
    }

    /**
     * Creates the node mappings.
     *
     * @param mtasTokenIdFactory
     *            the mtas token id factory
     * @param level
     *            the level
     * @param parentLevel
     *            the parent level
     */
    private void createNodeMappings(MtasTokenIdFactory mtasTokenIdFactory, Level level,
            Level parentLevel)
    {
        MtasToken nodeToken;
        if (level.node != null && level.positionStart != null && level.positionEnd != null) {
            nodeToken = new MtasTokenString(mtasTokenIdFactory.createTokenId(), level.node, "");
            nodeToken.setOffset(level.offsetStart, level.offsetEnd);
            nodeToken.setRealOffset(level.realOffsetStart, level.realOffsetEnd);
            nodeToken.addPositionRange(level.positionStart, level.positionEnd);
            tokenCollection.add(nodeToken);
            if (parentLevel != null) {
                parentLevel.tokens.add(nodeToken);
            }
            // only for first mapping(?)
            for (MtasToken token : level.tokens) {
                token.setParentId(nodeToken.getId());
            }
        }
    }

    /**
     * Creates the string mappings.
     *
     * @param mtasTokenIdFactory
     *            the mtas token id factory
     * @param level
     *            the level
     * @param stringValue
     *            the string value
     * @param offsetStart
     *            the offset start
     * @param offsetEnd
     *            the offset end
     * @param position
     *            the position
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void createStringMappings(MtasTokenIdFactory mtasTokenIdFactory, Level level,
            String stringValue, int offsetStart, int offsetEnd, int position)
        throws IOException
    {
        // System.out.println("createStringMappings string ");
        String[] stringValues = MtasPennTreebankReader.createStrings(stringValue,
                Pattern.quote(STRING_SPLITTER));
        if (stringValues.length > 0 && !stringValues[0].trim().isEmpty()) {
            MtasToken token = new MtasTokenString(mtasTokenIdFactory.createTokenId(), "t",
                    filterString(stringValues[0].trim()), position);
            token.setOffset(offsetStart, offsetEnd);
            tokenCollection.add(token);
            level.tokens.add(token);
        }
        if (stringValues.length > 1 && !stringValues[1].trim().isEmpty()) {
            MtasToken token = new MtasTokenString(mtasTokenIdFactory.createTokenId(), "lemma",
                    filterString(stringValues[1].trim()), position);
            token.setOffset(offsetStart, offsetEnd);
            tokenCollection.add(token);
            level.tokens.add(token);
        }
    }

    /**
     * Filter string.
     *
     * @param stringValue
     *            the string value
     * @return the string
     */
    private String filterString(String stringValue)
    {
        final String lrb = Pattern.quote("-LRB-");
        final String rrb = Pattern.quote("-RRB-");
        final String lcb = Pattern.quote("-LCB-");
        final String rcb = Pattern.quote("-RCB-");
        final String lsb = Pattern.quote("-LSB-");
        final String rsb = Pattern.quote("-RSB-");
        String filteredValue = stringValue.replaceAll("\\{TEXT:([^\\}]*)\\}", "$1");
        filteredValue = filteredValue.replaceAll(lrb, "(");
        filteredValue = filteredValue.replaceAll(rrb, ")");
        filteredValue = filteredValue.replaceAll(lcb, "{");
        filteredValue = filteredValue.replaceAll(rcb, "}");
        filteredValue = filteredValue.replaceAll(lsb, "[");
        filteredValue = filteredValue.replaceAll(rsb, "]");
        return filteredValue;
    }

    /**
     * Filter null element references.
     *
     * @param stringValues
     *            the string values
     * @return the string[]
     */
    public String[] filterNullElementReferences(String[] stringValues)
    {
        Objects.requireNonNull(stringValues, "no stringValues");
        final Pattern pattern = Pattern.compile("^([^" + Pattern.quote("-") + "]*)"
                + Pattern.quote("-") + "([0-9]+(" + Pattern.quote("-") + "[0-9]+)*)$");
        if (stringValues.length > 0) {
            Matcher matcher = pattern.matcher(stringValues[0]);
            stringValues[0] = stringValues[0].replaceAll(Pattern.quote("-") + ".*$", "");
            if (matcher.matches()) {
                return matcher.group(2).split(Pattern.quote("-"));
            }

        }
        return new String[0];
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.analysis.parser.MtasParser#printConfig()
     */
    @Override
    public String printConfig()
    {
        StringBuilder text = new StringBuilder();
        text.append("=== CONFIGURATION ===\n");
        text.append(config.toString());
        text.append("=== CONFIGURATION ===\n");
        return text.toString();
    }

    /**
     * The Class Level.
     */
    public static class Level
    {

        /** The node. */
        public String node;

        /** The offset start. */
        public Integer offsetStart;

        /** The offset end. */
        public Integer offsetEnd;

        /** The real offset start. */
        public Integer realOffsetStart;

        /** The real offset end. */
        public Integer realOffsetEnd;

        /** The ignore. */
        public boolean ignore;

        /** The code. */
        public boolean code;

        /** The position start. */
        public Integer positionStart;

        /** The position end. */
        public Integer positionEnd;

        /** The tokens. */
        public List<MtasToken> tokens;

        /**
         * Instantiates a new level.
         */
        public Level()
        {
            node = null;
            offsetStart = null;
            offsetEnd = null;
            realOffsetStart = null;
            realOffsetEnd = null;
            ignore = false;
            code = false;
            positionStart = null;
            positionEnd = null;
            tokens = new ArrayList<>();
        }

        /**
         * Adds the position.
         *
         * @param position
         *            the position
         */
        public void addPosition(int position)
        {
            positionStart = (positionStart == null) ? position : Math.min(positionStart, position);
            positionEnd = (positionEnd == null) ? position : Math.max(positionEnd, position);
        }

        /**
         * Adds the position range.
         *
         * @param startPosition
         *            the start position
         * @param endPosition
         *            the end position
         */
        public void addPositionRange(int startPosition, int endPosition)
        {
            positionStart = (positionStart == null) ? startPosition
                    : Math.min(positionStart, startPosition);
            positionEnd = (positionEnd == null) ? endPosition : Math.max(positionEnd, endPosition);
        }

    }

}
