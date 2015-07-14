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
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.dkpro.statistics.agreement.IAgreementMeasure;
import org.dkpro.statistics.agreement.IAnnotationUnit;
import org.dkpro.statistics.agreement.coding.CodingAnnotationStudy;
import org.dkpro.statistics.agreement.coding.CohenKappaAgreement;
import org.dkpro.statistics.agreement.coding.FleissKappaAgreement;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationItem;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;
import org.dkpro.statistics.agreement.coding.KrippendorffAlphaAgreement;
import org.dkpro.statistics.agreement.distance.NominalDistanceFunction;

import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.Position;

public class AgreementUtils
{
    public static enum ConcreteAgreementMeasure {
        COHEN_KAPPA_AGREEMENT(false),
        FLEISS_KAPPA_AGREEMENT(false),
        KRIPPENDORFF_ALPHA_NOMINAL_AGREEMENT(true);
        
        private final boolean nullValueSupported;
        
        private ConcreteAgreementMeasure(boolean aNullValueSupported)
        {
            nullValueSupported = aNullValueSupported;
        }
        
        public IAgreementMeasure make(ICodingAnnotationStudy aStudy)
        {
            switch (this) {
            case COHEN_KAPPA_AGREEMENT:
                return new CohenKappaAgreement(aStudy);
            case FLEISS_KAPPA_AGREEMENT:
                return new FleissKappaAgreement(aStudy);
            case KRIPPENDORFF_ALPHA_NOMINAL_AGREEMENT:
                return new KrippendorffAlphaAgreement(aStudy, new NominalDistanceFunction());
            default:   
                throw new IllegalArgumentException();
            }
        }
        
        public boolean isNullValueSupported()
        {
            return nullValueSupported;
        }
    }
    
    public static PairwiseAnnotationResult getPairwiseCohenKappaAgreement(DiffResult aDiff,
            String aType, String aFeature, Map<String, List<JCas>> aCasMap)
    {
        return getPairwiseAgreement(ConcreteAgreementMeasure.COHEN_KAPPA_AGREEMENT, true, aDiff,
                aType, aFeature, aCasMap);
    }

    public static PairwiseAnnotationResult getPairwiseAgreement(
            ConcreteAgreementMeasure aMeasure, boolean aExcludeIncomplete,
            DiffResult aDiff, String aType, String aFeature, Map<String, List<JCas>> aCasMap)
    {
        PairwiseAnnotationResult result = new PairwiseAnnotationResult();
        List<Entry<String, List<JCas>>> entryList = new ArrayList<>(aCasMap.entrySet());
        for (int m = 0; m < entryList.size(); m++) {
            for (int n = 0; n < entryList.size(); n++) {
                // Triangle matrix mirrored
                if (n < m) {
                    Map<String, List<JCas>> pairwiseCasMap = new LinkedHashMap<>();
                    pairwiseCasMap.put(entryList.get(m).getKey(), entryList.get(m).getValue());
                    pairwiseCasMap.put(entryList.get(n).getKey(), entryList.get(n).getValue());
                    AgreementResult res = getAgreement(aMeasure, aExcludeIncomplete, aDiff, aType,
                            aFeature, pairwiseCasMap);
                    result.add(entryList.get(m).getKey(), entryList.get(n).getKey(), res);
                }
            }
        }
        return result;
    }

    public static AgreementResult getCohenKappaAgreement(DiffResult aDiff, String aType,
            String aFeature, Map<String, List<JCas>> aCasMap)
    {
        return getAgreement(ConcreteAgreementMeasure.COHEN_KAPPA_AGREEMENT, true, aDiff, aType,
                aFeature, aCasMap);
    }

    public static AgreementResult getAgreement(ConcreteAgreementMeasure aMeasure,
            boolean aExcludeIncomplete, DiffResult aDiff, String aType, String aFeature,
            Map<String, List<JCas>> aCasMap)
    {
        if (aCasMap.size() != 2) {
            throw new IllegalArgumentException("CAS map must contain exactly two CASes");
        }
        
        AgreementResult agreementResult = AgreementUtils.makeStudy(aDiff, aType, aFeature,
                aExcludeIncomplete, aCasMap);
        try {
            IAgreementMeasure agreement = aMeasure.make(agreementResult.study);
            
            if (agreementResult.study.getItemCount() > 0) {
                agreementResult.setAgreement(agreement.calculateAgreement());
           }
            else {
                agreementResult.setAgreement(Double.NaN);
            }
            return agreementResult;
            
        }
        catch (RuntimeException e) {
            // FIXME
            AgreementUtils.dumpAgreementStudy(System.out, agreementResult);
            throw e;
        }
    }
    
