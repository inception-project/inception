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
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.isSame;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getFeature;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.isSameSentence;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectSentenceAt;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.setFeature;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Comment;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
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
    private final Log log = LogFactory.getLog(getClass());

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

        features = new LinkedHashMap<String, AnnotationFeature>();
        for (AnnotationFeature f : aFeatures) {
            features.put(f.getName(), f);
        }
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
    public void render(final JCas aJcas, List<AnnotationFeature> aFeatures,
            GetDocumentResponse aResponse, BratAnnotatorModel aBratAnnotatorModel,
            ColoringStrategy aColoringStrategy)
    {
        // The first sentence address in the display window!
        Sentence firstSentence = selectSentenceAt(aJcas,
                aBratAnnotatorModel.getSentenceBeginOffset(),
                aBratAnnotatorModel.getSentenceEndOffset());

        int lastAddressInPage = getLastSentenceAddressInDisplayWindow(aJcas,
                getAddr(firstSentence), aBratAnnotatorModel.getPreferences().getWindowSize());

        // the last sentence address in the display window
        Sentence lastSentenceInPage = (Sentence) selectByAddr(aJcas, FeatureStructure.class,
                lastAddressInPage);

        Type type = getType(aJcas.getCas(), annotationTypeName);
        Feature dependentFeature = type.getFeatureByBaseName(targetFeatureName);
        Feature governorFeature = type.getFeatureByBaseName(sourceFeatureName);

        Type spanType = getType(aJcas.getCas(), attachType);
        Feature arcSpanFeature = spanType.getFeatureByBaseName(attachFeatureName);

        FeatureStructure dependentFs;
        FeatureStructure governorFs;

        Map<Integer, Set<Integer>> relationLinks = getRelationLinks(aJcas, firstSentence,
                lastSentenceInPage, type, dependentFeature, governorFeature, arcSpanFeature);

        // if this is a governor for more than one dependent, avoid duplicate yield
        List<Integer> yieldDeps = new ArrayList<>();

        for (AnnotationFS fs : selectCovered(aJcas.getCas(), type, firstSentence.getBegin(),
                lastSentenceInPage.getEnd())) {
            if (attachFeatureName != null) {
                dependentFs = fs.getFeatureValue(dependentFeature).getFeatureValue(arcSpanFeature);
                governorFs = fs.getFeatureValue(governorFeature).getFeatureValue(arcSpanFeature);
            }
            else {
                dependentFs = fs.getFeatureValue(dependentFeature);
                governorFs = fs.getFeatureValue(governorFeature);
            }

            if (dependentFs == null || governorFs == null) {
                log.warn("Relation [" + layer.getName() + "] with id [" + getAddr(fs)
                        + "] has loose ends - cannot render.");
                continue;
            }

            List<Argument> argumentList = getArgument(governorFs, dependentFs);

            String bratLabelText = TypeUtil.getBratLabelText(this, fs, aFeatures);
            String bratTypeName = TypeUtil.getBratTypeName(this);
            String color = aColoringStrategy.getColor(fs, bratLabelText);

            aResponse.addRelation(new Relation(getAddr(fs), bratTypeName, argumentList,
                    bratLabelText, color));

            if (relationLinks.keySet().contains(getAddr(governorFs))
                    && !yieldDeps.contains(getAddr(governorFs))) {
                yieldDeps.add(getAddr(governorFs));

                // sort the annotations (begin, end)
                List<Integer> sortedDepFs = new ArrayList<>(relationLinks.get(getAddr(governorFs)));
                Collections.sort(sortedDepFs, new Comparator<Integer>()
                {
                    @Override
                    public int compare(Integer arg0, Integer arg1)
                    {
                        return selectByAddr(aJcas, arg0).getBegin()
                                - selectByAddr(aJcas, arg1).getBegin();
                    }
                });

                StringBuffer cm = getYieldMessage(aJcas, sortedDepFs);
                aResponse.addComments(new Comment(getAddr(governorFs), "Yield of relation", cm
                        .toString()));
            }
        }
    }
