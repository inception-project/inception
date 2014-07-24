/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * A class that is used to create Brat Arc to CAS relations and vice-versa
 *
 * @author Seid Muhie Yimam
 *
 */
public class ArcAdapter
    implements TypeAdapter
{
    /**
     * Prefix of the label value for Brat to make sure that different annotation types can use the
     * same label, e.g. a POS tag "N" and a named entity type "N".
     *
     */
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

    /*    *//**
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
    private final String attacheFeatureName;

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

    public ArcAdapter(long aTypeId, String aTypeName, String aTargetFeatureName,
            String aSourceFeatureName, /* String aArcSpanType, */
            String aAttacheFeatureName, String aAttachType)
    {
        typeId = aTypeId;
        annotationTypeName = aTypeName;
        sourceFeatureName = aSourceFeatureName;
        targetFeatureName = aTargetFeatureName;
        // arcSpanType = aArcSpanType;
        attacheFeatureName = aAttacheFeatureName;
        attachType = aAttachType;

    }

    /**
     * Add arc annotations from the CAS, which is controlled by the window size, to the brat
     * response {@link GetDocumentResponse}
     *
     * @param aJcas
     *            The JCAS object containing annotations
     * @param aResponse
     *            A brat response containing annotations in brat protocol
     * @param aBratAnnotatorModel
     *            Data model for brat annotations
     * @param aColoringStrategy
     *            the coloring strategy to render this layer
     */
    @Override
    public void render(JCas aJcas, List<AnnotationFeature> aFeatures,
            GetDocumentResponse aResponse, BratAnnotatorModel aBratAnnotatorModel,
            ColoringStrategy aColoringStrategy)
    {
        // The first sentence address in the display window!
        Sentence firstSentence = BratAjaxCasUtil.selectSentenceAt(aJcas,
                aBratAnnotatorModel.getSentenceBeginOffset(),
                aBratAnnotatorModel.getSentenceEndOffset());

        int lastAddressInPage = BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow(aJcas,
                firstSentence.getAddress(), aBratAnnotatorModel.getWindowSize());

        // the last sentence address in the display window
        Sentence lastSentenceInPage = (Sentence) BratAjaxCasUtil.selectByAddr(aJcas,
                FeatureStructure.class, lastAddressInPage);

        Type type = getType(aJcas.getCas(), annotationTypeName);
        Feature dependentFeature = type.getFeatureByBaseName(targetFeatureName);
        Feature governorFeature = type.getFeatureByBaseName(sourceFeatureName);

        Type spanType = getType(aJcas.getCas(), attachType);
        Feature arcSpanFeature = spanType.getFeatureByBaseName(attacheFeatureName);

        FeatureStructure dependentFs;
        FeatureStructure governorFs;

        for (AnnotationFS fs : selectCovered(aJcas.getCas(), type, firstSentence.getBegin(),
                lastSentenceInPage.getEnd())) {
            if (attacheFeatureName != null) {
                dependentFs = fs.getFeatureValue(dependentFeature).getFeatureValue(arcSpanFeature);
                governorFs = fs.getFeatureValue(governorFeature).getFeatureValue(arcSpanFeature);
            }
            else {
                dependentFs = fs.getFeatureValue(dependentFeature);
                governorFs = fs.getFeatureValue(governorFeature);
            }

            List<Argument> argumentList = getArgument(governorFs, dependentFs);

            String bratLabelText = TypeUtil.getBratLabelText(this, fs, aFeatures);
            String bratTypeName = TypeUtil.getBratTypeName(this);
            String color = aColoringStrategy.getColor(fs, bratLabelText);

            aResponse.addRelation(new Relation(((FeatureStructureImpl) fs).getAddress(),
                    bratTypeName, argumentList, bratLabelText, color));
        }
    }

    /**
     * Update the CAS with new/modification of arc annotations from brat
     *
     * @param aLabelValue
     *            the value of the annotation for the arc
     * @param aReverse
     *            If arc direction are in reverse direction, from Dependent to Governor
     * @throws BratAnnotationException
     */
    public void add(String aLabelValue, AnnotationFS aOriginFs, AnnotationFS aTargetFs, JCas aJCas,
            BratAnnotatorModel aBratAnnotatorModel, AnnotationFeature aFeature)
        throws BratAnnotationException
    {
        Sentence sentence = BratAjaxCasUtil.selectSentenceAt(aJCas,
                aBratAnnotatorModel.getSentenceBeginOffset(),
                aBratAnnotatorModel.getSentenceEndOffset());

        int beginOffset = sentence.getBegin();
        int endOffset = BratAjaxCasUtil.selectByAddr(
                aJCas,
                Sentence.class,
                BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow(aJCas, sentence.getAddress(),
                        aBratAnnotatorModel.getWindowSize())).getEnd();
        if (crossMultipleSentence
                || BratAjaxCasUtil.isSameSentence(aJCas, aOriginFs.getBegin(), aTargetFs.getEnd())) {
            updateCas(aJCas, beginOffset, endOffset, aOriginFs, aTargetFs, aLabelValue, aFeature);
        }
        else {
            throw new ArcCrossedMultipleSentenceException(
                    "Arc Annotation shouldn't cross sentence boundary");
        }
    }

    /**
     * A Helper method to {@link #addToCas(String, BratAnnotatorUIData)}
     */
    private void updateCas(JCas aJCas, int aBegin, int aEnd, AnnotationFS aOriginFs,
            AnnotationFS aTargetFs, String aValue, AnnotationFeature aFeature)
    {
        boolean duplicate = false;

        Type type = getType(aJCas.getCas(), annotationTypeName);
        Feature feature = type.getFeatureByBaseName(aFeature.getName());
        Feature dependentFeature = type.getFeatureByBaseName(targetFeatureName);
        Feature governorFeature = type.getFeatureByBaseName(sourceFeatureName);

        Type spanType = getType(aJCas.getCas(), attachType);
        Feature arcSpanFeature = spanType.getFeatureByBaseName(attacheFeatureName);

        Type tokenType = getType(aJCas.getCas(), attachType);

        AnnotationFS dependentFs = null;
        AnnotationFS governorFs = null;
        // List all sentence in this display window
        List<Sentence> sentences = selectCovered(aJCas, Sentence.class, aBegin, aEnd);
        for (Sentence sentence : sentences) {

            for (AnnotationFS fs : selectCovered(aJCas.getCas(), type, sentence.getBegin(),
                    sentence.getEnd())) {

                if (attacheFeatureName != null) {
                    dependentFs = (AnnotationFS) fs.getFeatureValue(dependentFeature)
                            .getFeatureValue(arcSpanFeature);
                    governorFs = (AnnotationFS) fs.getFeatureValue(governorFeature)
                            .getFeatureValue(arcSpanFeature);
                }
                else {
                    dependentFs = (AnnotationFS) fs.getFeatureValue(dependentFeature);
                    governorFs = (AnnotationFS) fs.getFeatureValue(governorFeature);
                }

                if (isDuplicate((AnnotationFS) governorFs, aOriginFs, (AnnotationFS) dependentFs,
                        aTargetFs, fs.getFeatureValueAsString(feature), aValue)
                        && !aValue.equals(WebAnnoConst.ROOT)) {

                    if (fs.getFeatureValueAsString(feature) == null) {
                        fs.setFeatureValueFromString(feature, aValue);
                        duplicate = true;
                        break;
                    }
                    if (!allowStacking) {
                        fs.setFeatureValueFromString(feature, aValue);
                        duplicate = true;
                        break;
                    }
                }
            }
        }
        // It is new ARC annotation, create it
        if (!duplicate) {
            if (aValue.equals("ROOT")) {
                governorFs = aOriginFs;
                dependentFs = governorFs;
            }
            else {
                dependentFs = aTargetFs;
                governorFs = aOriginFs;
            }
            // for POS annotation, since custom span layers do not have attach feature
            if (attacheFeatureName != null) {
                dependentFs = selectCovered(aJCas.getCas(), tokenType, dependentFs.getBegin(),
                        dependentFs.getEnd()).get(0);
                governorFs = selectCovered(aJCas.getCas(), tokenType, governorFs.getBegin(),
                        governorFs.getEnd()).get(0);
            }

            // if span A has (start,end)= (20, 26) and B has (start,end)= (30, 36)
            // arc drawn from A to B, dependency will have (start, end) = (20, 36)
            // arc drawn from B to A, still dependency will have (start, end) = (20, 36)
            AnnotationFS newAnnotation;
            if (dependentFs.getEnd() <= governorFs.getEnd()) {
                newAnnotation = aJCas.getCas().createAnnotation(type, dependentFs.getBegin(),
                        governorFs.getEnd());
                newAnnotation.setFeatureValueFromString(feature, aValue);
            }
            else {
                newAnnotation = aJCas.getCas().createAnnotation(type, governorFs.getBegin(),
                        dependentFs.getEnd());
                newAnnotation.setFeatureValueFromString(feature, aValue);
            }
            // If origin and target spans are multiple tokens, dependentFS.getBegin will be the
            // the begin position of the first token and dependentFS.getEnd will be the End
            // position of the last token.
            newAnnotation.setFeatureValue(dependentFeature, dependentFs);
            newAnnotation.setFeatureValue(governorFeature, governorFs);
            aJCas.addFsToIndexes(newAnnotation);
        }
    }

    @Override
    public void delete(JCas aJCas, int aAddress)
    {
        FeatureStructure fs = BratAjaxCasUtil.selectByAddr(aJCas, FeatureStructure.class, aAddress);
        aJCas.removeFsFromIndexes(fs);
    }

    @Override
    public void deleteBySpan(JCas aJCas, AnnotationFS afs, int aBegin, int aEnd)
    {
        Type type = getType(aJCas.getCas(), annotationTypeName);
        Feature targetFeature = type.getFeatureByBaseName(targetFeatureName);
        Feature sourceFeature = type.getFeatureByBaseName(sourceFeatureName);

        Type spanType = getType(aJCas.getCas(), attachType);
        Feature arcSpanFeature = spanType.getFeatureByBaseName(attacheFeatureName);

        Set<AnnotationFS> fsToDelete = new HashSet<AnnotationFS>();

        for (AnnotationFS fs : selectCovered(aJCas.getCas(), type, aBegin, aEnd)) {

            if (attacheFeatureName != null) {
                FeatureStructure dependentFs = fs.getFeatureValue(targetFeature).getFeatureValue(
                        arcSpanFeature);
                if (((FeatureStructureImpl) afs).getAddress() == ((FeatureStructureImpl) dependentFs)
                        .getAddress()) {
                    fsToDelete.add(fs);
                }
                FeatureStructure governorFs = fs.getFeatureValue(sourceFeature).getFeatureValue(
                        arcSpanFeature);
                if (((FeatureStructureImpl) afs).getAddress() == ((FeatureStructureImpl) governorFs)
                        .getAddress()) {
                    fsToDelete.add(fs);
                }
            }
            else {
                FeatureStructure dependentFs = fs.getFeatureValue(targetFeature);
                if (((FeatureStructureImpl) afs).getAddress() == ((FeatureStructureImpl) dependentFs)
                        .getAddress()) {
                    fsToDelete.add(fs);
                }
                FeatureStructure governorFs = fs.getFeatureValue(sourceFeature);
                if (((FeatureStructureImpl) afs).getAddress() == ((FeatureStructureImpl) governorFs)
                        .getAddress()) {
                    fsToDelete.add(fs);
                }
            }
        }
        for (AnnotationFS fs : fsToDelete) {
            aJCas.removeFsFromIndexes(fs);
        }

    }

    /**
     * Argument lists for the arc annotation
     *
     * @return
     */
    private List<Argument> getArgument(FeatureStructure aGovernorFs, FeatureStructure aDependentFs)
    {
        return asList(new Argument("Arg1", ((FeatureStructureImpl) aGovernorFs).getAddress()),
                new Argument("Arg2", ((FeatureStructureImpl) aDependentFs).getAddress()));
    }

    private boolean isDuplicate(AnnotationFS aAnnotationFSOldOrigin,
            AnnotationFS aAnnotationFSNewOrigin, AnnotationFS aAnnotationFSOldTarget,
            AnnotationFS aAnnotationFSNewTarget, String aTypeOld, String aTypeNew)
    {
        if (aAnnotationFSOldOrigin.getBegin() == aAnnotationFSNewOrigin.getBegin()
                && aAnnotationFSOldOrigin.getEnd() == aAnnotationFSNewOrigin.getEnd()
                && aAnnotationFSOldTarget.getBegin() == aAnnotationFSNewTarget.getBegin()
                && aAnnotationFSOldTarget.getEnd() == aAnnotationFSNewTarget.getEnd()) {
            return true;
        }
        else {
            return false;
        }
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
        return attacheFeatureName;
    }

    @Override
    public List<String> getAnnotation(JCas aJcas, AnnotationFeature aFeature, int begin, int end)
    {
        return new ArrayList<String>();
    }

    @Override
    public void automate(JCas aJcas, AnnotationFeature aFeature, List<String> labelValues)
        throws BratAnnotationException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(JCas aJCas, AnnotationFeature aFeature, int aBegin, int aEnd, String aValue)
    {
        // TODO Auto-generated method stub

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
    public void updateFeature(JCas aJcas, AnnotationFeature aFeature, int aAddress, String aValue)
    {
        Type type = CasUtil.getType(aJcas.getCas(), annotationTypeName);
        Feature feature = type.getFeatureByBaseName(aFeature.getName());
        FeatureStructure fs = BratAjaxCasUtil.selectByAddr(aJcas, FeatureStructure.class, aAddress);
        fs.setFeatureValueFromString(feature, aValue);
    }
}
