package mtas.analysis.parser;

import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtas.analysis.token.MtasToken;
import mtas.analysis.token.MtasTokenCollection;
import mtas.analysis.token.MtasTokenIdFactory;
import mtas.analysis.util.MtasConfigException;
import mtas.analysis.util.MtasParserException;
import mtas.analysis.util.MtasConfiguration;

/**
 * The Class MtasXMLParser.
 */
abstract class MtasXMLParser extends MtasBasicParser {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(MtasXMLParser.class);

  /** The namespace URI. */
  protected String namespaceURI = null;

  /** The namespace URI id. */
  protected String namespaceURI_id = null;

  /** The root tag. */
  protected String rootTag = null;

  /** The content tag. */
  protected String contentTag = null;

  /** The allow non content. */
  protected boolean allowNonContent = false;

  /** The relation key map. */
  private Map<String, SortedSet<String>> relationKeyMap = new HashMap<>();

  /** The q names. */
  private Map<String, QName> qNames = new HashMap<>();

  /** The relation types. */
  private Map<QName, MtasParserType<MtasParserMapping<?>>> relationTypes = new HashMap<>();

  /** The relation annotation types. */
  private Map<QName, MtasParserType<MtasParserMapping<?>>> relationAnnotationTypes = new HashMap<>();

  /** The ref types. */
  private Map<QName, MtasParserType<MtasParserMapping<?>>> refTypes = new HashMap<>();

  /** The group types. */
  private Map<QName, MtasParserType<MtasParserMapping<?>>> groupTypes = new HashMap<>();

  /** The group annotation types. */
  private Map<QName, MtasParserType<MtasParserMapping<?>>> groupAnnotationTypes = new HashMap<>();

  /** The word types. */
  private Map<QName, MtasParserType<MtasParserMapping<?>>> wordTypes = new HashMap<>();

  /** The word annotation types. */
  private Map<QName, MtasParserType<MtasParserMapping<?>>> wordAnnotationTypes = new HashMap<>();

  /** The variable types. */
  private Map<QName, MtasParserType<MtasParserVariable>> variableTypes = new HashMap<>();

  /** The Constant XML_VARIABLES. */
  private static final String XML_VARIABLES = "variables";

  /** The Constant XML_VARIABLE. */
  private static final String XML_VARIABLE = "variable";

  /** The Constant XML_VARIABLE_NAME. */
  private static final String XML_VARIABLE_NAME = "name";

  /** The Constant XML_VARIABLE_VALUE. */
  private static final String XML_VARIABLE_VALUE = "value";

  /** The Constant XML_REFERENCES. */
  private static final String XML_REFERENCES = "references";

  /** The Constant XML_REFERENCE. */
  private static final String XML_REFERENCE = "reference";

  /** The Constant XML_REFERENCE_NAME. */
  private static final String XML_REFERENCE_NAME = "name";

  /** The Constant XML_REFERENCE_REF. */
  private static final String XML_REFERENCE_REF = "ref";

  /** The Constant XML_MAPPINGS. */
  private static final String XML_MAPPINGS = "mappings";

  /** The Constant XML_MAPPING. */
  private static final String XML_MAPPING = "mapping";

  /** The Constant XML_MAPPING_TYPE. */
  private static final String XML_MAPPING_TYPE = "type";

  /** The Constant XML_MAPPING_NAME. */
  private static final String XML_MAPPING_NAME = "name";