    private static AgreementResult makeStudy(DiffResult aDiff, String aType, String aFeature,
            boolean aExcludeIncomplete, Map<String, List<JCas>> aCasMap)
    {
        return makeStudy(aDiff, aCasMap.keySet(), aType, aFeature, aExcludeIncomplete, aCasMap);
    }
    
    private static AgreementResult makeStudy(DiffResult aDiff, Collection<String> aUsers,
            String aType, String aFeature, boolean aExcludeIncomplete,
            Map<String, List<JCas>> aCasMap)
    {
        List<ConfigurationSet> completeSets = new ArrayList<>();
        List<ConfigurationSet> setsWithDifferences = new ArrayList<>();
        List<ConfigurationSet> incompleteSetsByPosition = new ArrayList<>();
        List<ConfigurationSet> incompleteSetsByLabel = new ArrayList<>();
        List<ConfigurationSet> pluralitySets = new ArrayList<>();
        List<ConfigurationSet> irrelevantSets = new ArrayList<>();
        CodingAnnotationStudy study = new CodingAnnotationStudy(aUsers.size());
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
                    if (aExcludeIncomplete) {
                        incompleteSetsByPosition.add(cfgSet);
                        continue nextPosition;
                    }
                    else {
                        values[i] = null;
                        i++;
                        continue;
                    }
                }
                
                // Make sure a single user didn't do multiple alternative annotations at a single
                // position. So there is currently no support for calculating agreement on stacking
                // annotations.
                List<Configuration> cfgs = cfgSet.getConfigurations(user);
                if (cfgs.size() > 1) {
                    pluralitySets.add(cfgSet);
                    continue nextPosition;
                }

                Configuration cfg = cfgs.get(0);
                
                // Only calculate agreement for the given feature
                FeatureStructure fs = cfg.getFs(user, cfg.getPosition().getCasId(), aCasMap);

                boolean isSubPosition = cfg.getPosition().getFeature() != null;
                boolean isPrimitive = fs.getType().getFeatureByBaseName(aFeature).getRange()
                        .isPrimitive();
                
                if (isPrimitive && !isSubPosition) {
                    // Primitive feature / primary position
                    values[i] = getFeature(fs, aFeature);
                }
                else if (!isPrimitive && isSubPosition && cfg.getPosition().getFeature().equals(aFeature)) {
                    // Link feature / sub-position
                    ArrayFS links = (ArrayFS) fs.getFeatureValue(fs.getType().getFeatureByBaseName(
                            aFeature));
                    FeatureStructure link = links.get(cfg.getAID(user).index);
                    
                    switch (cfg.getPosition().getLinkCompareBehavior()) {
                    case LINK_TARGET_AS_LABEL:
                        // FIXME The target feature name should be obtained from the feature definition!
                        AnnotationFS target = (AnnotationFS) link.getFeatureValue(link.getType()
                                .getFeatureByBaseName("target"));
                        
                        values[i] = target.getBegin() + "-" + target.getEnd();
                        break;
                    case LINK_ROLE_AS_LABEL:
                        // FIXME The role feature name should be obtained from the feature definition!
                        String role = link.getStringValue(link.getType().getFeatureByBaseName(
                                "role"));
                        
                        values[i] = role;
                        break;
                    default:
                        throw new IllegalStateException("Unknown link target comparison mode ["
                                + cfg.getPosition().getLinkCompareBehavior() + "]");
                    }
                }
                else {
                    // If we get here, then this position has nothing relevant to our feature to
                    // be evaluated for agreement. We can skip it directly without recording it
                    // as incomplete
                    irrelevantSets.add(cfgSet);
                    continue nextPosition;
                }

                // "null" cannot be used in agreement calculations. We treat these as incomplete
                if (aExcludeIncomplete && values[i] == null) {
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
        
        return new AgreementResult(aType, aFeature, aDiff, study, completeSets, irrelevantSets,
                setsWithDifferences, incompleteSetsByPosition, incompleteSetsByLabel,
                pluralitySets);
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

        aOut.printf("Relevant position count: %d%n", aAgreement.getRelevantSetCount());

        aOut.printf("%n== Complete sets: %d ==%n", aAgreement.getCompleteSets().size());
        dumpAgreementConfigurationSetsWithItems(aOut, aAgreement, aAgreement.getCompleteSets());
        
        aOut.printf("%n== Incomplete sets (by position): %d == %n", aAgreement.getIncompleteSetsByPosition().size());
        dumpAgreementConfigurationSets(aOut, aAgreement, aAgreement.getIncompleteSetsByPosition());

        aOut.printf("%n== Incomplete sets (by label): %d ==%n", aAgreement.getIncompleteSetsByLabel().size());
        dumpAgreementConfigurationSets(aOut, aAgreement, aAgreement.getIncompleteSetsByLabel());

        aOut.printf("%n== Plurality sets: %d ==%n", aAgreement.getPluralitySets().size());
        dumpAgreementConfigurationSets(aOut, aAgreement, aAgreement.getPluralitySets());
    }
    
