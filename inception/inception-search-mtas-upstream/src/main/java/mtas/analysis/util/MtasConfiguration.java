package mtas.analysis.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.util.ResourceLoader;

/**
 * The Class MtasConfiguration.
 */
public class MtasConfiguration {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(MtasConfiguration.class);

  /** The Constant CONFIGURATIONS_MTAS. */
  public static final String CONFIGURATIONS_MTAS = "mtas";

  /** The Constant CONFIGURATIONS_CONFIGURATIONS. */
  public static final String CONFIGURATIONS_CONFIGURATIONS = "configurations";

  /** The Constant CONFIGURATIONS_CONFIGURATION. */
  public static final String CONFIGURATIONS_CONFIGURATION = "configuration";

  /** The Constant CONFIGURATIONS_CONFIGURATION_NAME. */
  public static final String CONFIGURATIONS_CONFIGURATION_NAME = "name";

  /** The Constant TOKENIZER_CONFIGURATION_FILE. */
  public static final String TOKENIZER_CONFIGURATION_FILE = "file";

  /** The Constant CHARFILTER_CONFIGURATION_TYPE. */
  public static final String CHARFILTER_CONFIGURATION_TYPE = "type";

  /** The Constant CHARFILTER_CONFIGURATION_PREFIX. */
  public static final String CHARFILTER_CONFIGURATION_PREFIX = "prefix";

  /** The Constant CHARFILTER_CONFIGURATION_POSTFIX. */
  public static final String CHARFILTER_CONFIGURATION_POSTFIX = "postfix";

  /** The name. */
  public String name;

  /** The attributes. */
  public HashMap<String, String> attributes;

  /** The children. */
  public List<MtasConfiguration> children;

  /** The parent. */
  public MtasConfiguration parent;

  /**
   * Instantiates a new mtas configuration.
   */
  public MtasConfiguration() {
    name = null;
    attributes = new HashMap<String, String>();
    children = new ArrayList<MtasConfiguration>();
    parent = null;
  }

