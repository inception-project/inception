/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.formats;

import static de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionUtils.getInputStream;
import static de.tudarmstadt.ukp.dkpro.core.api.resources.MappingProviderFactory.createPosMappingProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.lucene.util.packed.DirectMonotonicReader.Meta;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.pos.POSUtils;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.resources.MappingProvider;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DependencyFlavor;
import de.tudarmstadt.ukp.inception.formats.perseus.internal.model.PerseusSentence;
import de.tudarmstadt.ukp.inception.formats.perseus.internal.model.PerseusWord;

public class PerseusReader
    extends JCasResourceCollectionReader_ImplBase
{
    /**
     * Read fine-grained part-of-speech information.
     */
    public static final String PARAM_READ_POS = ComponentParameters.PARAM_READ_POS;
    @ConfigurationParameter(name = PARAM_READ_POS, mandatory = true, defaultValue = "true")
    private boolean readPos;

    /**
     * Location of the mapping file for part-of-speech tags to UIMA types.
     */
    public static final String PARAM_POS_MAPPING_LOCATION = 
            ComponentParameters.PARAM_POS_MAPPING_LOCATION;
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
     * Read lemma information.
     */
    public static final String PARAM_READ_LEMMA = ComponentParameters.PARAM_READ_LEMMA;
    @ConfigurationParameter(name = PARAM_READ_LEMMA, mandatory = true, defaultValue = "true")
    private boolean readLemma;

    /**
     * Read syntactic dependency information.
     */
    public static final String PARAM_READ_DEPENDENCY = ComponentParameters.PARAM_READ_DEPENDENCY;
    @ConfigurationParameter(name = PARAM_READ_DEPENDENCY, mandatory = true, defaultValue = "true")
    private boolean readDependency;

    private MappingProvider posMappingProvider;

    @Override
    public void initialize(UimaContext aContext)
        throws ResourceInitializationException
    {
        super.initialize(aContext);

        posMappingProvider = createPosMappingProvider(mappingPosLocation, posTagset, getLanguage());
    }

    @Override
    public void getNext(JCas aJCas)
        throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);

        try {
            posMappingProvider.configure(aJCas.getCas());
        }
        catch (AnalysisEngineProcessException e) {
            throw new IOException(e);
        }

        try (InputStream is = getInputStream(res.getLocation(), res.getInputStream())) {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(is);

            JAXBContext context = JAXBContext.newInstance(Meta.class, PerseusSentence.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            JCasBuilder jb = new JCasBuilder(aJCas);

            XMLEvent e = null;
            while ((e = xmlEventReader.peek()) != null) {
                if (isStartElement(e, "sentence")) {
                    PerseusSentence sentence = unmarshaller
                            .unmarshal(xmlEventReader, PerseusSentence.class).getValue();
                    readSentence(jb, sentence);
                }
                else {
                    xmlEventReader.next();
                }

            }

            jb.close();
        }
        catch (XMLStreamException ex1) {
            throw new IOException(ex1);
        }
        catch (JAXBException ex2) {
            throw new IOException(ex2);
        }
    }

    protected void readSentence(JCasBuilder aBuilder, PerseusSentence aSentence)
    {
        int sentenceBegin = aBuilder.getPosition();
        int sentenceEnd = aBuilder.getPosition();
        Map<Integer, PerseusWord> perseusWords = new LinkedHashMap<>();
        Map<Integer, Token> tokens = new LinkedHashMap<>();
        
        for (PerseusWord w : aSentence.words) {
            Token token = aBuilder.add(w.form, Token.class);
            token.setId(w.id);
            tokens.put(Integer.valueOf(w.id), token);
            perseusWords.put(Integer.valueOf(w.id), w);
            
            if (readLemma && w.lemma != null) {
                Lemma lemma = new Lemma(aBuilder.getJCas(), token.getBegin(), token.getEnd());
                lemma.setValue(w.lemma);
                lemma.addToIndexes();
                token.setLemma(lemma);
            }

            if (readPos) {
                Type posType = posMappingProvider.getTagType(w.postag);
                POS posAnno = (POS) aBuilder.getJCas().getCas().createAnnotation(posType,
                        token.getBegin(), token.getEnd());
                if (w.postag != null) {
                    posAnno.setPosValue(w.postag.intern());
                }
                POSUtils.assignCoarseValue(posAnno);
                posAnno.addToIndexes();
                token.setPos(posAnno);
            }
            else {
                System.out.println();
            }

            token.addToIndexes();
            
            // Remember position before adding space
            sentenceEnd = aBuilder.getPosition();

            aBuilder.add(" ");

        }
        aBuilder.add("\n");

        // Dependencies
        if (readDependency) {
            for (PerseusWord word : perseusWords.values()) {
                int depId = Integer.valueOf(word.id);
                int govId = word.head;
                
                // Model the root as a loop onto itself
                Dependency rel;
                if (govId == 0) {
                    rel = new Dependency(aBuilder.getJCas());
                    rel.setGovernor(tokens.get(depId));
                    rel.setDependent(tokens.get(depId));
                    rel.setDependencyType(word.relation);
                    rel.setBegin(rel.getDependent().getBegin());
                    rel.setEnd(rel.getDependent().getEnd());
                    rel.setFlavor(DependencyFlavor.BASIC);
                    rel.addToIndexes();
                }
                else {
                    rel = new Dependency(aBuilder.getJCas());
                    rel.setGovernor(tokens.get(govId));
                    rel.setDependent(tokens.get(depId));
                    rel.setDependencyType(word.relation);
                    rel.setBegin(rel.getDependent().getBegin());
                    rel.setEnd(rel.getDependent().getEnd());
                    rel.setFlavor(DependencyFlavor.BASIC);
                    rel.addToIndexes();
                }
                
                if (rel.getDependent() == null) {
                    throw new IllegalStateException(
                            "Referred dependent with ID [" + depId + "] not found");
                }
                if (rel.getGovernor() == null) {
                    throw new IllegalStateException(
                            "Referred governor with ID [" + govId + "] not found");
                }
            }
        }        
        Sentence sentence = new Sentence(aBuilder.getJCas(), sentenceBegin, sentenceEnd);
        sentence.setId(String.valueOf(aSentence.id));
        sentence.addToIndexes();
    }

    public static boolean isStartElement(XMLEvent aEvent, String aElement)
    {
        return aEvent.isStartElement()
                && ((StartElement) aEvent).getName().getLocalPart().equals(aElement);
    }
}
