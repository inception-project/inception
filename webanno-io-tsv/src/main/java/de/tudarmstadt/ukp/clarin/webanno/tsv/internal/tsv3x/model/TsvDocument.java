/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
 */package de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model;

import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.PLACEHOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.RELATION_REF;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.SLOT_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.CHAIN;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.RELATION;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.SPAN;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.CHAIN_FIRST_FEAT;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.CHAIN_NEXT_FEAT;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_SLOT_TARGET;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class TsvDocument
{
    private final Pattern PATTERN_UNIT_ID = Pattern.compile(
            "^(?<SENT>\\d+)-(?<TOKEN>\\d+)(\\.(?<SUBTOKEN>\\d))?$");
    
    private final TsvFormatHeader format;
    private final TsvSchema schema;
    private final JCas jcas;
    private final Map<FeatureStructure, TsvUnit> fs2unitIndex = new HashMap<>();
    private final List<TsvSentence> sentences = new ArrayList<>();
    private final List<TsvChain> chains = new ArrayList<>();
    private final Map<AnnotationFS, TsvChain> fs2ChainIndex = new HashMap<>();
    private final Set<TsvColumn> activeColumns = new HashSet<>();
    private final Set<Type> activeTypes = new HashSet<>();
    private final Map<AnnotationFS, Integer> fs2IdIndex = new HashMap<>();
    private final Map<Integer, AnnotationFS> id2fsIndex = new HashMap<>();
    
    public TsvDocument(TsvFormatHeader aFormat, TsvSchema aSchema, JCas aJCas)
    {
        format = aFormat;
        schema = aSchema;
        jcas = aJCas;
    }
    
    public void activateColumn(TsvColumn aColumn)
    {
        activeColumns.add(aColumn);
    }

    public Set<TsvColumn> getActiveColumns()
    {
        return activeColumns;
    }
    
    public void activateType(Type aType)
    {
        activeTypes.add(aType);
    }

    public Set<Type> getActiveTypes()
    {
        return activeTypes;
    }
    
    /**
     * Get the unit which defines the TSV ID for the given feature structure. This can be either
     * a token or it could be a subtoken if the feature structure is properly nested in a token
     * of if it triggered the creation of a prefix or suffix subtoken.
     */
    public TsvUnit findIdDefiningUnit(FeatureStructure aFS)
    {
        return fs2unitIndex.get(aFS);
    }
    
    public void mapFS2Unit(FeatureStructure aFS, TsvUnit aUnit)
    {
        fs2unitIndex.put(aFS, aUnit);
    }
    
    public TsvChain createChain(Type aHeadType, Type aElementType, List<AnnotationFS> aElements)
    {
        TsvChain chain = new TsvChain(chains.size() + 1, aHeadType, aElementType, aElements,
                fs2ChainIndex);
        chains.add(chain);
        return chain;
    }

    public TsvChain createChain(int aChainId, Type aHeadType, Type aElementType)
    {
        TsvChain chain = new TsvChain(aChainId, aHeadType, aElementType, fs2ChainIndex);
        chains.add(chain);
        return chain;
    }
    
    public AnnotationFS getChainElement(int aChainId, int aElementIndex)
    {
        TsvChain chain = getChain(aChainId);
        if (chain != null) {
            return chain.getElement(aElementIndex);
        }
        else {
            return null;
        }
    }
    
    public TsvSentence createSentence(Sentence aUimaSentence)
    {
        TsvSentence sentence = new TsvSentence(this, aUimaSentence, sentences.size() + 1);
        sentences.add(sentence);
        return sentence;
    }
    
    public TsvToken createToken(TsvSentence aSentence, Token aUimaToken, int aPosition)
    {
        TsvToken token = new TsvToken(this, aSentence, aUimaToken, aPosition);
        mapFS2Unit(token.getUimaToken(), token);
        return token;
    }
    
    public TsvUnit getUnit(String aUnitId)
    {
        Matcher m = PATTERN_UNIT_ID.matcher(aUnitId);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid unit ID: [" + aUnitId + "]");
        }
        
        TsvToken token = getToken(Integer.valueOf(m.group("SENT")) - 1,
                Integer.valueOf(m.group("TOKEN")) - 1);
        
        String stid = m.group("SUBTOKEN");
        if (stid != null) {
            return token.getSubTokens().get(Integer.valueOf(stid) - 1);
        }
        else {
            return token;
        }
    }

    public TsvToken getToken(int aSentencePosition, int aTokenPosition)
    {
        return sentences.get(aSentencePosition).getTokens().get(aTokenPosition);
    }
    
    public List<TsvChain> getChains()
    {
        return chains;
    }

    public TsvChain getChain(int aChainId)
    {
        // This could be optimized if the chains were stored in a map instead of a list.
        return chains.stream().filter(c -> c.getId() == aChainId).findAny().orElse(null);
    }

    public TsvChain getChain(AnnotationFS aTargetFS)
    {
        return fs2ChainIndex.get(aTargetFS);
    }
    
    public List<TsvSentence> getSentences()
    {
        return sentences;
    }
    
    public TsvSchema getSchema()
    {
        return schema;
    }
    
    public JCas getJCas()
    {
        return jcas;
    }
        
    public void addDisambiguationId(AnnotationFS aAnnotation)
    {
        int newId = fs2IdIndex.size() + 1;
        boolean keyExisted = fs2IdIndex.putIfAbsent(aAnnotation, newId) != null;
        if (!keyExisted) {
            id2fsIndex.put(newId, aAnnotation);
        }
    }

    public void addDisambiguationId(AnnotationFS aAnnotation, int aId)
    {
        AnnotationFS oldEntry = id2fsIndex.put(aId, aAnnotation);
        assert oldEntry == null || aAnnotation.equals(oldEntry);
        fs2IdIndex.put(aAnnotation, aId);
    }

    public Integer getDisambiguationId(AnnotationFS aAnnotation)
    {
        return fs2IdIndex.get(aAnnotation);
    }
    
    public AnnotationFS getDisambiguatedAnnotation(int aDisambiguationId)
    {
        return id2fsIndex.get(aDisambiguationId);
    }
    
    public Set<AnnotationFS> getDisambiguatedAnnotations()
    {
        return fs2IdIndex.keySet();
    }
    
    public AnnotationFS resolveReference(Type aType, String aId,
            int aDisambiguationId)
    {
        AnnotationFS annotation;
        // If there is a disambiguation ID then we can easily look up the annotation via the ID.
        // A disambiguation ID of 0 used when a relation refers to a non-ambiguous target and
        // it is handled in the second case.
        if (aDisambiguationId > 0) {
            annotation = getDisambiguatedAnnotation(aDisambiguationId);
        }
        // Otherwise, we'll have to go through the source unit.
        else {
            annotation = getUnit(aId).getUimaAnnotation(aType, 0);
        }
        return annotation;
    }

    public TsvFormatHeader getFormatHeader()
    {
        return format;
    }

    public static TsvDocument of(TsvSchema aSchema, JCas aJCas)
    {
        TsvFormatHeader format = new TsvFormatHeader("WebAnno TSV", "3.2");
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
            for (FeatureStructure chainHead : CasUtil.selectFS(aJCas.getCas(), headType)) {
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
            LayerType layerType = aSchema.getLayerType(type);
            
            boolean addDisambiguationIdIfStacked = SPAN.equals(layerType);
            
            for (AnnotationFS annotation : CasUtil.select(aJCas.getCas(), type)) {
                doc.activateType(annotation.getType());
                
                TsvToken beginToken = tokenBeginIndex.floorEntry(annotation.getBegin()).getValue();
                TsvToken endToken = tokenEndIndex.ceilingEntry(annotation.getEnd()).getValue();
                boolean singleToken = beginToken == endToken;
                boolean zeroWitdh = annotation.getBegin() == annotation.getEnd();
                boolean multiTokenCapable = SPAN.equals(layerType) || CHAIN.equals(layerType);
                
                // Annotation exactly matches token boundaries - it doesn't really matter if the
                // begin and end tokens are the same; we don't have to create sub-token units
                // in either case.
                if (beginToken.getBegin() == annotation.getBegin()
                        && endToken.getEnd() == annotation.getEnd()) {
                    doc.mapFS2Unit(annotation, beginToken);
                    beginToken.addUimaAnnotation(annotation, addDisambiguationIdIfStacked);
                    
                    if (multiTokenCapable) {
                        endToken.addUimaAnnotation(annotation, addDisambiguationIdIfStacked);
                    }
                }
                else if (zeroWitdh) {
                    TsvSubToken t = beginToken.createSubToken(annotation.getBegin(),
                            min(beginToken.getEnd(), annotation.getEnd()));
                    doc.mapFS2Unit(annotation, t);
                    t.addUimaAnnotation(annotation, addDisambiguationIdIfStacked);
                } else {
                    // Annotation covers only suffix of the begin token - we need to create a 
                    // suffix sub-token unit on the begin token. The new sub-token defines the ID of
                    // the annotation.
                    if (beginToken.getBegin() < annotation.getBegin()) {
                        TsvSubToken t = beginToken.createSubToken(annotation.getBegin(),
                                min(beginToken.getEnd(), annotation.getEnd()));
                        doc.mapFS2Unit(annotation, t);
                        t.addUimaAnnotation(annotation, addDisambiguationIdIfStacked);
                    }
                    // If not the sub-token is ID-defining, then the begin token is ID-defining
                    else {
                        beginToken.addUimaAnnotation(annotation, addDisambiguationIdIfStacked);
                        doc.mapFS2Unit(annotation, beginToken);
                    }
                    
                    // Annotation covers only a prefix of the end token - we need to create a 
                    // prefix sub-token unit on the end token. If the current annotation is limited
                    // only to the sub-token unit, then it defines the ID. This is determined by
                    // checking if if singleToke is true.
                    if (endToken.getEnd() > annotation.getEnd()) {
                        TsvSubToken t = endToken.createSubToken(
                                max(endToken.getBegin(), annotation.getBegin()),
                                annotation.getEnd());
                        t.addUimaAnnotation(annotation, addDisambiguationIdIfStacked);
                        
                        if (!singleToken) {
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
                for (TsvSubToken subToken : token.getSubTokens()) {
                    scanUnitForActiveColumns(subToken);
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
                        col.setTargetTypeHint(target.getType());
                    }
                }
                
                // COMPATIBILITY NOTE:
                // WebAnnoTsv3Writer obtains the type of a slot target column not from the
                // type system definition but rather by looking at target used by the first 
                // actual annotation.
                if (SLOT_TARGET.equals(col.featureType)) {
                    AnnotationFS annotation = annotationsForColumn.get(0);
                    FeatureStructure[] links = getFeature(annotation, col.uimaFeature,
                            FeatureStructure[].class);
                    
                    if (links == null) {
                        throw new IllegalStateException(
                                "Span does not have its slot links feature (" + col.uimaFeature
                                        + ") set: " + annotation);
                    }
                    
                    if (links != null && links.length > 0) {
                        FeatureStructure target = getFeature(links[0],
                                FEAT_SLOT_TARGET, FeatureStructure.class);
                        col.setTargetTypeHint(target.getType());
                    }
                }
            }
        }
    }
}
