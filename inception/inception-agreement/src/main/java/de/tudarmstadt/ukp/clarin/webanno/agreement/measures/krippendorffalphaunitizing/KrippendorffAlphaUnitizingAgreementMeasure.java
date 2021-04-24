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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.dkpro.statistics.agreement.IAgreementMeasure;
import org.dkpro.statistics.agreement.unitizing.KrippendorffAlphaUnitizingAgreement;
import org.dkpro.statistics.agreement.unitizing.UnitizingAnnotationStudy;

import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing.UnitizingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public class KrippendorffAlphaUnitizingAgreementMeasure
    extends AgreementMeasure_ImplBase<//
            PairwiseAnnotationResult<UnitizingAgreementResult>, //
            KrippendorffAlphaUnitizingAgreementTraits>
{
    private final AnnotationSchemaService annotationService;

    public KrippendorffAlphaUnitizingAgreementMeasure(AnnotationFeature aFeature,
            KrippendorffAlphaUnitizingAgreementTraits aTraits,
            AnnotationSchemaService aAnnotationService)
    {
        super(aFeature, aTraits);
        annotationService = aAnnotationService;
    }

    @Override
    public PairwiseAnnotationResult<UnitizingAgreementResult> getAgreement(
            Map<String, List<CAS>> aCasMap)
    {
        PairwiseAnnotationResult<UnitizingAgreementResult> result = new PairwiseAnnotationResult<>(
                getFeature(), getTraits());
        List<Entry<String, List<CAS>>> entryList = new ArrayList<>(aCasMap.entrySet());
        for (int m = 0; m < entryList.size(); m++) {
            for (int n = 0; n < entryList.size(); n++) {
                // Triangle matrix mirrored
                if (n < m) {
                    Map<String, List<CAS>> pairwiseCasMap = new LinkedHashMap<>();
                    pairwiseCasMap.put(entryList.get(m).getKey(), entryList.get(m).getValue());
                    pairwiseCasMap.put(entryList.get(n).getKey(), entryList.get(n).getValue());
                    UnitizingAgreementResult res = calculatePairAgreement(pairwiseCasMap);
                    result.add(entryList.get(m).getKey(), entryList.get(n).getKey(), res);
                }
            }
        }
        return result;
    }

    public UnitizingAgreementResult calculatePairAgreement(Map<String, List<CAS>> aCasMap)
    {
        String typeName = getFeature().getLayer().getName();

        // Calculate a character offset continuum over all CASses. We assume here that the documents
        // all have the same size - since the users cannot change the document sizes, this should be
        // an universally true assumption.
        List<CAS> firstUserCasses = aCasMap.values().stream().findFirst().get();
        int docCount = firstUserCasses.size();
        int[] docSizes = new int[docCount];
        Arrays.fill(docSizes, 0);
        for (Entry<String, List<CAS>> set : aCasMap.entrySet()) {
            int i = 0;
            for (CAS cas : set.getValue()) {
                if (cas != null) {
                    assert docSizes[i] == 0 || docSizes[i] == cas.getDocumentText().length();

                    docSizes[i] = cas.getDocumentText().length();
                }
                i++;
            }
        }
        int continuumSize = Arrays.stream(docSizes).sum();

        // Create a unitizing study for that continuum.
        UnitizingAnnotationStudy study = new UnitizingAnnotationStudy(continuumSize);

        // For each annotator, extract the feature values from all the annotator's CASses and add
        // them to the unitizing study based on character offsets.
        for (Entry<String, List<CAS>> set : aCasMap.entrySet()) {
            int raterIdx = study.addRater(set.getKey());
            int docOffset = 0;
            int i = 0;
            for (CAS cas : set.getValue()) {
                // If a user has never worked on a source document, its CAS is null here - we
                // skip it.
                if (cas != null) {
                    Type t = cas.getTypeSystem().getType(typeName);
                    Feature f = t.getFeatureByBaseName(getFeature().getName());
                    int currentDocOffset = docOffset;
                    cas.select(t).map(fs -> (AnnotationFS) fs).forEach(fs -> {
                        study.addUnit(currentDocOffset + fs.getBegin(), fs.getEnd() - fs.getBegin(),
                                raterIdx, FSUtil.getFeature(fs, f, Object.class));
                    });
                }

                docOffset += docSizes[i];
                i++;
            }
        }

        UnitizingAgreementResult result = new UnitizingAgreementResult(typeName,
                getFeature().getName(), study, new ArrayList<>(aCasMap.keySet()),
                getTraits().isExcludeIncomplete());

        IAgreementMeasure agreement = new KrippendorffAlphaUnitizingAgreement(study);

        if (result.getStudy().getUnitCount() > 0) {
            result.setAgreement(agreement.calculateAgreement());
        }
        else {
            result.setAgreement(Double.NaN);
        }

        return result;
    }
}
