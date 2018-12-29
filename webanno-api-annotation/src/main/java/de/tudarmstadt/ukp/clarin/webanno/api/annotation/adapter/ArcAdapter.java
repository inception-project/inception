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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isSame;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isSameSentence;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.Collection;
import java.util.Optional;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.MultipleSentenceCoveredException;
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
    private final Logger log = LoggerFactory.getLogger(getClass());

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
        return add(new CreateRelationAnnotationRequest(aDocument, aUsername, aJCas, aOriginFs,
                aTargetFs, aWindowBegin, aWindowEnd));
    }

    public AnnotationFS add(CreateRelationAnnotationRequest aRequest)
        throws AnnotationException
    {
        CreateRelationAnnotationRequest request = aRequest;
        
        request = applyCrossSentenceBehavior(request);
        
        request = applyStackingBehavior(request);
        
        request = applyAttachFeatureBehavior(request);
        
        return createRelationAnnotation(request.getJcas().getCas(), request.getOriginFs(),
                request.getTargetFs());
    }

    private CreateRelationAnnotationRequest applyAttachFeatureBehavior(
            CreateRelationAnnotationRequest aRequest)
    {
        if (getLayer().getAttachFeature() == null) {
            return aRequest;
        }
        
        final CAS cas = aRequest.getJcas().getCas();
        final Type spanType = getType(cas, getLayer().getAttachType().getName());
        AnnotationFS originFS = aRequest.getOriginFs();
        AnnotationFS targetFS = aRequest.getTargetFs();
        targetFS = selectCovered(cas, spanType, targetFS.getBegin(), targetFS.getEnd()).get(0);
        originFS = selectCovered(cas, spanType, originFS.getBegin(), originFS.getEnd()).get(0);
        return aRequest.changeRelation(originFS, targetFS);
    }

    private CreateRelationAnnotationRequest applyStackingBehavior(
            CreateRelationAnnotationRequest aRequest)
        throws AnnotationException
    {
        final JCas jcas = aRequest.getJcas();
        final int windowBegin = aRequest.getWindowBegin();
        final int windowEnd = aRequest.getWindowEnd();
        final AnnotationFS originFS = aRequest.getOriginFs();
        final AnnotationFS targetFS = aRequest.getTargetFs();
        final Type type = getType(jcas.getCas(), getLayer().getName());
        final Feature dependentFeature = type.getFeatureByBaseName(targetFeatureName);
        final Feature governorFeature = type.getFeatureByBaseName(sourceFeatureName);
        
        // Locate the governor and dependent annotations - looking at the annotations that are
        // presently visible on screen is sufficient - we don't have to scan the whole CAS.
        for (AnnotationFS fs : selectCovered(jcas.getCas(), type, windowBegin, windowEnd)) {
            AnnotationFS existingTargetFS;
            AnnotationFS existingOriginFS;
            
            if (getLayer().getAttachFeature() != null) {
                final Type spanType = getType(jcas.getCas(), getLayer().getAttachType().getName());
                Feature arcSpanFeature = spanType
                        .getFeatureByBaseName(getLayer().getAttachFeature().getName());
                existingTargetFS = (AnnotationFS) fs.getFeatureValue(dependentFeature)
                        .getFeatureValue(arcSpanFeature);
                existingOriginFS = (AnnotationFS) fs.getFeatureValue(governorFeature)
                        .getFeatureValue(arcSpanFeature);
            }
            else {
                existingTargetFS = (AnnotationFS) fs.getFeatureValue(dependentFeature);
                existingOriginFS = (AnnotationFS) fs.getFeatureValue(governorFeature);
            }
        
            if (existingTargetFS == null || existingOriginFS == null) {
                log.warn("Relation [" + getLayer().getName() + "] with id [" + getAddr(fs)
                        + "] has loose ends - ignoring during while checking for duplicates.");
                continue;
            }

            // If stacking is not allowed and we would be creating a duplicate arc, then instead
            // update the label of the existing arc
            if (!getLayer().isAllowStacking()
                    && isDuplicate(existingOriginFS, originFS, existingTargetFS, targetFS)) {
                throw new AnnotationException("Cannot create another annotation of layer ["
                        + getLayer().getUiName() + "] at this location - stacking is not "
                        + "enabled for this layer.");
            }
        }
        
        return aRequest;
    }
    
    private CreateRelationAnnotationRequest applyCrossSentenceBehavior(
            CreateRelationAnnotationRequest aRequest)
        throws AnnotationException
    {
        if (!getLayer().isCrossSentence() && !isSameSentence(aRequest.getJcas(),
                aRequest.getOriginFs().getBegin(), aRequest.getTargetFs().getEnd())) {
            throw new MultipleSentenceCoveredException("Annotation coveres multiple sentences, "
                    + "limit your annotation to single sentence!");
        }

        return aRequest;
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

    private boolean isDuplicate(AnnotationFS aAnnotationFSOldOrigin,
            AnnotationFS aAnnotationFSNewOrigin, AnnotationFS aAnnotationFSOldTarget,
            AnnotationFS aAnnotationFSNewTarget)
    {
        return isSame(aAnnotationFSOldOrigin, aAnnotationFSNewOrigin)
                && isSame(aAnnotationFSOldTarget, aAnnotationFSNewTarget);
    }

    @Override
    public long getTypeId()
    {
        return getLayer().getId();
    }

    @Override
    public Type getAnnotationType(CAS cas)
    {
        return CasUtil.getType(cas, getLayer().getName());
    }

    @Override
    public String getAnnotationTypeName()
    {
        return getLayer().getName();
    }

    @Override
    public String getAttachFeatureName()
    {
        return Optional.ofNullable(getLayer().getAttachFeature())
                .map(AnnotationFeature::getName)
                .orElse(null);
    }

    public void delete(SourceDocument aDocument, String aUsername, JCas aJCas,
            AnnotationFeature aFeature, int aBegin, int aEnd, String aDepCoveredText,
            String aGovCoveredText, Object aValue)
    {
        Feature dependentFeature = getAnnotationType(aJCas.getCas())
                .getFeatureByBaseName(getTargetFeatureName());
        Feature governorFeature = getAnnotationType(aJCas.getCas())
                .getFeatureByBaseName(getSourceFeatureName());

        AnnotationFS dependentFs = null;
        AnnotationFS governorFs = null;
        
        Type type = CasUtil.getType(aJCas.getCas(), getAnnotationTypeName());
        Type spanType = getType(aJCas.getCas(), getAttachTypeName());
        Feature arcSpanFeature = spanType.getFeatureByBaseName(getAttachFeatureName());
        
        for (AnnotationFS fs : CasUtil.selectCovered(aJCas.getCas(), type, aBegin, aEnd)) {
            if (getAttachFeatureName() != null) {
                dependentFs = (AnnotationFS) fs.getFeatureValue(dependentFeature).getFeatureValue(
                        arcSpanFeature);
                governorFs = (AnnotationFS) fs.getFeatureValue(governorFeature).getFeatureValue(
                        arcSpanFeature);

            }
            else {
                dependentFs = (AnnotationFS) fs.getFeatureValue(dependentFeature);
                governorFs = (AnnotationFS) fs.getFeatureValue(governorFeature);
            }
            
            if (aDepCoveredText.equals(dependentFs.getCoveredText())
                    && aGovCoveredText.equals(governorFs.getCoveredText())) {
                if (ObjectUtils.equals(getFeatureValue(aFeature, fs), aValue)) {
                    delete(aDocument, aUsername, aJCas, new VID(getAddr(fs)));
                }
            }
        }
    }

    @Override
    public String getAttachTypeName()
    {
        return Optional.ofNullable(getLayer().getAttachType())
                .map(AnnotationLayer::getName)
                .orElse(null);
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