/**
 * The relations yield message
 * @return
 */
    private StringBuffer getYieldMessage(JCas aJCas, List<Integer> sortedDepFs)
    {
        StringBuffer cm = new StringBuffer();
        int end = -1;
        for (Integer depFs : sortedDepFs) {
            if (end == -1) {
                cm.append(selectByAddr(aJCas, depFs).getCoveredText());
                end = selectByAddr(aJCas, depFs).getEnd();
            }
            // if no space between token and punct
            else if (end==selectByAddr(aJCas, depFs).getBegin()){
                cm.append(selectByAddr(aJCas, depFs).getCoveredText());
                end = selectByAddr(aJCas, depFs).getEnd();
            }
            else if (end + 1 != selectByAddr(aJCas, depFs).getBegin()) {
                cm.append(" ... " + selectByAddr(aJCas, depFs).getCoveredText());
                end = selectByAddr(aJCas, depFs).getEnd();
            }
            else {
                cm.append(" " + selectByAddr(aJCas, depFs).getCoveredText());
                end = selectByAddr(aJCas, depFs).getEnd();
            }

        }
        return cm;
    }

    /**
     * Get relation links to display in relation yield
     *
     * @return
     */
    private Map<Integer, Set<Integer>> getRelationLinks(JCas aJcas, Sentence firstSentence,
            Sentence lastSentenceInPage, Type type, Feature dependentFeature,
            Feature governorFeature, Feature arcSpanFeature)
    {
        FeatureStructure dependentFs;
        FeatureStructure governorFs;
        Map<Integer, Set<Integer>> relations = new ConcurrentHashMap<>();

        for (AnnotationFS fs : selectCovered(aJcas.getCas(), type, firstSentence.getBegin(),
                lastSentenceInPage.getEnd())) {
            if (attachFeatureName != null) {
                dependentFs = fs.getFeatureValue(dependentFeature).getFeatureValue(arcSpanFeature);
                governorFs = fs.getFeatureValue(governorFeature).getFeatureValue(arcSpanFeature);
            }
            else {
                dependentFs = fs.getFeatureValue(dependentFeature);
                governorFs = fs.getFeatureValue(governorFeature);
            }
            if (dependentFs == null || governorFs == null) {
                log.warn("Relation [" + layer.getName() + "] with id [" + getAddr(fs)
                        + "] has loose ends - cannot render.");
                continue;
            }
            Set<Integer> links = relations.get(getAddr(governorFs));
            if (links == null) {
                links = new ConcurrentSkipListSet<>();
            }

            links.add(getAddr(dependentFs));
            relations.put(getAddr(governorFs), links);
        }

        // Update other subsequent links
        for (int i = 0; i < relations.keySet().size(); i++) {
            for (Integer fs : relations.keySet()) {
                updateLinks(relations, fs);
            }
        }
        // to start displaying the text from the governor, include it
        for (Integer fs : relations.keySet()) {
            relations.get(fs).add(fs);
        }
        return relations;
    }

    private void updateLinks(Map<Integer, Set<Integer>> aRelLinks, Integer aGov)
    {
        for (Integer dep : aRelLinks.get(aGov)) {
            if (aRelLinks.containsKey(dep) && !aRelLinks.get(aGov).containsAll(aRelLinks.get(dep))) {
                aRelLinks.get(aGov).addAll(aRelLinks.get(dep));
                updateLinks(aRelLinks, dep);
            }
            else {
                continue;
            }
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
     * @param aFeature
     *            the feature.
     * @param aLabelValue
     *            the value of the annotation for the arc
     * @return the ID.
     * @throws BratAnnotationException
     *             if the annotation could not be created/updated.
     */
    public AnnotationFS add(AnnotationFS aOriginFs, AnnotationFS aTargetFs, JCas aJCas, int aStart,
            int aEnd, AnnotationFeature aFeature, Object aLabelValue)
                throws BratAnnotationException
    {
          if (crossMultipleSentence
                || isSameSentence(aJCas, aOriginFs.getBegin(), aTargetFs.getEnd())) {
            return updateCas(aJCas, aStart, aEnd, aOriginFs, aTargetFs, aLabelValue,
                    aFeature);
        }
        else {
            throw new ArcCrossedMultipleSentenceException(
                    "Arc Annotation shouldn't cross sentence boundary");
        }
    }

    /**
     * A Helper method to {@link #addToCas(String, BratAnnotatorUIData)}
     */
    private AnnotationFS updateCas(JCas aJCas, int aBegin, int aEnd, AnnotationFS aOriginFs,
            AnnotationFS aTargetFs, Object aValue, AnnotationFeature aFeature)
    {
        Type type = getType(aJCas.getCas(), annotationTypeName);
        Feature dependentFeature = type.getFeatureByBaseName(targetFeatureName);
        Feature governorFeature = type.getFeatureByBaseName(sourceFeatureName);

        Type spanType = getType(aJCas.getCas(), attachType);

        AnnotationFS dependentFs = null;
        AnnotationFS governorFs = null;

        for (AnnotationFS fs : selectCovered(aJCas.getCas(), type, aBegin, aEnd)) {

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

    /**
     * Argument lists for the arc annotation
     */
    private List<Argument> getArgument(FeatureStructure aGovernorFs, FeatureStructure aDependentFs)
    {
        return asList(new Argument("Arg1", getAddr(aGovernorFs)), new Argument("Arg2",
                getAddr(aDependentFs)));
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
    public List<String> getAnnotation(JCas aJcas, AnnotationFeature aFeature, int begin, int end)
    {
        return new ArrayList<String>();
    }

    public void delete(JCas aJCas, AnnotationFeature aFeature, int aBegin, int aEnd,
            String aDepCoveredText, String aGovCoveredText, Object aValue)
    {
        Feature dependentFeature = getAnnotationType(aJCas.getCas()).getFeatureByBaseName(getTargetFeatureName());
        Feature governorFeature = getAnnotationType(aJCas.getCas()).getFeatureByBaseName(getSourceFeatureName());

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
         if(aDepCoveredText.equals(dependentFs.getCoveredText()) && aGovCoveredText.equals(governorFs.getCoveredText())){
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
    public void updateFeature(JCas aJcas, AnnotationFeature aFeature, int aAddress, Object aValue)
    {
        FeatureStructure fs = selectByAddr(aJcas, FeatureStructure.class, aAddress);
        setFeature(fs, aFeature, aValue);
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
