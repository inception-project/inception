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

import java.lang.invoke.MethodHandles;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class XmlParserUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String EXTTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    private static final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private XmlParserUtils()
    {
        // No instances
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

    private static void setFeature(SAXParserFactory aFactory, String aFeature, boolean aValue)
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

    public static SAXParser newSaxParser()
        throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException,
        SAXException
    {
        SAXParser saxParser = newSaxParserFactory().newSAXParser();
        saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return saxParser;
    }
}
