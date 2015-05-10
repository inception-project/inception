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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.Position;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.AnnotationStudy;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.IAnnotationStudy;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.IAnnotationStudy.IAnnotationItem;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.TwoRaterKappaAgreement;

public class AgreementUtils
{
    public static AgreementResult[][] getPairwiseTwoRaterAgreement(DiffResult aDiff, String aType,
            String aFeature, Map<String, List<JCas>> aCasMap)
    {
        AgreementResult[][] result = new AgreementResult[aCasMap.size()][aCasMap.size()];
        List<Entry<String, List<JCas>>> entryList = new ArrayList<>(aCasMap.entrySet());
        for (int m = 0; m < entryList.size(); m++) {
            for (int n = 0; n < entryList.size(); n++) {
                // Diagonal
                if (m == n) {
                    result[m][n] = new AgreementResult(aType, aFeature);
                    result[m][n].setAgreement(1.0d);
                }
                
                // Triangle matrix mirrored
                if (n < m) {
                    Map<String, List<JCas>> pairwiseCasMap = new LinkedHashMap<>();
                    pairwiseCasMap.put(entryList.get(m).getKey(), entryList.get(m).getValue());
                    pairwiseCasMap.put(entryList.get(n).getKey(), entryList.get(n).getValue());
                    result[m][n] = getTwoRaterAgreement(aDiff, aType, aFeature, pairwiseCasMap);
                    result[n][m] = result[m][n];
                }
            }
        }
        return result;
    }
    
    public static AgreementResult getTwoRaterAgreement(DiffResult aDiff, String aType,
            String aFeature, Map<String, List<JCas>> aCasMap)
    {
        if (aCasMap.size() != 2) {
            throw new IllegalArgumentException("CAS map must contain exactly two CASes");
        }
        
        AgreementResult agreementResult = AgreementUtils.makeStudy(aDiff, aType, aFeature, aCasMap);
        try {
            TwoRaterKappaAgreement agreement = new TwoRaterKappaAgreement(agreementResult.study);
            agreementResult.setAgreement( agreement.calculateAgreement());
            return agreementResult;
        }
        catch (RuntimeException e) {
            // FIXME
            AgreementUtils.dumpAgreementStudy(System.out, agreementResult);
            throw e;
        }
    }
    
    private static AgreementResult makeStudy(DiffResult aDiff, String aType, String aFeature,
            Map<String, List<JCas>> aCasMap)
    {
        return makeStudy(aDiff, aCasMap.keySet(), aType, aFeature, aCasMap);
    }
    
    private static AgreementResult makeStudy(DiffResult aDiff, Collection<String> aUsers,
            String aType, String aFeature, Map<String, List<JCas>> aCasMap)
    {
        List<ConfigurationSet> completeSets = new ArrayList<>();
        List<ConfigurationSet> setsWithDifferences = new ArrayList<>();
        List<ConfigurationSet> incompleteSetsByPosition = new ArrayList<>();
        List<ConfigurationSet> incompleteSetsByLabel = new ArrayList<>();
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
                if (!cfgSet.getCasGroupIds().contains(user)) {
                    incompleteSetsByPosition.add(cfgSet);
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
                FeatureStructure fs = cfgs.get(0).getFs(user, p.getCasId(), aCasMap);
                values[i] = getFeature(fs, aFeature);
                
                // "null" cannot be used in agreement calculations. We treat these as incomplete
                if (values[i] == null) {
                    incompleteSetsByLabel.add(cfgSet);
                    continue nextPosition;
                }
                
                i++;
            }

            if (ObjectUtils.notEqual(values[0], values[1])) {
                setsWithDifferences.add(cfgSet);
            }
            
            completeSets.add(cfgSet);
            study.addItemAsArray(values);
        }
        
