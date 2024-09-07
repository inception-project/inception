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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.relation;

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationRenderer;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public class RelationDiffAdapter
    extends DiffAdapter_ImplBase
{
    public static final RelationDiffAdapter DEPENDENCY_DIFF_ADAPTER = new RelationDiffAdapter(
            Dependency.class.getName(), FEAT_REL_TARGET, FEAT_REL_SOURCE, "DependencyType",
            "flavor");

    private String sourceFeature;
    private String targetFeature;

    public RelationDiffAdapter(String aType, String aSourceFeature, String aTargetFeature,
            String... aLabelFeatures)
    {
        this(aType, aSourceFeature, aTargetFeature, new HashSet<>(asList(aLabelFeatures)));
    }

    public RelationDiffAdapter(String aType, String aSourceFeature, String aTargetFeature,
            Set<String> aLabelFeatures)
    {
        super(aType, aLabelFeatures);
        sourceFeature = aSourceFeature;
        targetFeature = aTargetFeature;
    }

    public String getSourceFeature()
    {
        return sourceFeature;
    }

    public String getTargetFeature()
    {
        return targetFeature;
    }

    /**
     * @see RelationRenderer#selectAnnotationsInWindow
     */
    @Override
    public List<Annotation> selectAnnotationsInWindow(CAS aCas, int aWindowBegin, int aWindowEnd)
    {
        // return selectCovered(aCas, CasUtil.getType(aCas, getType()), aWindowBegin, aWindowEnd);

        var result = new ArrayList<Annotation>();
        for (var rel : aCas.<Annotation> select(getType())) {
            var sourceFs = getSourceFs(rel);
            var targetFs = getTargetFs(rel);

            if (sourceFs instanceof Annotation source && targetFs instanceof Annotation target) {
                var relBegin = Math.min(source.getBegin(), target.getBegin());
                var relEnd = Math.max(source.getEnd(), target.getEnd());

                if (overlapping(relBegin, relEnd, aWindowBegin, aWindowEnd)) {
                    result.add(rel);
                }
            }
        }

        return result;

    }

    @Override
    public Position getPosition(FeatureStructure aFS, String aFeature, String aRole,
            int aLinkTargetBegin, int aLinkTargetEnd,
            LinkFeatureMultiplicityMode aLinkCompareBehavior)
    {
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
                targetFS != null ? targetFS.getCoveredText() : null, aFeature, aRole,
                aLinkTargetBegin, aLinkTargetEnd, linkTargetText, aLinkCompareBehavior);
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