  /**
   * Instantiates a new mtas XML parser.
   *
   * @param config
   *          the config
   */
  public MtasXMLParser(MtasConfiguration config) {
    super(config);
    try {
      initParser();
      // System.out.print(printConfig());
    } catch (MtasConfigException e) {
      log.error("Error", e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.analysis.parser.MtasParser#printConfig()
   */
  @Override
  public String printConfig() {
    StringBuilder text = new StringBuilder();
    text.append("=== CONFIGURATION ===\n");
    text.append("type: " + variableTypes.size() + " x variable\n");
    text.append(printConfigVariableTypes(variableTypes));
    text.append("type: " + groupTypes.size() + " x group\n");
    text.append(printConfigMappingTypes(groupTypes));
    text.append("type: " + groupAnnotationTypes.size() + " x groupAnnotation");
    text.append(printConfigMappingTypes(groupAnnotationTypes));
    text.append("type: " + wordTypes.size() + " x word\n");
    text.append(printConfigMappingTypes(wordTypes));
    text.append("type: " + wordAnnotationTypes.size() + " x wordAnnotation");
    text.append(printConfigMappingTypes(wordAnnotationTypes));
    text.append("type: " + relationTypes.size() + " x relation\n");
    text.append(printConfigMappingTypes(relationTypes));
    text.append("type: " + relationAnnotationTypes.size() + " x relationAnnotation\n");
    text.append(printConfigMappingTypes(relationAnnotationTypes));
    text.append("type: " + refTypes.size() + " x references\n");
    text.append(printConfigMappingTypes(refTypes));
    text.append("=== CONFIGURATION ===\n");
    return text.toString();
  }

  /**
   * Prints the config mapping types.
   *
   * @param types
   *          the types
   * @return the string
   */
  private String printConfigMappingTypes(Map<QName, MtasParserType<MtasParserMapping<?>>> types) {
    StringBuilder text = new StringBuilder();
    for (Entry<QName, MtasParserType<MtasParserMapping<?>>> entry : types.entrySet()) {
      text.append("- " + entry.getKey().getLocalPart() + ": " + entry.getValue().items.size() + " mapping(s)\n");
      for (int i = 0; i < entry.getValue().items.size(); i++) {
        text.append("\t" + entry.getValue().items.get(i) + "\n");
      }
    }
    return text.toString();
  }

  /**
   * Prints the config variable types.
   *
   * @param types
   *          the types
   * @return the string
   */
  private String printConfigVariableTypes(Map<QName, MtasParserType<MtasParserVariable>> types) {
    StringBuilder text = new StringBuilder();
    for (Entry<QName, MtasParserType<MtasParserVariable>> entry : types.entrySet()) {
      text.append("- " + entry.getKey().getLocalPart() + ": " + entry.getValue().items.size() + " variables(s)\n");
      for (int i = 0; i < entry.getValue().items.size(); i++) {
        text.append("\t" + entry.getValue().items.get(i) + "\n");
      }
    }
    return text.toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.analysis.parser.MtasParser#initParser()
   */
  @Override
  protected void initParser() throws MtasConfigException {
    super.initParser();
    if (config != null) {
      // find namespaceURI
      for (int i = 0; i < config.children.size(); i++) {
        MtasConfiguration current = config.children.get(i);
        if (current.name.equals("namespaceURI")) {
          namespaceURI = current.attributes.get("value");
        }
      }
      // loop again
      for (int i = 0; i < config.children.size(); i++) {
        MtasConfiguration current = config.children.get(i);
        if (current.name.equals(XML_VARIABLES)) {
          for (int j = 0; j < current.children.size(); j++) {
            if (current.children.get(j).name.equals(XML_VARIABLE)) {
              MtasConfiguration variable = current.children.get(j);
              String nameVariable = variable.attributes.get(XML_VARIABLE_NAME);
              String valueVariable = variable.attributes.get(XML_VARIABLE_VALUE);
              if ((nameVariable != null) && (valueVariable != null)) {
                MtasParserVariable v = new MtasParserVariable(nameVariable, valueVariable);
                v.processConfig(variable);
                QName qn = getQName(nameVariable);
                if (variableTypes.containsKey(qn)) {
                  variableTypes.get(qn).addItem(v);
                } else {
                  MtasParserType<MtasParserVariable> t = new MtasParserType<>(nameVariable, valueVariable, false);
                  t.addItem(v);
                  variableTypes.put(qn, t);
                }
              }
            }
          }
        } else if (current.name.equals(XML_REFERENCES)) {
          for (int j = 0; j < current.children.size(); j++) {
            if (current.children.get(j).name.equals(XML_REFERENCE)) {
              MtasConfiguration reference = current.children.get(j);
              String name = reference.attributes.get(XML_REFERENCE_NAME);
              String ref = reference.attributes.get(XML_REFERENCE_REF);
              if ((name != null) && (ref != null)) {
                MtasParserType<MtasParserMapping<?>> t = new MtasParserType<>(MAPPING_TYPE_REF, name, false, ref);
                refTypes.put(getQName(t.getName()), t);
              }
            }
          }
        } else if (current.name.equals(XML_MAPPINGS)) {
          for (int j = 0; j < current.children.size(); j++) {
            if (current.children.get(j).name.equals(XML_MAPPING)) {
              MtasConfiguration mapping = current.children.get(j);
              String typeMapping = mapping.attributes.get(XML_MAPPING_TYPE);
              String nameMapping = mapping.attributes.get(XML_MAPPING_NAME);
              if ((typeMapping != null) && (nameMapping != null)) {
                if (typeMapping.equals(MAPPING_TYPE_RELATION)) {
                  MtasXMLParserMappingRelation m = new MtasXMLParserMappingRelation();
                  m.processConfig(mapping);
                  QName qn = getQName(nameMapping);
                  if (relationTypes.containsKey(qn)) {
                    relationTypes.get(qn).addItem(m);
                  } else {
                    MtasParserType<MtasParserMapping<?>> t = new MtasParserType<>(typeMapping, nameMapping, false);
                    t.addItem(m);
                    relationTypes.put(qn, t);
                  }
                } else if (typeMapping.equals(MAPPING_TYPE_RELATION_ANNOTATION)) {
                  MtasXMLParserMappingRelationAnnotation m = new MtasXMLParserMappingRelationAnnotation();
                  m.processConfig(mapping);
                  QName qn = getQName(nameMapping);
                  if (relationAnnotationTypes.containsKey(qn)) {
                    relationAnnotationTypes.get(qn).addItem(m);
                  } else {
                    MtasParserType<MtasParserMapping<?>> t = new MtasParserType<>(typeMapping, nameMapping, false);
                    t.addItem(m);
                    relationAnnotationTypes.put(qn, t);
                  }
                } else if (typeMapping.equals(MAPPING_TYPE_WORD)) {
                  MtasXMLParserMappingWord m = new MtasXMLParserMappingWord();
                  m.processConfig(mapping);
                  QName qn = getQName(nameMapping);
                  if (wordTypes.containsKey(qn)) {
                    wordTypes.get(qn).addItem(m);
                  } else {
                    MtasParserType<MtasParserMapping<?>> t = new MtasParserType<>(typeMapping, nameMapping, false);
                    t.addItem(m);
                    wordTypes.put(qn, t);
                  }
                } else if (typeMapping.equals(MAPPING_TYPE_WORD_ANNOTATION)) {
                  MtasXMLParserMappingWordAnnotation m = new MtasXMLParserMappingWordAnnotation();
                  m.processConfig(mapping);
                  QName qn = getQName(nameMapping);
                  if (wordAnnotationTypes.containsKey(qn)) {
                    wordAnnotationTypes.get(qn).addItem(m);
                  } else {
                    MtasParserType<MtasParserMapping<?>> t = new MtasParserType<>(typeMapping, nameMapping, false);
                    t.addItem(m);
                    wordAnnotationTypes.put(qn, t);
                  }
                } else if (typeMapping.equals(MAPPING_TYPE_GROUP)) {
                  MtasXMLParserMappingGroup m = new MtasXMLParserMappingGroup();
                  m.processConfig(mapping);
                  QName qn = getQName(nameMapping);
                  if (groupTypes.containsKey(qn)) {
                    groupTypes.get(qn).addItem(m);
                  } else {
                    MtasParserType<MtasParserMapping<?>> t = new MtasParserType<>(typeMapping, nameMapping, false);
                    t.addItem(m);
                    groupTypes.put(qn, t);
                  }
                } else if (typeMapping.equals(MAPPING_TYPE_GROUP_ANNOTATION)) {
                  MtasXMLParserMappingGroupAnnotation m = new MtasXMLParserMappingGroupAnnotation();
                  m.processConfig(mapping);
                  QName qn = getQName(nameMapping);
                  if (groupAnnotationTypes.containsKey(qn)) {
                    groupAnnotationTypes.get(qn).addItem(m);
                  } else {
                    MtasParserType<MtasParserMapping<?>> t = new MtasParserType<>(typeMapping, nameMapping, false);
                    t.addItem(m);
                    groupAnnotationTypes.put(qn, t);
                  }
                } else {
                  throw new MtasConfigException("unknown mapping type " + typeMapping);
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
  public MtasTokenCollection createTokenCollection(Reader reader) throws MtasParserException, MtasConfigException {
    Boolean hasRoot = rootTag == null ? true : false;
    Boolean parsingContent = contentTag == null ? true : false;
    String textContent = null;
    Integer unknownAncestors = 0;
    Integer lastOffset = 0;

    AtomicInteger position = new AtomicInteger(0);
    Map<String, Set<Integer>> idPositions = new HashMap<>();
    Map<String, Integer[]> idOffsets = new HashMap<>();

    Map<String, Map<Integer, Set<String>>> updateList = createUpdateList();
    Map<String, List<MtasParserObject>> currentList = createCurrentList();
    Map<String, Map<String, String>> variables = createVariables();

    tokenCollection = new MtasTokenCollection();
    MtasTokenIdFactory mtasTokenIdFactory = new MtasTokenIdFactory();
    XMLInputFactory factory = XMLInputFactory.newInstance();
    try {
      XMLStreamReader streamReader = factory.createXMLStreamReader(reader);
      QName qname;
      try {
        int event = streamReader.getEventType();
        MtasParserType<?> currentType;
        MtasParserType<?> tmpCurrentType;
        MtasParserType<?> tmpVariableType;
        MtasParserObject currentObject = null;
        MtasParserObject variableObject = null;
        while (true) {
          switch (event) {
            case XMLStreamConstants.START_DOCUMENT:
              log.debug("start of document");
              String encodingScheme = streamReader.getCharacterEncodingScheme();
              if (encodingScheme == null) {
                // ignore for now
                log.info("No encodingScheme found, assume utf-8");
                // throw new MtasParserException("No encodingScheme found");
              } else if (!encodingScheme.equalsIgnoreCase("utf-8")) {
                throw new MtasParserException("XML not UTF-8 encoded but '" + encodingScheme + "'");
              }
              break;
            case XMLStreamConstants.END_DOCUMENT:
              log.debug("end of document");
              break;
            case XMLStreamConstants.SPACE:
              // set offset (end of start-element)
              lastOffset = streamReader.getLocation().getCharacterOffset();
              break;
            case XMLStreamConstants.START_ELEMENT:
              // get data
              qname = streamReader.getName();
              // check for rootTag
              if (!hasRoot) {
                if (qname.equals(getQName(rootTag))) {
                  hasRoot = true;
                } else {
                  throw new MtasParserException("No " + rootTag);
                }
                // parse content
              } else {
                if ((tmpVariableType = variableTypes.get(qname)) != null) {
                  variableObject = new MtasParserObject(tmpVariableType);
                  collectAttributes(variableObject, streamReader);
                  computeVariablesFromObject(variableObject, currentList, variables);
                }
                if (parsingContent) {
                  // check for relation : not within word, not within
                  // groupAnnotation
                  if ((currentList.get(MAPPING_TYPE_WORD).isEmpty())
                      && (currentList.get(MAPPING_TYPE_GROUP_ANNOTATION).isEmpty())
                      && (tmpCurrentType = relationTypes.get(qname)) != null) {
                    currentObject = new MtasParserObject(tmpCurrentType);
                    collectAttributes(currentObject, streamReader);
                    currentObject.setUnknownAncestorNumber(unknownAncestors);
                    currentObject.setRealOffsetStart(lastOffset);
                    if (!prevalidateObject(currentObject, currentList)) {
                      unknownAncestors++;
                    } else {
                      currentType = tmpCurrentType;
                      currentList.get(MAPPING_TYPE_RELATION).add(currentObject);
                      unknownAncestors = 0;
                    }
                    // check for relation annotation: not within word, but within
                    // relation
                  } else if ((currentList.get(MAPPING_TYPE_WORD).isEmpty())
                      && (!currentList.get(MAPPING_TYPE_RELATION).isEmpty())
                      && (tmpCurrentType = relationAnnotationTypes.get(qname)) != null) {
                    currentObject = new MtasParserObject(tmpCurrentType);
                    collectAttributes(currentObject, streamReader);
                    currentObject.setUnknownAncestorNumber(unknownAncestors);
                    currentObject.setRealOffsetStart(lastOffset);
                    if (!prevalidateObject(currentObject, currentList)) {
                      unknownAncestors++;
                    } else {
                      currentType = tmpCurrentType;
                      currentList.get(MAPPING_TYPE_RELATION_ANNOTATION).add(currentObject);
                      unknownAncestors = 0;
                    }
                    // check for group: not within word, not within relation, not
                    // within groupAnnotation
                  } else if ((currentList.get(MAPPING_TYPE_WORD).isEmpty())
                      && (currentList.get(MAPPING_TYPE_RELATION).isEmpty())
                      && (currentList.get(MAPPING_TYPE_GROUP_ANNOTATION).isEmpty())
                      && (tmpCurrentType = groupTypes.get(qname)) != null) {
                    currentObject = new MtasParserObject(tmpCurrentType);
                    collectAttributes(currentObject, streamReader);
                    currentObject.setUnknownAncestorNumber(unknownAncestors);
                    currentObject.setRealOffsetStart(lastOffset);
                    if (!prevalidateObject(currentObject, currentList)) {
                      unknownAncestors++;
                    } else {
                      currentType = tmpCurrentType;
                      currentList.get(MAPPING_TYPE_GROUP).add(currentObject);
                      unknownAncestors = 0;
                    }
                    // check for group annotation: not within word, not within
                    // relation, but within group
                  } else if ((currentList.get(MAPPING_TYPE_WORD).isEmpty())
                      && (currentList.get(MAPPING_TYPE_RELATION).isEmpty())
                      && (!currentList.get(MAPPING_TYPE_GROUP).isEmpty())
                      && (tmpCurrentType = groupAnnotationTypes.get(qname)) != null) {
                    currentObject = new MtasParserObject(tmpCurrentType);
                    collectAttributes(currentObject, streamReader);
                    currentObject.setUnknownAncestorNumber(unknownAncestors);
                    currentObject.setRealOffsetStart(lastOffset);
                    if (!prevalidateObject(currentObject, currentList)) {
                      unknownAncestors++;
                    } else {
                      currentType = tmpCurrentType;
                      currentList.get(MAPPING_TYPE_GROUP_ANNOTATION).add(currentObject);
                      unknownAncestors = 0;
                    }
                    // check for word: not within relation, not within
                    // groupAnnotation, not within word, not within wordAnnotation
                  } else if ((currentList.get(MAPPING_TYPE_RELATION).isEmpty())
                      && (currentList.get(MAPPING_TYPE_GROUP_ANNOTATION).isEmpty())
                      && (currentList.get(MAPPING_TYPE_WORD).isEmpty())
                      && (currentList.get(MAPPING_TYPE_WORD_ANNOTATION).isEmpty())
                      && (tmpCurrentType = wordTypes.get(qname)) != null) {
                    currentObject = new MtasParserObject(tmpCurrentType);
                    collectAttributes(currentObject, streamReader);
                    currentObject.setUnknownAncestorNumber(unknownAncestors);
                    currentObject.setOffsetStart(lastOffset);
                    currentObject.setRealOffsetStart(lastOffset);
                    if (!prevalidateObject(currentObject, currentList)) {
                      unknownAncestors++;
                    } else {
                      currentType = tmpCurrentType;
                      currentObject.addPosition(position.getAndIncrement());
                      currentList.get(MAPPING_TYPE_WORD).add(currentObject);
                      unknownAncestors = 0;
                    }
                    // check for word annotation: not within relation, not within
                    // groupAnnotation, but within word
                  } else if ((currentList.get(MAPPING_TYPE_RELATION).isEmpty())
                      && (currentList.get(MAPPING_TYPE_GROUP_ANNOTATION).isEmpty())
                      && (!currentList.get(MAPPING_TYPE_WORD).isEmpty())
                      && (tmpCurrentType = wordAnnotationTypes.get(qname)) != null) {
                    currentObject = new MtasParserObject(tmpCurrentType);
                    collectAttributes(currentObject, streamReader);
                    currentObject.addPositions(currentList.get(MAPPING_TYPE_WORD)
                        .get((currentList.get(MAPPING_TYPE_WORD).size() - 1)).getPositions());
                    currentObject.setUnknownAncestorNumber(unknownAncestors);
                    currentObject.setRealOffsetStart(lastOffset);
                    if (!prevalidateObject(currentObject, currentList)) {
                      unknownAncestors++;
                    } else {
                      currentType = tmpCurrentType;
                      currentList.get(MAPPING_TYPE_WORD_ANNOTATION).add(currentObject);
                      unknownAncestors = 0;
                    }
                    // check for references: within relation
                  } else if (!currentList.get(MAPPING_TYPE_RELATION).isEmpty()
                      && (tmpCurrentType = refTypes.get(qname)) != null) {
                    currentObject = new MtasParserObject(tmpCurrentType);
                    collectAttributes(currentObject, streamReader);
                    currentObject.setUnknownAncestorNumber(unknownAncestors);
                    currentObject.setRealOffsetStart(lastOffset);
                    if (!prevalidateObject(currentObject, currentList)) {
                      unknownAncestors++;
                    } else {
                      currentType = tmpCurrentType;
                      currentList.get(MAPPING_TYPE_REF).add(currentObject);
                      unknownAncestors = 0;
                      // add reference to ancestor relations
                      for (MtasParserObject currentRelation : currentList.get(MAPPING_TYPE_RELATION)) {
                        currentRelation.addRefId(currentObject.getAttribute(currentType.getRefAttributeName()));
                        // register mapping for relation (for recursive relations)
                        SortedSet<String> keyMapList;
                        if (currentRelation.getId() != null) {
                          if (relationKeyMap.containsKey(currentRelation.getId())) {
                            keyMapList = relationKeyMap.get(currentRelation.getId());
                          } else {
                            keyMapList = new TreeSet<>();
                            relationKeyMap.put(currentRelation.getId(), keyMapList);
                          }
                          keyMapList.add(currentObject.getAttribute(currentType.getRefAttributeName()));
                        }
                      }
                    }
                  } else {
                    unknownAncestors++;
                  }
                  // check for start content
                } else if (qname.equals(getQName(contentTag))) {
                  parsingContent = true;
                  // unexpected
                } else if (!allowNonContent) {
                  throw new MtasParserException("Unexpected " + qname.getLocalPart() + " in document");
                }
              }
              // set offset (end of start-element)
              lastOffset = streamReader.getLocation().getCharacterOffset();
              break;
            case XMLStreamConstants.END_ELEMENT:
              // set offset (end of end-element)
              lastOffset = streamReader.getLocation().getCharacterOffset();
              // get data
              qname = streamReader.getName();
              // parse content
              if (parsingContent) {
                if (unknownAncestors > 0) {
                  unknownAncestors--;
                  // check for reference: because otherwise currentList should
                  // contain no references
                } else if (!currentList.get(MAPPING_TYPE_REF).isEmpty()) {
                  if ((currentType = refTypes.get(qname)) != null) {
                    currentObject = currentList.get(MAPPING_TYPE_REF)
                        .remove(currentList.get(MAPPING_TYPE_REF).size() - 1);
                    assert currentObject.getType().equals(currentType) : "object expected to be "
                        + currentObject.getType().getName() + ", not " + currentType.getName();
                    assert unknownAncestors == 0 : "error in administration " + currentObject.getType().getName();
                    // ignore text and realOffset: not relevant
                    idPositions.put(currentObject.getId(), currentObject.getPositions());
                    idOffsets.put(currentObject.getId(), currentObject.getOffset());
                    currentObject.updateMappings(idPositions, idOffsets);
                    unknownAncestors = currentObject.getUnknownAncestorNumber();
                    computeMappingsFromObject(mtasTokenIdFactory, currentObject, currentList, updateList);
                  } else {
                    // this shouldn't happen
                  }
                  // check for wordAnnotation: because otherwise currentList
                  // should contain no wordAnnotations
                } else if (!currentList.get(MAPPING_TYPE_WORD_ANNOTATION).isEmpty()) {
                  if ((currentType = wordAnnotationTypes.get(qname)) != null) {
                    currentObject = currentList.get(MAPPING_TYPE_WORD_ANNOTATION)
                        .remove(currentList.get(MAPPING_TYPE_WORD_ANNOTATION).size() - 1);
                    assert currentObject.getType().equals(currentType) : "object expected to be "
                        + currentObject.getType().getName() + ", not " + currentType.getName();
                    assert unknownAncestors == 0 : "error in administration " + currentObject.getType().getName();
                    currentObject.setRealOffsetEnd(lastOffset);
                    idPositions.put(currentObject.getId(), currentObject.getPositions());
                    idOffsets.put(currentObject.getId(), currentObject.getOffset());
                    // offset always null, so update later with word (should be
                    // possible)
                    if ((currentObject.getId() != null) && (!currentList.get(MAPPING_TYPE_WORD).isEmpty())) {
                      currentList.get(MAPPING_TYPE_WORD).get((currentList.get(MAPPING_TYPE_WORD).size() - 1))
                          .addUpdateableIdWithOffset(currentObject.getId());
                    }
                    currentObject.updateMappings(idPositions, idOffsets);
                    unknownAncestors = currentObject.getUnknownAncestorNumber();
                    computeMappingsFromObject(mtasTokenIdFactory, currentObject, currentList, updateList);
                  } else {
                    // this shouldn't happen
                  }
                  // check for word: because otherwise currentList should contain
                  // no words
                } else if (!currentList.get(MAPPING_TYPE_WORD).isEmpty()) {
                  if ((currentType = wordTypes.get(qname)) != null) {
                    currentObject = currentList.get(MAPPING_TYPE_WORD)
                        .remove(currentList.get(MAPPING_TYPE_WORD).size() - 1);
                    assert currentObject.getType().equals(currentType) : "object expected to be "
                        + currentObject.getType().getName() + ", not " + currentType.getName();
                    assert unknownAncestors == 0 : "error in administration " + currentObject.getType().getName();
                    currentObject.setOffsetEnd(lastOffset);
                    currentObject.setRealOffsetEnd(lastOffset);
                    // update ancestor groups with position and offset
                    for (MtasParserObject currentGroup : currentList.get(MAPPING_TYPE_GROUP)) {
                      currentGroup.addPositions(currentObject.getPositions());
                      currentGroup.addOffsetStart(currentObject.getOffsetStart());
                      currentGroup.addOffsetEnd(currentObject.getOffsetEnd());
                    }
                    idPositions.put(currentObject.getId(), currentObject.getPositions());
                    idOffsets.put(currentObject.getId(), currentObject.getOffset());
                    currentObject.updateMappings(idPositions, idOffsets);
                    unknownAncestors = currentObject.getUnknownAncestorNumber();
                    computeMappingsFromObject(mtasTokenIdFactory, currentObject, currentList, updateList);
                  } else {
                    // this shouldn't happen
                  }
                  // check for group annotation: because otherwise currentList
                  // should contain no groupAnnotations
                } else if (!currentList.get(MAPPING_TYPE_GROUP_ANNOTATION).isEmpty()) {
                  if ((currentType = groupAnnotationTypes.get(qname)) != null) {
                    currentObject = currentList.get(MAPPING_TYPE_GROUP_ANNOTATION)
                        .remove(currentList.get(MAPPING_TYPE_GROUP_ANNOTATION).size() - 1);
                    assert currentObject.getType().equals(currentType) : "object expected to be "
                        + currentObject.getType().getName() + ", not " + currentType.getName();
                    assert unknownAncestors == 0 : "error in administration " + currentObject.getType().getName();
                    currentObject.setRealOffsetEnd(lastOffset);
                    idPositions.put(currentObject.getId(), currentObject.getPositions());
                    idOffsets.put(currentObject.getId(), currentObject.getOffset());
                    currentObject.updateMappings(idPositions, idOffsets);
                    unknownAncestors = currentObject.getUnknownAncestorNumber();
                    computeMappingsFromObject(mtasTokenIdFactory, currentObject, currentList, updateList);
                  } else {
                    // this shouldn't happen
                  }
                  // check for relation annotation
                } else if (!currentList.get(MAPPING_TYPE_RELATION_ANNOTATION).isEmpty()) {
                  if ((currentType = relationAnnotationTypes.get(qname)) != null) {
                    currentObject = currentList.get(MAPPING_TYPE_RELATION_ANNOTATION)
                        .remove(currentList.get(MAPPING_TYPE_RELATION_ANNOTATION).size() - 1);
                    assert currentObject.getType().equals(currentType) : "object expected to be "
                        + currentObject.getType().getName() + ", not " + currentType.getName();
                    assert unknownAncestors == 0 : "error in administration " + currentObject.getType().getName();
                    currentObject.setRealOffsetEnd(lastOffset);
                    idPositions.put(currentObject.getId(), currentObject.getPositions());
                    idOffsets.put(currentObject.getId(), currentObject.getOffset());
                    currentObject.updateMappings(idPositions, idOffsets);
                    unknownAncestors = currentObject.getUnknownAncestorNumber();
                    computeMappingsFromObject(mtasTokenIdFactory, currentObject, currentList, updateList);
                  } else {
                    // this shouldn't happen
                  }
                  // check for relation
                } else if (!currentList.get(MAPPING_TYPE_RELATION).isEmpty()) {
                  if ((currentType = relationTypes.get(qname)) != null) {
                    currentObject = currentList.get(MAPPING_TYPE_RELATION)
                        .remove(currentList.get(MAPPING_TYPE_RELATION).size() - 1);
                    assert currentObject.getType().equals(currentType) : "object expected to be "
                        + currentObject.getType().getName() + ", not " + currentType.getName();
                    assert unknownAncestors == 0 : "error in administration " + currentObject.getType().getName();
                    // ignore text: should not occur
                    currentObject.setRealOffsetEnd(lastOffset);
                    idPositions.put(currentObject.getId(), currentObject.getPositions());
                    idOffsets.put(currentObject.getId(), currentObject.getOffset());
                    currentObject.updateMappings(idPositions, idOffsets);
                    unknownAncestors = currentObject.getUnknownAncestorNumber();
                    computeMappingsFromObject(mtasTokenIdFactory, currentObject, currentList, updateList);
                  } else {
                    // this shouldn't happen
                  }
                  // check for group
                } else if (!currentList.get(MAPPING_TYPE_GROUP).isEmpty()) {
                  if ((currentType = groupTypes.get(qname)) != null) {
                    currentObject = currentList.get(MAPPING_TYPE_GROUP)
                        .remove(currentList.get(MAPPING_TYPE_GROUP).size() - 1);
                    assert currentObject.getType().equals(currentType) : "object expected to be "
                        + currentObject.getType().getName() + ", not " + currentType.getName();
                    assert unknownAncestors == 0 : "error in administration " + currentObject.getType().getName();
                    // ignore text: should not occur
                    currentObject.setRealOffsetEnd(lastOffset);
                    idPositions.put(currentObject.getId(), currentObject.getPositions());
                    idOffsets.put(currentObject.getId(), currentObject.getOffset());
                    currentObject.updateMappings(idPositions, idOffsets);
                    unknownAncestors = currentObject.getUnknownAncestorNumber();
                    computeMappingsFromObject(mtasTokenIdFactory, currentObject, currentList, updateList);
                  } else {
                    unknownAncestors--;
                  }
                } else if (qname.equals(getQName("text"))) {
                  parsingContent = false;
                  assert unknownAncestors == 0 : "error in administration unknownAncestors";
                  assert currentList.get(MAPPING_TYPE_REF).isEmpty() : "error in administration references";
                  assert currentList.get(MAPPING_TYPE_GROUP).isEmpty() : "error in administration groups";
                  assert currentList.get(MAPPING_TYPE_GROUP_ANNOTATION)
                      .isEmpty() : "error in administration groupAnnotations";
                  assert currentList.get(MAPPING_TYPE_WORD).isEmpty() : "error in administration words";
                  assert currentList.get(MAPPING_TYPE_WORD_ANNOTATION)
                      .isEmpty() : "error in administration wordAnnotations";
                  assert currentList.get(MAPPING_TYPE_RELATION).isEmpty() : "error in administration relations";
                  assert currentList.get(MAPPING_TYPE_RELATION_ANNOTATION)
                      .isEmpty() : "error in administration relationAnnotations";
                }
              }
              // forget text
              textContent = null;
              break;
            case XMLStreamConstants.CHARACTERS:
              // set offset (end of start-element)
              lastOffset = streamReader.getLocation().getCharacterOffset();
              // check for text
              if (streamReader.hasText()) {
                textContent = streamReader.getText();
              }
              if (currentObject != null && unknownAncestors.equals(0)) {
                currentObject.addText(textContent);
              }
              break;
            default:
              break;
          }
          if (!streamReader.hasNext()) {
            break;
          }
          event = streamReader.next();
        }
      } finally {
        streamReader.close();
      }
      // final checks
      assert unknownAncestors == 0 : "error in administration unknownAncestors";
      assert hasRoot : "no " + rootTag;
    } catch (XMLStreamException e) {
      log.debug("Error", e);
      throw new MtasParserException("No valid XML: " + e.getMessage());
    }

    // update tokens with variable
    for (Entry<Integer, Set<String>> updateItem : updateList.get(UPDATE_TYPE_VARIABLE).entrySet()) {
      MtasToken token = tokenCollection.get(updateItem.getKey());
      String encodedPrefix = token.getPrefix();
      String encodedPostfix = token.getPostfix();
      token.setValue(decodeAndUpdateWithVariables(encodedPrefix, encodedPostfix, variables));
    }
    // update tokens with offset
    for (Entry<Integer, Set<String>> updateItem : updateList.get(UPDATE_TYPE_OFFSET).entrySet()) {
      Set<String> refIdList = new HashSet<>();
      for (String refId : updateItem.getValue()) {
        if (idPositions.containsKey(refId)) {
          refIdList.add(refId);
        }
        if (relationKeyMap.containsKey(refId)) {
          refIdList.addAll(recursiveCollect(refId, relationKeyMap, 10));
        }
      }
      for (String refId : refIdList) {
        Integer[] refOffset = idOffsets.get(refId);
        Integer tokenId = updateItem.getKey();
        if (tokenId != null && refOffset != null) {
          MtasToken token = tokenCollection.get(tokenId);
          token.addOffset(refOffset[0], refOffset[1]);
        }
      }
    }
    // update tokens with position
    for (Entry<Integer, Set<String>> updateItem : updateList.get(UPDATE_TYPE_POSITION).entrySet()) {
      HashSet<String> refIdList = new HashSet<>();
      for (String refId : updateItem.getValue()) {
        if (idPositions.containsKey(refId)) {
          refIdList.add(refId);
        }
        if (relationKeyMap.containsKey(refId)) {
          refIdList.addAll(recursiveCollect(refId, relationKeyMap, 10));
        }
      }
      for (String refId : refIdList) {
        Set<Integer> refPositions = idPositions.get(refId);
        Integer tokenId = updateItem.getKey();
        if (tokenId != null && refPositions != null) {
          MtasToken token = tokenCollection.get(tokenId);
          token.addPositions(refPositions);
        }
      }
    }

    // final check
    tokenCollection.check(autorepair, makeunique);
    return tokenCollection;
  }

  /**
   * Recursive collect.
   *
   * @param refId
   *          the ref id
   * @param relationKeyMap
   *          the relation key map
   * @param maxRecursion
   *          the max recursion
   * @return the collection<? extends string>
   */
  private Collection<? extends String> recursiveCollect(String refId, Map<String, SortedSet<String>> relationKeyMap,
      int maxRecursion) {
    Set<String> list = new HashSet<>();
    if (maxRecursion > 0 && relationKeyMap.containsKey(refId)) {
      SortedSet<String> subList = relationKeyMap.get(refId);
      for (String subRefId : subList) {
        list.add(subRefId);
        list.addAll(recursiveCollect(subRefId, relationKeyMap, maxRecursion - 1));
      }
    }
    return list;
  }

  /**
   * Gets the q name.
   *
   * @param key
   *          the key
   * @return the q name
   */
  private QName getQName(String key) {
    QName qname;
    if ((qname = qNames.get(key)) == null) {
      qname = new QName(namespaceURI, key);
      qNames.put(key, qname);
    }
    return qname;
  }

  /**
   * Collect attributes.
   *
   * @param currentObject
   *          the current object
   * @param streamReader
   *          the stream reader
   */
  public void collectAttributes(MtasParserObject currentObject, XMLStreamReader streamReader) {
    String attributeNamespaceURI;
    currentObject.objectAttributes.clear();
    currentObject.objectId = streamReader.getAttributeValue(namespaceURI_id, "id");
    for (int i = 0; i < streamReader.getAttributeCount(); i++) {
      attributeNamespaceURI = streamReader.getAttributeNamespace(i);
      if (attributeNamespaceURI == null || attributeNamespaceURI.equals("")) {
        attributeNamespaceURI = streamReader.getNamespaceURI();
      }
      if (namespaceURI == null || attributeNamespaceURI.equals(namespaceURI)) {
        currentObject.objectAttributes.put(streamReader.getAttributeLocalName(i), streamReader.getAttributeValue(i));
      } else {
        HashMap<String, String> otherMap;
        if (!currentObject.objectOtherAttributes.containsKey(attributeNamespaceURI)) {
          otherMap = new HashMap<>();
          currentObject.objectOtherAttributes.put(attributeNamespaceURI, otherMap);
        } else {
          otherMap = currentObject.objectOtherAttributes.get(attributeNamespaceURI);
        }
        otherMap.put(streamReader.getAttributeLocalName(i), streamReader.getAttributeValue(i));
      }
    }
  }

  /**
   * The Class MtasXMLParserMappingRelation.
   */
  private class MtasXMLParserMappingRelation extends MtasParserMapping<MtasXMLParserMappingRelation> {

    /**
     * Instantiates a new mtas XML parser mapping relation.
     */
    public MtasXMLParserMappingRelation() {
      super();
      this.position = SOURCE_REFS;
      this.realOffset = SOURCE_OWN;
      this.offset = SOURCE_REFS;
      this.type = MAPPING_TYPE_RELATION;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.analysis.parser.MtasBasicParser.MtasParserMapping#self()
     */
    @Override
    protected MtasXMLParserMappingRelation self() {
      return this;
    }
  }

  /**
   * The Class MtasXMLParserMappingRelationAnnotation.
   */
  private class MtasXMLParserMappingRelationAnnotation
      extends MtasParserMapping<MtasXMLParserMappingRelationAnnotation> {

    /**
     * Instantiates a new mtas XML parser mapping relation annotation.
     */
    public MtasXMLParserMappingRelationAnnotation() {
      super();
      this.position = SOURCE_ANCESTOR_RELATION;
      this.realOffset = SOURCE_OWN;
      this.offset = SOURCE_ANCESTOR_RELATION;
      this.type = MAPPING_TYPE_RELATION_ANNOTATION;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.analysis.parser.MtasBasicParser.MtasParserMapping#self()
     */
    @Override
    protected MtasXMLParserMappingRelationAnnotation self() {
      return this;
    }

  }

  /**
   * The Class MtasXMLParserMappingGroup.
   */
  private class MtasXMLParserMappingGroup extends MtasParserMapping<MtasXMLParserMappingGroup> {

    /**
     * Instantiates a new mtas XML parser mapping group.
     */
    public MtasXMLParserMappingGroup() {
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
    protected MtasXMLParserMappingGroup self() {
      return this;
    }
  }

  /**
   * The Class MtasXMLParserMappingGroupAnnotation.
   */
  private class MtasXMLParserMappingGroupAnnotation extends MtasParserMapping<MtasXMLParserMappingGroupAnnotation> {

    /**
     * Instantiates a new mtas XML parser mapping group annotation.
     */
    public MtasXMLParserMappingGroupAnnotation() {
      super();
      this.position = SOURCE_ANCESTOR_GROUP;
      this.realOffset = SOURCE_OWN;
      this.offset = SOURCE_ANCESTOR_GROUP;
      this.type = MAPPING_TYPE_GROUP_ANNOTATION;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.analysis.parser.MtasBasicParser.MtasParserMapping#self()
     */
    @Override
    protected MtasXMLParserMappingGroupAnnotation self() {
      return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.analysis.parser.MtasBasicParser.MtasParserMapping#setStartEnd(java.
     * lang.String, java.lang.String)
     */
    @Override
    protected void setStartEnd(String start, String end) {
      super.setStartEnd(start, end);
      if (start != null && end != null) {
        position = SOURCE_REFS;
        offset = SOURCE_REFS;
      }
    }

  }

  /**
   * The Class MtasXMLParserMappingWord.
   */
  private class MtasXMLParserMappingWord extends MtasParserMapping<MtasXMLParserMappingWord> {

    /**
     * Instantiates a new mtas XML parser mapping word.
     */
    public MtasXMLParserMappingWord() {
      super();
      this.position = SOURCE_OWN;
      this.realOffset = SOURCE_OWN;
      this.offset = SOURCE_OWN;
      this.type = MAPPING_TYPE_WORD;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.analysis.parser.MtasBasicParser.MtasParserMapping#self()
     */
    @Override
    protected MtasXMLParserMappingWord self() {
      return this;
    }
  }

  /**
   * The Class MtasXMLParserMappingWordAnnotation.
   */
  private class MtasXMLParserMappingWordAnnotation extends MtasParserMapping<MtasXMLParserMappingWordAnnotation> {

    /**
     * Instantiates a new mtas XML parser mapping word annotation.
     */
    public MtasXMLParserMappingWordAnnotation() {
      super();
      this.position = SOURCE_OWN;
      this.realOffset = SOURCE_OWN;
      this.offset = SOURCE_ANCESTOR_WORD;
      this.type = MAPPING_TYPE_WORD_ANNOTATION;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.analysis.parser.MtasBasicParser.MtasParserMapping#self()
     */
    @Override
    protected MtasXMLParserMappingWordAnnotation self() {
      return this;
    }
  }

}
