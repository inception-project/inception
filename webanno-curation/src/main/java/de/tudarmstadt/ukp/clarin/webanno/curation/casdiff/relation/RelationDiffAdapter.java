/*
 * Copyright 2019
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
 */
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.relation;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.FEAT_REL_TARGET;
import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class RelationDiffAdapter extends DiffAdapter_ImplBase
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
    
    @Override
    public Position getPosition(int aCasId, FeatureStructure aFS, String aFeature, String aRole,
            int aLinkTargetBegin, int aLinkTargetEnd, LinkCompareBehavior aLinkCompareBehavior)
    {
        Type type = aFS.getType();
        AnnotationFS sourceFS = (AnnotationFS) aFS.getFeatureValue(type
                .getFeatureByBaseName(sourceFeature));
        AnnotationFS targetFS = (AnnotationFS) aFS.getFeatureValue(type
                .getFeatureByBaseName(targetFeature));
        
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
            linkTargetText = aFS.getCAS().getDocumentText()
                    .substring(aLinkTargetBegin, aLinkTargetEnd);
        }
        
        return new RelationPosition(collectionId, documentId, aCasId, getType(), 
                sourceFS != null ? sourceFS.getBegin() : -1,
                sourceFS != null ? sourceFS.getEnd() : -1,
                sourceFS != null ? sourceFS.getCoveredText() : null,
                targetFS != null ? targetFS.getBegin() : -1,
                targetFS != null ? targetFS.getEnd() : -1,
                targetFS != null ? targetFS.getCoveredText() : null,
                aFeature, aRole, aLinkTargetBegin, aLinkTargetEnd, linkTargetText,
                aLinkCompareBehavior);
    }
}
