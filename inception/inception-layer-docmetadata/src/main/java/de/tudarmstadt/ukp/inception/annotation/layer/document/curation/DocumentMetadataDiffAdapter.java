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
package de.tudarmstadt.ukp.inception.annotation.layer.document.curation;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.cas.AnnotationBase;

import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentPosition;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapter_ImplBase;
import de.tudarmstadt.ukp.inception.curation.api.Position;

public class DocumentMetadataDiffAdapter
    extends DiffAdapter_ImplBase
{
    public DocumentMetadataDiffAdapter(String aType, String... aLabelFeatures)
    {
        this(aType, new HashSet<>(asList(aLabelFeatures)));
    }

    public DocumentMetadataDiffAdapter(String aType, Set<String> aLabelFeatures)
    {
        super(aType, aLabelFeatures);
    }

    @Override
    public List<AnnotationBase> selectAnnotationsInWindow(CAS aCas, int aWindowBegin,
            int aWindowEnd)
    {
        return aCas.<AnnotationBase> select(getType()).asList();
    }

    @Override
    public Position getPosition(AnnotationBase aFS)
    {
        return DocumentPosition.builder() //
                .forAnnotation(aFS) //
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
                var pos = DocumentPosition.builder() //
                        .forAnnotation(aFs) //
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
