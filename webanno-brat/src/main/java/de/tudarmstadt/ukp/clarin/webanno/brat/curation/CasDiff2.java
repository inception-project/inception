/*******************************************************************************
 * Copyright 2015
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.curation;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.CasUtil.select;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class CasDiff2
{
    private final Log log = LogFactory.getLog(getClass());
    
    private Map<String, List<CAS>> cases = new LinkedHashMap<>();
    
    private final Map<Position, ConfigurationSet> configSets = new TreeMap<>();

    private final Map<String, String[]> sortedFeaturesCache = new HashMap<>();

    private int begin;
    
    private int end;
    
    private final Map<String, DiffAdapter> typeAdapters = new HashMap<>();
    
    private CasDiff2(int aBegin, int aEnd, Collection<? extends DiffAdapter> aAdapters)
    {
        begin = aBegin;
        end = aEnd;
        if (aAdapters != null) {
            for (DiffAdapter adapter : aAdapters) {
                typeAdapters.put(adapter.getType(), adapter);
            }
        }
    }

    /**
     * Calculate the differences between CASes. <b>This is for testing</b>. Normally you should use
     * {@link #doDiff(List, Collection, Map)}.
     * 
     * @param aEntryType
     *            the type for which differences are to be calculated.
     * @param aAdapters
     *            a diff adapter telling how the diff algorithm should handle different features
     * @param aCasMap
     *            a set of CASes, each associated with an ID
     * @return a diff result.
     */
    public static <T extends TOP> DiffResult doDiff(Class<T> aEntryType,
            Collection<? extends DiffAdapter> aAdapters, Map<String, JCas> aCasMap)
    {
        Map<String, List<JCas>> map2 = new LinkedHashMap<>();
        for (Entry<String, JCas> e : aCasMap.entrySet()) {
            map2.put(e.getKey(), asList(e.getValue()));
        }
        return doDiff(asList(aEntryType.getName()), aAdapters, map2);
    }

    /**
     * Calculate the differences between CASes. <b>This is for testing</b>. Normally you should use
     * {@link #doDiff(List, Collection, Map)}.
     * 
     * @param aEntryType
     *            the type for which differences are to be calculated.
     * @param aAdapter
     *            a set of diff adapters telling how the diff algorithm should handle different
     *            features
     * @param aCasMap
     *            a set of CASes, each associated with an ID
     * @return a diff result.
     */
    public static <T extends TOP> DiffResult doDiff(Class<T> aEntryType, DiffAdapter aAdapter,
            Map<String, List<JCas>> aCasMap)
    {
        return doDiff(asList(aEntryType.getName()), asList(aAdapter), aCasMap);
    }

    /**
     * Calculate the differences between CASes.
     * 
     * @param aEntryTypes
     *            the type for which differences are to be calculated.
     * @param aAdapters
     *            a set of diff adapters how the diff algorithm should handle different features
     * @param aCasMap
     *            a set of CASes, each associated with an ID
     * @return a diff result.
     */
    public static DiffResult doDiff(List<String> aEntryTypes,
            Collection<? extends DiffAdapter> aAdapters, Map<String, List<JCas>> aCasMap)
    {
        if (aCasMap.isEmpty()) {
            return new DiffResult(new CasDiff2(0,0, aAdapters));
        }
        
        List<JCas> casList = aCasMap.values().iterator().next();
        if (casList.isEmpty()) {
            return new DiffResult(new CasDiff2(0,0, aAdapters));
        }
        
        return doDiff(aEntryTypes, aAdapters, aCasMap, -1, -1);
    }
    
    /**
     * Calculate the differences between CASes. This method scopes the calculation of differences
     * to a span instead of calculating them on the whole text.
     * 
     * @param aEntryTypes
     *            the types for which differences are to be calculated.
     * @param aAdapters
     *            a set of diff adapters telling how the diff algorithm should handle different
     *            features
          * @param aCasMap
     *            a set of CASes, each associated with an ID
     * @param aBegin begin of the span for which differences should be calculated.
     * @param aEnd end of the span for which differences should be calculated.
     * @return a diff result.
     */
    public static DiffResult doDiff(List<String> aEntryTypes,
            Collection<? extends DiffAdapter> aAdapters, Map<String, List<JCas>> aCasMap, int aBegin,
            int aEnd)
    {
        sanityCheck(aCasMap);
        
        CasDiff2 diff = new CasDiff2(aBegin, aEnd, aAdapters);
        
        for (Entry<String, List<JCas>> e : aCasMap.entrySet()) {
            int casId = 0;
            for (String type : aEntryTypes) {
                for (JCas jcas : e.getValue()) {
                    // null elements in the list can occur if a user has never worked on a CAS
                    diff.addCas(e.getKey(), casId, jcas != null ? jcas.getCas() : null, type);
                }
                casId++;
            }
        }
        
        return new DiffResult(diff);
    }
    
    /**
     * Sanity check - all CASes should have the same text.
     */
    private static void sanityCheck(Map<String, List<JCas>> aCasMap)
    {
        if (aCasMap.isEmpty()) {
            return;
        }
        
        // little hack to check if asserts are enabled
        boolean assertsEnabled = false;
        assert assertsEnabled = true; // Intentional side effect!
        if (assertsEnabled) {
            Iterator<List<JCas>> i = aCasMap.values().iterator();
            
            List<JCas> ref = i.next();
            while (i.hasNext()) {
                List<JCas> cur = i.next();
                assert ref.size() == cur.size();
                for (int n = 0; n < ref.size(); n++) {
                    JCas refCas = ref.get(n);
                    JCas curCas = cur.get(n);
                    // null elements in the list can occur if a user has never worked on a CAS
                    assert !(refCas != null && curCas != null)
                            || StringUtils.equals(refCas.getDocumentText(),
                                    curCas.getDocumentText());
                }
            }
        }
        // End sanity check
    }
    
    private DiffAdapter getAdapter(String aType)
    {
        DiffAdapter adapter = typeAdapters.get(aType);
        if (adapter == null) {
            log.warn("No diff adapter for type [" + aType + "] -- treating as without features");
            adapter = new SpanDiffAdapter(aType, Collections.EMPTY_SET);
            typeAdapters.put(aType, adapter);
        }
        return adapter;
    }
    
    /**
     * CASes are added to the diff one after another, building the diff iteratively. A CAS can be
     * added multiple times for different types. Make sure a CAS is not added twice with the same
     * type!
     * 
     * @param aCasGroupId
     *            the ID of the CAS group to add.
     * @param aCas
     *            the CAS itself.
     * @param aType
     *            the type on which to calculate the diff.
     */
    private void addCas(String aCasGroupId, int aCasId, CAS aCas, String aType)
    {
        // Remember that we have already seen this CAS.
        List<CAS> casList = cases.get(aCasGroupId);
        if (casList == null) {
            casList = new ArrayList<>();
            cases.put(aCasGroupId, casList);
        }
        assert casList.size() == aCasId : "Expected CAS ID [" + casList.size() + "] but was ["
                + aCasId + "]";
        casList.add(aCas);
        
        // null elements in the list can occur if a user has never worked on a CAS
        // We add these to the internal list above, but then we bail out here.
        if (aCas == null) {
            return;
        }
        
        Collection<AnnotationFS> annotations;
        if (begin == -1 && end == -1) {
            annotations = select(aCas, getType(aCas, aType));
        }
        else {
            annotations = selectCovered(aCas, getType(aCas, aType), begin, end);
        }
        
        for (AnnotationFS fs : annotations) {
            // Get/create configuration set at the current position
            Position pos = getAdapter(aType).getPosition(aCasId, fs);
            ConfigurationSet configSet = configSets.get(pos);
            if (configSet == null) {
                configSet = new ConfigurationSet(pos);
                configSets.put(pos, configSet);
            }
            
            if (pos.getClass() != configSet.position.getClass()) {
                pos.compareTo(configSet.position);
            }
            
            assert pos.getClass() == configSet.position.getClass() : "Position type mismatch ["
                    + pos.getClass() + "] vs [" + configSet.position.getClass() + "]";

            // Merge FS into current set
            configSet.addConfiguration(aCasGroupId, fs);
        }
//        
//        // Remember that we have processed the type
//        entryTypes.add(aType);
    }
    
    /**
     * Represents a logical position in the text. All annotations considered to be at the same logical
     * position in the document are collected under this. Within the position, there are groups
     * that represent the different configurations of the annotation made by different users.
     */
    public static interface Position extends Comparable<Position>
    {
        /**
         * @return the CAS id.
         */
        int getCasId();
        
        /**
         * @return the type.
         */
        String getType();
    }
    
    public static abstract class Position_ImplBase implements Position
    {
        private final String type;
        private final int casId;
        
        public Position_ImplBase(int aCasId, String aType)
        {
            type = aType;
            casId = aCasId;
        }
        
        public String getType()
        {
            return type;
        }
        
        @Override
        public int getCasId()
        {
            return casId;
        }
        
        @Override
        public int compareTo(Position aOther) {
            if (casId != aOther.getCasId()) {
                return casId - aOther.getCasId();
            }
            else {
                return type.compareTo(aOther.getType());
            }
        }
    }
    
    /**
     * Represents a span position in the text.
     */
    public static class SpanPosition extends Position_ImplBase
    {
        private final int begin;
        private final int end;

        public SpanPosition(int aCasId, String aType, int aBegin, int aEnd)
        {
            super(aCasId, aType);
            begin = aBegin;
            end = aEnd;
        }
        
        /**
         * @return the begin offset.
         */
        public int getBegin()
        {
            return begin;
        }

        /**
         * @return the end offset.
         */
        public int getEnd()
        {
            return end;
        }

        @Override
        public int compareTo(Position aOther)
        {
            int superCompare = super.compareTo(aOther);
            if (superCompare != 0) {
                return superCompare;
            }
            // Order doesn't really matter, but this should sort in the same way as UIMA does:
            // begin ascending
            // end descending
            else {
                SpanPosition otherSpan = (SpanPosition) aOther;
                if (begin == otherSpan.begin) {
                    return otherSpan.end - end;
                }
                else {
                    return begin - otherSpan.begin;
                }
            }
        }

        @Override
        public String toString()
        {
            return "Span [cas=" + getCasId() + ", type="
                    + StringUtils.substringAfterLast(getType(), ".") + ", span=(" + begin + "-"
                    + end + ")]";
        }
    }

    /**
     * Represents a span position in the text.
     */
    public static class ArcPosition extends Position_ImplBase
    {
        private final int sourceBegin;
        private final int sourceEnd;
        private final int targetBegin;
        private final int targetEnd;

        public ArcPosition(int aCasId, String aType, int aSourceBegin, int aSourceEnd,
                int aTargetBegin, int aTargetEnd)
        {
            super(aCasId, aType);
            sourceBegin = aSourceBegin;
            sourceEnd = aSourceEnd;
            targetBegin = aTargetBegin;
            targetEnd = aTargetEnd;
        }
        
        /**
         * @return the source begin offset.
         */
        public int getSourceBegin()
        {
            return sourceBegin;
        }

        /**
         * @return the source end offset.
         */
        public int getSourceEnd()
        {
            return sourceEnd;
        }

        /**
         * @return the target begin offset.
         */
        public int getTargetBegin()
        {
            return targetBegin;
        }

        /**
         * @return the target end offset.
         */
        public int getTargetEnd()
        {
            return targetEnd;
        }

        @Override
        public int compareTo(Position aOther)
        {
            int superCompare = super.compareTo(aOther);
            if (superCompare != 0) {
                return superCompare;
            }
            // Order doesn't really matter, but this should sort in the same way as UIMA does:
            // begin ascending
            // end descending
            else {
                ArcPosition otherSpan = (ArcPosition) aOther;
                if (sourceBegin != otherSpan.sourceBegin) {
                    return sourceBegin - otherSpan.sourceBegin;
                }
                else if (sourceEnd != otherSpan.sourceEnd) {
                    return otherSpan.sourceEnd - sourceEnd;
                }
                else if (targetBegin != otherSpan.targetBegin) {
                    return targetBegin - otherSpan.targetBegin;
                }
                else {
                    return otherSpan.targetEnd - targetEnd;
                }
            }
        }

        @Override
        public String toString()
        {
            return "Arc [cas=" + getCasId() + ", type="
                    + StringUtils.substringAfterLast(getType(), ".") + ", source=(" + sourceBegin
                    + "-" + sourceEnd + "), target=(" + targetBegin + "-" + targetEnd + ")]";
        }
    }

    /**
     * The set of configurations seen at a particular position.
     */
    public class ConfigurationSet
    {
        private final Position position;
        private List<Configuration> configurations = new ArrayList<>();
        private Set<String> casGroupIds = new LinkedHashSet<>();
        
        public ConfigurationSet(Position aPosition)
        {
            position = aPosition;
        }
        
        private void addConfiguration(String aCasGroupId, FeatureStructure aFS)
        {
            if (aFS instanceof SofaFS) {
                return;
            }
            
            // Check if this configuration is already present
            Configuration configuration = null;
            for (Configuration cfg : configurations) {
                if (equalsFS(cfg.getRepresentative(), aFS)) {
                    configuration = cfg;
                }
            }

            // Not found, add new one
            if (configuration == null) {
                configuration = new Configuration(aCasGroupId, position, aFS);
                configurations.add(configuration);
            }
            
            configuration.add(aCasGroupId, aFS);
            casGroupIds.add(aCasGroupId);
        }
        
        /**
         * Gets the total number of configurations recorded in this set. If a configuration has been
         * seen in multiple CASes, it will be counted multiple times. 
         */
        public int getRecordedConfigurationCount()
        {
            int i = 0;
            for (Configuration cfg : configurations) {
                i += cfg.getAddressByCasId().size();
            }
            return i;
        }
        
        /**
         * @return the IDs of the CASes in which this configuration set has been observed.
         */
        public Set<String> getCasGroupIds()
        {
            return casGroupIds;
        }
                
        /**
         * @return the different configurations observed in this set.
         */
        public List<Configuration> getConfigurations()
        {
            return configurations;
        }
        
        /**
         * @param aCasGroupId
         *            a CAS ID
         * @return the different configurations observed in this set for the given CAS ID.
         */
        public List<Configuration> getConfigurations(String aCasGroupId)
        {
            List<Configuration> configurationsForUser = new ArrayList<>();
            for (Configuration cfg : configurations) {
                if (cfg.fsAddresses.keySet().contains(aCasGroupId)) {
                    configurationsForUser.add(cfg);
                }
            }
            return configurationsForUser;
        }
        
        /**
         * @return the position of this configuration set.
         */
        public Position getPosition()
        {
            return position;
        }
    }
    
    /**
     * Compare two feature structure to each other. Comparison is done recursively, but stops at
     * feature values that are annotations. For these, only offsets are checked, but feature values
     * are not inspected further. If the annotations are relevant, their type should be added to the
     * entry types and will then be checked and grouped separately.
     * 
     * @param aFS1
     *            first feature structure.
     * @param aFS2
     *            second feature structure.
     * @return {@code true} if they are equal.
     */
    public boolean equalsFS(FeatureStructure aFS1, FeatureStructure aFS2)
    {
        // Trivial case
        if (aFS1 == aFS2) {
            return true;
        }
        
        // Null check
        if (aFS1 == null || aFS2 == null) {
            return false;
        }

        // Trivial case
        if (aFS1.getCAS() == aFS2.getCAS() && getAddr(aFS1) == getAddr(aFS2)) {
            return true;
        }
        
        Type type1 = aFS1.getType();
        Type type2 = aFS2.getType();
        
        // Types must be the same
        if (!type1.getName().equals(type2.getName())) {
            return false;
        }

        assert type1.getNumberOfFeatures() == type2.getNumberOfFeatures();

        // Sort features by name to be independent over implementation details that may change the
        // order of the features as returned from Type.getFeatures().
        String[] sortedFeatures = sortedFeaturesCache.get(type1.getName());
        if (sortedFeatures == null) {
            sortedFeatures = new String[type1.getNumberOfFeatures()];
            int i = 0;
            for (Feature f : aFS1.getType().getFeatures()) {
                sortedFeatures[i] = f.getShortName();
                i++;
            }
            sortedFeaturesCache.put(type1.getName(), sortedFeatures);
        }
        
        Set<String> labelFeatures = typeAdapters.containsKey(type1.getName()) ? typeAdapters.get(
                type1.getName()).getLabelFeatures() : null;

        if (labelFeatures == null) {
            log.warn("No diff adapter for type [" + type1.getName() + "] -- ignoring!");
        }
                
        for (String feature : sortedFeatures) {
            // Only consider label features. In particular these must not include position features
            // such as begin, end, etc.
            if (labelFeatures == null || !labelFeatures.contains(feature)) {
                continue;
            }

            Feature f1 = type1.getFeatureByBaseName(feature);
            Feature f2 = type2.getFeatureByBaseName(feature);
            
            switch (f1.getRange().getName()) {
            case CAS.TYPE_NAME_BOOLEAN:
                if (aFS1.getBooleanValue(f1) != aFS2.getBooleanValue(f2)) {
                    return false;
                }
                break;
            case CAS.TYPE_NAME_BYTE:
                if (aFS1.getByteValue(f1) != aFS2.getByteValue(f2)) {
                    return false;
                }
                break;
            case CAS.TYPE_NAME_DOUBLE:
                if (aFS1.getDoubleValue(f1) != aFS2.getDoubleValue(f2)) {
                    return false;
                }
                break;
            case CAS.TYPE_NAME_FLOAT:
                if (aFS1.getFloatValue(f1) != aFS2.getFloatValue(f2)) {
                    return false;
                }
                break;
            case CAS.TYPE_NAME_INTEGER:
                if (aFS1.getIntValue(f1) != aFS2.getIntValue(f2)) {
                    return false;
                }
                break;
            case CAS.TYPE_NAME_LONG:
                if (aFS1.getLongValue(f1) != aFS2.getLongValue(f2)) {
                    return false;
                }
                break;
            case CAS.TYPE_NAME_SHORT:
                if (aFS1.getShortValue(f1) != aFS2.getShortValue(f2)) {
                    return false;
                }
                break;
            case CAS.TYPE_NAME_STRING:
                if (!StringUtils.equals(aFS1.getStringValue(f1), aFS2.getStringValue(f2))) {
                    return false;
                }
                break;
            default: {
                // Must be some kind of feature structure then
                FeatureStructure valueFS1 = aFS1.getFeatureValue(f1);
                FeatureStructure valueFS2 = aFS2.getFeatureValue(f2);
                
                // Ignore the SofaFS - we already checked that the CAS is the same.
                if (valueFS1 instanceof SofaFS) {
                    continue;
                }
                
                // If the feature value is an annotation, we just check the position is the same,
                // but we do not go in deeper. If we we wanted to know differences on this type,
                // then it should have been added as an entry type.
                //
                // Q: Why do we not check if they are the same based on the CAS address?
                // A: Because we are checking across CASes and addresses can differ.
                //
                // Q: Why do we not check recursively?
                // A: Because e.g. for chains, this would mean we consider the whole chain as a 
                //    single annotation, but we want to consider each link as an annotation
                TypeSystem ts1 = aFS1.getCAS().getTypeSystem();
                if (ts1.subsumes(ts1.getType(CAS.TYPE_NAME_ANNOTATION), type1)) {
                    // Null check
                    if (aFS1 == null || aFS2 == null) {
                        return false;
                    }

                    // Position check
                    DiffAdapter adapter = getAdapter(aFS1.getType().getName());
                    Position pos1 = adapter.getPosition(0, aFS1);
                    Position pos2 = adapter.getPosition(0, aFS2);
                    
                    if (pos1.compareTo(pos2) != 0) {
                        return false;
                    }
                }
                
                // If the feature type is not an annotation we are still in the "feature tier"
                // just dealing with structured features. It is ok to check these deeply.
                if (!equalsFS(valueFS1, valueFS2)) {
                    return false;
                }
            }
            }
        }
         
        return true;
    }
    
    /**
     * A single configuration seen at a particular position. The configuration may have been
     * observed in multiple CASes. 
     */
    public class Configuration
    {
        private final Position position;
        private final Map<String, Integer> fsAddresses = new TreeMap<>();
        
        public Configuration(String aCasGroupId, Position aPosition, FeatureStructure aFS)
        {
            position = aPosition;
            add(aCasGroupId, aFS);
        }
        
        private void add(String aCasGroupId, FeatureStructure aFS) {
            fsAddresses.put(aCasGroupId, getAddr(aFS));            
        }
        
        private FeatureStructure getRepresentative()
        {
            Entry<String, Integer> e = fsAddresses.entrySet().iterator().next();
            return selectByAddr(cases.get(e.getKey()).get(position.getCasId()), e.getValue());
        }
        
        public Map<String, Integer> getAddressByCasId()
        {
            return fsAddresses;
        }
        
        public <T extends FeatureStructure> T getFs(String aCasGroupId, int aCasId,
                Class<T> aClass, Map<String, List<JCas>> aCasMap)
        {
            return selectByAddr(aCasMap.get(aCasGroupId).get(aCasId), aClass,
                    fsAddresses.get(aCasGroupId));
        }

        public FeatureStructure getFs(String aCasGroupId, int aCasId,
                Map<String, List<JCas>> aCasMap)
        {
            return getFs(aCasGroupId, aCasId, FeatureStructure.class, aCasMap);
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (Entry<String, Integer> e : fsAddresses.entrySet()) {
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(e.getKey());
                sb.append(':');
                sb.append(e.getValue());
            }
            sb.append("] -> ");
            sb.append(getRepresentative());
            return sb.toString();
        }
    }
    
    /**
     * A description of the differences between CASes.
     */
    public static class DiffResult
    {
        private final Map<Position, ConfigurationSet> data;
        private final Set<String> casGroupIds;
        private final Map<ConfigurationSet, Boolean> completenessCache = new HashMap<>();
        
        private DiffResult(CasDiff2 aDiff)
        {
            data = Collections.unmodifiableMap(aDiff.configSets);
            casGroupIds = new LinkedHashSet<>(aDiff.cases.keySet());
        }
        
        public Collection<Position> getPositions() {
            return data.keySet();
        }
        
        public Collection<ConfigurationSet> getConfigurationSets()
        {
            return data.values();
        }
        
        /**
         * @param aPosition a position.
         * @return the configuration set for the given position.
         */
        public ConfigurationSet getConfigurtionSet(Position aPosition)
        {
            return data.get(aPosition);
        }
        
        /**
         * Determine if all CASes see agreed on the given configuration set. This method returns
         * {@code false} if there was disagreement (there are multiple configurations in the set).
         * When using this method, make sure you also take into account whether the set is
         * actually complete (cf. {@link #isComplete(ConfigurationSet)}.
         * 
         * @param aConfigurationSet
         *            a configuration set.
         * @return if all seen CASes agreed on this set.
         */
        public boolean isAgreement(ConfigurationSet aConfigurationSet)
        {
            if (!data.containsValue(aConfigurationSet)) {
                throw new IllegalArgumentException("Configuration set does not belong to this diff");
            }

            if (data.get(aConfigurationSet.position) != aConfigurationSet) {
                throw new IllegalArgumentException("Configuration set position mismatch");
            }

            return aConfigurationSet.configurations.size() == 1;
        }
        
        /**
         * Determine if the given set has been observed in all CASes.
         * 
         * @param aConfigurationSet
         *            a configuration set.
         * @return if seen in all CASes.
         */
        public boolean isComplete(ConfigurationSet aConfigurationSet)
        {
            if (!data.containsValue(aConfigurationSet)) {
                throw new IllegalArgumentException("Configuration set does not belong to this diff");
            }

            if (data.get(aConfigurationSet.position) != aConfigurationSet) {
                throw new IllegalArgumentException("Configuration set position mismatch");
            }

            Boolean complete = completenessCache.get(aConfigurationSet);
            if (complete == null) {
                HashSet<String> unseenGroupCasIDs = new HashSet<>(casGroupIds);
                for (Configuration cfg : aConfigurationSet.configurations) {
                    unseenGroupCasIDs.removeAll(cfg.fsAddresses.keySet());
                }
                complete = unseenGroupCasIDs.isEmpty();
                completenessCache.put(aConfigurationSet, complete);
            }
            
            return complete;
        }
        
        public Map<Position, ConfigurationSet> getDifferingConfigurations()
        {
            Map<Position, ConfigurationSet> diffs = new LinkedHashMap<>();
            for (Entry<Position, ConfigurationSet> e : data.entrySet()) {
                if (!isAgreement(e.getValue())) {
                    diffs.put(e.getKey(), e.getValue());
                }
            }
            
            return diffs;
        }

        public Map<Position, ConfigurationSet> getIncompleteConfigurations()
        {
            Map<Position, ConfigurationSet> diffs = new LinkedHashMap<>();
            for (Entry<Position, ConfigurationSet> e : data.entrySet()) {
                if (!isComplete(e.getValue())) {
                    diffs.put(e.getKey(), e.getValue());
                }
            }
            
            return diffs;
        }

        public int size()
        {
            return data.size();
        }

        public int size(String aType)
        {
            int n = 0;
            for (Position pos : data.keySet()) {
                if (pos.getType().equals(aType)) {
                    n++;
                }
            }
            
            return n;
        }
        
        public void print(PrintStream aOut)
        {
            for (Position p : getPositions()) {
                ConfigurationSet configurationSet = getConfigurtionSet(p);
                aOut.printf("=== %s -> %s %s%n", p, 
                        isAgreement(configurationSet) ? "AGREE" : "DISAGREE",
                        isComplete(configurationSet) ? "COMPLETE" : "INCOMPLETE");
                if (!isAgreement(configurationSet) || !isComplete(configurationSet)) {
                    aOut.println();
                    for (Configuration cfg : configurationSet.getConfigurations()) {
                        aOut.println();
                        aOut.println(cfg);
                    }
                }
            }
        }
    }
    
    public static interface DiffAdapter
    {
        String getType();
        
        Set<String> getLabelFeatures();
        
        Position getPosition(int aCasId, FeatureStructure aFS);
    }
    
    public static abstract class DiffAdapter_ImplBase implements DiffAdapter
    {
        private final String type;
        
        private final Set<String> labelFeatures;
        
        public DiffAdapter_ImplBase(String aType, Set<String> aLabelFeatures)
        {
            type = aType;
            labelFeatures = Collections.unmodifiableSet(new HashSet<>(aLabelFeatures));
        }
        
        public String getType()
        {
            return type;
        }
        
        public Set<String> getLabelFeatures()
        {
            return labelFeatures;
        }
    }

    public static class SpanDiffAdapter extends DiffAdapter_ImplBase
    {
        public static final SpanDiffAdapter POS = new SpanDiffAdapter(POS.class.getName(),
                "PosValue");
        
        public <T extends TOP> SpanDiffAdapter(Class<T> aType, String... aLabelFeatures)
        {
            this(aType.getName(), new HashSet<String>(asList(aLabelFeatures)));
        }
        
        public SpanDiffAdapter(String aType, String... aLabelFeatures)
        {
            this(aType, new HashSet<String>(asList(aLabelFeatures)));
        }
        
        public SpanDiffAdapter(String aType, Set<String> aLabelFeatures)
        {
            super(aType, aLabelFeatures);
        }
        
        public Position getPosition(int aCasId, FeatureStructure aFS)
        {
            AnnotationFS annoFS = (AnnotationFS) aFS;
            return new SpanPosition(aCasId, getType(), annoFS.getBegin(), annoFS.getEnd());
        }
    }

    public static class ArcDiffAdapter extends DiffAdapter_ImplBase
    {
        public static final ArcDiffAdapter DEPENDENCY = new ArcDiffAdapter(
                Dependency.class.getName(), "Dependent", "Governor", "DependencyType");
        
        private String sourceFeature;
        private String targetFeature;
        
        public <T extends TOP> ArcDiffAdapter(Class<T> aType, String aSourceFeature, String aTargetFeature,
                String... aLabelFeatures)
        {
            this(aType.getName(), aSourceFeature, aTargetFeature, new HashSet<String>(
                    asList(aLabelFeatures)));
        }
        
        public ArcDiffAdapter(String aType, String aSourceFeature, String aTargetFeature,
                String... aLabelFeatures)
        {
            this(aType, aSourceFeature, aTargetFeature, new HashSet<String>(asList(aLabelFeatures)));
        }
        
        public ArcDiffAdapter(String aType, String aSourceFeature, String aTargetFeature,
                Set<String> aLabelFeatures)
        {
            super(aType, aLabelFeatures);
            sourceFeature = aSourceFeature;
            targetFeature = aTargetFeature;
        }
        
        public Position getPosition(int aCasId, FeatureStructure aFS)
        {
            Type type = aFS.getType();
            AnnotationFS sourceFS = (AnnotationFS) aFS.getFeatureValue(type
                    .getFeatureByBaseName(sourceFeature));
            AnnotationFS targetFS = (AnnotationFS) aFS.getFeatureValue(type
                    .getFeatureByBaseName(targetFeature));
            return new ArcPosition(aCasId, getType(), 
                    sourceFS != null ? sourceFS.getBegin() : -1,
                    sourceFS != null ? sourceFS.getEnd() : -1,
                    targetFS != null ? targetFS.getBegin() : -1,
                    targetFS != null ? targetFS.getEnd() : -1);
        }
    }

//  private Set<String> entryTypes = new LinkedHashSet<>();

//  /**
//   * Clear the attachment to CASes allowing the class to be serialized.
//   */
//  public void detach()
//  {
//      if (cases != null) {
//          cases.clear();
//      }
//  }
  
//  /**
//   * Rebuilds the diff with the current offsets and entry types. This can be used to fix the diff
//   * after reattaching to CASes that have changed. Mind that the diff results can be differnent
//   * due to the changes.
//   */
//  public void rebuild()
//  {
//      Map<String, CAS> oldCases = cases;
//      cases = new HashMap<>();
//      
//      for (String t : entryTypes) {
//          for (Entry<String, CAS> e : oldCases.entrySet()) {
//              addCas(e.getKey(), e.getValue(), t);
//          }
//      }
//  }
  
//  /**
//   * Attach CASes back so that representatives can be resolved. CASes must not have been changed
//   * or upgraded between detaching and reattaching - the CAS addresses of the feature structures
//   * must still be the same.
//   */
//  public void attach(Map<String, CAS> aCases)
//  {
//      cases = new HashMap<>(aCases);
//  }
}
