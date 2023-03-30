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

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDnonNegativeInteger;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDstring;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import org.apache.jena.irix.IRIs;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Heading;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class DKPro2Nif
{
    public static void convert(JCas aJCas, OntModel aTarget)
    {
        // Shorten down variable name for model
        OntModel m = aTarget;

        // Set up query instances
        final Resource tContext = m.createResource(NIF.TYPE_CONTEXT);
        final Resource tSentence = m.createResource(NIF.TYPE_SENTENCE);
        final Resource tWord = m.createResource(NIF.TYPE_WORD);
        final Resource tTitle = m.createResource(NIF.TYPE_TITLE);
        final Resource tParagraph = m.createResource(NIF.TYPE_PARAGRAPH);
        final Resource tEntityOccurrence = m.createResource(NIF.TYPE_ENTITY_OCCURRENCE);
        final Resource tOffsetBasedString = m.createResource(NIF.TYPE_OFFSET_BASED_STRING);

        final Property pReferenceContext = m.createProperty(NIF.PROP_REFERENCE_CONTEXT);
        final Property pIsString = m.createProperty(NIF.PROP_IS_STRING);
        final Property pAnchorOf = m.createProperty(NIF.PROP_ANCHOR_OF);
        final Property pBeginIndex = m.createProperty(NIF.PROP_BEGIN_INDEX);
        final Property pEndIndex = m.createProperty(NIF.PROP_END_INDEX);
        final Property pStem = m.createProperty(NIF.PROP_STEM);
        final Property pLemma = m.createProperty(NIF.PROP_LEMMA);
        final Property pPosTag = m.createProperty(NIF.PROP_POS_TAG);
        final Property pWord = m.createProperty(NIF.PROP_WORD);
        final Property pNextWord = m.createProperty(NIF.PROP_NEXT_WORD);
        final Property pPreviousWord = m.createProperty(NIF.PROP_PREVIOUS_WORD);
        final Property pSentence = m.createProperty(NIF.PROP_SENTENCE);
        final Property pNextSentence = m.createProperty(NIF.PROP_NEXT_SENTENCE);
        final Property pPreviousSentence = m.createProperty(NIF.PROP_PREVIOUS_SENTENCE);
        final Property pTaIdentRef = m.createProperty(ITS.PROP_TA_IDENT_REF);
        final Property pTaClassRef = m.createProperty(ITS.PROP_TA_CLASS_REF);

        // Get a URI for the document
        DocumentMetaData dmd = DocumentMetaData.get(aJCas);
        String docuri = dmd.getDocumentUri() != null ? dmd.getDocumentUri()
                : "urn:" + dmd.getDocumentId();

        // Convert document -> context node
        Individual context;
        {
            String uri = String.format("%s#offset_%d_%d", docuri, 0,
                    aJCas.getDocumentText().length());
            context = m.createIndividual(uri, tContext);
            context.addRDFType(tOffsetBasedString);
            context.addLiteral(pIsString, m.createTypedLiteral(aJCas.getDocumentText(), XSDstring));
            context.addLiteral(pBeginIndex, m.createTypedLiteral(0, XSDnonNegativeInteger));
            context.addLiteral(pEndIndex,
                    m.createTypedLiteral(aJCas.getDocumentText().length(), XSDnonNegativeInteger));
        }

        // Convert headings/titles
        for (Heading uimaHeading : select(aJCas, Heading.class)) {
            String headingUri = String.format("%s#offset_%d_%d", docuri, uimaHeading.getBegin(),
                    uimaHeading.getEnd());
            Individual nifTitle = m.createIndividual(headingUri, tTitle);
            nifTitle.addRDFType(tOffsetBasedString);
            nifTitle.addProperty(pReferenceContext, context);
            nifTitle.addLiteral(pAnchorOf, uimaHeading.getCoveredText());
            nifTitle.addLiteral(pBeginIndex,
                    m.createTypedLiteral(uimaHeading.getBegin(), XSDnonNegativeInteger));
            nifTitle.addLiteral(pEndIndex,
                    m.createTypedLiteral(uimaHeading.getEnd(), XSDnonNegativeInteger));
        }

        // Convert paragraphs
        for (Paragraph uimaParagraph : select(aJCas, Paragraph.class)) {
            String paragraphUri = String.format("%s#offset_%d_%d", docuri, uimaParagraph.getBegin(),
                    uimaParagraph.getEnd());
            Individual nifParagraph = m.createIndividual(paragraphUri, tParagraph);
            nifParagraph.addRDFType(tOffsetBasedString);
            nifParagraph.addProperty(pReferenceContext, context);
            nifParagraph.addLiteral(pAnchorOf, uimaParagraph.getCoveredText());
            nifParagraph.addLiteral(pBeginIndex,
                    m.createTypedLiteral(uimaParagraph.getBegin(), XSDnonNegativeInteger));
            nifParagraph.addLiteral(pEndIndex,
                    m.createTypedLiteral(uimaParagraph.getEnd(), XSDnonNegativeInteger));
        }

        // Convert sentences
        Individual previousNifSentence = null;
        for (Sentence uimaSentence : select(aJCas, Sentence.class)) {
            String sentenceUri = String.format("%s#offset_%d_%d", docuri, uimaSentence.getBegin(),
                    uimaSentence.getEnd());
            Individual nifSentence = m.createIndividual(sentenceUri, tSentence);
            nifSentence.addRDFType(tOffsetBasedString);
            nifSentence.addProperty(pReferenceContext, context);
            nifSentence.addLiteral(pAnchorOf, uimaSentence.getCoveredText());
            nifSentence.addLiteral(pBeginIndex,
                    m.createTypedLiteral(uimaSentence.getBegin(), XSDnonNegativeInteger));
            nifSentence.addLiteral(pEndIndex,
                    m.createTypedLiteral(uimaSentence.getEnd(), XSDnonNegativeInteger));

            // Link word sequence
            if (previousNifSentence != null) {
                previousNifSentence.addProperty(pNextSentence, nifSentence);
                nifSentence.addProperty(pPreviousSentence, previousNifSentence);
            }
            previousNifSentence = nifSentence;

            // Convert tokens
            Individual previousNifWord = null;
            for (Token uimaToken : selectCovered(Token.class, uimaSentence)) {
                String wordUri = String.format("%s#offset_%d_%d", docuri, uimaToken.getBegin(),
                        uimaToken.getEnd());
                Individual nifWord = m.createIndividual(wordUri, tWord);
                nifWord.addRDFType(tOffsetBasedString);
                nifWord.addProperty(pReferenceContext, context);
                nifWord.addLiteral(pAnchorOf, uimaToken.getText());
                nifWord.addLiteral(pBeginIndex,
                        m.createTypedLiteral(uimaToken.getBegin(), XSDnonNegativeInteger));
                nifWord.addLiteral(pEndIndex,
                        m.createTypedLiteral(uimaToken.getEnd(), XSDnonNegativeInteger));

                // Link sentence <-> word
                nifWord.addProperty(pSentence, nifSentence);
                nifSentence.addProperty(pWord, nifWord);

                // Link word sequence
                if (previousNifWord != null) {
                    previousNifWord.addProperty(pNextWord, nifWord);
                    nifWord.addProperty(pPreviousWord, previousNifWord);
                }
                previousNifWord = nifWord;

                // Convert stem
                if (uimaToken.getStemValue() != null) {
                    nifWord.addProperty(pStem, uimaToken.getStemValue());
                }

                // Convert lemma
                if (uimaToken.getLemmaValue() != null) {
                    nifWord.addProperty(pLemma, uimaToken.getLemmaValue());
                }

                // Convert posTag (this is discouraged, the better alternative should be oliaLink)
                if (uimaToken.getPosValue() != null) {
                    nifWord.addProperty(pPosTag, uimaToken.getPosValue());
                }
            }
        }

        // Convert named entities
        //
        // Actually, the named entity in NIF is different from the one in DKPro Core. NIF uses
        // taIdentRef to link to a unique instance of an entity. Named entity recognizers in DKPro
        // Core just categorizes the entity, e.g. as a person, location, or whatnot. For what NIF
        // uses, we'd need a named entity linker, not just a recognizer.
        //
        // We create NEs using the NIF 2.1 class "EntityOccurence".
        //
        // So here, we check if the DKPro Core NE value/identifier looks like a URI and if yes, then
        // we store it into the NIF taIdentRef property - otherwise we ignore it because NIF does
        // not have the concept of a NE category.
        for (NamedEntity uimaNamedEntity : select(aJCas, NamedEntity.class)) {
            String neClass = uimaNamedEntity.getValue();
            String neIdentifier = uimaNamedEntity.getIdentifier();

            boolean neClassIsUri = neClass != null && IRIs.check(neClass);
            boolean neIdentifierIsUri = neIdentifier != null && IRIs.check(neIdentifier);

            if (!neClassIsUri && !neIdentifierIsUri) {
                continue;
            }

            String neUri = String.format("%s#offset_%d_%d", docuri, uimaNamedEntity.getBegin(),
                    uimaNamedEntity.getEnd());
            Individual nifNamedEntity = m.createIndividual(neUri, tEntityOccurrence);
            nifNamedEntity.addRDFType(tOffsetBasedString);
            nifNamedEntity.addProperty(pReferenceContext, context);
            nifNamedEntity.addLiteral(pAnchorOf, uimaNamedEntity.getCoveredText());
            nifNamedEntity.addLiteral(pBeginIndex,
                    m.createTypedLiteral(uimaNamedEntity.getBegin(), XSDnonNegativeInteger));
            nifNamedEntity.addLiteral(pEndIndex,
                    m.createTypedLiteral(uimaNamedEntity.getEnd(), XSDnonNegativeInteger));

            if (neClassIsUri) {
                nifNamedEntity.addProperty(pTaClassRef, m.createResource(neClass));
            }

            if (neIdentifierIsUri) {
                nifNamedEntity.addProperty(pTaIdentRef, m.createResource(neIdentifier));
            }
        }
    }
}
