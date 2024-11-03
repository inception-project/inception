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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

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

    public SpanDiffAdapter(String aType, String... aFeatures)
    {
        this(aType, new HashSet<>(asList(aFeatures)));
    }

    public SpanDiffAdapter(String aType, Set<String> aFeatures)
    {
        super(aType, aFeatures);
    }

    @Override
    public List<Annotation> selectAnnotationsInWindow(CAS aCas, int aWindowBegin, int aWindowEnd)
    {
        return aCas.select(getType()) //
                .coveredBy(0, aWindowEnd) //
                .includeAnnotationsWithEndBeyondBounds() //
                .map(fs -> (Annotation) fs) //
                .filter(ann -> ann.overlapping(aWindowBegin, aWindowEnd)) //
                .collect(toList());
    }

    @Override
    public Position getPosition(AnnotationBase aFS)
    {
        return SpanPosition.builder() //
                .forAnnotation((Annotation) aFS) //
                .build();
    }

    @Override
    public List<? extends Position> generateSubPositions(AnnotationBase aFs)
    {
        var subPositions = new ArrayList<Position>();

        for (var decl : getLinkFeaturesDecls()) {
            var linkFeature = aFs.getType().getFeatureByBaseName(decl.getName());
            var array = FSUtil.getFeature(aFs, linkFeature, ArrayFS.class);

            if (array == null) {
                continue;
            }

            for (var linkFS : array.toArray()) {
                var role = linkFS.getStringValue(
                        linkFS.getType().getFeatureByBaseName(decl.getRoleFeature()));
                var target = (AnnotationFS) linkFS.getFeatureValue(
                        linkFS.getType().getFeatureByBaseName(decl.getTargetFeature()));
                var pos = SpanPosition.builder() //
                        .forAnnotation((Annotation) aFs) //
                        .withLinkFeature(decl.getName()) //
                        .withLinkFeatureMultiplicityMode(decl.getMultiplicityMode()) //
                        .withLinkRole(role) //
                        .withLinkTarget(target) //
                        .build();
                subPositions.add(pos);
            }
        }

        return subPositions;
    }
}
