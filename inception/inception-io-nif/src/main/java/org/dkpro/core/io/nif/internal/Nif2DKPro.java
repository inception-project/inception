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
package org.dkpro.core.io.nif.internal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.iterators.IteratorIterable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.dkpro.core.api.lexmorph.pos.POSUtils;
import org.dkpro.core.api.resources.MappingProvider;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Heading;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class Nif2DKPro
{
    private MappingProvider posMappingProvider;

    public void setPosMappingProvider(MappingProvider aPosMappingProvider)
    {
        posMappingProvider = aPosMappingProvider;
    }

    public void convert(Statement aContext, JCas aJCas)
    {
        Model m = aContext.getModel();

        final Resource tSentence = m.createResource(NIF.TYPE_SENTENCE);
        final Resource tWord = m.createResource(NIF.TYPE_WORD);
        final Resource tTitle = m.createResource(NIF.TYPE_TITLE);
        final Resource tParagraph = m.createResource(NIF.TYPE_PARAGRAPH);

        final Property pReferenceContext = m.createProperty(NIF.PROP_REFERENCE_CONTEXT);
        final Property pIsString = m.createProperty(NIF.PROP_IS_STRING);
        final Property pBeginIndex = m.createProperty(NIF.PROP_BEGIN_INDEX);
        final Property pEndIndex = m.createProperty(NIF.PROP_END_INDEX);
        final Property pLemma = m.createProperty(NIF.PROP_LEMMA);
        final Property pStem = m.createProperty(NIF.PROP_STEM);
        final Property pPosTag = m.createProperty(NIF.PROP_POS_TAG);
        final Property pTaMsClassRef = m.createProperty(NIF.PROP_TA_MS_CLASS_REF);
        final Property pTaIdentRef = m.createProperty(ITS.PROP_TA_IDENT_REF);
        final Property pTaClassRef = m.createProperty(ITS.PROP_TA_CLASS_REF);

        // Convert context node -> document text
        String text = m.getProperty(aContext.getSubject(), pIsString).getString();
        aJCas.setDocumentText(text);

        // Convert headings/titles
        Iterator<Resource> headingIterator = m.listResourcesWithProperty(RDF.type, tTitle)
                .filterKeep(res -> res.getProperty(pReferenceContext).getResource()
                        .equals(aContext.getSubject()));
        for (Resource nifTitle : new IteratorIterable<Resource>(headingIterator)) {
            int begin = nifTitle.getProperty(pBeginIndex).getInt();
            int end = nifTitle.getProperty(pEndIndex).getInt();
            Heading uimaHeading = new Heading(aJCas, begin, end);
            uimaHeading.addToIndexes();

            assert assertSanity(nifTitle, uimaHeading);
        }

        // Convert paragraphs
        Iterator<Resource> paragraphIterator = m.listResourcesWithProperty(RDF.type, tParagraph)
                .filterKeep(res -> res.getProperty(pReferenceContext).getResource()
                        .equals(aContext.getSubject()));
        for (Resource nifParagraph : new IteratorIterable<Resource>(paragraphIterator)) {
            int begin = nifParagraph.getProperty(pBeginIndex).getInt();
            int end = nifParagraph.getProperty(pEndIndex).getInt();
            Paragraph uimaParagraph = new Paragraph(aJCas, begin, end);
            uimaParagraph.addToIndexes();

            assert assertSanity(nifParagraph, uimaParagraph);
        }

        // Convert sentences
        List<Resource> nifSentences = m
                .listResourcesWithProperty(RDF.type, tSentence).filterKeep(res -> res
                        .getProperty(pReferenceContext).getResource().equals(aContext.getSubject()))
                .toList();
        nifSentences.sort((a, b) -> a.getProperty(pBeginIndex).getInt()
                - b.getProperty(pBeginIndex).getInt());
        for (Resource nifSentence : nifSentences) {
            int begin = nifSentence.getProperty(pBeginIndex).getInt();
            int end = nifSentence.getProperty(pEndIndex).getInt();
            Sentence uimaSentence = new Sentence(aJCas, begin, end);
            uimaSentence.addToIndexes();

            assert assertSanity(nifSentence, uimaSentence);
        }

        // Convert tokens
        Iterator<Resource> tokenIterator = m.listResourcesWithProperty(RDF.type, tWord)
                .filterKeep(res -> res.getProperty(pReferenceContext).getResource()
                        .equals(aContext.getSubject()));
        for (Resource nifWord : new IteratorIterable<Resource>(tokenIterator)) {
            int begin = nifWord.getProperty(pBeginIndex).getInt();
            int end = nifWord.getProperty(pEndIndex).getInt();
            Token uimaToken = new Token(aJCas, begin, end);
            uimaToken.addToIndexes();

            assert assertSanity(nifWord, uimaToken);

            // Convert lemma
            if (nifWord.hasProperty(pLemma)) {
                Lemma uimaLemma = new Lemma(aJCas, uimaToken.getBegin(), uimaToken.getEnd());
                uimaLemma.setValue(nifWord.getProperty(pLemma).getString());
                uimaLemma.addToIndexes();
                uimaToken.setLemma(uimaLemma);
            }

            // Convert stem
            if (nifWord.hasProperty(pLemma)) {
                Stem uimaStem = new Stem(aJCas, uimaToken.getBegin(), uimaToken.getEnd());
                uimaStem.setValue(nifWord.getProperty(pStem).getString());
                uimaStem.addToIndexes();
                uimaToken.setStem(uimaStem);
            }

            // Convert posTag (this is discouraged, the better alternative should be oliaLink)
            if (nifWord.hasProperty(pPosTag)) {
                String tag = nifWord.getProperty(pStem).getString();
                Type posTag = posMappingProvider.getTagType(tag);
                POS uimaPos = (POS) aJCas.getCas().createAnnotation(posTag, uimaToken.getBegin(),
                        uimaToken.getEnd());
                uimaPos.setPosValue(tag != null ? tag.intern() : null);
                POSUtils.assignCoarseValue(uimaPos);
                uimaPos.addToIndexes();
                uimaToken.setPos(uimaPos);
            }
        }

        // Convert named entities
        //
        // NIF uses taIdentRef to link to a unique instance of an entity and taClassRef to identify
        // the category of the entity. Named entity recognizers in DKPro Core just categorizes the
        // entity, e.g. as a person, location, or whatnot. For what NIF uses, we'd need a named
        // entity linker, not just a recognizer. Furthermore, the DKPro Core named entity
        // recognizers are not mapped to a common tag set (unlike e.g. POS which is mapped to
        // the universal POS tags).
        //
        // So, what we do here is treating the URI of the taClassRef in NIF simply as the
        // named entity category and store it.
        //
        // Here we use duck-typing, i.e. it has a taClassRef property then it is likely a named
        // entity. NIF 2.1 [1] appears to introduce a representation of named entities using the
        // class "EntityOccurrence", but e.g. kore50 [2] doesn't seem to use that - it uses "Phrase"
        // instead.
        //
        // [1] http://nif.readthedocs.io/en/2.1-rc/prov-and-conf.html
        // [2] https://datahub.io/dataset/kore-50-nif-ner-corpus
        Set<Resource> nifNamedEntitiesTaIdentRef = m
                .listResourcesWithProperty(pTaIdentRef).filterKeep(res -> res
                        .getProperty(pReferenceContext).getResource().equals(aContext.getSubject()))
                .toSet();
        Set<Resource> nifNamedEntitiesTaClassRef = m
                .listResourcesWithProperty(pTaClassRef).filterKeep(res -> res
                        .getProperty(pReferenceContext).getResource().equals(aContext.getSubject()))
                .toSet();
        Set<Resource> nifNamedEntitiesTaMsClassRef = m
                .listResourcesWithProperty(pTaMsClassRef).filterKeep(res -> res
                        .getProperty(pReferenceContext).getResource().equals(aContext.getSubject()))
                .toSet();
        Set<Resource> nifNamedEntities = new HashSet<Resource>();
        nifNamedEntities.addAll(nifNamedEntitiesTaIdentRef);
        nifNamedEntities.addAll(nifNamedEntitiesTaClassRef);
        nifNamedEntities.addAll(nifNamedEntitiesTaMsClassRef);
        for (Resource nifNamedEntity : nifNamedEntities) {
            int begin = nifNamedEntity.getProperty(pBeginIndex).getInt();
            int end = nifNamedEntity.getProperty(pEndIndex).getInt();
            NamedEntity uimaNamedEntity = new NamedEntity(aJCas, begin, end);

            // If there is a most-specific class, then we use that
            if (nifNamedEntity.hasProperty(pTaMsClassRef)) {
                uimaNamedEntity
                        .setValue(nifNamedEntity.getProperty(pTaMsClassRef).getResource().getURI());
            }
            // ... else, we use some class
            else if (nifNamedEntity.hasProperty(pTaClassRef)) {
                uimaNamedEntity
                        .setValue(nifNamedEntity.getProperty(pTaClassRef).getResource().getURI());
            }

            // If the entity is linked, then we keep the identifier
            if (nifNamedEntity.hasProperty(pTaIdentRef)) {
                uimaNamedEntity.setIdentifier(
                        nifNamedEntity.getProperty(pTaIdentRef).getResource().getURI());
            }
            uimaNamedEntity.addToIndexes();

            assert assertSanity(nifNamedEntity, uimaNamedEntity);
        }
    }

    private static boolean assertSanity(Resource aNif, Annotation aUima)
    {
        final Property pAnchorOf = aNif.getModel().createProperty(NIF.PROP_ANCHOR_OF);

        int docLength = aUima.getCAS().getDocumentText().length();

        if (aNif.hasProperty(pAnchorOf)) {
            String nifText = aNif.getProperty(pAnchorOf).getString();
            String uimaText = aUima.getCoveredText();
            assert nifText.equals(uimaText);
        }
        assert aUima.getBegin() >= 0 && aUima.getBegin() <= docLength;
        assert aUima.getEnd() >= 0 && aUima.getEnd() <= docLength;

        return true;
    }
}
