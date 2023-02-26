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
package de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x;

import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.PLACEHOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.RELATION_REF;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.SLOT_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.CHAIN;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.RELATION;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.SPAN;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.CHAIN_FIRST_FEAT;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.CHAIN_NEXT_FEAT;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_REL_TARGET;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvColumn;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvDocument;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvFormatHeader;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSentence;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSubToken;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvToken;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvUnit;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class Tsv3XCasDocumentBuilder
{
    private static final Logger LOG = LoggerFactory.getLogger(Tsv3XCasDocumentBuilder.class);

    public static TsvDocument of(TsvSchema aSchema, JCas aJCas)
    {
        TsvFormatHeader format = new TsvFormatHeader("WebAnno TSV", "3.3");
        TsvDocument doc = new TsvDocument(format, aSchema, aJCas);

        // Fill document with all the sentences and tokens
        for (Sentence uimaSentence : select(aJCas, Sentence.class)) {
            TsvSentence sentence = doc.createSentence(uimaSentence);
            for (Token uimaToken : selectCovered(Token.class, uimaSentence)) {
                sentence.createToken(uimaToken);
            }
        }

        // Scan for chains
        for (Type headType : aSchema.getChainHeadTypes()) {
            for (FeatureStructure chainHead : aJCas.select(headType)) {
                List<AnnotationFS> elements = new ArrayList<>();
                AnnotationFS link = getFeature(chainHead, CHAIN_FIRST_FEAT, AnnotationFS.class);
                while (link != null) {
                    elements.add(link);
                    link = getFeature(link, CHAIN_NEXT_FEAT, AnnotationFS.class);
                }
                if (!elements.isEmpty()) {
                    Type elementType = headType.getFeatureByBaseName(CHAIN_FIRST_FEAT).getRange();
                    doc.createChain(headType, elementType, elements);
                }
            }
        }

        // Build indexes over the token start and end positions such that we can quickly locate
        // tokens based on their offsets.
        NavigableMap<Integer, TsvToken> tokenBeginIndex = new TreeMap<>();
        NavigableMap<Integer, TsvToken> tokenEndIndex = new TreeMap<>();
        List<TsvToken> tokens = new ArrayList<>();
        for (TsvSentence sentence : doc.getSentences()) {
            for (TsvToken token : sentence.getTokens()) {
                tokenBeginIndex.put(token.getBegin(), token);
                tokenEndIndex.put(token.getEnd(), token);
                tokens.add(token);
            }
        }

        // Scan all annotations of the types defined in the schema and use them to set up sub-token
        // units.
        for (Type type : aSchema.getUimaTypes()) {
            var annotations = CasUtil.select(aJCas.getCas(), type);

            if (aSchema.getIgnoredTypes().contains(type)) {
                if (!annotations.isEmpty()) {
                    LOG.warn(
                            "The layer [{}] is incompatible with WebAnno TSV but contains {} "
                                    + "annotations - these will not be exported.",
                            type, annotations.size());
                }
                continue;
            }

            LayerType layerType = aSchema.getLayerType(type);

            boolean addDisambiguationIdIfStacked = SPAN.equals(layerType);

            for (AnnotationFS annotation : annotations) {
                // Mind that we might actually get an annotation here which is a subtype of `type`!
                doc.activateType(type);

                // Get the relevant begin and end offsets for the current annotation
                int begin = annotation.getBegin();
                int end = annotation.getEnd();

                // According to DKPro Core conventions, the offsets of relations must match
                // those of the target (i.e. the offsets of a Dependency relation must match
                // those of the dependent). Thus, we obtain the offsets from the target, just
                // to be sure.
                if (RELATION.equals(layerType)) {
                    AnnotationFS targetFS = getFeature(annotation, FEAT_REL_TARGET,
                            AnnotationFS.class);
                    begin = targetFS.getBegin();
                    end = targetFS.getEnd();
                }

                Entry<Integer, TsvToken> beginTokenEntry = tokenBeginIndex.floorEntry(begin);
                // If the current annotation has leading whitespace, we have wrongly fetched the
                // token before the start token using floorEntry(end) - so let's try to correct this
                if (
                // found begin token but found the wrong one
                (beginTokenEntry != null && beginTokenEntry.getValue().getEnd() < begin
                        && tokenEndIndex.higherEntry(begin) != null) ||
                // didn't find begin token because annotation starts before the first token
                        beginTokenEntry == null) {
                    beginTokenEntry = tokenEndIndex.higherEntry(begin);
                }
                if (beginTokenEntry == null) {
                    throw new IllegalStateException(
                            "Unable to find begin token starting at or before " + begin
                                    + " (first token starts at "
                                    + tokenBeginIndex.pollFirstEntry().getKey()
                                    + ") for annotation: " + annotation);
                }

                Entry<Integer, TsvToken> endTokenEntry = tokenEndIndex.ceilingEntry(end);
                // If the current annotation has trailing whitespace, we have wrongly fetched the
                // token after the end token using ceilingEntry(end) - so let's try to correct this
                if (
                // found end token but found the wrong one
                (endTokenEntry != null && endTokenEntry.getValue().getBegin() > end
                        && tokenEndIndex.lowerEntry(end) != null) ||
                // didn't find end token because annotation ends beyond the last token
                        endTokenEntry == null) {
                    endTokenEntry = tokenEndIndex.lowerEntry(end);
                }
                if (endTokenEntry == null) {
                    throw new IllegalStateException("Unable to find end token ending at or after "
                            + end + " (last token ends at " + tokenEndIndex.pollLastEntry().getKey()
                            + ") for annotation: " + annotation);
                }

                TsvToken beginToken = beginTokenEntry.getValue();
                TsvToken endToken = endTokenEntry.getValue();

                // For zero-width annotations, the begin token must match the end token.
                // Zero-width annotations between two directly adjacent tokens are always
                // considered to be at the end of the first token rather than at the beginning
                // of the second token, so we trust the tokenEndIndex here and override the
                // value obtained from the tokenBeginIndex.
                if (begin == end) {
                    beginToken = endToken;
                }

                boolean singleToken = beginToken == endToken;
                boolean zeroWitdh = begin == end;
                boolean multiTokenCapable = SPAN.equals(layerType) || CHAIN.equals(layerType);

                // Annotation exactly matches token boundaries - it doesn't really matter if the
                // begin and end tokens are the same; we don't have to create sub-token units
                // in either case.
                if (beginToken.getBegin() == begin && endToken.getEnd() == end) {
                    doc.mapFS2Unit(annotation, beginToken);
                    beginToken.addUimaAnnotation(annotation, addDisambiguationIdIfStacked);

                    if (multiTokenCapable) {
                        endToken.addUimaAnnotation(annotation, addDisambiguationIdIfStacked);
                    }
                }
                else if (zeroWitdh) {
                    // If the zero-width annotation happens in the space between tokens or after
                    // the last token, we move it to the end of the closest preceding token in order
                    // not to have to drop it entirely.
                    int position = min(beginToken.getEnd(), end);
                    // ... or if the annotation is before the first token, then we move it to the
                    // begin of the first token
                    if (position < beginToken.getBegin()) {
                        position = beginToken.getBegin();
                    }
                    TsvSubToken t = beginToken.createSubToken(position, position);
                    doc.mapFS2Unit(annotation, t);
                    t.addUimaAnnotation(annotation, addDisambiguationIdIfStacked);
                }
                else {
                    // Annotation covers only suffix of the begin token - we need to create a
                    // suffix sub-token unit on the begin token. The new sub-token defines the ID of
                    // the annotation.
                    if (beginToken.getBegin() < begin) {
                        TsvSubToken t = beginToken.createSubToken(begin,
                                min(beginToken.getEnd(), end));
                        doc.mapFS2Unit(annotation, t);
                        t.addUimaAnnotation(annotation, addDisambiguationIdIfStacked);
                    }

                    // If not the sub-token is ID-defining, then the begin token is ID-defining
                    if (begin <= beginToken.getBegin() && beginToken.getEnd() <= end) {
                        beginToken.addUimaAnnotation(annotation, addDisambiguationIdIfStacked);
                        doc.mapFS2Unit(annotation, beginToken);
                    }

                    // Annotation covers only a prefix of the end token - we need to create a
                    // prefix sub-token unit on the end token. If the current annotation is limited
                    // only to the sub-token unit, then it defines the ID. This is determined by
                    // checking if if singleToke is true.
                    if (endToken.getEnd() > end) {
                        TsvSubToken t = endToken.createSubToken(max(endToken.getBegin(), begin),
                                end);
                        t.addUimaAnnotation(annotation, addDisambiguationIdIfStacked);

                        if (singleToken) {
                            doc.mapFS2Unit(annotation, t);
                        }
                    }
                    else if (!singleToken && multiTokenCapable) {
                        endToken.addUimaAnnotation(annotation, addDisambiguationIdIfStacked);
                    }
                }

                // The annotation must also be added to all tokens between the begin token and
                // the end token
                if (multiTokenCapable && !singleToken) {
                    ListIterator<TsvToken> i = tokens.listIterator(tokens.indexOf(beginToken));
                    TsvToken t;
                    while ((t = i.next()) != endToken) {
                        if (t != beginToken) {
                            t.addUimaAnnotation(annotation, addDisambiguationIdIfStacked);
                        }
                    }
                }

                // Multi-token span annotations must get a disambiguation ID
                if (SPAN.equals(layerType) && !singleToken) {
                    doc.addDisambiguationId(annotation);
                }
            }
        }

        // Scan all created units to see which columns actually contains values
        for (TsvSentence sentence : doc.getSentences()) {
            for (TsvToken token : sentence.getTokens()) {
                scanUnitForActiveColumns(token);
                scanUnitForAmbiguousSlotReferences(token);
                for (TsvSubToken subToken : token.getSubTokens()) {
                    scanUnitForActiveColumns(subToken);
                    scanUnitForAmbiguousSlotReferences(subToken);
                }
            }
        }

        // Activate the placeholder columns for any active types for which no other columns are
        // active.
        Set<Type> activeTypesNeedingPlaceholders = new HashSet<>(doc.getActiveTypes());
        for (TsvColumn col : doc.getActiveColumns()) {
            activeTypesNeedingPlaceholders.remove(col.uimaType);
        }
        for (TsvColumn col : doc.getSchema().getColumns()) {
            if (PLACEHOLDER.equals(col.featureType)
                    && activeTypesNeedingPlaceholders.contains(col.uimaType)) {
                doc.activateColumn(col);
            }
        }

        return doc;
    }

    private static void scanUnitForActiveColumns(TsvUnit aUnit)
    {
        for (TsvColumn col : aUnit.getDocument().getSchema().getColumns()) {
            List<AnnotationFS> annotationsForColumn = aUnit.getAnnotationsForColumn(col);
            if (!annotationsForColumn.isEmpty()) {
                // if (SPAN.equals(col.layerType) && SLOT_TARGET.equals(col.featureType)) {
                // for (AnnotationFS aFS : annotationsForColumn) {
                // FeatureStructure[] links = getFeature(aFS, col.uimaFeature,
                // FeatureStructure[].class);
                // if (links != null && links.length > 0) {
                // }
                // }
                // }

                if (!PLACEHOLDER.equals(col.featureType)) {
                    aUnit.getDocument().activateColumn(col);
                }

                // COMPATIBILITY NOTE:
                // WebAnnoTsv3Writer obtains the type of a relation target column not from the
                // type system definition but rather by looking at target used by the first
                // actual annotation.
                if (RELATION.equals(col.layerType) && RELATION_REF.equals(col.featureType)) {
                    AnnotationFS annotation = annotationsForColumn.get(0);
                    FeatureStructure target = FSUtil.getFeature(annotation, FEAT_REL_SOURCE,
                            FeatureStructure.class);

                    if (target == null) {
                        throw new IllegalStateException(
                                "Relation does not have its source feature (" + FEAT_REL_SOURCE
                                        + ") set: " + annotation);
                    }

                    if (col.uimaType.getName().equals(Dependency.class.getName())) {
                        // COMPATIBILITY NOTE:
                        // WebAnnoTsv3Writer hard-changes the target type for DKPro Core
                        // Dependency annotations from Token to POS - the reason is not really
                        // clear. Probably because the Dependency relations in the WebAnno UI
                        // attach to POS (Token's are not visible as annotations in the UI).
                        col.setTargetTypeHint(aUnit.getDocument().getJCas().getTypeSystem()
                                .getType(POS.class.getName()));
                    }
                    else {
                        TsvSchema schema = aUnit.getDocument().getSchema();
                        col.setTargetTypeHint(schema.getEffectiveType(target));
                    }
                }
            }
        }
    }

    /**
     * If a slot feature has the target type Annotation, then any kind of annotation can be used as
     * slot filler. In this case, the targets are ambiguous and require an disambiguaton ID.
     */
    private static void scanUnitForAmbiguousSlotReferences(TsvUnit aUnit)
    {
        for (TsvColumn col : aUnit.getDocument().getSchema().getColumns()) {
            if (SPAN.equals(col.layerType) && SLOT_TARGET.equals(col.featureType)
                    && CAS.TYPE_NAME_ANNOTATION.equals(col.getTargetTypeHint().getName())) {
                List<AnnotationFS> annotationsForColumn = aUnit.getAnnotationsForColumn(col);
                for (AnnotationFS aFS : annotationsForColumn) {
                    FeatureStructure[] links = getFeature(aFS, col.uimaFeature,
                            FeatureStructure[].class);
                    if (links != null) {
                        for (FeatureStructure link : links) {
                            AnnotationFS targetFS = getFeature(link, TsvSchema.FEAT_SLOT_TARGET,
                                    AnnotationFS.class);
                            if (targetFS == null) {
                                throw new IllegalStateException("Slot link has no target: " + link);
                            }
                            aUnit.getDocument().addDisambiguationId(targetFS);
                        }
                    }
                }
            }
        }
    }
}
