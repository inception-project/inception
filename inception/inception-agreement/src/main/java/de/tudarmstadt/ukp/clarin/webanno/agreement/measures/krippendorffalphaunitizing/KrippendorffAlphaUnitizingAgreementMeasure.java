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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.dkpro.statistics.agreement.unitizing.KrippendorffAlphaUnitizingAgreement;
import org.dkpro.statistics.agreement.unitizing.UnitizingAnnotationStudy;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing.FullUnitizingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public class KrippendorffAlphaUnitizingAgreementMeasure
    extends AgreementMeasure_ImplBase<//
            FullUnitizingAgreementResult, //
            DefaultAgreementTraits>
{
    public KrippendorffAlphaUnitizingAgreementMeasure(AnnotationFeature aFeature,
            DefaultAgreementTraits aTraits)
    {
        super(aFeature, aTraits);
    }

    @Override
    public FullUnitizingAgreementResult getAgreement(Map<String, CAS> aCasMap)
    {
        var typeName = getFeature().getLayer().getName();

        // Calculate a character offset continuum. We assume here that the documents
        // all have the same size - since the users cannot change the document sizes, this should be
        // an universally true assumption.
        var someCas = aCasMap.values().stream().filter(Objects::nonNull).findAny().get();

        // Create a unitizing study for that continuum.
        var study = new UnitizingAnnotationStudy(someCas.getDocumentText().length());

        // For each annotator, extract the feature values from all the annotator's CASses and add
        // them to the unitizing study based on character offsets.
        for (Entry<String, CAS> set : aCasMap.entrySet()) {
            int raterIdx = study.addRater(set.getKey());
            var cas = set.getValue();
            // If a user has never worked on a source document, its CAS is null here - we
            // skip it.
            if (cas != null) {
                var t = cas.getTypeSystem().getType(typeName);
                var f = t.getFeatureByBaseName(getFeature().getName());
                cas.select(t).map(fs -> (AnnotationFS) fs).forEach(fs -> {
                    var featureValue = FSUtil.getFeature(fs, f, Object.class);
                    if (featureValue instanceof Collection) {
                        for (var value : (Collection<?>) featureValue) {
                            study.addUnit(fs.getBegin(), fs.getEnd() - fs.getBegin(), raterIdx,
                                    value);
                        }
                    }
                    else {
                        study.addUnit(fs.getBegin(), fs.getEnd() - fs.getBegin(), raterIdx,
                                featureValue);
                    }
                });
            }
        }

        var result = new FullUnitizingAgreementResult(typeName, getFeature().getName(), study,
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
}
