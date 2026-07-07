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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures.gamma;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.dkpro.statistics.agreement.InsufficientDataException;
import org.dkpro.statistics.agreement.aligning.AlignableAnnotationUnit;
import org.dkpro.statistics.agreement.aligning.AligningAnnotationStudy;
import org.dkpro.statistics.agreement.aligning.GammaAgreement;
import org.dkpro.statistics.agreement.aligning.data.AnnotationSet;
import org.dkpro.statistics.agreement.aligning.data.Rater;
import org.dkpro.statistics.agreement.aligning.disorder.StatisticalContinuumDisorderSampler;
import org.dkpro.statistics.agreement.aligning.dissimilarity.CombinedCategoricalDissimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.aligning.FullAligningAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public class GammaAgreementMeasure
    extends AgreementMeasure_ImplBase<//
            FullAligningAgreementResult, //
            DefaultAgreementTraits>
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Set<String> POSITION_VALUE = Set.of(POSITION);

    // Fixed seed for the Monte-Carlo chance model so that repeated calculations on the same data
    // yield the same agreement value.
    private static final long SEED = 428_984_162_539_017_403L;

    public GammaAgreementMeasure(AnnotationLayer aLayer, AnnotationFeature aFeature,
            DefaultAgreementTraits aTraits)
    {
        super(aLayer, aFeature, aTraits);
    }

    @Override
    public FullAligningAgreementResult getAgreement(Map<String, CAS> aCasMap)
    {
        var typeName = getLayer().getName();
        var categoryFeature = getFeature() != null ? getFeature().getName() : POSITION;

        var study = new AligningAnnotationStudy();
        var raterIdx = 0;
        for (var entry : aCasMap.entrySet()) {
            var cas = entry.getValue();
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

            var rater = new Rater(entry.getKey(), raterIdx++);
            var f = getFeature() != null ? t.getFeatureByBaseName(getFeature().getName()) : null;
            for (var ann : cas.<Annotation> select(t)) {
                if (ann.getBegin() >= ann.getEnd()) {
                    LOG.trace("[{}] Not adding zero-width unit at [{}-{}]", entry.getKey(),
                            ann.getBegin(), ann.getEnd());
                    continue;
                }

                for (var value : getValues(ann, f)) {
                    // A null or empty value is represented as an unlabelled unit (empty feature
                    // map) which the disorder sampler treats as a category of its own. Keeping
                    // null and empty equivalent is consistent with the coding measures.
                    Map<String, String> features = isNotEmpty(value)
                            ? Map.of(categoryFeature, value)
                            : emptyMap();
                    study.addUnit(new AlignableAnnotationUnit(rater, (String) null, ann.getBegin(),
                            ann.getEnd(), features));
                }
            }
        }

        LOG.trace("Raters in study: {}", study.getRaterCount());
        LOG.trace("Units in study : {}", study.getUnitCount());

        var featureName = getFeature() != null ? getFeature().getName() : null;
        var result = new FullAligningAgreementResult(typeName, featureName, study,
                new ArrayList<>(aCasMap.keySet()), getTraits().isExcludeIncomplete());

        if (result.isEmpty() || study.getRaterCount() < 2) {
            result.setAgreement(Double.NaN);
            return result;
        }

        var measure = GammaAgreement.builder() //
                .withAnnotationSet(new AnnotationSet(study.getUnits())) //
                .withDissimilarity(new CombinedCategoricalDissimilarity()) //
                .withDisorderSampler(
                        m -> new StatisticalContinuumDisorderSampler(m, categoryFeature)) //
                .withSeed(SEED) //
                .build();

        try {
            result.setAgreement(measure.calculateAgreement());
        }
        catch (InsufficientDataException e) {
            LOG.debug("Unable to calculate gamma agreement", e);
            result.setAgreement(Double.NaN);
        }

        return result;
    }

    private Collection<String> getValues(Annotation aAnn, Feature aFeature)
    {
        if (aFeature == null) {
            return POSITION_VALUE;
        }

        var featureValue = FSUtil.getFeature(aAnn, aFeature, Object.class);
        if (featureValue instanceof Collection<?> collectionValue) {
            return collectionValue.stream().map(GammaAgreementMeasure::toLabel).toList();
        }

        return singletonList(toLabel(featureValue));
    }

    private static String toLabel(Object aValue)
    {
        return aValue != null ? String.valueOf(aValue) : null;
    }
}
