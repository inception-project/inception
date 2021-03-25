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
package de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public abstract class CodingAgreementMeasure_ImplBase<T extends DefaultAgreementTraits>
    extends AgreementMeasure_ImplBase<PairwiseAnnotationResult<CodingAgreementResult>, T>
{
    public CodingAgreementMeasure_ImplBase(AnnotationFeature aFeature, T aTraits)
    {
        super(aFeature, aTraits);
    }

    @Override
    public PairwiseAnnotationResult<CodingAgreementResult> getAgreement(
            Map<String, List<CAS>> aCasMap)
    {
        PairwiseAnnotationResult<CodingAgreementResult> result = new PairwiseAnnotationResult<>(
                getFeature(), getTraits());
        List<Entry<String, List<CAS>>> entryList = new ArrayList<>(aCasMap.entrySet());
        for (int m = 0; m < entryList.size(); m++) {
            for (int n = 0; n < entryList.size(); n++) {
                // Triangle matrix mirrored
                if (n < m) {
                    Map<String, List<CAS>> pairwiseCasMap = new LinkedHashMap<>();
                    pairwiseCasMap.put(entryList.get(m).getKey(), entryList.get(m).getValue());
                    pairwiseCasMap.put(entryList.get(n).getKey(), entryList.get(n).getValue());
                    CodingAgreementResult res = calculatePairAgreement(pairwiseCasMap);
                    result.add(entryList.get(m).getKey(), entryList.get(n).getKey(), res);
                }
            }
        }
        return result;
    }

    public abstract CodingAgreementResult calculatePairAgreement(Map<String, List<CAS>> aCasMap);
}
