/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.io.xml.dkprocore;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.METHOD;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;
import static org.apache.commons.lang3.ArrayUtils.indexOf;
import static org.apache.commons.lang3.ArrayUtils.subarray;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.Strings.CS;
import static org.apache.uima.fit.util.FSCollectionFactory.createFSArray;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamResult;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCopier;
import org.dkpro.core.api.xml.type.XmlAttribute;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.dkpro.core.api.xml.type.XmlNode;
import org.dkpro.core.api.xml.type.XmlTextNode;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.tudarmstadt.ukp.inception.support.xml.XmlParserUtils;

public final class XmlNodeUtils
{
    private static final String XMLNS = "xmlns";
    private static final String XMLNS_PREFIX = "xmlns:";

    private XmlNodeUtils()
    {
        // No instances
    }

    /**
     * Check whether the given element has an attribute with the given qualified name and value.
     *
     * @param e
     *            the element to check
     * @param aAttribute
     *            the qualified name of the attribute
     * @param aValue
     *            the value to compare against
     * @return {@code true} if the attribute exists and its value equals {@code aValue}
     */
    public static boolean hasAttributeWithValue(XmlElement e, String aAttribute, String aValue)
    {
        return getAttributeValue(e, aAttribute).map(v -> aValue.equals(v)).orElse(false);
    }

    static String serializeCasToXmlString(JCas aJCas) throws IOException
    {
        try (var out = new StringWriter()) {
            var tf = XmlParserUtils.newTransformerFactory();
            var th = tf.newTransformerHandler();
            th.getTransformer().setOutputProperty(OMIT_XML_DECLARATION, "yes");
            th.getTransformer().setOutputProperty(METHOD, "xml");
            th.getTransformer().setOutputProperty(INDENT, "no");
            th.setResult(new StreamResult(out));

            var serializer = new Cas2SaxEvents(th);
            serializer.process(aJCas);

            return out.toString();
        }
        catch (SAXException | TransformerConfigurationException e) {
            throw new IOException(e);
        }
    }

    static void parseXmlStringToCas(JCas aJCas, String aXml) throws IOException
    {
        try (var reader = new StringReader(aXml)) {
            parseXmlToCas(aJCas, new InputSource(reader));
        }
    }

