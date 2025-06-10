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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalphaunitizing;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparingInt;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.dkpro.statistics.agreement.unitizing.KrippendorffAlphaUnitizingAgreement;
import org.dkpro.statistics.agreement.unitizing.UnitizingAnnotationStudy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing.FullUnitizingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public class KrippendorffAlphaUnitizingAgreementMeasure
    extends AgreementMeasure_ImplBase<//
            FullUnitizingAgreementResult, //
            DefaultAgreementTraits>
{
    private static final List<Object> NULL_VALUE = asList(new Object[] { null });
    private static final Set<String> POSITION_VALUE = Set.of(POSITION);
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public KrippendorffAlphaUnitizingAgreementMeasure(AnnotationLayer aLayer,
            AnnotationFeature aFeature, DefaultAgreementTraits aTraits)
    {
        super(aLayer, aFeature, aTraits);
    }

    @Override
    public FullUnitizingAgreementResult getAgreement(Map<String, CAS> aCasMap)
    {
        var typeName = getLayer().getName();

        // Calculate a character offset continuum. We assume here that the documents
        // all have the same size - since the users cannot change the document sizes, this should be
        // an universally true assumption.
        var someCas = aCasMap.values().stream().filter(Objects::nonNull).findAny().get();

        // Create a unitizing study for that continuum.
        var study = new UnitizingAnnotationStudy(someCas.getDocumentText().length());

        // For each annotator, extract the feature values from all the annotator's CASses and add
        // them to the unitizing study based on character offsets.
        for (var set : aCasMap.entrySet()) {
            var cas = set.getValue();
            if (cas == null) {
                // If a user has never worked on a source document, its CAS is null here - we
                // skip it.
                continue;
            }

            var t = cas.getTypeSystem().getType(typeName);
            if (t == null) {
                // CAS not upgraded yet
                continue;
            }

            var annotations = cas.<Annotation> select(t).asList();
            // // Sort annotations by start position (and by end position in case of ties). This is
            // // more efficient for maintaining the active annotations list than using the default
            // // UIMA sort order
            // annotations.sort(comparingInt(Annotation::getBegin) //
            // .thenComparingInt(Annotation::getEnd));
            // That say, maybe the longest match being added to the study is better than the
            // shortest match

            var activeAnnotations = new TreeMap<Annotation, Set<Object>>(
                    comparingInt(Annotation::getEnd));

            var raterIdx = study.addRater(set.getKey());
            var f = getFeature() != null ? t.getFeatureByBaseName(getFeature().getName()) : null;
            for (var ann : annotations) {
                // Remove annotations that have ended (non-overlapping anymore)
                activeAnnotations.keySet().removeIf(a -> a.getEnd() <= ann.getBegin());

                var values = getValues(ann, f);

                for (var value : values) {
                    var maybeOverlapAnn = activeAnnotations.entrySet().stream()
                            .filter($ -> $.getValue().contains(value)).findFirst();
                    if (maybeOverlapAnn.isPresent()) {
                        var overlapAnn = maybeOverlapAnn.get().getKey();
                        LOG.trace("[{}] Not adding unit [{}]@[{}-{}] due to overlap with [{}-{}]",
                                set.getKey(), value, ann.getBegin(), ann.getEnd(),
                                overlapAnn.getBegin(), overlapAnn.getEnd());
                        continue;
                    }

                    study.addUnit(ann.getBegin(), ann.getEnd() - ann.getBegin(), raterIdx, value);
                }

                // Add the current annotation to the active set
                activeAnnotations.computeIfAbsent(ann, $ -> new HashSet<Object>()).addAll(values);
            }
        }

        LOG.trace("Raters im study: {}", study.getRaterCount());
        LOG.trace("Units in study : {}", study.getUnitCount());

        // for (var u : study.getUnits()) {
        // System.out.printf("addUnit(study, %d, %d, %d);%n", u.getOffset(), u.getEndOffset(),
        // u.getRaterIdx());
        // }
        //
        // for (var r = 0; r < study.getRaterCount(); r++) {
        // var finalr = r;
        // var runits = study.getUnits().stream().filter(u -> u.getRaterIdx() == finalr).toList();
        // for (var u1 : runits) {
        // for (var u2 : runits) {
        // if (u1 == u2) {
        // continue;
        // }
        //
        // if (AnnotationPredicates.overlapping((int) u1.getOffset(),
        // (int) u1.getEndOffset(), (int) u2.getOffset(), (int) u2.getEndOffset())
        // && Objects.equals(u1.getCategory(), u2.getCategory())) {
        // LOG.trace("Oops");
        // }
        // }
        // }
        // }

        var featureName = getFeature() != null ? getFeature().getName() : null;
        var result = new FullUnitizingAgreementResult(typeName, featureName, study,
                new ArrayList<>(aCasMap.keySet()), getTraits().isExcludeIncomplete());

        if (result.isEmpty()) {
            result.setAgreement(Double.NaN);
        }
        else {
            var measure = new KrippendorffAlphaUnitizingAgreement(study);
            result.setAgreement(measure.calculateAgreement());
        }

        return result;
    }

    private Collection<?> getValues(Annotation ann, Feature f)
    {
        if (f == null) {
            return POSITION_VALUE;
        }

        var featureValue = FSUtil.getFeature(ann, f, Object.class);
        if (featureValue instanceof Collection collectionValue) {
            return collectionValue;
        }

        if (featureValue == null) {
            return NULL_VALUE;
        }

        return Set.of(featureValue);
    }
}
