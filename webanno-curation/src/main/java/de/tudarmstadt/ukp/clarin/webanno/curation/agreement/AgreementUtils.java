/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.curation.agreement;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getFeature;
import static java.util.Arrays.asList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.ArcDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.ArcPosition;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.Position;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.IAgreementMeasure;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.IAnnotationUnit;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.CodingAnnotationStudy;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.CohenKappaAgreement;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.FleissKappaAgreement;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.ICodingAnnotationItem;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.KrippendorffAlphaAgreement;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.distance.NominalDistanceFunction;

public class AgreementUtils
{
    public enum AgreementReportExportFormat {
        CSV(".csv"),
        DEBUG(".txt");
        
        private final String extension;
        
        AgreementReportExportFormat(String aExtension)
        {
            extension = aExtension;
        }

        public String getExtension()
        {
            return extension;
        }
    }
    
    public enum ConcreteAgreementMeasure {
        COHEN_KAPPA_AGREEMENT(false),
        FLEISS_KAPPA_AGREEMENT(false),
        KRIPPENDORFF_ALPHA_NOMINAL_AGREEMENT(true);
        
        private final boolean nullValueSupported;
        
        ConcreteAgreementMeasure(boolean aNullValueSupported)
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
            String aType, String aFeature, Map<String, List<CAS>> aCasMap)
    {
        return getPairwiseAgreement(ConcreteAgreementMeasure.COHEN_KAPPA_AGREEMENT, true, aDiff,
                aType, aFeature, aCasMap);
    }