    static void parseXmlToCas(JCas aJCas, InputSource aSource) throws IOException
    {
        var handler = new CasXmlHandler(aJCas);

        try {
            var parser = XmlParserUtils.newSaxParser();
            parser.parse(aSource, handler);
        }
        catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    /**
     * Check whether the given element has an attribute with the given qualified name and value.
     * This variant uses {@link Objects#equals(Object,Object)} for comparison.
     *
     * @param aElement
     *            the element to check
     * @param aAttribute
     *            the qualified name of the attribute
     * @param aValue
     *            the value to compare against
     * @return {@code true} if the attribute exists and its value equals {@code aValue}
     */
    public static boolean hasAttributeValue(XmlElement aElement, String aAttribute, String aValue)
    {
        return getAttributeValue(aElement, aAttribute) //
                .map(value -> Objects.equals(value, aValue)) //
                .orElse(false);
    }

    /**
     * Get the value of the attribute with the given qualified name from the element.
     *
     * @param aElement
     *            the element to query
     * @param aAttribute
     *            the qualified name of the attribute
     * @return an {@link Optional} containing the attribute value if present, otherwise empty
     */
    public static Optional<String> getAttributeValue(XmlElement aElement, String aAttribute)
    {
        var attributes = aElement.getAttributes();

        if (attributes == null) {
            return Optional.empty();
        }

        return attributes.select(XmlAttribute.class) //
                .filter(a -> aAttribute.equals(a.getQName())) //
                .findFirst() //
                .map(XmlAttribute::getValue);
    }

    /**
     * Get the value of the attribute with the given qualified name from the element. If the
     * attribute is not present an {@link IllegalArgumentException} is thrown.
     *
     * @param aElement
     *            the element to query
     * @param aAttribute
     *            the qualified name of the attribute
     * @return the attribute value
     * @throws IllegalArgumentException
     *             if the element has no attributes or the named attribute is missing
     */
    public static String getMandatoryAttributeValue(XmlElement aElement, String aAttribute)
    {
        var attributes = aElement.getAttributes();

        if (attributes == null) {
            throw new IllegalArgumentException("Element has no attribute [" + aAttribute + "]");
        }

        return attributes.select(XmlAttribute.class) //
                .filter(a -> aAttribute.equals(a.getQName())) //
                .findFirst() //
                .map(XmlAttribute::getValue) //
                .orElseThrow(() -> new IllegalArgumentException(
                        "Element has no attribute [" + aAttribute + "]"));
    }

    /**
     * Return the concatenated textual content of the given element. Captured text nodes return
     * their covered text; uncaptured text nodes return their raw text.
     *
     * @param aElement
     *            the element whose text content to extract
     * @return the concatenated text content
     */
    public static String textContent(XmlElement aElement)
    {
        var buf = new StringBuilder();
        dfs(aElement, n -> {
            if (n instanceof XmlTextNode) {
                var tn = (XmlTextNode) n;
                if (tn.getCaptured()) {
                    buf.append(tn.getCoveredText());
                }
                else {
                    buf.append(tn.getText());
                }
            }
        });
        return buf.toString();
    }

    /**
     * Perform a depth-first traversal starting at the given node and return the nodes in visit
     * order.
     *
     * @param aNode
     *            the starting node
     * @return a list of nodes visited in depth-first order
     */
    public static List<XmlNode> dfs(XmlNode aNode)
    {
        var nodes = new ArrayList<XmlNode>();
        dfs(aNode, nodes::add);
        return nodes;
    }

    private static void dfs(XmlNode aNode, Consumer<XmlNode> aVisitor)
    {
        if (aNode instanceof XmlElement node) {
            var children = node.getChildren();
            if (children != null) {
                children.forEach($ -> dfs($, aVisitor));
            }
        }
        aVisitor.accept(aNode);
    }

    /**
     * Removes the given element and all descendants except captures text nodes. Captured text nodes
     * must be preserved, otherwise it may break rendering or serialization because the text of the
     * captured text nodes is considered part of the document text.
     * 
     * @param aElement
     *            the element to remove.
     */
    public static void removeWithDescendantsFromTree(XmlElement aElement)
    {
        var newSiblings = new ArrayList<XmlNode>();
        var parent = aElement.getParent();
        var siblings = parent.getChildren().toArray(new XmlNode[parent.getChildren().size()]);
        var i = indexOf(siblings, aElement);
        asList(subarray(siblings, 0, i)).forEach(newSiblings::add);
        for (var node : dfs(aElement)) {
            if (node instanceof XmlElement element) {
                removeElement(element);
            }
            if (node instanceof XmlTextNode textNode && textNode.getCaptured()) {
                newSiblings.add(textNode);
            }
            else {
                node.removeFromIndexes();
            }
        }
        asList(subarray(siblings, i + 1, siblings.length)).forEach(newSiblings::add);
        parent.setChildren(createFSArray(aElement.getJCas(), newSiblings));
    }

    /**
     * Remove the given element(s) from their parent, hoisting their children into the parent
     * (preserving captured text nodes). The removed element itself is removed from indexes.
     *
     * @param aElement
     *            one or more elements to remove
     */
    public static void removeFromTree(XmlElement... aElement)
    {
        for (var e : aElement) {
            var newSiblings = new ArrayList<XmlNode>();
            var parent = e.getParent();
            var siblings = parent.getChildren().toArray(new XmlNode[parent.getChildren().size()]);
            var i = indexOf(siblings, e);
            asList(subarray(siblings, 0, i)).forEach(newSiblings::add);
            var children = e.getChildren();
            if (children != null) {
                children.forEach(child -> hoistNode(newSiblings, parent, child));
            }
            asList(subarray(siblings, i + 1, siblings.length)).forEach(newSiblings::add);
            parent.setChildren(createFSArray(e.getJCas(), newSiblings));
            removeElement(e);
        }
    }

    private static void removeElement(XmlElement e)
    {
        var attributes = e.getAttributes();
        if (attributes != null) {
            for (var attribute : attributes) {
                attribute.removeFromIndexes();
            }
            attributes.removeFromIndexes();
        }
        e.removeFromIndexes();
    }

    private static void hoistNode(ArrayList<XmlNode> newSiblings, XmlElement parent, XmlNode child)
    {
        child.setParent(parent);
        newSiblings.add(child);
    }

    /**
     * Select elements that match the given path of qualified names starting from the supplied
     * element. The first element in {@code aPath} must match {@code aElement}.
     *
     * @param aElement
     *            the element to start the selection from
     * @param aPath
     *            the path of qualified names to match
     * @return a list of matching {@link XmlElement}s, or an empty list if none match
     */
    public static List<XmlElement> selectElements(XmlElement aElement, List<String> aPath)
    {
        if (aPath.size() == 0 || !aElement.getQName().equals(aPath.get(0))) {
            return emptyList();
        }

        if (aPath.size() == 1) {
            if (!aElement.getQName().equals(aPath.get(0))) {
                return emptyList();
            }

            return asList(aElement);
        }

        var result = new ArrayList<XmlElement>();
        for (var child : aElement.getChildren().select(XmlElement.class)) {
            result.addAll(selectElements(child, aPath.subList(1, aPath.size())));
        }
        return result;
    }

    /**
     * Return the first child element of {@code aElement} that has the given qualified name.
     *
     * @param aElement
     *            the parent element
     * @param aQName
     *            the qualified name of the child to find
     * @return an {@link Optional} containing the first matching child, or empty if none found
     */
    public static Optional<XmlElement> firstChild(XmlElement aElement, String aQName)
    {
        if (aElement.getChildren() == null) {
            return Optional.empty();
        }

        return aElement.getChildren().select(XmlElement.class)
                .filter(e -> aQName.equals(e.getQName())).findFirst();
    }

    /**
     * Build the path of qualified names from the root element down to the supplied element.
     *
     * @param aElement
     *            the element to build the path for
     * @return a list of qualified names starting with the root and ending with {@code aElement}
     */
    public static List<String> rootPath(XmlElement aElement)
    {
        var path = new ArrayList<String>();
        var e = aElement;
        while (e != null) {
            path.add(0, e.getQName());
            e = e.getParent();
        }
        return path;
    }

    /**
     * Determine whether the CAS contains the XML document structure types and at least one
     * {@link XmlDocument} instance.
     *
     * @param aCas
     *            the CAS to inspect
     * @return {@code true} if the CAS contains XML document structure, {@code false} otherwise
     */
    public static boolean containsXmlDocumentStructure(CAS aCas)
    {
        var ts = aCas.getTypeSystem();
        if (ts.getType(XmlDocument._TypeName) == null) {
            return false;
        }

        return !aCas.select(XmlDocument.class).isEmpty();
    }

    /**
     * Copy all XML document structure (types and instances) from the source CAS into the target
     * CAS. Only copies when both source and target have the required XML types. Returns the number
     * of feature structures copied.
     *
     * @param aTarget
     *            the CAS to copy into
     * @param aSource
     *            the CAS to copy from
     * @return the number of feature structures successfully copied
     */
    public static int transferXmlDocumentStructure(CAS aTarget, CAS aSource)
    {
        var sourceTS = aSource.getTypeSystem();
        var targetTS = aTarget.getTypeSystem();
        if (sourceTS.getType(XmlDocument._TypeName) == null
                || sourceTS.getType(XmlNode._TypeName) == null
                || targetTS.getType(XmlDocument._TypeName) == null
                || targetTS.getType(XmlNode._TypeName) == null) {
            return 0;
        }

        var copied = new AtomicInteger();
        var casCopier = new CasCopier(aSource, aTarget);
        Consumer<FeatureStructure> copyFunc = fs -> {
            var copy = casCopier.copyFs(fs);
            if (copy != null) {
                copied.incrementAndGet();
                aTarget.addFsToIndexes(copy);
            }
        };

        aSource.select(XmlDocument.class).forEach(copyFunc);
        aSource.select(XmlNode.class).forEach(copyFunc);

        return copied.get();
    }

    /**
     * Remove all XML document structure feature structures (documents, nodes, attributes) from the
     * supplied CAS.
     *
     * @param aCas
     *            the CAS to clean
     * @return the number of feature structures removed
     */
    public static int removeXmlDocumentStructure(CAS aCas)
    {
        var toDelete = new ArrayList<FeatureStructure>();
        aCas.select(XmlDocument.class).forEach(toDelete::add);
        aCas.select(XmlNode.class).forEach(toDelete::add);
        aCas.select(XmlAttribute.class).forEach(toDelete::add);
        toDelete.forEach(aCas::removeFsFromIndexes);
        return toDelete.size();
    }

    public static Map<String, String> prefixMappings(XmlElement aElement)
    {
        var mappings = new LinkedHashMap<String, String>();
        collectPrefixMappings(aElement, mappings);
        return mappings;
    }

    private static void collectPrefixMappings(XmlElement aElement, Map<String, String> aMappings)
    {
        if (aElement.getAttributes() != null) {
            for (var attr : aElement.getAttributes()) {
                if (XMLNS.equals(attr.getQName())) {
                    aMappings.put("", attr.getValue());
                }
                if (CS.startsWith(attr.getQName(), XMLNS_PREFIX)) {
                    aMappings.put(attr.getQName().substring(XMLNS_PREFIX.length()),
                            attr.getValue());
                }
            }
        }
    }

    /**
     * Collect prefix mappings from all ancestors of the supplied element (including the element
     * itself). Mappings are collected from the root down to the element so that mappings defined
     * closer to the element override mappings defined closer to the root.
     *
     * @param aElement
     *            the element to inspect
     * @return a map of prefix -> namespace URI
     */
    public static Map<String, String> allPrefixMappings(XmlElement aElement)
    {
        if (aElement == null) {
            return emptyMap();
        }

        var ancestors = new ArrayDeque<XmlElement>();
        var e = aElement;
        while (e != null) {
            ancestors.addFirst(e);
            e = e.getParent();
        }

        var mappings = new LinkedHashMap<String, String>();
        for (var elem : ancestors) {
            collectPrefixMappings(elem, mappings);
        }

        return mappings;
    }

    /**
     * Check whether the given element's name matches any of the supplied selectors.
     *
     * <p>
     * If the element has a namespace prefix (i.e. its QName contains {@code ':'}) the prefix is
     * looked up in {@code aNamespaceMappings} to obtain the namespace URI. The selector that is
     * checked in this case is formed as {@code "{" + namespaceURI + "}" +
     * localName} (the same notation as used by {@code javax.xml.namespace.QName#toString()}). If
     * the prefix is not present in {@code aNamespaceMappings} the element does not match any
     * selector.
     *
     * <p>
     * If the element has no prefix the element's qualified name (QName) is checked directly against
     * the selectors.
     *
     * @param aElement
     *            the element whose name to test
     * @param aNamespaceMappings
     *            mapping from prefix to namespace URI used to resolve prefixed QNames
     * @param aSelectors
     *            collection of selectors to match against; selectors are either QNames or expanded
     *            names in the form {@code {namespace}localName}
     * @return {@code true} if the element's name matches any selector, {@code false} otherwise
     */
    public static boolean matchesAny(XmlElement aElement, Map<String, String> aNamespaceMappings,
            Collection<String> aSelectors)
    {
        var nsSep = aElement.getQName().indexOf(':');

        if (nsSep > -1) {
            var prefix = aElement.getQName().substring(0, nsSep);
            var localName = aElement.getQName().substring(nsSep + 1);
            var ns = aNamespaceMappings.get(prefix);
            if (ns != null) {
                return aSelectors.contains("{" + ns + "}" + localName);
            }
            return false;
        }

        return aSelectors.contains(aElement.getQName());
    }

    /**
     * Obtain the attributes of the given {@link XmlElement} as a SAX {@link Attributes} instance.
     *
     * @param aElement
     *            the element whose attributes to convert
     * @return an {@link Attributes} object (never {@code null})
     */
    public static AttributesImpl attributes(XmlElement aElement)
    {
        // FIXME: SAX parsers would skip xmlns attributes if namespace support is enabled. Maybe
        // we should do the same?

        var attrs = new AttributesImpl();
        if (aElement.getAttributes() != null) {
            for (var attr : aElement.getAttributes()) {
                attrs.addAttribute(defaultString(attr.getUri()), defaultString(attr.getLocalName()),
                        defaultString(attr.getQName()),
                        Objects.toString(attr.getValueType(), "CDATA"),
                        defaultString(attr.getValue()));
            }
        }
        return attrs;
    }

}
