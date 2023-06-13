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
package de.tudarmstadt.ukp.inception.support.xml;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Comparator.comparing;
import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.METHOD;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;

import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class XmlParserUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String EXTTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    public static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    public static final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
    public static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private XmlParserUtils()
    {
        // No instances
    }

    public static Comparator<QName> caseInsensitiveQNameComparator()
    {
        return comparing(QName::getNamespaceURI).thenComparing(QName::getLocalPart,
                CASE_INSENSITIVE_ORDER);
    }

    public static String getQName(QName aElement)
    {
        var qName = aElement.getLocalPart();
        if (!aElement.getPrefix().isEmpty()) {
            qName = aElement.getPrefix() + ':' + qName;
        }
        return qName;
    }

    public static ContentHandler makeXmlSerializer(Writer aOut)
        throws TransformerConfigurationException
    {
        SAXTransformerFactory tf = newTransformerFactory();
        TransformerHandler th = tf.newTransformerHandler();
        th.getTransformer().setOutputProperty(OMIT_XML_DECLARATION, "yes");
        th.getTransformer().setOutputProperty(METHOD, "xml");
        th.getTransformer().setOutputProperty(INDENT, "no");
        th.setResult(new StreamResult(aOut));
        return th;
    }

    public static SAXTransformerFactory newTransformerFactory()
        throws TransformerConfigurationException
    {
        var factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return (SAXTransformerFactory) factory;
    }

    public static SAXParserFactory newSaxParserFactory()
        throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setXIncludeAware(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setFeature(factory, EXTTERNAL_GENERAL_ENTITIES, false);
        setFeature(factory, EXTERNAL_PARAMETER_ENTITIES, false);
        setFeature(factory, DISALLOW_DOCTYPE_DECL, true);
        setFeature(factory, LOAD_EXTERNAL_DTD, false);
        return factory;
    }

    public static SAXParserFactory enableNamespaceSupport(SAXParserFactory aFactory)
        throws ParserConfigurationException
    {
        XmlParserUtils.setFeature(aFactory, "http://xml.org/sax/features/namespaces", true);
        return aFactory;
    }

    public static void setFeature(SAXParserFactory aFactory, String aFeature, boolean aValue)
        throws ParserConfigurationException
    {
        try {
            aFactory.setFeature(aFeature, aValue);
        }
        catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            if (LOG.isTraceEnabled()) {
                LOG.warn("Unable to set SAX parser option [{}] to [{}]", aFeature, aValue, e);
            }
            else {
                LOG.warn("Unable to set SAX parser option [{}] to [{}]", aFeature, aValue);
            }
        }
    }

    public static SAXParser newSaxParser(SAXParserFactory aFactory)
        throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException,
        SAXException
    {
        SAXParser saxParser = aFactory.newSAXParser();
        saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return saxParser;
    }

    public static SAXParser newSaxParser()
        throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException,
        SAXException
    {
        return newSaxParser(newSaxParserFactory());
    }

    public static XMLInputFactory newXmlInputFactory()
    {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
        return xmlInputFactory;
    }

    public static boolean isStartElement(XMLEvent aEvent, String aElement)
    {
        return aEvent.isStartElement()
                && ((StartElement) aEvent).getName().getLocalPart().equals(aElement);
    }
}