    private static void dumpAgreementConfigurationSetsWithItems(PrintStream aOut,
            AgreementResult aAgreement, List<ConfigurationSet> aSets)
    {
        int i = 0;
        for (ICodingAnnotationItem item : aAgreement.getStudy().getItems()) {
            StringBuilder sb = new StringBuilder();
            sb.append(aSets.get(i).getPosition());
            for (IAnnotationUnit unit : item.getUnits()) {
                if (sb.length() > 0) {
                    sb.append(" \t");
                }
                sb.append(unit.getCategory());
            }
            aOut.println(sb);
            i++;
        }
    }

    private static void dumpAgreementConfigurationSets(PrintStream aOut,
            AgreementResult aAgreement, List<ConfigurationSet> aSets)
    {
        for (ConfigurationSet cfgSet : aSets) {
            StringBuilder sb = new StringBuilder();
            sb.append(cfgSet.getPosition());
            for (Configuration cfg : cfgSet.getConfigurations()) {
                if (sb.length() > 0) {
                    sb.append(" \t");
                }
                sb.append(cfg.toString());
            }
            aOut.println(sb);
        }
    }

    public static void dumpStudy(PrintStream aOut, ICodingAnnotationStudy aStudy)
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
        
        for (ICodingAnnotationItem item : aStudy.getItems()) {
            StringBuilder sb = new StringBuilder();
            for (IAnnotationUnit unit : item.getUnits()) {
                if (sb.length() > 0) {
                    sb.append(" \t");
                }
                sb.append(unit.getCategory());
            }
            aOut.println(sb);
        }
    }

    public static class AgreementResult
    {
        private final String type;
        private final String feature;
        private final DiffResult diff;
        private final ICodingAnnotationStudy study;
        private final List<ConfigurationSet> setsWithDifferences;
        private final List<ConfigurationSet> completeSets;
        private final List<ConfigurationSet> irrelevantSets;
        private final List<ConfigurationSet> incompleteSetsByPosition;
        private final List<ConfigurationSet> incompleteSetsByLabel;
        private final List<ConfigurationSet> pluralitySets;
        private double agreement;

        public AgreementResult(String aType, String aFeature)
        {
            type = aType;
            feature = aFeature;
            diff = null;
            study = null;
            setsWithDifferences = null;
            completeSets = null;
            irrelevantSets = null;
            incompleteSetsByPosition = null;
            incompleteSetsByLabel = null;
            pluralitySets = null;
        }

        public AgreementResult(String aType, String aFeature, DiffResult aDiff,
                ICodingAnnotationStudy aStudy, List<ConfigurationSet> aComplete,
                List<ConfigurationSet> aIrrelevantSets,
                List<ConfigurationSet> aSetsWithDifferences,
                List<ConfigurationSet> aIncompleteByPosition,
                List<ConfigurationSet> aIncompleteByLabel,
                List<ConfigurationSet> aPluralitySets)
        {
            type = aType;
            feature = aFeature;
            diff = aDiff;
            study = aStudy;
            setsWithDifferences = aSetsWithDifferences;
            completeSets = Collections.unmodifiableList(new ArrayList<>(aComplete));
            irrelevantSets = aIrrelevantSets;
            incompleteSetsByPosition = Collections.unmodifiableList(new ArrayList<>(
                    aIncompleteByPosition));
            incompleteSetsByLabel = Collections
                    .unmodifiableList(new ArrayList<>(aIncompleteByLabel));
            pluralitySets = Collections
                    .unmodifiableList(new ArrayList<>(aPluralitySets));
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
         * Positions that were seen in all CAS groups, but labels are unset (null).
         */
        public List<ConfigurationSet> getIncompleteSetsByLabel()
        {
            return incompleteSetsByLabel;
        }

        public List<ConfigurationSet> getPluralitySets()
        {
            return pluralitySets;
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
        
        public List<ConfigurationSet> getIrrelevantSets()
        {
            return irrelevantSets;
        }
        
        public int getDiffSetCount()
        {
            return setsWithDifferences.size();
        }
        
        public int getUnusableSetCount()
        {
            return incompleteSetsByPosition.size() + incompleteSetsByLabel.size()
                    + pluralitySets.size();
        }
        
        public Object getCompleteSetCount()
        {
            return completeSets.size();
        }

        public int getTotalSetCount()
        {
            return diff.getPositions().size();
        }
        
        public int getRelevantSetCount()
        {
            return diff.getPositions().size() - irrelevantSets.size();
        }
        
        public double getAgreement()
        {
            return agreement;
        }
        
        public ICodingAnnotationStudy getStudy()
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
                    + getDiffSetCount() + ", unusableSets=" + getUnusableSetCount()
                    + ", agreement=" + agreement + "]";
        }
    }
}
