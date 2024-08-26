package mtas.analysis.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtas.analysis.token.MtasTokenCollection;
import mtas.analysis.token.MtasTokenIdFactory;
import mtas.analysis.util.MtasBufferedReader;
import mtas.analysis.util.MtasConfigException;
import mtas.analysis.util.MtasConfiguration;
import mtas.analysis.util.MtasParserException;

/**
 * The Class MtasCRMParser.
 */

public class MtasCRMParser
    extends MtasBasicParser
{

    /** The Constant log. */
    private static final Logger log = LoggerFactory.getLogger(MtasCRMParser.class);

    /** The word type. */
    private MtasParserType<MtasParserMapping<?>> wordType = null;

    /** The word annotation types. */
    private HashMap<String, MtasParserType<MtasParserMapping<?>>> wordAnnotationTypes = new HashMap<>();

    /** The crm sentence types. */
    private HashMap<String, MtasParserType<MtasParserMapping<?>>> crmSentenceTypes = new HashMap<>();

    /** The crm clause types. */
    private HashMap<String, MtasParserType<MtasParserMapping<?>>> crmClauseTypes = new HashMap<>();

    /** The crm pair types. */
    private HashMap<String, MtasParserType<MtasParserMapping<?>>> crmPairTypes = new HashMap<>();

    /** The functions. */
    private HashMap<String, HashMap<String, MtasCRMParserFunction>> functions = new HashMap<>();

    private HashMap<Integer, HashMap<String, String>> filterReplace = new HashMap<>();

    /** The Constant MAPPING_TYPE_CRM_SENTENCE. */
    protected static final String MAPPING_TYPE_CRM_SENTENCE = "crmSentence";

    /** The Constant MAPPING_TYPE_CRM_CLAUSE. */
    protected static final String MAPPING_TYPE_CRM_CLAUSE = "crmClause";

    /** The Constant MAPPING_TYPE_CRM_PAIR. */
    protected static final String MAPPING_TYPE_CRM_PAIR = "crmPair";

    protected static final String FILTER_TYPE_REPLACE = "replace";

    /** The history pair. */
    private HashMap<String, HashMap<String, MtasParserObject>> historyPair = new HashMap<>();

    /** The pair pattern. */
    Pattern pairPattern = Pattern.compile("^([b|e])([a-z])([0-9]+)$");

    /**
     * Instantiates a new mtas CRM parser.
     *
     * @param config
     *            the config
     */
    public MtasCRMParser(MtasConfiguration config)
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
    @SuppressWarnings("unchecked")
    @Override
    protected void initParser() throws MtasConfigException
    {
        super.initParser();
        if (config != null) {
            // always word, no mappings
            wordType = new MtasParserType<>(MAPPING_TYPE_WORD, null, false);
            for (int i = 0; i < config.children.size(); i++) {
                MtasConfiguration current = config.children.get(i);
                if (current.name.equals("filters")) {
                    for (int j = 0; j < current.children.size(); j++) {
                        if (current.children.get(j).name.equals("filter")) {
                            MtasConfiguration filter = current.children.get(j);
                            String typeFilter = filter.attributes.get("type");
                            String nameFilter = filter.attributes.get("name");
                            if (typeFilter != null) {
                                if (typeFilter.equals(FILTER_TYPE_REPLACE)) {
                                    String value = filter.attributes.get("value");
                                    String replace = filter.attributes.get("replace");
                                    if (nameFilter != null && value != null && replace != null) {
                                        String[] names = nameFilter.split(Pattern.quote(","));
                                        for (String name : names) {
                                            try {
                                                int nameInt = Integer.parseInt(name);
                                                HashMap<String, String> nameMap;
                                                if (!filterReplace.containsKey(nameInt)) {
                                                    nameMap = new HashMap<>();
                                                    filterReplace.put(nameInt, nameMap);
                                                }
                                                else {
                                                    nameMap = filterReplace.get(nameInt);
                                                }
                                                nameMap.put(value, replace);
                                            }
                                            catch (NumberFormatException e) {
                                                log.info("Error", e);
                                            }
                                        }
                                    }
                                    else {
                                        throw new MtasConfigException(
                                                "no name, value or replace for filter "
                                                        + typeFilter);
                                    }
                                }
                                else {
                                    throw new MtasConfigException(
                                            "unknown filter type " + typeFilter);
                                }
                            }
                            else {
                                throw new MtasConfigException("no type provided for filter");
                            }
                        }
                    }
                }
                else if (current.name.equals("mappings")) {
                    for (int j = 0; j < current.children.size(); j++) {
                        if (current.children.get(j).name.equals("mapping")) {
                            MtasConfiguration mapping = current.children.get(j);
                            String typeMapping = mapping.attributes.get("type");
                            String nameMapping = mapping.attributes.get("name");
                            if ((typeMapping != null)) {
                                if (typeMapping.equals(MAPPING_TYPE_WORD)) {
                                    MtasCRMParserMappingWordAnnotation m = new MtasCRMParserMappingWordAnnotation();
                                    m.processConfig(mapping);
                                    wordType.addItem(m);
                                }
                                else if (typeMapping.equals(MAPPING_TYPE_WORD_ANNOTATION)
                                        && (nameMapping != null)) {
                                    MtasCRMParserMappingWordAnnotation m = new MtasCRMParserMappingWordAnnotation();
                                    m.processConfig(mapping);
                                    if (wordAnnotationTypes.containsKey(nameMapping)) {
                                        wordAnnotationTypes.get(nameMapping).addItem(m);
                                    }
                                    else {
                                        MtasParserType<MtasParserMapping<?>> t = new MtasParserType<>(
                                                typeMapping, nameMapping, false);
                                        t.addItem(m);
                                        wordAnnotationTypes.put(nameMapping, t);
                                    }
                                }
                                else if (typeMapping.equals(MAPPING_TYPE_CRM_SENTENCE)) {
                                    MtasCRMParserMappingCRMSentence m = new MtasCRMParserMappingCRMSentence();
                                    m.processConfig(mapping);
                                    if (crmSentenceTypes.containsKey(nameMapping)) {
                                        crmSentenceTypes.get(nameMapping).addItem(m);
                                    }
                                    else {
                                        MtasParserType<MtasParserMapping<?>> t = new MtasParserType<>(
                                                MAPPING_TYPE_GROUP, nameMapping, true);
                                        t.addItem(m);
                                        crmSentenceTypes.put(nameMapping, t);
                                    }
                                }
                                else if (typeMapping.equals(MAPPING_TYPE_CRM_CLAUSE)) {
                                    MtasCRMParserMappingCRMSentence m = new MtasCRMParserMappingCRMSentence();
                                    m.processConfig(mapping);
                                    if (crmClauseTypes.containsKey(nameMapping)) {
                                        crmClauseTypes.get(nameMapping).addItem(m);
                                    }
                                    else {
                                        MtasParserType<MtasParserMapping<?>> t = new MtasParserType<>(
                                                MAPPING_TYPE_GROUP, nameMapping, true);
                                        t.addItem(m);
                                        crmClauseTypes.put(nameMapping, t);
                                    }
                                }
                                else if (typeMapping.equals(MAPPING_TYPE_CRM_PAIR)) {
                                    MtasCRMParserMappingCRMPair m = new MtasCRMParserMappingCRMPair();
                                    m.processConfig(mapping);
                                    if (crmPairTypes.containsKey(nameMapping)) {
                                        crmPairTypes.get(nameMapping).addItem(m);
                                    }
                                    else {
                                        MtasParserType<MtasParserMapping<?>> t = new MtasParserType<>(
                                                MAPPING_TYPE_RELATION, nameMapping, true);
                                        t.addItem(m);
                                        crmPairTypes.put(nameMapping, t);
                                    }
                                }
                                else {
                                    throw new MtasConfigException("unknown mapping type "
                                            + typeMapping + " or missing name");
                                }
                            }
                        }
                    }
                }
                else if (current.name.equals("functions")) {
                    for (int j = 0; j < current.children.size(); j++) {
                        if (current.children.get(j).name.equals("function")) {
                            MtasConfiguration function = current.children.get(j);
                            String nameFunction = function.attributes.get("name");
                            String typeFunction = function.attributes.get("type");
                            String splitFunction = function.attributes.get("split");
                            if (nameFunction != null && typeFunction != null) {
                                MtasCRMParserFunction mtasCRMParserFunction = new MtasCRMParserFunction(
                                        typeFunction, splitFunction);
                                if (!functions.containsKey(typeFunction)) {
                                    functions.put(typeFunction,
                                            new HashMap<String, MtasCRMParserFunction>());
                                }
                                functions.get(typeFunction).put(nameFunction,
                                        mtasCRMParserFunction);
                                MtasConfiguration subCurrent = current.children.get(j);
                                for (int k = 0; k < subCurrent.children.size(); k++) {
                                    if (subCurrent.children.get(k).name.equals("condition")) {
                                        MtasConfiguration subSubCurrent = subCurrent.children
                                                .get(k);
                                        if (subSubCurrent.attributes.containsKey("value")) {
                                            String[] valuesCondition = subSubCurrent.attributes
                                                    .get("value").split(Pattern.quote(","));
                                            ArrayList<MtasCRMParserFunctionOutput> valueOutputList = new ArrayList<>();
                                            for (int l = 0; l < subSubCurrent.children
                                                    .size(); l++) {
                                                if (subSubCurrent.children.get(l).name
                                                        .equals("output")) {
                                                    String valueOutput = subSubCurrent.children
                                                            .get(l).attributes.get("value");
                                                    String nameOutput = subSubCurrent.children
                                                            .get(l).attributes.get("name");
                                                    if (nameOutput != null) {
                                                        MtasCRMParserFunctionOutput o = new MtasCRMParserFunctionOutput(
                                                                nameOutput, valueOutput);
                                                        valueOutputList.add(o);
                                                    }
                                                }
                                            }
                                            if (!valueOutputList.isEmpty()) {
                                                for (String valueCondition : valuesCondition) {
                                                    if (mtasCRMParserFunction.output
                                                            .containsKey(valueCondition)) {
                                                        mtasCRMParserFunction.output
                                                                .get(valueCondition)
                                                                .addAll((Collection<? extends MtasCRMParserFunctionOutput>) valueOutputList
                                                                        .clone());
                                                    }
                                                    else {
                                                        mtasCRMParserFunction.output.put(
                                                                valueCondition,
                                                                (ArrayList<MtasCRMParserFunctionOutput>) valueOutputList
                                                                        .clone());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
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
        AtomicInteger position = new AtomicInteger(0);
        MtasCRMAncestors unknownAncestors = new MtasCRMAncestors();

        Map<String, Set<Integer>> idPositions = new HashMap<>();
        Map<String, Integer[]> idOffsets = new HashMap<>();

        Map<String, Map<Integer, Set<String>>> updateList = createUpdateList();
        Map<String, List<MtasParserObject>> currentList = createCurrentList();

        tokenCollection = new MtasTokenCollection();
        MtasTokenIdFactory mtasTokenIdFactory = new MtasTokenIdFactory();
        try (MtasBufferedReader br = new MtasBufferedReader(reader)) {
            String line;
            int currentOffset;
            int previousOffset = br.getPosition();
            MtasParserObject currentObject;
            Pattern headerPattern = Pattern.compile("^@ @ @(.*)$");
            Pattern regularPattern = Pattern
                    .compile("^([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+)$");
            Matcher matcherHeader;
            Matcher matcherRegular = null;
            Set<MtasParserObject> newPreviousSentence = new HashSet<>();
            Set<MtasParserObject> previousSentence = new HashSet<>();
            Set<MtasParserObject> newPreviousClause = new HashSet<>();
            Set<MtasParserObject> previousClause = new HashSet<>();
            String[] matcherList = new String[8];
            while ((line = br.readLine()) != null) {
                currentOffset = br.getPosition();
                matcherHeader = headerPattern.matcher(line.trim());
                matcherRegular = regularPattern.matcher(line.trim());
                if (matcherRegular.matches()) {
                    newPreviousSentence.clear();
                    matcherList = createMatcherList(matcherRegular);
                    for (int i = 4; i < 8; i++) {
                        List<MtasCRMParserFunctionOutput> functionOutputList = new ArrayList<>();
                        Set<MtasParserObject> tmpList = processCRMSentence(mtasTokenIdFactory,
                                String.valueOf(i), matcherList[i], currentOffset,
                                functionOutputList, unknownAncestors, currentList, updateList,
                                idPositions, idOffsets, previousSentence, previousClause);
                        if (tmpList != null) {
                            newPreviousSentence.addAll(tmpList);
                        }
                        for (MtasCRMParserFunctionOutput functionOutput : functionOutputList) {
                            tmpList = processCRMSentence(mtasTokenIdFactory, functionOutput.name,
                                    functionOutput.value, currentOffset, functionOutputList,
                                    unknownAncestors, currentList, updateList, idPositions,
                                    idOffsets, previousSentence, previousClause);
                            if (tmpList != null) {
                                newPreviousSentence.addAll(tmpList);
                            }
                        }
                    }
                    if (!newPreviousSentence.isEmpty()) {
                        previousSentence.clear();
                        previousSentence.addAll(newPreviousSentence);
                    }
                    newPreviousClause.clear();
                    for (int i = 4; i < 8; i++) {
                        ArrayList<MtasCRMParserFunctionOutput> functionOutputList = new ArrayList<>();
                        Set<MtasParserObject> tmpList = processCRMClause(mtasTokenIdFactory,
                                String.valueOf(i), matcherList[i], currentOffset,
                                functionOutputList, unknownAncestors, currentList, updateList,
                                idPositions, idOffsets, previousClause);
                        if (tmpList != null) {
                            newPreviousClause.addAll(tmpList);
                        }
                        for (MtasCRMParserFunctionOutput functionOutput : functionOutputList) {
                            tmpList = processCRMClause(mtasTokenIdFactory, functionOutput.name,
                                    functionOutput.value, currentOffset, functionOutputList,
                                    unknownAncestors, currentList, updateList, idPositions,
                                    idOffsets, previousClause);
                            if (tmpList != null) {
                                newPreviousClause.addAll(tmpList);
                            }
                        }
                    }
                    if (!newPreviousClause.isEmpty()) {
                        previousClause.clear();
                        previousClause.addAll(newPreviousClause);
                    }
                }

                if (matcherRegular.matches() && !matcherHeader.matches()) {
                    matcherRegular = regularPattern.matcher(line.trim());
                    if (matcherRegular.matches()) {
                        // regular line - start word
                        currentObject = new MtasParserObject(wordType);
                        currentObject.setOffsetStart(previousOffset);
                        currentObject.setRealOffsetStart(previousOffset);
                        currentObject.setUnknownAncestorNumber(unknownAncestors.unknown);
                        if (!prevalidateObject(currentObject, currentList)) {
                            unknownAncestors.unknown++;
                        }
                        else {
                            int p = position.getAndIncrement();
                            currentObject.addPosition(p);
                            currentObject.objectId = "word_" + p;
                            currentList.get(MAPPING_TYPE_WORD).add(currentObject);
                            unknownAncestors.unknown = 0;
                            // check for crmPair
                            for (int i = 0; i < 8; i++) {
                                List<MtasCRMParserFunctionOutput> functionOutputList = new ArrayList<>();
                                processCRMPair(mtasTokenIdFactory, p, String.valueOf(i),
                                        matcherList[i], currentOffset, functionOutputList,
                                        unknownAncestors, currentList, updateList, idPositions,
                                        idOffsets);
                                for (MtasCRMParserFunctionOutput functionOutput : functionOutputList) {
                                    processCRMPair(mtasTokenIdFactory, p, functionOutput.name,
                                            functionOutput.value, currentOffset, functionOutputList,
                                            unknownAncestors, currentList, updateList, idPositions,
                                            idOffsets);
                                }
                            }
                            // compute word annotations
                            for (int i = 0; i < 8; i++) {
                                ArrayList<MtasCRMParserFunctionOutput> functionOutputList = new ArrayList<>();
                                functionOutputList.addAll(processWordAnnotation(mtasTokenIdFactory,
                                        String.valueOf(i), matcherList[i], previousOffset,
                                        currentOffset, unknownAncestors, currentList, updateList,
                                        idPositions, idOffsets));
                                for (MtasCRMParserFunctionOutput functionOutput : functionOutputList) {
                                    processWordAnnotation(mtasTokenIdFactory, functionOutput.name,
                                            functionOutput.value, previousOffset, currentOffset,
                                            unknownAncestors, currentList, updateList, idPositions,
                                            idOffsets);
                                }
                            }
                        }
                        // finish word
                        if (unknownAncestors.unknown > 0) {
                            unknownAncestors.unknown--;
                        }
                        else {
                            currentObject = currentList.get(MAPPING_TYPE_WORD)
                                    .remove(currentList.get(MAPPING_TYPE_WORD).size() - 1);
                            assert unknownAncestors.unknown == 0 : "error in administration "
                                    + currentObject.getType().getName();
                            currentObject.setText(null);
                            currentObject.setOffsetEnd(currentOffset - 1);
                            currentObject.setRealOffsetEnd(currentOffset - 1);
                            // update ancestor groups with position and offset
                            for (MtasParserObject currentGroup : currentList
                                    .get(MAPPING_TYPE_GROUP)) {
                                currentGroup.addPositions(currentObject.getPositions());
                                currentGroup.addOffsetStart(currentObject.getOffsetStart());
                                currentGroup.addOffsetEnd(currentObject.getOffsetEnd());
                            }
                            idPositions.put(currentObject.getId(), currentObject.getPositions());
                            idOffsets.put(currentObject.getId(), currentObject.getOffset());
                            currentObject.updateMappings(idPositions, idOffsets);
                            unknownAncestors.unknown = currentObject.getUnknownAncestorNumber();
                            computeMappingsFromObject(mtasTokenIdFactory, currentObject,
                                    currentList, updateList);
                        }

                    }
                    else {
                        // System.out.println("PROBLEM: " + line);
                    }
                }
                previousOffset = br.getPosition();
            }
            closePrevious(mtasTokenIdFactory, previousSentence, previousOffset, unknownAncestors,
                    currentList, updateList, idPositions, idOffsets);
            closePrevious(mtasTokenIdFactory, previousClause, previousOffset, unknownAncestors,
                    currentList, updateList, idPositions, idOffsets);
        }
        catch (IOException e) {
            log.debug("Error", e);
            throw new MtasParserException(e.getMessage());
        }
        // final check
        tokenCollection.check(autorepair, makeunique);
        return tokenCollection;

    }

    private String[] createMatcherList(Matcher matcher)
    {
        String[] list = new String[8];
        String value;
        for (int i = 0; i < 8; i++) {
            value = matcher.group((i + 1));
            if (filterReplace.containsKey(i)) {
                for (Entry<String, String> entry : filterReplace.get(i).entrySet()) {
                    value = value.replaceAll(Pattern.quote(entry.getKey()), entry.getValue());
                }
            }
            list[i] = value;
        }
        return list;
    }

    /**
     * Process word annotation.
     *
     * @param mtasTokenIdFactory
     *            the mtas token id factory
     * @param name
     *            the name
     * @param text
     *            the text
     * @param previousOffset
     *            the previous offset
     * @param currentOffset
     *            the current offset
     * @param unknownAncestors
     *            the unknown ancestors
     * @param currentList
     *            the current list
     * @param updateList
     *            the update list
     * @param idPositions
     *            the id positions
     * @param idOffsets
     *            the id offsets
     * @return the list
     * @throws MtasParserException
     *             the mtas parser exception
     * @throws MtasConfigException
     *             the mtas config exception
     */
    private List<MtasCRMParserFunctionOutput> processWordAnnotation(
            MtasTokenIdFactory mtasTokenIdFactory, String name, String text, Integer previousOffset,
            Integer currentOffset, MtasCRMAncestors unknownAncestors,
            Map<String, List<MtasParserObject>> currentList,
            Map<String, Map<Integer, Set<String>>> updateList,
            Map<String, Set<Integer>> idPositions, Map<String, Integer[]> idOffsets)
        throws MtasParserException, MtasConfigException
    {
        MtasParserType tmpCurrentType;
        MtasParserObject currentObject;
        List<MtasCRMParserFunctionOutput> functionOutputList = new ArrayList<>();
        if ((tmpCurrentType = wordAnnotationTypes.get(name)) != null) {
            // start word annotation
            currentObject = new MtasParserObject(tmpCurrentType);
            currentObject.setRealOffsetStart(previousOffset);
            currentObject.addPositions(currentList.get(MAPPING_TYPE_WORD)
                    .get((currentList.get(MAPPING_TYPE_WORD).size() - 1)).getPositions());
            currentObject.setUnknownAncestorNumber(unknownAncestors.unknown);
            if (!prevalidateObject(currentObject, currentList)) {
                unknownAncestors.unknown++;
            }
            else {
                currentList.get(MAPPING_TYPE_WORD_ANNOTATION).add(currentObject);
                unknownAncestors.unknown = 0;
            }
            // finish word annotation
            if (unknownAncestors.unknown > 0) {
                unknownAncestors.unknown--;
            }
            else {
                currentObject = currentList.get(MAPPING_TYPE_WORD_ANNOTATION)
                        .remove(currentList.get(MAPPING_TYPE_WORD_ANNOTATION).size() - 1);
                assert unknownAncestors.unknown == 0 : "error in administration "
                        + currentObject.getType().getName();
                if (functions.containsKey(MAPPING_TYPE_WORD_ANNOTATION)
                        && functions.get(MAPPING_TYPE_WORD_ANNOTATION).containsKey(name)
                        && text != null) {
                    MtasCRMParserFunction function = functions.get(MAPPING_TYPE_WORD_ANNOTATION)
                            .get(name);
                    String[] value;
                    if (function.split != null) {
                        value = text.split(Pattern.quote(function.split));
                    }
                    else {
                        value = new String[] { text };
                    }
                    for (int c = 0; c < value.length; c++) {
                        if (function.output.containsKey(value[c])) {
                            functionOutputList.addAll(function.output.get(value[c]));
                        }
                    }
                }
                currentObject.setText(text);
                currentObject.setRealOffsetEnd(currentOffset - 1);
                idPositions.put(currentObject.getId(), currentObject.getPositions());
                idOffsets.put(currentObject.getId(), currentObject.getOffset());
                // offset always null, so update later with word (should be possible)
                if ((currentObject.getId() != null)
                        && (!currentList.get(MAPPING_TYPE_WORD).isEmpty())) {
                    currentList.get(MAPPING_TYPE_WORD)
                            .get((currentList.get(MAPPING_TYPE_WORD).size() - 1))
                            .addUpdateableIdWithOffset(currentObject.getId());
                }
                currentObject.updateMappings(idPositions, idOffsets);
                unknownAncestors.unknown = currentObject.getUnknownAncestorNumber();
                computeMappingsFromObject(mtasTokenIdFactory, currentObject, currentList,
                        updateList);
            }
        }
        return functionOutputList;
    }

    /**
     * Process CRM sentence.
     *
     * @param mtasTokenIdFactory
     *            the mtas token id factory
     * @param name
     *            the name
     * @param text
     *            the text
     * @param currentOffset
     *            the current offset
     * @param functionOutputList
     *            the function output list
     * @param unknownAncestors
     *            the unknown ancestors
     * @param currentList
     *            the current list
     * @param updateList
     *            the update list
     * @param idPositions
     *            the id positions
     * @param idOffsets
     *            the id offsets
     * @param previous
     *            the previous
     * @param previousClause
     *            the previous clause
     * @return the sets the
     * @throws MtasParserException
     *             the mtas parser exception
     * @throws MtasConfigException
     *             the mtas config exception
     */
    private Set<MtasParserObject> processCRMSentence(MtasTokenIdFactory mtasTokenIdFactory,
            String name, String text, Integer currentOffset,
            List<MtasCRMParserFunctionOutput> functionOutputList, MtasCRMAncestors unknownAncestors,
            Map<String, List<MtasParserObject>> currentList,
            Map<String, Map<Integer, Set<String>>> updateList,
            Map<String, Set<Integer>> idPositions, Map<String, Integer[]> idOffsets,
            Set<MtasParserObject> previous, Set<MtasParserObject> previousClause)
        throws MtasParserException, MtasConfigException
    {
        MtasParserType tmpCurrentType;
        MtasParserObject currentObject;
        if ((tmpCurrentType = crmSentenceTypes.get(name)) != null) {
            String filteredText = text.replaceAll("[^0-9\\-]", "");
            currentObject = new MtasParserObject(tmpCurrentType);
            currentObject.setUnknownAncestorNumber(unknownAncestors.unknown);
            currentObject.setRealOffsetStart(currentOffset);
            currentObject.setText(filteredText);
            if (!prevalidateObject(currentObject, currentList)) {
                return new HashSet<>();
            }
            else {
                closePrevious(mtasTokenIdFactory, previousClause, currentOffset, unknownAncestors,
                        currentList, updateList, idPositions, idOffsets);
                closePrevious(mtasTokenIdFactory, previous, currentOffset, unknownAncestors,
                        currentList, updateList, idPositions, idOffsets);
                previous.clear();
                currentList.get(MAPPING_TYPE_GROUP).add(currentObject);
                unknownAncestors.unknown = 0;
                return new HashSet<>(Arrays.asList(currentObject));
            }
        }
        return new HashSet<>();
    }

    /**
     * Process CRM clause.
     *
     * @param mtasTokenIdFactory
     *            the mtas token id factory
     * @param name
     *            the name
     * @param text
     *            the text
     * @param currentOffset
     *            the current offset
     * @param functionOutputList
     *            the function output list
     * @param unknownAncestors
     *            the unknown ancestors
     * @param currentList
     *            the current list
     * @param updateList
     *            the update list
     * @param idPositions
     *            the id positions
     * @param idOffsets
     *            the id offsets
     * @param previous
     *            the previous
     * @return the sets the
     * @throws MtasParserException
     *             the mtas parser exception
     * @throws MtasConfigException
     *             the mtas config exception
     */
    private Set<MtasParserObject> processCRMClause(MtasTokenIdFactory mtasTokenIdFactory,
            String name, String text, Integer currentOffset,
            List<MtasCRMParserFunctionOutput> functionOutputList, MtasCRMAncestors unknownAncestors,
            Map<String, List<MtasParserObject>> currentList,
            Map<String, Map<Integer, Set<String>>> updateList,
            Map<String, Set<Integer>> idPositions, Map<String, Integer[]> idOffsets,
            Set<MtasParserObject> previous)
        throws MtasParserException, MtasConfigException
    {
        MtasParserType tmpCurrentType;
        MtasParserObject currentObject;
        if ((tmpCurrentType = crmClauseTypes.get(name)) != null) {
            String filteredText = text.replaceAll("[^0-9\\-]", "");
            currentObject = new MtasParserObject(tmpCurrentType);
            currentObject.setUnknownAncestorNumber(unknownAncestors.unknown);
            currentObject.setRealOffsetStart(currentOffset);
            currentObject.setText(filteredText);
            if (!prevalidateObject(currentObject, currentList)) {
                return new HashSet<>();
            }
            else {
                closePrevious(mtasTokenIdFactory, previous, currentOffset, unknownAncestors,
                        currentList, updateList, idPositions, idOffsets);
                previous.clear();
                currentList.get(MAPPING_TYPE_GROUP).add(currentObject);
                unknownAncestors.unknown = 0;
                return new HashSet<>(Arrays.asList(currentObject));
            }
        }
        return new HashSet<>();
    }

    /**
     * Close previous.
     *
     * @param mtasTokenIdFactory
     *            the mtas token id factory
     * @param previous
     *            the previous
     * @param currentOffset
     *            the current offset
     * @param unknownAncestors
     *            the unknown ancestors
     * @param currentList
     *            the current list
     * @param updateList
     *            the update list
     * @param idPositions
     *            the id positions
     * @param idOffsets
     *            the id offsets
     * @throws MtasParserException
     *             the mtas parser exception
     * @throws MtasConfigException
     *             the mtas config exception
     */
    private void closePrevious(MtasTokenIdFactory mtasTokenIdFactory,
            Set<MtasParserObject> previous, Integer currentOffset,
            MtasCRMAncestors unknownAncestors, Map<String, List<MtasParserObject>> currentList,
            Map<String, Map<Integer, Set<String>>> updateList,
            Map<String, Set<Integer>> idPositions, Map<String, Integer[]> idOffsets)
        throws MtasParserException, MtasConfigException
    {
        for (MtasParserObject previousObject : previous) {
            previousObject.setRealOffsetEnd(currentOffset);
            idPositions.put(previousObject.getId(), previousObject.getPositions());
            idOffsets.put(previousObject.getId(), previousObject.getOffset());
            previousObject.updateMappings(idPositions, idOffsets);
            unknownAncestors.unknown = previousObject.getUnknownAncestorNumber();
            computeMappingsFromObject(mtasTokenIdFactory, previousObject, currentList, updateList);
            currentList.get(MAPPING_TYPE_GROUP).remove(previousObject);
        }
    }

    /**
     * Process CRM pair.
     *
     * @param mtasTokenIdFactory
     *            the mtas token id factory
     * @param position
     *            the position
     * @param name
     *            the name
     * @param text
     *            the text
     * @param currentOffset
     *            the current offset
     * @param functionOutputList
     *            the function output list
     * @param unknownAncestors
     *            the unknown ancestors
     * @param currentList
     *            the current list
     * @param updateList
     *            the update list
     * @param idPositions
     *            the id positions
     * @param idOffsets
     *            the id offsets
     * @throws MtasParserException
     *             the mtas parser exception
     * @throws MtasConfigException
     *             the mtas config exception
     */
    private void processCRMPair(MtasTokenIdFactory mtasTokenIdFactory, int position, String name,
            String text, Integer currentOffset,
            List<MtasCRMParserFunctionOutput> functionOutputList, MtasCRMAncestors unknownAncestors,
            Map<String, List<MtasParserObject>> currentList,
            Map<String, Map<Integer, Set<String>>> updateList,
            Map<String, Set<Integer>> idPositions, Map<String, Integer[]> idOffsets)
        throws MtasParserException, MtasConfigException
    {

        MtasParserType tmpCurrentType;
        MtasParserObject currentObject;

        if ((tmpCurrentType = crmPairTypes.get(name)) != null) {
            // get history
            HashMap<String, MtasParserObject> currentNamePairHistory;
            if (!historyPair.containsKey(name)) {
                currentNamePairHistory = new HashMap<>();
                historyPair.put(name, currentNamePairHistory);
            }
            else {
                currentNamePairHistory = historyPair.get(name);
            }
            Matcher m = pairPattern.matcher(text);
            if (m.find()) {
                String thisKey = m.group(1) + m.group(2);
                String otherKey = (m.group(1).equals("b") ? "e" : "b") + m.group(2);
                if (currentNamePairHistory.containsKey(otherKey)) {
                    currentObject = currentNamePairHistory.remove(otherKey);
                    currentObject.setText(currentObject.getText() + "+" + text);
                    currentObject.addPosition(position);
                    processFunctions(name, text, MAPPING_TYPE_CRM_PAIR, functionOutputList);
                    currentObject.setRealOffsetEnd(currentOffset + 1);
                    currentObject.setOffsetEnd(currentOffset + 1);
                    idPositions.put(currentObject.getId(), currentObject.getPositions());
                    idOffsets.put(currentObject.getId(), currentObject.getOffset());
                    currentObject.updateMappings(idPositions, idOffsets);
                    unknownAncestors.unknown = currentObject.getUnknownAncestorNumber();
                    computeMappingsFromObject(mtasTokenIdFactory, currentObject, currentList,
                            updateList);
                }
                else {
                    currentObject = new MtasParserObject(tmpCurrentType);
                    currentObject.setUnknownAncestorNumber(unknownAncestors.unknown);
                    currentObject.setRealOffsetStart(currentOffset);
                    currentObject.setOffsetStart(currentOffset);
                    currentObject.setText(text);
                    currentObject.addPosition(position);
                    if (!prevalidateObject(currentObject, currentList)) {
                        unknownAncestors.unknown++;
                    }
                    else {
                        currentNamePairHistory.put(thisKey, currentObject);
                        processFunctions(name, text, MAPPING_TYPE_CRM_PAIR, functionOutputList);
                        currentObject.setRealOffsetEnd(currentOffset + 1);
                        currentObject.setOffsetEnd(currentOffset + 1);
                        idPositions.put(currentObject.getId(), currentObject.getPositions());
                        idOffsets.put(currentObject.getId(), currentObject.getOffset());
                        // offset always null, so update later with word (should be
                        // possible)
                        if ((currentObject.getId() != null)
                                && (!currentList.get(MAPPING_TYPE_WORD).isEmpty())) {
                            currentList.get(MAPPING_TYPE_WORD)
                                    .get((currentList.get(MAPPING_TYPE_WORD).size() - 1))
                                    .addUpdateableIdWithOffset(currentObject.getId());
                        }

                    }
                }
            }

        }

    }

    /**
     * Process functions.
     *
     * @param name
     *            the name
     * @param text
     *            the text
     * @param type
     *            the type
     * @param functionOutputList
     *            the function output list
     */
    private void processFunctions(String name, String text, String type,
            List<MtasCRMParserFunctionOutput> functionOutputList)
    {
        if (functions.containsKey(type) && functions.get(type).containsKey(name) && text != null) {
            MtasCRMParserFunction function = functions.get(type).get(name);
            String[] value;
            if (function.split != null) {
                value = text.split(Pattern.quote(function.split));
            }
            else {
                value = new String[] { text };
            }
            for (int c = 0; c < value.length; c++) {
                boolean checkedEmpty = false;
                if (value[c].equals("")) {
                    checkedEmpty = true;
                }
                if (function.output.containsKey(value[c])) {
                    ArrayList<MtasCRMParserFunctionOutput> list = function.output.get(value[c]);
                    for (MtasCRMParserFunctionOutput listItem : list) {
                        functionOutputList.add(listItem.create(value[c]));
                    }
                }
                if (!checkedEmpty && function.output.containsKey("")) {
                    ArrayList<MtasCRMParserFunctionOutput> list = function.output.get("");
                    for (MtasCRMParserFunctionOutput listItem : list) {
                        functionOutputList.add(listItem.create(value[c]));
                    }
                }
            }
        }
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
        text.append("type: " + wordAnnotationTypes.size() + " x wordAnnotation");
        text.append(printConfigTypes(wordAnnotationTypes));
        text.append("=== CONFIGURATION ===\n");
        return text.toString();
    }

    /**
     * Prints the config types.
     *
     * @param types
     *            the types
     * @return the string
     */
    private String printConfigTypes(HashMap<?, MtasParserType<MtasParserMapping<?>>> types)
    {
        StringBuilder text = new StringBuilder();
        for (Entry<?, MtasParserType<MtasParserMapping<?>>> entry : types.entrySet()) {
            text.append(
                    "- " + entry.getKey() + ": " + entry.getValue().items.size() + " mapping(s)\n");
            for (int i = 0; i < entry.getValue().items.size(); i++) {
                text.append("\t" + entry.getValue().items.get(i) + "\n");
            }
        }
        return text.toString();
    }

    /**
     * The Class MtasCRMAncestors.
     */
    private static class MtasCRMAncestors
    {

        /** The unknown. */
        public int unknown = 0;
    }

    /**
     * The Class MtasCRMParserFunction.
     */
    private static class MtasCRMParserFunction
    {

        /** The split. */
        public String split;

        /** The output. */
        public Map<String, ArrayList<MtasCRMParserFunctionOutput>> output;

        /**
         * Instantiates a new mtas CRM parser function.
         *
         * @param type
         *            the type
         * @param split
         *            the split
         */
        public MtasCRMParserFunction(String type, String split)
        {
            this.split = split;
            output = new HashMap<>();
        }

    }

    /**
     * The Class MtasCRMParserFunctionOutput.
     */
    private class MtasCRMParserFunctionOutput
    {

        /** The name. */
        public String name;

        /** The value. */
        public String value;

        /**
         * Instantiates a new mtas CRM parser function output.
         *
         * @param name
         *            the name
         * @param value
         *            the value
         */
        public MtasCRMParserFunctionOutput(String name, String value)
        {
            this.name = name;
            this.value = value;
        }

        /**
         * Creates the.
         *
         * @param originalValue
         *            the original value
         * @return the mtas CRM parser function output
         */
        public MtasCRMParserFunctionOutput create(String originalValue)
        {
            if (value != null) {
                return this;
            }
            else {
                return new MtasCRMParserFunctionOutput(name, originalValue);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return "MtasCRMParserFunctionOutput[" + name + "," + value + "]";
        }
    }

    /**
     * The Class MtasCRMParserMappingWordAnnotation.
     */
    private class MtasCRMParserMappingWordAnnotation
        extends MtasParserMapping<MtasCRMParserMappingWordAnnotation>
    {

        /**
         * Instantiates a new mtas CRM parser mapping word annotation.
         */
        public MtasCRMParserMappingWordAnnotation()
        {
            super();
            this.position = SOURCE_OWN;
            this.realOffset = SOURCE_OWN;
            this.offset = SOURCE_ANCESTOR_WORD;
            this.type = MAPPING_TYPE_WORD_ANNOTATION;
        }

        /*
         * (non-Javadoc)
         * 
         * @see mtas.analysis.parser.MtasParser.MtasParserMapping#self()
         */
        @Override
        protected MtasCRMParserMappingWordAnnotation self()
        {
            return this;
        }
    }

    /**
     * The Class MtasCRMParserMappingCRMSentence.
     */
    private class MtasCRMParserMappingCRMSentence
        extends MtasParserMapping<MtasCRMParserMappingCRMSentence>
    {

        /**
         * Instantiates a new mtas CRM parser mapping CRM sentence.
         */
        public MtasCRMParserMappingCRMSentence()
        {
            super();
            this.position = SOURCE_OWN;
            this.realOffset = SOURCE_OWN;
            this.offset = SOURCE_OWN;
            this.type = MAPPING_TYPE_GROUP;
        }

        /*
         * (non-Javadoc)
         * 
         * @see mtas.analysis.parser.MtasBasicParser.MtasParserMapping#self()
         */
        @Override
        protected MtasCRMParserMappingCRMSentence self()
        {
            return this;
        }
    }

    /**
     * The Class MtasCRMParserMappingCRMPair.
     */
    private class MtasCRMParserMappingCRMPair
        extends MtasParserMapping<MtasCRMParserMappingCRMPair>
    {

        /**
         * Instantiates a new mtas CRM parser mapping CRM pair.
         */
        public MtasCRMParserMappingCRMPair()
        {
            super();
            this.position = SOURCE_OWN;
            this.realOffset = SOURCE_OWN;
            this.offset = SOURCE_OWN;
            this.type = MAPPING_TYPE_RELATION;
        }

        /*
         * (non-Javadoc)
         * 
         * @see mtas.analysis.parser.MtasBasicParser.MtasParserMapping#self()
         */
        @Override
        protected MtasCRMParserMappingCRMPair self()
        {
            return this;
        }
    }

}