    public static PairwiseAnnotationResult getPairwiseAgreement(
            ConcreteAgreementMeasure aMeasure, boolean aExcludeIncomplete,
            DiffResult aDiff, String aType, String aFeature, Map<String, List<CAS>> aCasMap)
    {
        PairwiseAnnotationResult result = new PairwiseAnnotationResult();
        List<Entry<String, List<CAS>>> entryList = new ArrayList<>(aCasMap.entrySet());
        for (int m = 0; m < entryList.size(); m++) {
            for (int n = 0; n < entryList.size(); n++) {
                // Triangle matrix mirrored
                if (n < m) {
                    Map<String, List<CAS>> pairwiseCasMap = new LinkedHashMap<>();
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
            String aFeature, Map<String, List<CAS>> aCasMap)
    {
        return getAgreement(ConcreteAgreementMeasure.COHEN_KAPPA_AGREEMENT, true, aDiff, aType,
                aFeature, aCasMap);
    }

    public static AgreementResult getAgreement(ConcreteAgreementMeasure aMeasure,
            boolean aExcludeIncomplete, DiffResult aDiff, String aType, String aFeature,
            Map<String, List<CAS>> aCasMap)
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
    
    public static AgreementResult makeStudy(DiffResult aDiff, String aType, String aFeature,
            boolean aExcludeIncomplete, Map<String, List<CAS>> aCasMap)
    {
        return makeStudy(aDiff, aCasMap.keySet(), aType, aFeature, aExcludeIncomplete, true,
                aCasMap);
    }
    
    private static CAS findSomeCas(Map<String, List<CAS>> aCasMap)
    {
        for (List<CAS> l : aCasMap.values()) {
            if (l != null) {
                for (CAS cas : l) {
                    if (cas != null) {
                        return cas;
                    }
                }
            }
        }
        
        return null;
    }
    
    private static AgreementResult makeStudy(DiffResult aDiff, Collection<String> aUsers,
            String aType, String aFeature, boolean aExcludeIncomplete, boolean aNullLabelsAsEmpty,
            Map<String, List<CAS>> aCasMap)
    {
        List<String> users = new ArrayList<>(aUsers);
        Collections.sort(users);
        
        List<ConfigurationSet> completeSets = new ArrayList<>();
        List<ConfigurationSet> setsWithDifferences = new ArrayList<>();
        List<ConfigurationSet> incompleteSetsByPosition = new ArrayList<>();
        List<ConfigurationSet> incompleteSetsByLabel = new ArrayList<>();
        List<ConfigurationSet> pluralitySets = new ArrayList<>();
        List<ConfigurationSet> irrelevantSets = new ArrayList<>();
        CodingAnnotationStudy study = new CodingAnnotationStudy(users.size());
        
        // Check if the feature we are looking at is a primitive feature or a link feature
        // We do this by looking it up in the first available CAS. Mind that at this point all
        // CASes should have exactly the same typesystem.
        CAS someCas = findSomeCas(aCasMap);
        if (someCas == null) {
            // Well... there is NOTHING here!
            // All positions are irrelevant
            aDiff.getPositions().forEach(p -> irrelevantSets.add(aDiff.getConfigurtionSet(p)));
            
            return new AgreementResult(aType, aFeature, aDiff, study, users, completeSets,
                    irrelevantSets, setsWithDifferences, incompleteSetsByPosition,
                    incompleteSetsByLabel, pluralitySets, aExcludeIncomplete);
        }
        TypeSystem ts = someCas.getTypeSystem();
        
        // This happens in our testcases when we feed the process with uninitialized CASes.
        // We should just do the right thing here which is: do nothing
        if (ts.getType(aType) == null) {
            // All positions are irrelevant
            aDiff.getPositions().forEach(p -> irrelevantSets.add(aDiff.getConfigurtionSet(p)));
            
            return new AgreementResult(aType, aFeature, aDiff, study, users, completeSets,
                    irrelevantSets, setsWithDifferences, incompleteSetsByPosition,
                    incompleteSetsByLabel, pluralitySets, aExcludeIncomplete);
        }
        
        // Check that the feature really exists instead of just getting a NPE later
        if (ts.getType(aType).getFeatureByBaseName(aFeature) == null) {
            throw new IllegalArgumentException("Type [" + aType + "] has no feature called ["
                    + aFeature + "]");
        }

        boolean isPrimitiveFeature = ts.getType(aType).getFeatureByBaseName(aFeature).getRange()
                .isPrimitive();
        
        nextPosition: for (Position p : aDiff.getPositions()) {
            ConfigurationSet cfgSet = aDiff.getConfigurtionSet(p);

            // Only calculate agreement for the given layer
            if (!cfgSet.getPosition().getType().equals(aType)) {
                // We don't even consider these as irrelevant, they are just filtered out
                continue;
            }

            // If the feature on a position is set, then it is a subposition
            boolean isSubPosition = p.getFeature() != null;

            // Check if this position is irrelevant:
            // - if we are looking for a primitive type and encounter a subposition
            // - if we are looking for a non-primitive type and encounter a primary position
            // this is an inverted XOR!
            if (!(isPrimitiveFeature ^ isSubPosition)) {
                irrelevantSets.add(cfgSet);
                continue;
            }
            
            // Check if subposition is for the feature we are looking for or for a different 
            // feature
            if (isSubPosition && !aFeature.equals(cfgSet.getPosition().getFeature())) {
                irrelevantSets.add(cfgSet);
                continue nextPosition;
            }
            
            // If non of the current users has made any annotation at this position, then skip it
            if (users.stream().filter(u -> cfgSet.getCasGroupIds().contains(u)).count() == 0) {
                irrelevantSets.add(cfgSet);
                continue nextPosition;
            }
            
            Object[] values = new Object[users.size()];
            int i = 0;
            for (String user : users) {
                // Set has to include all users, otherwise we cannot calculate the agreement for
                // this configuration set.
                if (!cfgSet.getCasGroupIds().contains(user)) {
                    incompleteSetsByPosition.add(cfgSet);
                    if (aExcludeIncomplete) {
                        // Record as incomplete
                        continue nextPosition;
                    }
                    else {
                        // Record as missing value
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
                
                // Check if source and/or targets of a relation are stacked
                if (cfg.getPosition() instanceof ArcPosition) {
                    ArcPosition pos = (ArcPosition) cfg.getPosition();
                    FeatureStructure arc = cfg.getFs(user, pos.getCasId(), aCasMap);

                    ArcDiffAdapter adapter = (ArcDiffAdapter) aDiff.getDiffAdapter(pos.getType());

                    // Check if the source of the relation is stacked
                    AnnotationFS source = FSUtil.getFeature(arc, adapter.getSourceFeature(),
                            AnnotationFS.class);
                    List<AnnotationFS> sourceCandidates = CasUtil.selectAt(arc.getCAS(),
                            source.getType(), source.getBegin(), source.getEnd());
                    if (sourceCandidates.size() > 1) {
                        pluralitySets.add(cfgSet);
                        continue nextPosition;
                    }
                    
                    // Check if the target of the relation is stacked
                    AnnotationFS target = FSUtil.getFeature(arc, adapter.getTargetFeature(),
                            AnnotationFS.class);
                    List<AnnotationFS> targetCandidates = CasUtil.selectAt(arc.getCAS(),
                            target.getType(), target.getBegin(), target.getEnd());
                    if (targetCandidates.size() > 1) {
                        pluralitySets.add(cfgSet);
                        continue nextPosition;
                    }
                }
                
                // Only calculate agreement for the given feature
                FeatureStructure fs = cfg.getFs(user, cfg.getPosition().getCasId(), aCasMap);

                // BEGIN PARANOIA
                assert fs.getType().getFeatureByBaseName(aFeature).getRange()
                        .isPrimitive() == isPrimitiveFeature;
                // primitive implies not subposition - if this is primitive and subposition, we
                // should never have gotten here in the first place.
                assert !isPrimitiveFeature || !isSubPosition; 
                // END PARANOIA
                
                if (isPrimitiveFeature && !isSubPosition) {
                    // Primitive feature / primary position
                    values[i] = getFeature(fs, aFeature);
                }
                else if (!isPrimitiveFeature && isSubPosition) {
                    // Link feature / sub-position
                    ArrayFS links = (ArrayFS) fs.getFeatureValue(fs.getType().getFeatureByBaseName(
                            aFeature));
                    FeatureStructure link = links.get(cfg.getAID(user).index);
                    
                    switch (cfg.getPosition().getLinkCompareBehavior()) {
                    case LINK_TARGET_AS_LABEL:
                        // FIXME The target feature name should be obtained from the feature
                        // definition!
                        AnnotationFS target = (AnnotationFS) link.getFeatureValue(link.getType()
                                .getFeatureByBaseName("target"));
                        
                        values[i] = target.getBegin() + "-" + target.getEnd() + " ["
                                + target.getCoveredText() + "]";
                        break;
                    case LINK_ROLE_AS_LABEL:
                        // FIXME The role feature name should be obtained from the feature
                        // definition!
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
                    throw new IllegalStateException("Should never get here: primitive: "
                            + fs.getType().getFeatureByBaseName(aFeature).getRange()
                                    .isPrimitive() + "; subpos: " + isSubPosition);
                }

                // Consider empty/null feature values to be the same and do not exclude them from
                // agreement calculation. The empty label is still a valid label.
                if (aNullLabelsAsEmpty && values[i] == null) {
                    values[i] = "";
                }
                
                // "null" cannot be used in agreement calculations. We treat these as incomplete
                if (values[i] == null) {
                    incompleteSetsByLabel.add(cfgSet);
                    if (aExcludeIncomplete) {
                        continue nextPosition;
                    }
                }

                i++;
            }

            if (ObjectUtils.notEqual(values[0], values[1])) {
                setsWithDifferences.add(cfgSet);
            }
            
            // If the position feature is set (subposition), then it must match the feature we
            // are calculating agreement over
            assert cfgSet.getPosition().getFeature() == null
                    || cfgSet.getPosition().getFeature().equals(aFeature);
            
            completeSets.add(cfgSet);
            study.addItemAsArray(values);
        }
        
        return new AgreementResult(aType, aFeature, aDiff, study, users, completeSets,
                irrelevantSets, setsWithDifferences, incompleteSetsByPosition,
                incompleteSetsByLabel, pluralitySets, aExcludeIncomplete);
    }
    
    public static void toCSV(CSVPrinter aOut, AgreementResult aAgreement) throws IOException
    {
        try {
            aOut.printComment(String.format("Category count: %d%n", aAgreement.getStudy()
                    .getCategoryCount()));
        }
        catch (Throwable e) {
            aOut.printComment(String.format("Category count: %s%n",
                    ExceptionUtils.getRootCauseMessage(e)));
        }
        try {
            aOut.printComment(String.format("Item count: %d%n", aAgreement.getStudy()
                    .getItemCount()));
        }
        catch (Throwable e) {
            aOut.printComment(String.format("Item count: %s%n",
                    ExceptionUtils.getRootCauseMessage(e)));
        }

        aOut.printComment(String.format("Relevant position count: %d%n",
                aAgreement.getRelevantSetCount()));

        // aOut.printf("%n== Complete sets: %d ==%n", aAgreement.getCompleteSets().size());
        configurationSetsWithItemsToCsv(aOut, aAgreement, aAgreement.getCompleteSets());
        //
        // aOut.printf("%n== Incomplete sets (by position): %d == %n",
        // aAgreement.getIncompleteSetsByPosition().size());
        // dumpAgreementConfigurationSets(aOut, aAgreement,
        // aAgreement.getIncompleteSetsByPosition());
        //
        // aOut.printf("%n== Incomplete sets (by label): %d ==%n",
        // aAgreement.getIncompleteSetsByLabel().size());
        // dumpAgreementConfigurationSets(aOut, aAgreement, aAgreement.getIncompleteSetsByLabel());
        //
        // aOut.printf("%n== Plurality sets: %d ==%n", aAgreement.getPluralitySets().size());
        // dumpAgreementConfigurationSets(aOut, aAgreement, aAgreement.getPluralitySets());
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
        
        aOut.printf("%n== Incomplete sets (by position): %d == %n",
                aAgreement.getIncompleteSetsByPosition().size());
        dumpAgreementConfigurationSets(aOut, aAgreement, aAgreement.getIncompleteSetsByPosition());

        aOut.printf("%n== Incomplete sets (by label): %d ==%n",
                aAgreement.getIncompleteSetsByLabel().size());
        dumpAgreementConfigurationSets(aOut, aAgreement, aAgreement.getIncompleteSetsByLabel());

        aOut.printf("%n== Plurality sets: %d ==%n", aAgreement.getPluralitySets().size());
        dumpAgreementConfigurationSets(aOut, aAgreement, aAgreement.getPluralitySets());
    }
    
    private static void configurationSetsWithItemsToCsv(CSVPrinter aOut,
            AgreementResult aAgreement, List<ConfigurationSet> aSets)
        throws IOException
    {
        List<String> headers = new ArrayList<>(
                asList("Type", "Collection", "Document", "Layer", "Feature", "Position"));
        headers.addAll(aAgreement.getCasGroupIds());
        aOut.printRecord(headers);
        
        int i = 0;
        for (ICodingAnnotationItem item : aAgreement.getStudy().getItems()) {
            Position pos = aSets.get(i).getPosition();
            List<String> values = new ArrayList<>();
            values.add(pos.getClass().getSimpleName());
            values.add(pos.getCollectionId());
            values.add(pos.getDocumentId());
            values.add(pos.getType());
            values.add(aAgreement.getFeature());
            values.add(aSets.get(i).getPosition().toMinimalString());
            for (IAnnotationUnit unit : item.getUnits()) {
                values.add(String.valueOf(unit.getCategory()));
            }
            aOut.printRecord(values);
            i++;
        }
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
        private List<String> casGroupIds;
        private final boolean excludeIncomplete;

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
            excludeIncomplete = false;
        }

        public AgreementResult(String aType, String aFeature, DiffResult aDiff,
                ICodingAnnotationStudy aStudy, List<String> aCasGroupIds,
                List<ConfigurationSet> aComplete,
                List<ConfigurationSet> aIrrelevantSets,
                List<ConfigurationSet> aSetsWithDifferences,
                List<ConfigurationSet> aIncompleteByPosition,
                List<ConfigurationSet> aIncompleteByLabel,
                List<ConfigurationSet> aPluralitySets,
                boolean aExcludeIncomplete)
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
            casGroupIds = Collections.unmodifiableList(new ArrayList<>(aCasGroupIds));
            excludeIncomplete = aExcludeIncomplete;
        }
        
        public List<String> getCasGroupIds()
        {
            return casGroupIds;
        }
        
        public boolean isAllNull(String aCasGroupId)
        {
            for (ICodingAnnotationItem item : study.getItems()) {
                if (item.getUnit(casGroupIds.indexOf(aCasGroupId)).getCategory() != null) {
                    return false;
                }
            }
            return true;
        }
        
        public int getNonNullCount(String aCasGroupId)
        {
            int i = 0;
            for (ICodingAnnotationItem item : study.getItems()) {
                if (item.getUnit(casGroupIds.indexOf(aCasGroupId)).getCategory() != null) {
                    i++;
                }
            }
            return i;
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
        
        public boolean isExcludeIncomplete()
        {
            return excludeIncomplete;
        }

        @Override
        public String toString()
        {
            return "AgreementResult [type=" + type + ", feature=" + feature + ", diffs="
                    + getDiffSetCount() + ", unusableSets=" + getUnusableSetCount()
                    + ", agreement=" + agreement + "]";
        }
    }
    
    public static InputStream generateCsvReport(AgreementResult aResult)
        throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(buf, "UTF-8"),
                CSVFormat.RFC4180)) {
            AgreementUtils.toCSV(printer, aResult);
        }

        return new ByteArrayInputStream(buf.toByteArray());
    }
}