  /**
   * Read configurations.
   *
   * @param resourceLoader
   *          the resource loader
   * @param configFile
   *          the config file
   * @param className
   *          the class name
   * @return the hash map
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  private static HashMap<String, HashMap<String, String>> readConfigurations(
      ResourceLoader resourceLoader, String configFile, String className)
      throws IOException {
    HashMap<String, HashMap<String, String>> configs = null;
    InputStream reader = resourceLoader.openResource(configFile);
    // parse xml
    XMLInputFactory factory = XMLInputFactory.newInstance();
    try {
      XMLStreamReader streamReader = factory.createXMLStreamReader(reader);
      String currentElement = null;
      ArrayList<String> currentElements = new ArrayList<String>();
      QName qname;
      boolean skipCurrentConfigurations = false;
      try {
        int event = streamReader.getEventType();
        while (true) {
          switch (event) {
          case XMLStreamConstants.START_DOCUMENT:
            if (!streamReader.getCharacterEncodingScheme().equals("UTF-8")) {
              throw new IOException("XML not UTF-8 encoded");
            }
            break;
          case XMLStreamConstants.END_DOCUMENT:
            break;
          case XMLStreamConstants.SPACE:
            break;
          case XMLStreamConstants.START_ELEMENT:
            // get data
            qname = streamReader.getName();
            if (configs == null) {
              if (qname.getLocalPart().equals(CONFIGURATIONS_MTAS)) {
                configs = new HashMap<String, HashMap<String, String>>();
              } else {
                throw new IOException("no Mtas Configurations File");
              }
            } else if (currentElement != null
                && currentElement.equals(CONFIGURATIONS_MTAS)) {
              if (qname.getLocalPart().equals(CONFIGURATIONS_CONFIGURATIONS)) {
                skipCurrentConfigurations = true;
                if (className != null) {
                  for (int i = 0; i < streamReader.getAttributeCount(); i++) {
                    if (streamReader.getAttributeLocalName(i).equals("type")) {
                      if (streamReader.getAttributeValue(i).equals(className)) {
                        skipCurrentConfigurations = false;
                      }
                    }
                  }
                }
              } else {
                throw new IOException("unexpected " + qname.getLocalPart());
              }
            } else if (currentElement != null
                && currentElement.equals(CONFIGURATIONS_CONFIGURATIONS)
                && !skipCurrentConfigurations) {
              if (qname.getLocalPart().equals(CONFIGURATIONS_CONFIGURATION)) {
                String configurationName = null;
                HashMap<String, String> configurationValues = new HashMap<String, String>();
                for (int i = 0; i < streamReader.getAttributeCount(); i++) {
                  if (streamReader.getAttributeLocalName(i)
                      .equals(CONFIGURATIONS_CONFIGURATION_NAME)) {
                    configurationName = streamReader.getAttributeValue(i);
                  } else {
                    configurationValues.put(
                        streamReader.getAttributeLocalName(i),
                        streamReader.getAttributeValue(i));
                  }
                }
                if (configurationName != null) {
                  configs.put(configurationName, configurationValues);
                } else {
                  throw new IOException("configuration without "
                      + CONFIGURATIONS_CONFIGURATION_NAME);
                }
              } else {
                throw new IOException("unexpected tag " + qname.getLocalPart());
              }
            }
            currentElement = qname.getLocalPart();
            currentElements.add(currentElement);
            break;
          case XMLStreamConstants.END_ELEMENT:
            if (currentElement != null
                && currentElement.equals(CONFIGURATIONS_CONFIGURATIONS)) {
              skipCurrentConfigurations = false;
            }
            int i = currentElements.size();
            currentElements.remove((i - 1));
            if (i > 1) {
              currentElement = currentElements.get(i - 2);
            } else {
              currentElement = null;
            }
            break;
          case XMLStreamConstants.CHARACTERS:
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
    } catch (XMLStreamException e) {
      log.debug("Error", e);
    }
    return configs;
  }

  /**
   * Read mtas char filter configurations.
   *
   * @param resourceLoader
   *          the resource loader
   * @param configFile
   *          the config file
   * @return the hash map
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static HashMap<String, MtasConfiguration> readMtasCharFilterConfigurations(
      ResourceLoader resourceLoader, String configFile) throws IOException {
    HashMap<String, HashMap<String, String>> configs = readConfigurations(
        resourceLoader, configFile, MtasCharFilterFactory.class.getName());
    if (configs == null) {
      throw new IOException("no configurations");
    } else {
      HashMap<String, MtasConfiguration> result = new HashMap<String, MtasConfiguration>();
      for (Entry<String, HashMap<String, String>> entry : configs.entrySet()) {
        HashMap<String, String> config = entry.getValue();
        if (config.containsKey(CHARFILTER_CONFIGURATION_TYPE)) {
          MtasConfiguration item = new MtasConfiguration();
          item.attributes.put(CHARFILTER_CONFIGURATION_TYPE,
              config.get(CHARFILTER_CONFIGURATION_TYPE));
          item.attributes.put(CHARFILTER_CONFIGURATION_PREFIX,
              config.get(CHARFILTER_CONFIGURATION_PREFIX));
          item.attributes.put(CHARFILTER_CONFIGURATION_POSTFIX,
              config.get(CHARFILTER_CONFIGURATION_POSTFIX));
          result.put(entry.getKey(), item);
        } else {
          throw new IOException("configuration " + entry.getKey() + " has no "
              + CHARFILTER_CONFIGURATION_TYPE);
        }
      }
      return result;
    }
  }

  /**
   * Read mtas tokenizer configurations.
   *
   * @param resourceLoader
   *          the resource loader
   * @param configFile
   *          the config file
   * @return the hash map
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static HashMap<String, MtasConfiguration> readMtasTokenizerConfigurations(
      ResourceLoader resourceLoader, String configFile) throws IOException {
    HashMap<String, HashMap<String, String>> configs = readConfigurations(
        resourceLoader, configFile, MtasTokenizerFactory.class.getName());
    if (configs == null) {
      throw new IOException("no configurations");
    } else {
      HashMap<String, MtasConfiguration> result = new HashMap<String, MtasConfiguration>();
      for (Entry<String, HashMap<String, String>> entry : configs.entrySet()) {
        HashMap<String, String> config = entry.getValue();
        if (config.containsKey(TOKENIZER_CONFIGURATION_FILE)) {
          result.put(entry.getKey(), readConfiguration(resourceLoader
              .openResource(config.get(TOKENIZER_CONFIGURATION_FILE))));
        } else {
          throw new IOException("configuration " + entry.getKey() + " has no "
              + TOKENIZER_CONFIGURATION_FILE);
        }
      }
      return result;
    }
  }

  /**
   * Read configuration.
   *
   * @param reader
   *          the reader
   * @return the mtas configuration
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static MtasConfiguration readConfiguration(InputStream reader)
      throws IOException {
    MtasConfiguration currentConfig = null;
    // parse xml
    XMLInputFactory factory = XMLInputFactory.newInstance();
    try {
      XMLStreamReader streamReader = factory.createXMLStreamReader(reader);
      QName qname;
      try {
        int event = streamReader.getEventType();
        while (true) {
          switch (event) {
          case XMLStreamConstants.START_DOCUMENT:
            if (!streamReader.getCharacterEncodingScheme().equals("UTF-8")) {
              throw new IOException("XML not UTF-8 encoded");
            }
            break;
          case XMLStreamConstants.END_DOCUMENT:
          case XMLStreamConstants.SPACE:
            break;
          case XMLStreamConstants.START_ELEMENT:
            // get data
            qname = streamReader.getName();
            if (currentConfig == null) {
              if (qname.getLocalPart().equals("mtas")) {
                currentConfig = new MtasConfiguration();
              } else {
                throw new IOException("no Mtas Configuration");
              }
            } else {
              MtasConfiguration parentConfig = currentConfig;
              currentConfig = new MtasConfiguration();
              parentConfig.children.add(currentConfig);
              currentConfig.parent = parentConfig;
              currentConfig.name = qname.getLocalPart();
              for (int i = 0; i < streamReader.getAttributeCount(); i++) {
                currentConfig.attributes.put(
                    streamReader.getAttributeLocalName(i),
                    streamReader.getAttributeValue(i));
              }
            }
            break;
          case XMLStreamConstants.END_ELEMENT:
            if (currentConfig.parent == null) {
              return currentConfig;
            } else {
              currentConfig = currentConfig.parent;
            }
            break;
          case XMLStreamConstants.CHARACTERS:
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
    } catch (XMLStreamException e) {
      log.debug("Error", e);
    }
    return null;
  }

  @Override
public String toString() {
    return toString(0);
  }

  private String toString(int indent) {
    String text = "";
    if (name != null) {
      text += (indent > 0 ? String.format("%" + indent + "s", "") : "")
          + "name: " + name + "\n";
    }
    if (attributes != null) {
      for (Entry<String,String> entry : attributes.entrySet()) {
        text += (indent > 0 ? String.format("%" + indent + "s", "") : "") + entry.getKey()
            + ":" + entry.getValue() + "\n";
      }
    }
    if (children != null) {
      for (MtasConfiguration child : children) {
        text += (indent > 0 ? String.format("%" + indent + "s", "") : "")
            + child.toString(indent + 2);
      }
    }
    return text;
  }

}
