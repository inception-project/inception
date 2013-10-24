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
import static org.uimafit.util.CasUtil.getType;
import static org.uimafit.util.CasUtil.selectCovered;

import java.util.List;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorUIData;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

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
    private String labelPrefix;

    /**
     * The UIMA type name.
     */
    private String annotationTypeName;

    /**
     * The feature of an UIMA annotation containing the label to be displayed in the UI.
     */
    private String labelFeatureName;
    /**
     * The feature of an UIMA annotation containing the label to be used as a governor for arc
     * annotations
     */
    private String governorFeatureName;
    /**
     * The feature of an UIMA annotation containing the label to be used as a dependent for arc
     * annotations
     */

    private String dependentFeatureName;

    /**
     * The UIMA type name used as for origin/target span annotations, e.g. {@link POS} for
     * {@link Dependency}
     */
    private String arcSpanType;

    /**
     * The feature of an UIMA annotation containing the label to be used as origin/target spans for
     * arc annotations
     */
    private String arcSpanTypeFeatureName;

    /**
     * as Governor and Dependent of Dependency annotation type are based on Token, we need the UIMA
     * type for token
     */
    private String arcTokenType;

    public ArcAdapter(String aLabelPrefix, String aTypeName, String aLabelFeatureName,
            String aDependentFeatureName, String aGovernorFeatureName, String aArcSpanType,
            String aArcSpanTypeFeatureName, String aTokenType)
    {
        labelPrefix = aLabelPrefix;
        labelFeatureName = aLabelFeatureName;
        annotationTypeName = aTypeName;
        governorFeatureName = aGovernorFeatureName;
        dependentFeatureName = aDependentFeatureName;
        arcSpanType = aArcSpanType;
        arcSpanTypeFeatureName = aArcSpanTypeFeatureName;
        arcTokenType = aTokenType;

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
     */
    @Override
    public void render(JCas aJcas, GetDocumentResponse aResponse,
            BratAnnotatorModel aBratAnnotatorModel)
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

        boolean reverse = aBratAnnotatorModel.getProject().isReverseDependencyDirection();

        Type type = getType(aJcas.getCas(), annotationTypeName);
        Feature dependentFeature = type.getFeatureByBaseName(dependentFeatureName);
        Feature governorFeature = type.getFeatureByBaseName(governorFeatureName);

        Type spanType = getType(aJcas.getCas(), arcSpanType);
        Feature arcSpanFeature = spanType.getFeatureByBaseName(arcSpanTypeFeatureName);
        // List all sentence in this display window
        List<Sentence> sentences = JCasUtil.selectCovered(aJcas, Sentence.class,
                firstSentence.getBegin(), lastSentenceInPage.getEnd());
        for (Sentence sentence : sentences) {
            for (AnnotationFS fs : selectCovered(aJcas.getCas(), type, sentence.getBegin(),
                    sentence.getEnd())) {

                FeatureStructure dependentFs = fs.getFeatureValue(dependentFeature)
                        .getFeatureValue(arcSpanFeature);
                FeatureStructure governorFs = fs.getFeatureValue(governorFeature).getFeatureValue(
                        arcSpanFeature);

                List<Argument> argumentList = getArgument(governorFs, dependentFs, reverse);

                Feature labelFeature = fs.getType().getFeatureByBaseName(labelFeatureName);

                aResponse.addRelation(new Relation(((FeatureStructureImpl) fs).getAddress(),
                        labelPrefix + fs.getStringValue(labelFeature), argumentList));
            }
        }
    }

    /**
     * Update the CAS with new/modification of arc annotations from brat
     *
     * @param aLabelValue
     *            the value of the annotation for the arc
     * @param aReverse
     *            If arc direction are in reverse direction, from Dependent to Governor
     */
    public void add(String aLabelValue, AnnotationFS aOriginFs, AnnotationFS aTargetFs, JCas aJCas,
            BratAnnotatorModel aBratAnnotatorModel, boolean aReverse)
    {
        AnnotationFS temp;
        // swap
        if (aReverse) {
            temp = aOriginFs;
            aOriginFs = aTargetFs;
            aTargetFs = temp;
        }

        Sentence sentence = BratAjaxCasUtil.selectSentenceAt(aJCas,
                aBratAnnotatorModel.getSentenceBeginOffset(),
                aBratAnnotatorModel.getSentenceEndOffset());

        int beginOffset = sentence.getBegin();
        int endOffset = BratAjaxCasUtil.selectByAddr(
                aJCas,
                Sentence.class,
                BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow(aJCas, sentence.getAddress(),
                        aBratAnnotatorModel.getWindowSize())).getEnd();

        updateCas(aJCas, beginOffset, endOffset, aOriginFs, aTargetFs, aLabelValue);
    }

    /**
     * A Helper method to {@link #addToCas(String, BratAnnotatorUIData)}
     */
    private void updateCas(JCas aJCas, int aBegin, int aEnd, AnnotationFS aOriginFs,
            AnnotationFS aTargetFs, String aValue)
    {
        boolean duplicate = false;

        Type type = getType(aJCas.getCas(), annotationTypeName);
        Feature feature = type.getFeatureByBaseName(labelFeatureName);
        Feature dependentFeature = type.getFeatureByBaseName(dependentFeatureName);
        Feature governorFeature = type.getFeatureByBaseName(governorFeatureName);

        Type spanType = getType(aJCas.getCas(), arcSpanType);
        Feature arcSpanFeature = spanType.getFeatureByBaseName(arcSpanTypeFeatureName);

        Type tokenType = getType(aJCas.getCas(), arcTokenType);
        // List all sentence in this display window
        List<Sentence> sentences = JCasUtil.selectCovered(aJCas, Sentence.class, aBegin, aBegin);
        for (Sentence sentence : sentences) {

            for (AnnotationFS fs : selectCovered(aJCas.getCas(), type, sentence.getBegin(),
                    sentence.getEnd())) {

                FeatureStructure dependentFs = fs.getFeatureValue(dependentFeature)
                        .getFeatureValue(arcSpanFeature);
                FeatureStructure governorFs = fs.getFeatureValue(governorFeature).getFeatureValue(
                        arcSpanFeature);
                if (isDuplicate((AnnotationFS) governorFs, aOriginFs, (AnnotationFS) dependentFs,
                        aTargetFs, fs.getStringValue(feature), aValue)
                        && !aValue.equals(AnnotationTypeConstant.ROOT)) {

                    // It is update of arc value, update it
                    if (!fs.getStringValue(feature).equals(aValue)) {
                        fs.setStringValue(feature, aValue);
                    }
                    duplicate = true;
                    break;
                }
            }
        }
        // It is new ARC annotation, create it
        if (!duplicate) {
            AnnotationFS dependentFS;
            AnnotationFS governorFS;
            if (aValue.equals("ROOT")) {
                governorFS = (AnnotationFS) BratAjaxCasUtil.selectByAddr(aJCas,
                        FeatureStructure.class, ((FeatureStructureImpl) aOriginFs).getAddress());
                dependentFS = governorFS;
            }
            else {
                dependentFS = (AnnotationFS) BratAjaxCasUtil.selectByAddr(aJCas,
                        FeatureStructure.class, ((FeatureStructureImpl) aTargetFs).getAddress());
                governorFS = (AnnotationFS) BratAjaxCasUtil.selectByAddr(aJCas,
                        FeatureStructure.class, ((FeatureStructureImpl) aOriginFs).getAddress());
            }
            // if span A has (start,end)= (20, 26) and B has (start,end)= (30, 36)
            // arc drawn from A to B, dependency will have (start, end) = (20, 36)
            // arc drawn from B to A, still dependency will have (start, end) = (20, 36)
            AnnotationFS newAnnotation;
            if (dependentFS.getEnd() <= governorFS.getEnd()) {
                newAnnotation = aJCas.getCas().createAnnotation(type, dependentFS.getBegin(),
                        governorFS.getEnd());
                newAnnotation.setStringValue(feature, aValue);
            }
            else {
                newAnnotation = aJCas.getCas().createAnnotation(type, governorFS.getBegin(),
                        dependentFS.getEnd());
                newAnnotation.setStringValue(feature, aValue);
            }
            // If origin and target spans are multiple tokens, dependentFS.getBegin will be the
            // the begin position of the first token and dependentFS.getEnd will be the End
            // position of the last token.
            newAnnotation.setFeatureValue(
                    dependentFeature,
                    selectCovered(aJCas.getCas(), tokenType, dependentFS.getBegin(),
                            dependentFS.getEnd()).get(0));
            newAnnotation.setFeatureValue(
                    governorFeature,
                    selectCovered(aJCas.getCas(), tokenType, governorFS.getBegin(),
                            governorFS.getEnd()).get(0));
            aJCas.addFsToIndexes(newAnnotation);
        }
    }

    /**
     * Delete arc annotation from CAS
     *
     * @param aJCas
     * @param aId
     */
    public void delete(JCas aJCas, int aAddress)
    {
        FeatureStructure fs = (FeatureStructure) BratAjaxCasUtil.selectByAddr(aJCas,
                FeatureStructure.class, aAddress);
        aJCas.removeFsFromIndexes(fs);
    }

    /**
     * Convenience method to get an adapter for Dependency Parsing.
     *
     * NOTE: This is not meant to stay. It's just a convenience during refactoring!
     */
    public static final ArcAdapter getDependencyAdapter()
    {
        ArcAdapter adapter = new ArcAdapter(AnnotationTypeConstant.POS_PREFIX,
                Dependency.class.getName(), AnnotationTypeConstant.DEPENDENCY_FEATURENAME,
                AnnotationTypeConstant.DEPENDENCY_DEPENDENT_FEATURENAME,
                AnnotationTypeConstant.DEPENDENCY_GOVERNOR_FEATURENAME, Token.class.getName(),
                AnnotationTypeConstant.ARC_POS_FEATURE_NAME, Token.class.getName());
        return adapter;
    }

    /**
     * Argument lists for the arc annotation
     *
     * @return
     */
    private List<Argument> getArgument(FeatureStructure aGovernorFs, FeatureStructure aDependentFs,
            boolean arevers)
    {
        if (arevers) {
            return asList(new Argument("Arg1", ((FeatureStructureImpl) aDependentFs).getAddress()),
                    new Argument("Arg2", ((FeatureStructureImpl) aGovernorFs).getAddress()));
        }
        else {
            return asList(new Argument("Arg1", ((FeatureStructureImpl) aGovernorFs).getAddress()),
                    new Argument("Arg2", ((FeatureStructureImpl) aDependentFs).getAddress()));
        }
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
    public String getLabelFeatureName()
    {
        return labelFeatureName;
    }

    @Override
    public String getLabelPrefix()
    {
        return labelPrefix;
    }
}
