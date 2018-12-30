/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.util.Collection;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * A class that is used to create Brat Arc to CAS relations and vice-versa
 */
public class ArcAdapter
    extends TypeAdapter_ImplBase
{
    /**
     * The feature of an UIMA annotation containing the label to be used as a governor for arc
     * annotations
     */
    private final String sourceFeatureName;

    /**
     * The feature of an UIMA annotation containing the label to be used as a dependent for arc
     * annotations
     */
    private final String targetFeatureName;

    /**
     * @deprecated
     * @param aAttacheFeatureName argument is ignored and value obtained from layer instead.
     * @param aAttachType argument is ignored and value obtained from layer instead.
     * @param aTypeId argument is ignored and value obtained from layer instead.
     * @param aLayer argument is ignored and value obtained from layer instead.
     */
    @Deprecated
    public ArcAdapter(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationLayer aLayer, long aTypeId,
            String aTypeName, String aTargetFeatureName, String aSourceFeatureName, 
            /* String aArcSpanType, */ String aAttacheFeatureName, String aAttachType, 
            Collection<AnnotationFeature> aFeatures)
    {
        this(aFeatureSupportRegistry, aEventPublisher, aLayer,
                aTargetFeatureName, aSourceFeatureName, aFeatures);
    }
    
    public ArcAdapter(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationLayer aLayer,
            String aTargetFeatureName, String aSourceFeatureName,
            Collection<AnnotationFeature> aFeatures)
    {
        super(aFeatureSupportRegistry, aEventPublisher, aLayer, aFeatures);
        
        sourceFeatureName = aSourceFeatureName;
        targetFeatureName = aTargetFeatureName;
    }

    /**
     * Update the CAS with new/modification of arc annotations from brat
     *
     * @param aDocument
     *            the document to which the CAS belongs
     * @param aUsername
     *            the user to which the CAS belongs
     * @param aOriginFs
     *            the origin FS.
     * @param aTargetFs
     *            the target FS.
     * @param aJCas
     *            the JCas.
     * @param aWindowBegin
     *            begin offset of the first visible sentence
     * @param aWindowEnd
     *            end offset of the last visible sentence
     * @return the ID.
     * @throws AnnotationException
     *             if the annotation could not be created/updated.
     */
    public AnnotationFS add(SourceDocument aDocument, String aUsername, AnnotationFS aOriginFs,
            AnnotationFS aTargetFs, JCas aJCas, int aWindowBegin, int aWindowEnd)
        throws AnnotationException
    {
        return handle(new CreateRelationAnnotationRequest(aDocument, aUsername, aJCas, aOriginFs,
                aTargetFs, aWindowBegin, aWindowEnd));
    }

    public AnnotationFS handle(CreateRelationAnnotationRequest aRequest)
        throws AnnotationException
    {
        CreateRelationAnnotationRequest request = aRequest;
        
        request = new RelationCrossSentenceBehavior().onCreate(this, request);
        
        request = new RelationStackingBehavior().onCreate(this, request);
        
        request = new RelationAttachmentBehavior().onCreate(this, request);
        
        return createRelationAnnotation(request.getJcas().getCas(), request.getOriginFs(),
                request.getTargetFs());
    }

    private AnnotationFS createRelationAnnotation(CAS cas, AnnotationFS originFS,
            AnnotationFS targetFS)
        throws AnnotationException
    {
        if (targetFS == null || originFS == null) {
            throw new AnnotationException("Relation must have a source and a target!");
        }

        // Set the relation offsets in DKPro Core style - the relation recieves the offsets from
        // the dependent
        // If origin and target spans are multiple tokens, dependentFS.getBegin will be the
        // the begin position of the first token and dependentFS.getEnd will be the End
        // position of the last token.
        final Type type = getType(cas, getLayer().getName());
        final Feature dependentFeature = type.getFeatureByBaseName(targetFeatureName);
        final Feature governorFeature = type.getFeatureByBaseName(sourceFeatureName);

        AnnotationFS newAnnotation = cas.createAnnotation(type, targetFS.getBegin(),
                targetFS.getEnd());
        newAnnotation.setFeatureValue(dependentFeature, targetFS);
        newAnnotation.setFeatureValue(governorFeature, originFS);
        cas.addFsToIndexes(newAnnotation);
        return newAnnotation;
    }

    @Override
    public void delete(SourceDocument aDocument, String aUsername, JCas aJCas, VID aVid)
    {
        FeatureStructure fs = selectByAddr(aJCas, FeatureStructure.class, aVid.getId());
        aJCas.removeFsFromIndexes(fs);
    }

    @Override
    public String getAnnotationTypeName()
    {
        return getLayer().getName();
    }

    public String getSourceFeatureName()
    {
        return sourceFeatureName;
    }

    public String getTargetFeatureName()
    {
        return targetFeatureName;
    }
}
