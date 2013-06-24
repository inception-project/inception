/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorUIData;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
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
    private String typePrefix;

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

    public ArcAdapter(String aTypePrefix, String aTypeName, String aLabelFeatureName,
            String aDependentFeatureName, String aGovernorFeatureName, String aArcSpanType,
            String aArcSpanTypeFeatureName, String aTokenType)
    {
        typePrefix = aTypePrefix;
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
    public void addToBrat(JCas aJcas, GetDocumentResponse aResponse,
            BratAnnotatorModel aBratAnnotatorModel)
    {

        // Remove prefixes from response if it is in curation mode
        if (aBratAnnotatorModel.getMode().equals(Mode.CURATION)) {
            typePrefix = "";
        }

        boolean reverse = aBratAnnotatorModel.getProject().isReverseDependencyDirection();
        // The first sentence address in the display window!
        Sentence firstSentence = (Sentence) BratAjaxCasUtil.selectAnnotationByAddress(aJcas,
                FeatureStructure.class, aBratAnnotatorModel.getSentenceAddress());
        int i = aBratAnnotatorModel.getSentenceAddress();

        // Loop based on window size
        // j, controlling variable to display sentences based on window size
        // i, address of each sentences
        int j = 1;
        while (j <= aBratAnnotatorModel.getWindowSize()) {
            if (i >= aBratAnnotatorModel.getLastSentenceAddress()) {
                Sentence sentence = (Sentence) BratAjaxCasUtil.selectAnnotationByAddress(aJcas,
                        FeatureStructure.class, i);
                updateResponse(sentence, aResponse, firstSentence.getBegin(), reverse);
                break;
            }
            else {
                Sentence sentence = (Sentence) BratAjaxCasUtil.selectAnnotationByAddress(aJcas,
                        FeatureStructure.class, i);
                updateResponse(sentence, aResponse, firstSentence.getBegin(), reverse);
                i = BratAjaxCasUtil.getFollowingSentenceAddress(aJcas, i);
            }
            j++;
        }
    }

    /**
     * a helper method to the {@link #addToBrat(JCas, GetDocumentResponse, BratAnnotatorModel)}
     * 
     * @param aSentence
     *            The current sentence in the CAS annotation, with annotations
     * @param aResponse
     *            The {@link GetDocumentResponse} object with the annotation from CAS annotation
     * @param aFirstSentenceOffset
     *            The first sentence offset. The actual offset in the brat visualization window will
     *            be <b>X-Y</b>, where <b>X</b> is the offset of the annotated span and <b>Y</b> is
     *            aFirstSentenceOffset
     */
    private void updateResponse(Sentence aSentence, GetDocumentResponse aResponse,
            int aFirstSentenceOffset, boolean aReverse)
    {
        Type type = CasUtil.getType(aSentence.getCAS(), annotationTypeName);
        Feature dependentFeature = type.getFeatureByBaseName(dependentFeatureName);
        Feature governorFeature = type.getFeatureByBaseName(governorFeatureName);

        Type spanType = CasUtil.getType(aSentence.getCAS(), arcSpanType);
        Feature arcSpanFeature = spanType.getFeatureByBaseName(arcSpanTypeFeatureName);

        for (AnnotationFS fs : CasUtil.selectCovered(type, aSentence)) {

            FeatureStructure dependentFs = fs.getFeatureValue(dependentFeature).getFeatureValue(
                    arcSpanFeature);
            FeatureStructure governorFs = fs.getFeatureValue(governorFeature).getFeatureValue(
                    arcSpanFeature);

            List<Argument> argumentList = getArgument(governorFs, dependentFs, aReverse);

            Feature labelFeature = fs.getType().getFeatureByBaseName(labelFeatureName);

            aResponse.addRelation(new Relation(((FeatureStructureImpl) fs).getAddress(), typePrefix
                    + fs.getStringValue(labelFeature), argumentList));
        }
    }

    /**
     * Update the CAS with new/modification of arc annotations from brat
     * 
     * @param aLabelValue
     *            the value of the annotation for the arc
     * @param aUIData
     *            Other information obtained from brat such as the start and end offsets
     * @param aReverse
     *            If arc direction are in reverse direction, from Dependent to Governor
     */
    public void addToCas(String aLabelValue, BratAnnotatorUIData aUIData,
            BratAnnotatorModel aBratAnnotatorModel, boolean aReverse)
    {
        int originAddress;
        int targetAddress;

        if (aReverse) {
            originAddress = aUIData.getTarget();
            targetAddress = aUIData.getOrigin();
        }
        else {
            originAddress = aUIData.getOrigin();
            targetAddress = aUIData.getTarget();
        }

        int beginOffset = BratAjaxCasUtil.selectAnnotationByAddress(aUIData.getjCas(),
                Sentence.class, aBratAnnotatorModel.getSentenceAddress()).getBegin();
        int endOffset = BratAjaxCasUtil.selectAnnotationByAddress(
                aUIData.getjCas(),
                Sentence.class,
                BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow(aUIData.getjCas(),
                        aBratAnnotatorModel.getSentenceAddress(),
                        aBratAnnotatorModel.getWindowSize())).getEnd();

        aUIData.setAnnotationOffsetStart(beginOffset);
        aUIData.setAnnotationOffsetEnd(endOffset);
        updateCas(aUIData.getjCas(), aUIData.getAnnotationOffsetStart(),
                aUIData.getAnnotationOffsetEnd(), originAddress, targetAddress, aLabelValue);
    }

    /**
     * A Helper method to {@link #addToCas(String, BratAnnotatorUIData)}
     */
    private void updateCas(JCas aJCas, int aBegin, int aEnd, int aOriginAddress,
            int aTargetAddress, String aValue)
    {
        boolean duplicate = false;

        Type type = CasUtil.getType(aJCas.getCas(), annotationTypeName);
        Feature feature = type.getFeatureByBaseName(labelFeatureName);
        Feature dependentFeature = type.getFeatureByBaseName(dependentFeatureName);
        Feature governorFeature = type.getFeatureByBaseName(governorFeatureName);

        Type spanType = CasUtil.getType(aJCas.getCas(), arcSpanType);
        Feature arcSpanFeature = spanType.getFeatureByBaseName(arcSpanTypeFeatureName);

        Type tokenType = CasUtil.getType(aJCas.getCas(), arcTokenType);

        for (AnnotationFS fs : CasUtil.selectCovered(aJCas.getCas(), type, aBegin, aEnd)) {

            FeatureStructure dependentFs = fs.getFeatureValue(dependentFeature).getFeatureValue(
                    arcSpanFeature);
            FeatureStructure governorFs = fs.getFeatureValue(governorFeature).getFeatureValue(
                    arcSpanFeature);
            if (((FeatureStructureImpl) dependentFs).getAddress() == aOriginAddress
                    && ((FeatureStructureImpl) governorFs).getAddress() == aTargetAddress
                    && !aValue.equals(AnnotationTypeConstant.ROOT)) {

                // It is update of arc value, update it
                if (!fs.getStringValue(feature).equals(aValue)) {
                    fs.setStringValue(feature, aValue);
                }
                duplicate = true;
                break;
            }
        }
        // It is new ARC annotation, create it
        if (!duplicate) {
            AnnotationFS dependentFS;
            AnnotationFS governorFS;
            if (aValue.equals("ROOT")) {
                dependentFS = (AnnotationFS) BratAjaxCasUtil.selectAnnotationByAddress(aJCas,
                        FeatureStructure.class, aOriginAddress);
                governorFS = dependentFS;
            }
            else {
                dependentFS = (AnnotationFS) BratAjaxCasUtil.selectAnnotationByAddress(aJCas,
                        FeatureStructure.class, aOriginAddress);
                governorFS = (AnnotationFS) BratAjaxCasUtil.selectAnnotationByAddress(aJCas,
                        FeatureStructure.class, aTargetAddress);
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
                    CasUtil.selectCovered(aJCas.getCas(), tokenType, dependentFS.getBegin(),
                            dependentFS.getEnd()).get(0));
            newAnnotation.setFeatureValue(
                    governorFeature,
                    CasUtil.selectCovered(aJCas.getCas(), tokenType, governorFS.getBegin(),
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
    public void deleteFromCas(BratAnnotatorUIData aUIData, BratAnnotatorModel aBratAnnotatorModel)
    {
        int originAddress;
        int targetAddress;
        JCas jCas = aUIData.getjCas();

        if (aBratAnnotatorModel.getProject().isReverseDependencyDirection()) {
            originAddress = aUIData.getTarget();
            targetAddress = aUIData.getOrigin();
        }
        else {
            originAddress = aUIData.getOrigin();
            targetAddress = aUIData.getTarget();
        }
        FeatureStructure fsToDelete = null;

        Type type = CasUtil.getType(jCas.getCas(), annotationTypeName);
        Feature dependentFeature = type.getFeatureByBaseName(dependentFeatureName);
        Feature governorFeature = type.getFeatureByBaseName(governorFeatureName);

        Type spanType = CasUtil.getType(jCas.getCas(), arcSpanType);
        Feature arcSpanFeature = spanType.getFeatureByBaseName(arcSpanTypeFeatureName);

        for (AnnotationFS fs : CasUtil.select(jCas.getCas(), type)) {
            FeatureStructure dependentFs = fs.getFeatureValue(dependentFeature).getFeatureValue(
                    arcSpanFeature);
            FeatureStructure governorFs = fs.getFeatureValue(governorFeature).getFeatureValue(
                    arcSpanFeature);
            if (((FeatureStructureImpl) dependentFs).getAddress() == originAddress
                    && ((FeatureStructureImpl) governorFs).getAddress() == targetAddress) {
                fsToDelete = fs;
                break;
            }
        }
        jCas.removeFsFromIndexes(fsToDelete);
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
            return asList(new Argument("Arg1", ((FeatureStructureImpl) aGovernorFs).getAddress()),
                    new Argument("Arg2", ((FeatureStructureImpl) aDependentFs).getAddress()));
        }
        else {
            return asList(new Argument("Arg1", ((FeatureStructureImpl) aDependentFs).getAddress()),
                    new Argument("Arg2", ((FeatureStructureImpl) aGovernorFs).getAddress()));
        }
    }

    @Override
    public String getLabelFeatureName()
    {
        return labelFeatureName;
    }
}
