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
package de.tudarmstadt.ukp.inception.annotation.layer.relation.curation;

import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationDiffAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationPosition;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapter_ImplBase;
import de.tudarmstadt.ukp.inception.curation.api.Position;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public class RelationDiffAdapterImpl
    extends DiffAdapter_ImplBase
    implements RelationDiffAdapter
{
    public static final RelationDiffAdapterImpl DEPENDENCY_DIFF_ADAPTER = new RelationDiffAdapterImpl(
            Dependency.class.getName(), FEAT_REL_TARGET, FEAT_REL_SOURCE, "DependencyType",
            "flavor");

    private String sourceFeature;
    private String targetFeature;

    public RelationDiffAdapterImpl(String aType, String aSourceFeature, String aTargetFeature,
            String... aFeatures)
    {
        this(aType, aSourceFeature, aTargetFeature, new HashSet<>(asList(aFeatures)));
    }

    public RelationDiffAdapterImpl(String aType, String aSourceFeature, String aTargetFeature,
            Set<String> aFeatures)
    {
        super(aType, aFeatures);
        sourceFeature = aSourceFeature;
        targetFeature = aTargetFeature;
    }

    @Override
    public String getSourceFeature()
    {
        return sourceFeature;
    }

    @Override
    public String getTargetFeature()
    {
        return targetFeature;
    }

    /**
     * See {@code RelationRenderer#selectAnnotationsInWindow}
     */
    @Override
    public List<Annotation> selectAnnotationsInWindow(CAS aCas, int aWindowBegin, int aWindowEnd)
    {
        var result = new ArrayList<Annotation>();
        for (var rel : aCas.<Annotation> select(getType())) {
            var sourceFs = getSourceFs(rel);
            var targetFs = getTargetFs(rel);

            if (sourceFs instanceof Annotation source && targetFs instanceof Annotation target) {
                var relBegin = min(source.getBegin(), target.getBegin());
                var relEnd = max(source.getEnd(), target.getEnd());

                if (overlapping(relBegin, relEnd, aWindowBegin, aWindowEnd)) {
                    result.add(rel);
                }
            }
        }

        return result;
    }

    @Override
    public Position getPosition(AnnotationBase aFS)
    {
        int aLinkTargetBegin = -1;
        int aLinkTargetEnd = -1;
        var type = aFS.getType();
        var sourceFS = (AnnotationFS) aFS.getFeatureValue(type.getFeatureByBaseName(sourceFeature));
        var targetFS = (AnnotationFS) aFS.getFeatureValue(type.getFeatureByBaseName(targetFeature));

        String collectionId = null;
        String documentId = null;
        try {
            var dmd = WebAnnoCasUtil.getDocumentMetadata(aFS.getCAS());
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

        return new RelationPosition(collectionId, documentId, getType(),
                sourceFS != null ? sourceFS.getBegin() : -1,
                sourceFS != null ? sourceFS.getEnd() : -1,
                sourceFS != null ? sourceFS.getCoveredText() : null,
                targetFS != null ? targetFS.getBegin() : -1,
                targetFS != null ? targetFS.getEnd() : -1,
                targetFS != null ? targetFS.getCoveredText() : null, null, null, aLinkTargetBegin,
                aLinkTargetEnd, linkTargetText, null);
    }

    @Override
    public Collection<? extends Position> generateSubPositions(AnnotationBase aFs)
    {
        // Relation layers do not support link features
        return emptyList();
    }

    private FeatureStructure getSourceFs(FeatureStructure fs)
    {
        // if (attachFeature != null) {
        // return fs.getFeatureValue(sourceFeature).getFeatureValue(attachFeature);
        // }

        return FSUtil.getFeature(fs, sourceFeature, FeatureStructure.class);
    }

    private FeatureStructure getTargetFs(FeatureStructure fs)
    {
        // if (attachFeature != null) {
        // return fs.getFeatureValue(targetFeature).getFeatureValue(attachFeature);
        // }

        return FSUtil.getFeature(fs, targetFeature, FeatureStructure.class);
    }
}
