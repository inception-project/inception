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
package de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class TsvDocument
{
    private final Pattern PATTERN_UNIT_ID = Pattern
            .compile("^(?<SENT>\\d+)-(?<TOKEN>\\d+)(\\.(?<SUBTOKEN>\\d))?$");

    private final TsvFormatHeader format;
    private final TsvSchema schema;
    private final JCas jcas;
    private final Map<AnnotationFS, TsvUnit> fs2unitIndex = new HashMap<>();
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
     * Get the unit which defines the TSV ID for the given feature structure. This can be either a
     * token or it could be a sub-token if the feature structure is properly nested in a token of if
     * it triggered the creation of a prefix or suffix sub-token.
     * 
     * @param aFS
     *            an annotation.
     * 
     * @return the unit defining the TSV ID for the given feature structure
     */
    public TsvUnit findIdDefiningUnit(AnnotationFS aFS)
    {
        TsvUnit unit = fs2unitIndex.get(aFS);
        if (unit == null) {
            throw new NoSuchElementException("No ID-defining unit found for annotation: " + aFS);
        }
        return unit;
    }

    public void mapFS2Unit(AnnotationFS aFS, TsvUnit aUnit)
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
        assert oldEntry == null || aAnnotation.equals(oldEntry) : "Disambiguation ID [" + aId
                + "] is not unique";
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

    public AnnotationFS resolveReference(Type aType, String aId, int aDisambiguationId)
    {
        AnnotationFS annotation;
        // If there is a disambiguation ID then we can easily look up the annotation via the ID.
        // A disambiguation ID of 0 used when a relation refers to a non-ambiguous target and
        // it is handled in the second case.
        if (aDisambiguationId > 0) {
            annotation = getDisambiguatedAnnotation(aDisambiguationId);
            if (annotation == null) {
                throw new IllegalStateException("Unable to resolve reference to disambiguation ID ["
                        + aDisambiguationId + "]");
            }
        }
        // Otherwise, we'll have to go through the source unit.
        else {
            annotation = getUnit(aId).getUimaAnnotation(aType, 0);
            if (annotation == null) {
                throw new IllegalStateException(
                        "Unable to resolve reference to unambiguous annotation of type ["
                                + aType.getName() + "] in unit [" + aId + "]");
            }
        }

        return annotation;
    }

    public TsvFormatHeader getFormatHeader()
    {
        return format;
    }
}
