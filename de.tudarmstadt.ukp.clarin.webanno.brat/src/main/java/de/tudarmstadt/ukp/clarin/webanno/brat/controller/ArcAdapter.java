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

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorUIData;
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
     * Update the CAS with new/modification of arc annotations from brat
     *
     * @param aLabelValue
     *            the value of the annotation for the arc
     * @param aUIData
     *            Other information obtained from brat such as the start and end offsets
     * @param aReverse If arc direction are in reverse direction, from Dependent to Governor
     */
    public void addToCas(String aLabelValue, BratAnnotatorUIData aUIData,
            BratAnnotatorModel aBratAnnotatorModel, boolean aReverse)
    {
        int originAddress;
        int targetAddress;

        if (aReverse) {
            originAddress = Integer.parseInt(aUIData.getTarget().replaceAll("[\\D]", ""));
            targetAddress = Integer.parseInt(aUIData.getOrigin().replaceAll("[\\D]", ""));
        }
        else {
            originAddress = Integer.parseInt(aUIData.getOrigin().replaceAll("[\\D]", ""));
            targetAddress = Integer.parseInt(aUIData.getTarget().replaceAll("[\\D]", ""));
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
                dependentFS = getFS(aJCas, aOriginAddress);
                governorFS = dependentFS;
            }
            else {
                dependentFS = getFS(aJCas, aOriginAddress);
                governorFS = getFS(aJCas, aTargetAddress);
            }
            AnnotationFS newAnnotation = aJCas.getCas().createAnnotation(type,
                    dependentFS.getBegin(), governorFS.getEnd());
            newAnnotation.setStringValue(feature, aValue);
            // TODO - manage if arc types are based on multiple tokens
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
     * @param aJCas
     * @param aId
     */
    public void deleteFromCas(BratAnnotatorUIData aUIData,
            BratAnnotatorModel aBratAnnotatorModel)
    {

        int originAddress;
        int targetAddress;
        JCas jCas = aUIData.getjCas();

        if(aBratAnnotatorModel.getProject().isReverseDependencyDirection()){
            originAddress = Integer.parseInt(aUIData.getTarget()
                    .replaceAll("[\\D]", ""));
            targetAddress = Integer.parseInt(aUIData.getOrigin()
                    .replaceAll("[\\D]", ""));
        }
        else{
        originAddress = Integer.parseInt(aUIData.getOrigin()
                .replaceAll("[\\D]", ""));
        targetAddress = Integer.parseInt(aUIData.getTarget()
                .replaceAll("[\\D]", ""));
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
     * Fetch a feature structure by its address from the CAS.
     *
     * @param aJCas
     *            the CAS.
     * @param aType
     *            the type of the feature structure to fetch.
     * @param aAddress
     *            the address of the feature structure.
     * @return the feature structure.
     */
    @SuppressWarnings("unchecked")
    private static <T extends FeatureStructure> T getFS(JCas aJCas, int aAddress)
    {
        return (T) aJCas.getLowLevelCas().ll_getFSForRef(aAddress);
    }

}