        return new AgreementResult(aType, aFeature, aDiff, study, completeSets,
                setsWithDifferences, incompleteSetsByPosition, incompleteSetsByLabel);
    }
    
    public static void dumpAgreementStudy(PrintStream aOut, AgreementResult aAgreement)
    {
        try {
            aOut.printf("Category count: %d%n", aAgreement.getStudy().getCategoryCount());
        }
        catch (Throwable e) {
            aOut.printf("Category count: %s%n", ExceptionUtils.getRootCauseMessage(e));
        }
        try {
            aOut.printf("Item count: %d%n", aAgreement.getStudy().getItemCount());
        }
        catch (Throwable e) {
            aOut.printf("Item count: %s%n", ExceptionUtils.getRootCauseMessage(e));
        }
        
        List<ConfigurationSet> completeSets = aAgreement.getCompleteSets();
        int i = 0;
        for (IAnnotationItem item : aAgreement.getStudy().getItems()) {
            StringBuilder sb = new StringBuilder();
            sb.append(completeSets.get(i).getPosition());
            for (Object obj : item.getAnnotations()) {
                if (sb.length() > 0) {
                    sb.append(" \t");
                }
                sb.append(obj);
            }
            aOut.println(sb);
            i++;
        }
    }

    public static void dumpStudy(PrintStream aOut, IAnnotationStudy aStudy)
    {
        try {
            aOut.printf("Category count: %d%n", aStudy.getCategoryCount());
        }
        catch (Throwable e) {
            aOut.printf("Category count: %s%n", ExceptionUtils.getRootCauseMessage(e));
        }
        try {
            aOut.printf("Item count: %d%n", aStudy.getItemCount());
        }
        catch (Throwable e) {
            aOut.printf("Item count: %s%n", ExceptionUtils.getRootCauseMessage(e));
        }
        
        for (IAnnotationItem item : aStudy.getItems()) {
            StringBuilder sb = new StringBuilder();
            for (Object obj : item.getAnnotations()) {
                if (sb.length() > 0) {
                    sb.append(" \t");
                }
                sb.append(obj);
            }
            aOut.println(sb);
        }
    }

    public static class AgreementResult
    {
        private final String type;
        private final String feature;
        private final DiffResult diff;
        private final IAnnotationStudy study;
        private final List<ConfigurationSet> setsWithDifferences;
        private final List<ConfigurationSet> completeSets;
        private final List<ConfigurationSet> incompleteSetsByPosition;
        private final List<ConfigurationSet> incompleteSetsByLabel;
        private double agreement;

        public AgreementResult(String aType, String aFeature)
        {
            type = aType;
            feature = aFeature;
            diff = null;
            study = null;
            setsWithDifferences = null;
            completeSets = null;
            incompleteSetsByPosition = null;
            incompleteSetsByLabel = null;
        }

        public AgreementResult(String aType, String aFeature, DiffResult aDiff,
                IAnnotationStudy aStudy, List<ConfigurationSet> aComplete,
                List<ConfigurationSet> aSetsWithDifferences,
                List<ConfigurationSet> aIncompleteByPosition,
                List<ConfigurationSet> aIncompleteByLabel)
        {
            type = aType;
            feature = aFeature;
            diff = aDiff;
            study = aStudy;
            setsWithDifferences = aSetsWithDifferences;
            completeSets = Collections.unmodifiableList(new ArrayList<>(aComplete));
            incompleteSetsByPosition = Collections.unmodifiableList(new ArrayList<>(
                    aIncompleteByPosition));
            incompleteSetsByLabel = Collections
                    .unmodifiableList(new ArrayList<>(aIncompleteByLabel));
        }
        
        private void setAgreement(double aAgreement)
        {
            agreement = aAgreement;
        }
        
        /**
         * Positions that were not seen in all CAS groups.
         */
        public List<ConfigurationSet> getIncompleteSetsByPosition()
        {
            return incompleteSetsByPosition;
        }

        /**
         * Positions that were  seen in all CAS groups, but labels are unset (null).
         */
        public List<ConfigurationSet> getIncompleteSetsByLabel()
        {
            return incompleteSetsByLabel;
        }

        /**
         * @return sets differing with respect to the type and feature used to calculate agreement.
         */
        public List<ConfigurationSet> getSetsWithDifferences()
        {
            return setsWithDifferences;
        }
        
        public List<ConfigurationSet> getCompleteSets()
        {
            return completeSets;
        }
        
        public int getDiffSetCount()
        {
            return setsWithDifferences.size();
        }
        
        public int getIncompleteSetCount()
        {
            return incompleteSetsByPosition.size() + incompleteSetsByLabel.size();
        }
        
        public Object getCompleteSetCount()
        {
            return completeSets.size();
        }

        public int getTotalSetCount()
        {
            return diff.getPositions().size();
        }
        
        public double getAgreement()
        {
            return agreement;
        }
        
        public IAnnotationStudy getStudy()
        {
            return study;
        }
        
        public DiffResult getDiff()
        {
            return diff;
        }
        
        public String getType()
        {
            return type;
        }
        
        public String getFeature()
        {
            return feature;
        }

        @Override
        public String toString()
        {
            return "AgreementResult [type=" + type + ", feature=" + feature + ", diffs="
                    + getDiffSetCount() + ", incompleteSets=" + getIncompleteSetCount()
                    + ", agreement=" + agreement + "]";
        }
    }
}
