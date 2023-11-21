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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.span;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationPredicates;
import org.apache.uima.fit.util.FSUtil;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanRenderer;

public class SpanDiffAdapter
    extends DiffAdapter_ImplBase
{
    public static final SpanDiffAdapter TOKEN_DIFF_ADAPTER = new SpanDiffAdapter(
            Token.class.getName());

    public static final SpanDiffAdapter SENTENCE_DIFF_ADAPTER = new SpanDiffAdapter(
            Sentence.class.getName());

    public static final SpanDiffAdapter POS_DIFF_ADAPTER = new SpanDiffAdapter(POS.class.getName(),
            "PosValue", "coarseValue");

    public static final SpanDiffAdapter NER_DIFF_ADAPTER = new SpanDiffAdapter(
            NamedEntity.class.getName(), "value", "identifier");

    public SpanDiffAdapter(String aType, String... aLabelFeatures)
    {
        this(aType, new HashSet<>(asList(aLabelFeatures)));
    }

    public SpanDiffAdapter(String aType, Set<String> aLabelFeatures)
    {
        super(aType, aLabelFeatures);
    }

    /**
     * @see SpanRenderer#selectAnnotationsInWindow(CAS, int, int)
     */
    @Override
    public List<AnnotationFS> selectAnnotationsInWindow(CAS aCas, int aWindowBegin, int aWindowEnd)
    {
        return aCas.select(getType()).coveredBy(0, aWindowEnd)
                .includeAnnotationsWithEndBeyondBounds().map(fs -> (AnnotationFS) fs)
                .filter(ann -> AnnotationPredicates.overlapping(ann, aWindowBegin, aWindowEnd))
                .collect(toList());
    }

    @Override
    public Position getPosition(int aCasId, FeatureStructure aFS, String aFeature, String aRole,
            int aLinkTargetBegin, int aLinkTargetEnd, LinkCompareBehavior aLinkCompareBehavior)
    {
        AnnotationFS annoFS = (AnnotationFS) aFS;

        String collectionId = null;
        String documentId = null;
        try {
            FeatureStructure dmd = WebAnnoCasUtil.getDocumentMetadata(aFS.getCAS());
            collectionId = FSUtil.getFeature(dmd, "collectionId", String.class);
            documentId = FSUtil.getFeature(dmd, "documentId", String.class);
        }
        catch (IllegalArgumentException e) {
            // We use this information only for debugging - so we can ignore if the information
            // is missing.
        }

        String linkTargetText = null;
        if (aLinkTargetBegin != -1 && aFS.getCAS().getDocumentText() != null) {
            linkTargetText = aFS.getCAS().getDocumentText().substring(aLinkTargetBegin,
                    aLinkTargetEnd);
        }

        return new SpanPosition(collectionId, documentId, aCasId, getType(), annoFS.getBegin(),
                annoFS.getEnd(), annoFS.getCoveredText(), aFeature, aRole, aLinkTargetBegin,
                aLinkTargetEnd, linkTargetText, aLinkCompareBehavior);
    }
}
