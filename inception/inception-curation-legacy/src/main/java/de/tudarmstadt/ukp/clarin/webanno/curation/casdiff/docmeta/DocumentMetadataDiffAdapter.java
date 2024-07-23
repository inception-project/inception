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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.docmeta;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.cas.AnnotationBase;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanRenderer;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

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

    /**
     * @see SpanRenderer#selectAnnotationsInWindow
     */
    @Override
    public List<AnnotationBase> selectAnnotationsInWindow(CAS aCas, int aWindowBegin,
            int aWindowEnd)
    {
        return aCas.<AnnotationBase> select(getType()).asList();
    }

    @Override
    public Position getPosition(FeatureStructure aFS, String aFeature, String aRole,
            int aLinkTargetBegin, int aLinkTargetEnd, LinkFeatureMultiplicityMode aLinkCompareBehavior)
    {
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

        return new DocumentPosition(collectionId, documentId, getType(), aFeature, aRole,
                aLinkTargetBegin, aLinkTargetEnd, linkTargetText, aLinkCompareBehavior);
    }
}
