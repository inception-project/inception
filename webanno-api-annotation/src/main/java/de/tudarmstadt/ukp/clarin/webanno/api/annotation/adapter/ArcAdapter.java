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
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getFeature;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isSame;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isSameSentence;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.setFeature;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.ArcCrossedMultipleSentenceException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * A class that is used to create Brat Arc to CAS relations and vice-versa
 *
 *
 */
public class ArcAdapter
    implements TypeAdapter, AutomationTypeAdapter
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final long typeId;

    /**
     * The UIMA type name.
     */
    private final String annotationTypeName;

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

    /*
     * The UIMA type name used as for origin/target span annotations, e.g. {@link POS} for
     * {@link Dependency}
     */
    /*
     * private final String arcSpanType;
     */
    /**
     * The feature of an UIMA annotation containing the label to be used as origin/target spans for
     * arc annotations
     */
    private final String attachFeatureName;

    /**
     * as Governor and Dependent of Dependency annotation type are based on Token, we need the UIMA
     * type for token
     */
    private final String attachType;

    private boolean deletable;

    /**
     * Allow multiple annotations of the same layer (only when the type value is different)
     */
    private boolean allowStacking;

    private boolean crossMultipleSentence;

    private AnnotationLayer layer;

    private Map<String, AnnotationFeature> features;

    public ArcAdapter(AnnotationLayer aLayer, long aTypeId, String aTypeName,
            String aTargetFeatureName, String aSourceFeatureName, /* String aArcSpanType, */
            String aAttacheFeatureName, String aAttachType, Collection<AnnotationFeature> aFeatures)
    {
        layer = aLayer;
        typeId = aTypeId;
        annotationTypeName = aTypeName;
        sourceFeatureName = aSourceFeatureName;
        targetFeatureName = aTargetFeatureName;
        // arcSpanType = aArcSpanType;
        attachFeatureName = aAttacheFeatureName;
        attachType = aAttachType;

        features = new LinkedHashMap<>();
        for (AnnotationFeature f : aFeatures) {
            features.put(f.getName(), f);
        }
    }

    /**
     * Update the CAS with new/modification of arc annotations from brat
     *
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
     * @param aFeature
     *            the feature.
     * @param aLabelValue
     *            the value of the annotation for the arc
     * @return the ID.
     * @throws AnnotationException
     *             if the annotation could not be created/updated.
     */
    public AnnotationFS add(AnnotationFS aOriginFs, AnnotationFS aTargetFs, JCas aJCas,
            int aWindowBegin, int aWindowEnd, AnnotationFeature aFeature, Object aLabelValue)
        throws AnnotationException
    {
        if (crossMultipleSentence
                || isSameSentence(aJCas, aOriginFs.getBegin(), aTargetFs.getEnd())) {
            return interalAddToCas(aJCas, aWindowBegin, aWindowEnd, aOriginFs, aTargetFs,
                    aLabelValue, aFeature);
        }
        else {
            throw new ArcCrossedMultipleSentenceException(
                    "Arc Annotation shouldn't cross sentence boundary");
        }
    }

    /**
     * @param aWindowBegin
     *            begin offset of the first visible sentence
     * @param aWindowEnd
     *            end offset of the last visible sentence
     */
    private AnnotationFS interalAddToCas(JCas aJCas, int aWindowBegin, int aWindowEnd,
            AnnotationFS aOriginFs, AnnotationFS aTargetFs, Object aValue,
            AnnotationFeature aFeature)
        throws AnnotationException
    {
        Type type = getType(aJCas.getCas(), annotationTypeName);
        Feature dependentFeature = type.getFeatureByBaseName(targetFeatureName);
        Feature governorFeature = type.getFeatureByBaseName(sourceFeatureName);

        Type spanType = getType(aJCas.getCas(), attachType);

        AnnotationFS dependentFs = null;
        AnnotationFS governorFs = null;

        // Locate the governor and dependent annotations - looking at the annotations that are
        // presently visible on screen is sufficient - we don't have to scan the whole CAS.
        for (AnnotationFS fs : selectCovered(aJCas.getCas(), type, aWindowBegin, aWindowEnd)) {

            if (attachFeatureName != null) {
                Feature arcSpanFeature = spanType.getFeatureByBaseName(attachFeatureName);
                dependentFs = (AnnotationFS) fs.getFeatureValue(dependentFeature).getFeatureValue(
                        arcSpanFeature);
                governorFs = (AnnotationFS) fs.getFeatureValue(governorFeature).getFeatureValue(
                        arcSpanFeature);
            }
            else {
                dependentFs = (AnnotationFS) fs.getFeatureValue(dependentFeature);
                governorFs = (AnnotationFS) fs.getFeatureValue(governorFeature);
            }

            if (dependentFs == null || governorFs == null) {
                log.warn("Relation [" + layer.getName() + "] with id [" + getAddr(fs)
                        + "] has loose ends - ignoring during while checking for duplicates.");
                continue;
            }

            // If stacking is not allowed and we would be creating a duplicate arc, then instead
            // update the label of the existing arc
            if (!allowStacking && isDuplicate(governorFs, aOriginFs, dependentFs, aTargetFs)) {
                setFeature(fs, aFeature, aValue);
                return fs;
            }
        }

        // It is new ARC annotation, create it
        dependentFs = aTargetFs;
        governorFs = aOriginFs;

        // for POS annotation, since custom span layers do not have attach feature
        if (attachFeatureName != null) {
            dependentFs = selectCovered(aJCas.getCas(), spanType, dependentFs.getBegin(),
                    dependentFs.getEnd()).get(0);
            governorFs = selectCovered(aJCas.getCas(), spanType, governorFs.getBegin(),
                    governorFs.getEnd()).get(0);
        }

        if (dependentFs == null || governorFs == null) {
            throw new AnnotationException("Relation must have a source and a target!");
        }

        
        // if span A has (start,end)= (20, 26) and B has (start,end)= (30, 36)
        // arc drawn from A to B, dependency will have (start, end) = (20, 36)
        // arc drawn from B to A, still dependency will have (start, end) = (20, 36)
        AnnotationFS newAnnotation;
        if (dependentFs.getEnd() <= governorFs.getEnd()) {
            newAnnotation = aJCas.getCas().createAnnotation(type, dependentFs.getBegin(),
                    governorFs.getEnd());
        }
        else {
            newAnnotation = aJCas.getCas().createAnnotation(type, governorFs.getBegin(),
                    dependentFs.getEnd());
        }

        // If origin and target spans are multiple tokens, dependentFS.getBegin will be the
        // the begin position of the first token and dependentFS.getEnd will be the End
        // position of the last token.
        newAnnotation.setFeatureValue(dependentFeature, dependentFs);
        newAnnotation.setFeatureValue(governorFeature, governorFs);
        setFeature(newAnnotation, aFeature, aValue);
        aJCas.addFsToIndexes(newAnnotation);
        return newAnnotation;
    }

    @Override
    public void delete(JCas aJCas, VID aVid)
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
        return typeId;
    }

    @Override
    public Type getAnnotationType(CAS cas)
    {
        return CasUtil.getType(cas, annotationTypeName);
    }

    @Override
    public String getAnnotationTypeName()
    {
        return annotationTypeName;
    }

    @Override
    public boolean isDeletable()
    {
        return deletable;
    }

    @Override
    public String getAttachFeatureName()
    {
        return attachFeatureName;
    }

    @Override
    public List<String> getAnnotation(Sentence aSentence, AnnotationFeature aFeature)
    {
        return new ArrayList<>();
    }

    public void delete(JCas aJCas, AnnotationFeature aFeature, int aBegin, int aEnd,
            String aDepCoveredText, String aGovCoveredText, Object aValue)
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
                if (ObjectUtils.equals(getFeature(fs, aFeature), aValue)) {
                    delete(aJCas, new VID(getAddr(fs)));
                }
            }
        }
    }

    public boolean isCrossMultipleSentence()
    {
        return crossMultipleSentence;
    }

    public void setCrossMultipleSentence(boolean crossMultipleSentence)
    {
        this.crossMultipleSentence = crossMultipleSentence;
    }

    public boolean isAllowStacking()
    {
        return allowStacking;
    }

    public void setAllowStacking(boolean allowStacking)
    {
        this.allowStacking = allowStacking;
    }

    // FIXME this is the version that treats each tag as a separate type in brat - should be removed
    // public static String getBratTypeName(TypeAdapter aAdapter, AnnotationFS aFs,
    // List<AnnotationFeature> aFeatures)
    // {
    // String annotations = "";
    // for (AnnotationFeature feature : aFeatures) {
    // if (!(feature.isEnabled() || feature.isEnabled())) {
    // continue;
    // }
    // Feature labelFeature = aFs.getType().getFeatureByBaseName(feature.getName());
    // if (annotations.equals("")) {
    // annotations = aAdapter.getTypeId()
    // + "_"
    // + (aFs.getFeatureValueAsString(labelFeature) == null ? " " : aFs
    // .getFeatureValueAsString(labelFeature));
    // }
    // else {
    // annotations = annotations
    // + " | "
    // + (aFs.getFeatureValueAsString(labelFeature) == null ? " " : aFs
    // .getFeatureValueAsString(labelFeature));
    // }
    // }
    // return annotations;
    // }

    @Override
    public String getAttachTypeName()
    {
        return attachType;
    }

    @Override
    public AnnotationLayer getLayer()
    {
        return layer;
    }

    @Override
    public Collection<AnnotationFeature> listFeatures()
    {
        return features.values();
    }

    public String getSourceFeatureName()
    {
        return sourceFeatureName;
    }

    public String getTargetFeatureName()
    {
        return targetFeatureName;
    }

    @Override
    public void delete(JCas aJCas, AnnotationFeature feature, int aBegin, int aEnd, Object aValue)
    {
        // TODO Auto-generated method stub
        
    }
}
