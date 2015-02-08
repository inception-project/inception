/*******************************************************************************
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.tei;

import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.SAXWriter;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom4j.Dom4jXPath;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Reader for the TEI XML.
 *
 * @author Richard Eckart de Castilho
 * @author Seid Muhie Yimam
 */
@TypeCapability(outputs = { "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS",
        "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity" })
public class TeiReader
    extends ResourceCollectionReaderBase
{
    /**
     * Write token annotations to the CAS.
     */
    public static final String PARAM_WRITE_TOKEN = ComponentParameters.PARAM_WRITE_TOKEN;
    @ConfigurationParameter(name = PARAM_WRITE_TOKEN, mandatory = true, defaultValue = "true")
    private boolean writeTokens;

    /**
     * Write part-of-speech annotations to the CAS.
     */
    public static final String PARAM_WRITE_POS = ComponentParameters.PARAM_WRITE_POS;
    @ConfigurationParameter(name = PARAM_WRITE_POS, mandatory = true, defaultValue = "true")
    private boolean writePOS;

    /**
     * Write lemma annotations to the CAS.
     */
    public static final String PARAM_WRITE_LEMMA = ComponentParameters.PARAM_WRITE_LEMMA;
    @ConfigurationParameter(name = PARAM_WRITE_LEMMA, mandatory = true, defaultValue = "true")
    private boolean writeLemma;

    /**
     * Write sentence annotations to the CAS.
     */
    public static final String PARAM_WRITE_SENTENCE = ComponentParameters.PARAM_WRITE_SENTENCE;
    @ConfigurationParameter(name = PARAM_WRITE_SENTENCE, mandatory = true, defaultValue = "true")
    private boolean writeSentences;

    /**
     * Use the xml:id attribute on the TEI elements as document ID. Mind that many TEI files may not
     * have this attribute on all TEI elements and you may end up with no document ID at all. Also
     * mind that the IDs should be unique.
     */
    public static final String PARAM_USE_XML_ID = "useXmlId";
    @ConfigurationParameter(name = PARAM_USE_XML_ID, mandatory = true, defaultValue = "false")
    private boolean useXmlId;

    /**
     * When not using the XML ID, use only the filename instead of the whole URL as ID. Mind that
     * the filenames should be unique in this case.
     */
    public static final String PARAM_USE_FILENAME_ID = "useFilenameId";
    @ConfigurationParameter(name = PARAM_USE_FILENAME_ID, mandatory = true, defaultValue = "false")
    private boolean useFilenameId;

    /**
     * Do not write <em>ignoreable whitespace</em> from the XML file to the CAS.
     */
    // REC: This does not seem to work. Maybe because SAXWriter does not generate this event?
    public static final String PARAM_OMIT_IGNORABLE_WHITESPACE = "omitIgnorableWhitespace";
    @ConfigurationParameter(name = PARAM_OMIT_IGNORABLE_WHITESPACE, mandatory = true, defaultValue = "false")
    private boolean omitIgnorableWhitespace;

    /**
     * Location of the mapping file for part-of-speech tags to UIMA types.
     */
    public static final String PARAM_POS_MAPPING_LOCATION = ComponentParameters.PARAM_POS_MAPPING_LOCATION;
    @ConfigurationParameter(name = PARAM_POS_MAPPING_LOCATION, mandatory = false)
    protected String mappingPosLocation;

    /**
     * Use this part-of-speech tag set to use to resolve the tag set mapping instead of using the
     * tag set defined as part of the model meta data. This can be useful if a custom model is
     * specified which does not have such meta data, or it can be used in readers.
     */
    public static final String PARAM_POS_TAG_SET = ComponentParameters.PARAM_POS_TAG_SET;
    @ConfigurationParameter(name = PARAM_POS_TAG_SET, mandatory = false)
    protected String posTagset;

    /**
     * (character) contains a significant punctuation mark identified with an attribute type of p
     * for non space characters and s for space character.
     */
    private static final String TAG_CHARACTER = "c";

    private static final String TAG_LANG = "language";

    private static final String SPACE_CHAR = " ";
    private static final String ANA = "ana";
    private static final String FROM = "from";
    private static final String INDENT = "ident";

    /**
     * (word) represents a grammatical (not necessarily orthographic) word.
     */
    private static final String TAG_WORD = "w";

    /**
     * (s-unit) contains a sentence-like division of a text.
     */
    private static final String TAG_SUNIT = "s";

    /**
     * A tag for a group of annotations such as lemm, pos and sense (Named Enity layer used here)
     * annotations
     */
    private static final String TAG_SPAN_GRP = "spanGrp";

    /**
     * An annotation which comprises of actual annotations together with the id of the token(
     * TAG_WORD/TAG_CHARACTER)
     */
    private static final String TAG_SPAN = "span";

    private Iterator<Element> teiElementIterator;
    private Element currentTeiElement;
    private Resource currentResource;
    @SuppressWarnings("unused")
    private int currentTeiElementNumber;

    @Override
    public void initialize(UimaContext aContext)
        throws ResourceInitializationException
    {
        super.initialize(aContext);

        if (writePOS && !writeTokens) {
            throw new ResourceInitializationException(new IllegalArgumentException(
                    "Setting writePOS to 'true' requires writeToken to be 'true' too."));
        }

        try {
            // Init with an empty iterator
            teiElementIterator = asList(new Element[0]).iterator();

            // Make sure we know about the first element;
            nextTeiElement();
        }
        catch (CollectionException e) {
            new ResourceInitializationException(e);
        }
        catch (IOException e) {
            new ResourceInitializationException(e);
        }
    }

    private void nextTeiElement()
        throws CollectionException, IOException
    {
        if (teiElementIterator == null) {
            currentTeiElement = null;
            return;
        }

        while (!teiElementIterator.hasNext() && super.hasNext()) {
            currentResource = nextFile();

            InputStream is = null;

            try {
                is = currentResource.getInputStream();

                if (currentResource.getPath().endsWith(".gz")) {
                    is = new GZIPInputStream(is);
                }

                InputSource source = new InputSource(is);
                source.setPublicId(currentResource.getLocation());
                source.setSystemId(currentResource.getLocation());

                SAXReader reader = new SAXReader();
                Document xml = reader.read(source);

                final XPath teiPath = new Dom4jXPath("//tei:TEI");
                teiPath.addNamespace("tei", "http://www.tei-c.org/ns/1.0");

                @SuppressWarnings("unchecked")
                List<Element> teiElements = teiPath.selectNodes(xml);

                teiElementIterator = teiElements.iterator();
                currentTeiElementNumber = 0;
            }
            catch (DocumentException e) {
                throw new IOException(e);
            }
            catch (JaxenException e) {
                throw new IOException(e);
            }
            finally {
                closeQuietly(is);
            }
        }

        currentTeiElement = teiElementIterator.hasNext() ? teiElementIterator.next() : null;
        currentTeiElementNumber++;

        if (!super.hasNext() && !teiElementIterator.hasNext()) {
            // Mark end of processing.
            teiElementIterator = null;
        }
    }

    @Override
    public boolean hasNext()
        throws IOException, CollectionException
    {
        return teiElementIterator != null || currentTeiElement != null;
    }

    @Override
    public void getNext(CAS aCAS)
        throws IOException, CollectionException
    {
        initCas(aCAS, currentResource);

        InputStream is = null;

        try {
            JCas jcas = aCAS.getJCas();

            // Create handler
            Handler handler = newSaxHandler();
            handler.setJCas(jcas);
            handler.setLogger(getLogger());

            // Parse TEI text
            SAXWriter writer = new SAXWriter(handler);
            writer.write(currentTeiElement);
            handler.endDocument();
        }
        catch (CASException e) {
            throw new CollectionException(e);
        }
        catch (SAXException e) {
            throw new IOException(e);
        }
        catch (Exception e) {
            throw new IOException("This is not a valid WebAnno CPH TEI file");
        }
        finally {
            closeQuietly(is);
        }

        // Move currentTeiElement to the next text
        nextTeiElement();
    }

    protected Handler newSaxHandler()
    {
        return new TeiHandler();
    }

    /**
     * @author Richard Eckart de Castilho
     */
    protected abstract static class Handler
        extends DefaultHandler
    {
        private JCas jcas;
        private Logger logger;

        public void setJCas(final JCas aJCas)
        {
            jcas = aJCas;
        }

        protected JCas getJCas()
        {
            return jcas;
        }

        public void setLogger(Logger aLogger)
        {
            logger = aLogger;
        }

        public Logger getLogger()
        {
            return logger;
        }
    }

    public class TeiHandler
        extends Handler
    {
        private boolean isSpaceChar = false;
        private boolean addLemma = false;
        private boolean addPos = false;
        private boolean addNe = false;

        private boolean captureText = false;
        private int sentenceStart = -1;
        private int tokenStart = -1;

        String tokenId = null;
        private String lemma = null;
        private String posTag = null;
        private String neTag = null;

        private String language = null;

        Map<String, Token> tokenIds = new LinkedHashMap<String, Token>();

        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void endDocument()
            throws SAXException
        {
            getJCas().setDocumentText(buffer.toString());
            // Set up language
            if (language != null) {
                getJCas().setDocumentLanguage(language);
            }
            else if (getConfigParameterValue(PARAM_LANGUAGE) != null) {
                getJCas().setDocumentLanguage((String) getConfigParameterValue(PARAM_LANGUAGE));
            }
        }

        protected StringBuilder getBuffer()
        {
            return buffer;
        }

        @Override
        public void startElement(String aUri, String aLocalName, String aName,
                Attributes aAttributes)
            throws SAXException
        {
            if (TAG_WORD.equals(aName) || TAG_CHARACTER.equals(aName)) {
                if (aAttributes.getValue("type") != null
                        && aAttributes.getValue("type").equals("s")) {
                    isSpaceChar = true;
                }
                else {
                    isSpaceChar = false;
                }
                tokenId = aAttributes.getValue("xml:id");
                captureText = true;
                tokenStart = getBuffer().length();
            }
            else if (TAG_SUNIT.equals(aName)) {
                captureText = false;
                sentenceStart = getBuffer().length();
            }
            else if (TAG_SPAN_GRP.equals(aName)) {
                if (aAttributes.getValue(ANA).equals("#ePOSlemmatizer")) {
                    addLemma = true;
                    addPos = false;
                    addNe = false;
                }
                else if (aAttributes.getValue(ANA).equals("#ePOStagger")) {
                    addLemma = false;
                    addPos = true;
                    addNe = false;
                }
                else if (aAttributes.getValue(ANA).equals("#automatic-supersense-from-dannet")) {
                    addLemma = false;
                    addPos = false;
                    addNe = true;
                }
                captureText = false;
            }
            else if (TAG_SPAN.equals(aName)) {
                captureText = true;
                tokenId = aAttributes.getValue(FROM);
            }
            else if (TAG_LANG.equals(aName)) {
                captureText = false;
                language = aAttributes.getValue(INDENT);
            }
            else {
                captureText = false;
            }
        }

        @Override
        public void endElement(String aUri, String aLocalName, String aName)
            throws SAXException
        {
            if (TAG_SUNIT.equals(aName)) {
                if (writeSentences) {
                    new Sentence(getJCas(), sentenceStart, getBuffer().length()).addToIndexes();
                }
                sentenceStart = -1;
            }
            else if (TAG_WORD.equals(aName) || TAG_CHARACTER.equals(aName)) {
                if (isNotBlank(getBuffer().substring(tokenStart, getBuffer().length()))) {
                    Token token = new Token(getJCas(), tokenStart, getBuffer().length());

                    tokenIds.put(tokenId, token);
                    if (writeTokens) {
                        token.addToIndexes();
                    }
                }

                tokenStart = -1;
            }
            else if (TAG_SPAN.equals(aName)) {
                Token token = tokenIds.get(tokenId.substring(1));
                if (addPos) {

                    boolean duplicate = false;
                    for (POS pos : JCasUtil.selectCovered(getJCas(), POS.class, token.getBegin(),
                            token.getEnd())) {
                        if (pos.getBegin() == token.getBegin() && pos.getEnd() == token.getEnd()) {
                            if (pos.getPosValue().equals(this.posTag)) {
                                duplicate = true;
                                break;
                            }
                        }

                    }
                    if (!duplicate) {
                        POS pos = new POS(getJCas(), token.getBegin(), token.getEnd());
                        pos.setPosValue(this.posTag);
                        pos.addToIndexes();
                        token.setPos(pos);
                        token.addToIndexes();
                    }
                }
                else if (addLemma) {
                    boolean duplicate = false;
                    for (Lemma lemma : JCasUtil.selectCovered(getJCas(), Lemma.class,
                            token.getBegin(), token.getEnd())) {
                        if (lemma.getBegin() == token.getBegin()
                                && lemma.getEnd() == token.getEnd()) {
                            if (lemma.getValue().equals(this.lemma)) {
                                duplicate = true;
                                break;
                            }
                        }

                    }
                    if (!duplicate) {
                        Lemma lemma = new Lemma(getJCas(), token.getBegin(), token.getEnd());
                        lemma.setValue(this.lemma);
                        lemma.addToIndexes();
                        token.setLemma(lemma);
                        token.addToIndexes();
                    }
                }
                else if (addNe) {
                    boolean duplicate = false;
                    for (NamedEntity ne : JCasUtil.selectCovered(getJCas(), NamedEntity.class,
                            token.getBegin(), token.getEnd())) {
                        if (ne.getBegin() == token.getBegin() && ne.getEnd() == token.getEnd()) {
                            if (ne.getValue().equals(this.neTag)) {
                                duplicate = true;
                                break;
                            }
                        }

                    }
                    if (!duplicate) {
                        NamedEntity ne = new NamedEntity(getJCas(), token.getBegin(),
                                token.getEnd());
                        ne.setValue(this.neTag);
                        ne.addToIndexes();
                    }
                }
            }
        }

        @Override
        public void characters(char[] aCh, int aStart, int aLength)
            throws SAXException
        {
            StringBuffer sb = new StringBuffer();
            sb.append(aCh, aStart, aLength);
            if (captureText) {
                if (isSpaceChar && !buffer.toString().isEmpty()) {
                    buffer.append(SPACE_CHAR);
                }
                else if (addLemma) {
                    lemma = sb.toString().trim();
                }
                else if (addPos) {
                    posTag = sb.toString().trim();
                }
                else if (addNe) {
                    neTag = sb.toString().trim();
                }
                else {
                    buffer.append(sb.toString().trim());
                }

            }
        }

    }
}
