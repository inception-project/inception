/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universit?t Darmstadt
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
package de.tudarmstadt.ukp.clarin.webanno.brat.curation;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.Position;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.AnnotationStudy;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.IAnnotationStudy;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.TwoRaterKappaAgreement;

public class AgreementUtils
{
    public static AgreementResult getTwoRaterAgreement(DiffResult aDiff, String aType,
            String aFeature, Map<String, JCas> aCasMap)
    {
        if (aCasMap.size() != 2) {
            throw new IllegalArgumentException("CAS map must contain exactly two CASes");
        }
        
        AgreementResult agreementResult = AgreementUtils.makeStudy(aDiff, aType, aFeature, aCasMap);
        TwoRaterKappaAgreement agreement = new TwoRaterKappaAgreement(agreementResult.study);
        agreementResult.setAgreement( agreement.calculateAgreement());
        return agreementResult;
    }
    
    private static AgreementResult makeStudy(DiffResult aDiff, String aType, String aFeature,
            Map<String, JCas> aCasMap)
    {
        List<String> users = new ArrayList<String>(aCasMap.keySet());
        Collections.sort(users);
        return makeStudy(aDiff, users, aType, aFeature, aCasMap);
    }
    
    private static AgreementResult makeStudy(DiffResult aDiff, List<String> aUsers,
            String aType, String aFeature, Map<String, JCas> aCasMap)
    {
        List<ConfigurationSet> incompleteSets = new ArrayList<>();
        AnnotationStudy study = new AnnotationStudy(aUsers.size());
        nextPosition: for (Position p : aDiff.getPositions()) {
            ConfigurationSet cfgSet = aDiff.getConfigurtionSet(p);

            // Only calculate agreement for the given type
            if (!cfgSet.getPosition().getType().equals(aType)) {
                continue;
            }
            
            Object[] values = new Object[aUsers.size()];
            int i = 0;
            for (String user : aUsers) {
                // Set has to include all users, otherwise we cannot calculate the agreement for
                // this configuration set.
                if (!cfgSet.getCasIds().contains(user)) {
                    incompleteSets.add(cfgSet);
                    continue nextPosition;
                }
                
                // Make sure a single user didn't do multiple alternative annotations at a single
                // position. So there is currently no support for calculating agreement on stacking
                // annotations.
                List<Configuration> cfgs = cfgSet.getConfigurations(user);
                if (cfgs.size() > 1) {
                    throw new IllegalStateException(
                            "Agreement for interpretation plurality not yet supported! User ["
                                    + user + "] has [" + cfgs.size()
                                    + "] differnet configurations.");
                }
                
                // Only calculate agreement for the given feature
                FeatureStructure fs = cfgs.get(0).getFs(user, aCasMap);
                values[i] = getFeature(fs, aFeature);
                i++;
            }
            
            study.addItemAsArray(values);
        }
        
        return new AgreementResult(study, incompleteSets);
    }
    
    public static class AgreementResult
    {
        private final IAnnotationStudy study;
        private final List<ConfigurationSet> incompleteSets;
        private double agreement;
        
        public AgreementResult(IAnnotationStudy aStudy, List<ConfigurationSet> aIncomplete)
        {
            study = aStudy;
            incompleteSets = Collections.unmodifiableList(new ArrayList<>(aIncomplete));
        }
        
        private void setAgreement(double aAgreement)
        {
            agreement = aAgreement;
        }
        
        public List<ConfigurationSet> getIncompleteSets()
        {
            return incompleteSets;
        }
        
        public double getAgreement()
        {
            return agreement;
        }
        
        public IAnnotationStudy getStudy()
        {
            return study;
        }

        @Override
        public String toString()
        {
            return "AgreementResult [incompleteSets=" + incompleteSets.size() + ", agreement="
                    + agreement + "]";
        }
    }
}
