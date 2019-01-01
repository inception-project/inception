/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

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

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class CasDiff2
{
    private final static Logger LOG = LoggerFactory.getLogger(CasDiff2.class);
    
    private Map<String, List<CAS>> cases = new LinkedHashMap<>();
    
    private final Map<Position, ConfigurationSet> configSets = new TreeMap<>();

    private final Map<String, String[]> sortedFeaturesCache = new HashMap<>();

    private int begin;
    
    private int end;
    
    private final Map<String, DiffAdapter> typeAdapters = new HashMap<>();
    
    private final LinkCompareBehavior linkCompareBehavior;

    private boolean recurseIntoLinkFeatures = false;
    
    private CasDiff2(int aBegin, int aEnd, Collection<? extends DiffAdapter> aAdapters,
            LinkCompareBehavior aLinkCompareBehavior)
    {
        begin = aBegin;
        end = aEnd;
        linkCompareBehavior = aLinkCompareBehavior;
        if (aAdapters != null) {
            for (DiffAdapter adapter : aAdapters) {
                typeAdapters.put(adapter.getType(), adapter);
            }
        }
    }

    /**
     * Calculate the differences between CASes. <b>This is for testing</b>. Normally you should use
     * {@link #doDiff(List, Collection, Map, int, int, LinkCompareBehavior)}.
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
            Collection<? extends DiffAdapter> aAdapters, LinkCompareBehavior aLinkCompareBehavior,
            Map<String, JCas> aCasMap)
    {
        Map<String, List<JCas>> map2 = new LinkedHashMap<>();
        for (Entry<String, JCas> e : aCasMap.entrySet()) {
            map2.put(e.getKey(), asList(e.getValue()));
        }
        return doDiff(asList(aEntryType.getName()), aAdapters, aLinkCompareBehavior, map2);
    }

    /**
     * Calculate the differences between CASes. <b>This is for testing</b>. Normally you should use
     * {@link #doDiff(List, Collection, Map, int, int, LinkCompareBehavior)}.
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
            LinkCompareBehavior aLinkCompareBehavior, Map<String, List<JCas>> aCasMap)
    {
        return doDiff(asList(aEntryType.getName()), asList(aAdapter), aLinkCompareBehavior,
                aCasMap);
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
            Collection<? extends DiffAdapter> aAdapters, LinkCompareBehavior aLinkCompareBehavior,
            Map<String, List<JCas>> aCasMap)
    {
        if (aCasMap.isEmpty()) {
            return new DiffResult(new CasDiff2(0,0, aAdapters, aLinkCompareBehavior));
        }
        
        List<JCas> casList = aCasMap.values().iterator().next();
        if (casList.isEmpty()) {
            return new DiffResult(new CasDiff2(0,0, aAdapters, aLinkCompareBehavior));
        }
        
        return doDiff(aEntryTypes, aAdapters, aCasMap, -1, -1, aLinkCompareBehavior);
    }
    
    /**
     * Calculate the differences between CASes. This method scopes the calculation of differences to
     * a span instead of calculating them on the whole text.
     * 
     * @param aService
     *            the annotation service.
     * @param aProject
     *            a project.
     * @param aEntryTypes
     *            the types for which differences are to be calculated.
     * @param aCasMap
     *            a set of CASes, each associated with an ID
     * @param aBegin
     *            begin of the span for which differences should be calculated.
     * @param aEnd
     *            end of the span for which differences should be calculated.
     * @return a diff result.
     */
    public static DiffResult doDiffSingle(AnnotationSchemaService aService, Project aProject,
            List<Type> aEntryTypes, LinkCompareBehavior aLinkCompareBehavior,
            Map<String, JCas> aCasMap, int aBegin, int aEnd)
    {
        List<DiffAdapter> adapters = CasDiff2.getAdapters(aService, aProject);
        
        return doDiffSingle(aEntryTypes, adapters, aLinkCompareBehavior, aCasMap, aBegin, aEnd);
    }
    
    /**
     * Calculate the differences between CASes. This method scopes the calculation of differences to
     * a span instead of calculating them on the whole text.
     * 
     * @param aAdapters
     *            a set of diff adapters telling how the diff algorithm should handle different
     *            features
     * @param aEntryTypes
     *            the types for which differences are to be calculated.
     * @param aCasMap
     *            a set of CASes, each associated with an ID
     * @param aBegin
     *            begin of the span for which differences should be calculated.
     * @param aEnd
     *            end of the span for which differences should be calculated.
     * @return a diff result.
     */
    public static DiffResult doDiffSingle(List<Type> aEntryTypes,
            Collection<? extends DiffAdapter> aAdapters, LinkCompareBehavior aLinkCompareBehavior,
            Map<String, JCas> aCasMap, int aBegin, int aEnd)
    {
        List<String> entryTypes = new ArrayList<>();
        for (Type t : aEntryTypes) {
            entryTypes.add(t.getName());
        }
        
        Map<String, List<JCas>> casMap = new LinkedHashMap<>();
        for (Entry<String, JCas> e : aCasMap.entrySet()) {
            casMap.put(e.getKey(), asList(e.getValue()));
        }
        
        return doDiff(entryTypes, aAdapters, casMap, aBegin, aEnd, aLinkCompareBehavior);
    }

    /**
     * Calculate the differences between CASes. This method scopes the calculation of differences to
     * a span instead of calculating them on the whole text.
     * 
     * @param aEntryTypes
     *            the types for which differences are to be calculated.
     * @param aAdapters
     *            a set of diff adapters telling how the diff algorithm should handle different
     *            features
     * @param aCasMap
     *            a set of CASes, each associated with an ID
     * @param aBegin
     *            begin of the span for which differences should be calculated.
     * @param aEnd
     *            end of the span for which differences should be calculated.
     * @return a diff result.
     */
    public static DiffResult doDiff(List<String> aEntryTypes,
            Collection<? extends DiffAdapter> aAdapters, Map<String, List<JCas>> aCasMap,
            int aBegin, int aEnd, LinkCompareBehavior aLinkCompareBehavior)
    {
        long startTime = System.currentTimeMillis();
        
        sanityCheck(aCasMap);
        
        CasDiff2 diff = new CasDiff2(aBegin, aEnd, aAdapters, aLinkCompareBehavior);
        
        for (Entry<String, List<JCas>> e : aCasMap.entrySet()) {
            int casId = 0;
            for (JCas jcas : e.getValue()) {
                for (String type : aEntryTypes) {
                    // null elements in the list can occur if a user has never worked on a CAS
                    diff.addCas(e.getKey(), casId, jcas != null ? jcas.getCas() : null, type);
                }
                casId++;
            }
        }
        
        LOG.trace("CASDiff2 completed in {} ms", System.currentTimeMillis() - startTime);
        
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
            LOG.warn("No diff adapter for type [" + aType + "] -- treating as without features");
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
        
        // Avoid adding same CAS twice in cases where we add multiple types from a CAS. If the
        // current CAS ID is greater than the size of the current CAS list, then we did not add
        // it yet. Before, we checked whether the casList already contained the current CAS, but
        // that failed when we had multiple "null" CASes.
        if ((casList.size() - 1) < aCasId) {
            casList.add(aCas);
        }
        assert (casList.size() - 1) == aCasId : "Expected CAS ID [" + (casList.size() - 1)
                + "] but was [" + aCasId + "]";
        
        // null elements in the list can occur if a user has never worked on a CAS
        // We add these to the internal list above, but then we bail out here.
        if (aCas == null) {
            LOG.debug("CAS group [" + aCasGroupId + "] does not contain a CAS at index [" + aCasId
                    + "].");
            return;
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing CAS group [" + aCasGroupId + "] CAS [" + aCasId
                    + "].");
            
            String collectionId = null;
            String documentId = null;
            try {
                DocumentMetaData dmd = DocumentMetaData.get(aCas);
                collectionId = dmd.getCollectionId();
                documentId = dmd.getDocumentId();
                
                LOG.debug("User [" + collectionId + "] - Document [" + documentId + "]");
            }
            catch (IllegalArgumentException e) {
                // We use this information only for debugging - so we can ignore if the information
                // is missing.
            }
        }
        
        Collection<AnnotationFS> annotations;
        if (begin == -1 && end == -1) {
            annotations = select(aCas, getType(aCas, aType));
        }
        else {
            annotations = selectCovered(aCas, getType(aCas, aType), begin, end);
        }
        
        if (annotations.isEmpty()) {
            LOG.debug("CAS group [" + aCasGroupId + "] CAS [" + aCasId
                    + "] contains no annotations of type [" + aType + "]");
            return;
        }
        else {
            LOG.debug("CAS group [" + aCasGroupId + "] CAS [" + aCasId + "] contains ["
                    + annotations.size() + "] annotations of type [" + aType + "]");
        }

        int posBefore = configSets.keySet().size();
        LOG.debug("Positions before: [" + posBefore + "]");

        for (AnnotationFS fs : annotations) {
            List<Position> positions = new ArrayList<>();
            
            // Get/create configuration set at the current position
            positions.add(getAdapter(aType).getPosition(aCasId, fs));
            
            // Generate secondary positions for multi-link features
            positions.addAll(
                    getAdapter(aType).generateSubPositions(aCasId, fs, linkCompareBehavior));

            for (Position pos : positions) {
                ConfigurationSet configSet = configSets.get(pos);
                if (configSet == null) {
                    configSet = new ConfigurationSet(pos);
                    configSets.put(pos, configSet);
                }
                
    //          REC: appears to be left-over debug code that can be removed...  
    //            if (pos.getClass() != configSet.position.getClass()) {
    //                pos.compareTo(configSet.position);
    //            }
                
                assert pos.getClass() == configSet.position.getClass() : "Position type mismatch ["
                        + pos.getClass() + "] vs [" + configSet.position.getClass() + "]";
    
                // Merge FS into current set
                configSet.addConfiguration(aCasGroupId, fs);
            }
        }

        LOG.debug("Positions after: [" + configSets.keySet().size() + "] (delta: "
                + (configSets.keySet().size() - posBefore) + ")");

//        
//        // Remember that we have processed the type
//        entryTypes.add(aType);
    }
    
    public enum LinkCompareBehavior
    {
        /**
         * The link target is considered to be the label. As a consequence, the
         * {@link Position#compareTo} method includes the role label into comparison but not the
         * link target.
         */
        LINK_TARGET_AS_LABEL,

        /**
         * The link role is considered to be the label and the {@link Position#compareTo} method
         * takes the link target into account
         */
        LINK_ROLE_AS_LABEL;
        
        public String getName()
        {
            return toString();
        }
    }
    
    /**
     * Represents a logical position in the text. All annotations considered to be at the same
     * logical position in the document are collected under this. Within the position, there are
     * groups that represent the different configurations of the annotation made by different users.
     */
    public interface Position extends Comparable<Position>
    {
        /**
         * @return the CAS id.
         */
        int getCasId();
        
        /**
         * @return the type.
         */
        String getType();
        
        /**
         * @return the feature if this is a sub-position for a link feature.
         */
        String getFeature();
        
        String getRole();
        
        int getLinkTargetBegin();
        
        int getLinkTargetEnd();
        
        /**
         * Get the way in which links are compared and labels for links are generated.
         */
        LinkCompareBehavior getLinkCompareBehavior();
        
        String getCollectionId();
        String getDocumentId();
        
        String toMinimalString();
    }
    
    public static abstract class Position_ImplBase implements Position
    {
        private final String type;
        private final int casId;
        private final String feature;

        private final String role;
        
        private final int linkTargetBegin;
        private final int linkTargetEnd;
        private final String linkTargetText;

        private final LinkCompareBehavior linkCompareBehavior;
        
        private final String collectionId;
        private final String documentId;

        public Position_ImplBase(String aCollectionId, String aDocumentId, int aCasId,
                String aType, String aFeature, String aRole, int aLinkTargetBegin,
                int aLinkTargetEnd, String aLinkTargetText, LinkCompareBehavior aBehavior)
        {
            type = aType;
            casId = aCasId;
            feature = aFeature;

            linkCompareBehavior = aBehavior;

            role = aRole;
            linkTargetBegin = aLinkTargetBegin;
            linkTargetEnd = aLinkTargetEnd;
            linkTargetText = aLinkTargetText;

            collectionId = aCollectionId;
            documentId = aDocumentId;
        }

        @Override
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
        public String getFeature()
        {
            return feature;
        }
        
        @Override
        public String getRole()
        {
            return role;
        }
        
        @Override
        public int getLinkTargetBegin()
        {
            return linkTargetBegin;
        }
        
        @Override
        public int getLinkTargetEnd()
        {
            return linkTargetEnd;
        }
        
        public String getLinkTargetText()
        {
            return linkTargetText;
        }
        
        @Override
        public String getCollectionId()
        {
            return collectionId;
        }
        
        @Override
        public String getDocumentId()
        {
            return documentId;
        }
        
        @Override
        public LinkCompareBehavior getLinkCompareBehavior()
        {
            return linkCompareBehavior;
        }
        
        @Override
        public int compareTo(Position aOther) {
            if (casId != aOther.getCasId()) {
                return casId - aOther.getCasId();
            }
            
            int typeCmp = type.compareTo(aOther.getType());
            if (typeCmp != 0) {
                return typeCmp;
            }

            int featureCmp = ObjectUtils.compare(feature, aOther.getFeature());
            if (featureCmp != 0) {
                return featureCmp;
            }

            int linkCmpCmp = ObjectUtils.compare(linkCompareBehavior,
                    aOther.getLinkCompareBehavior());
            if (linkCmpCmp != 0) {
                // If the linkCompareBehavior is not the same, then we are dealing with different
                // positions
                return linkCmpCmp;
            }
            
            // If linkCompareBehavior is equal, then we still only have to continue if it is non-
            // null.
            else if (linkCompareBehavior != null) {
                // If we are dealing with sub-positions generated for link features, then we need to
                // check this, otherwise linkTargetBegin, linkTargetEnd, linkCompareBehavior,
                // feature and role are all unset.
                switch (linkCompareBehavior) {
                case LINK_TARGET_AS_LABEL:
                    // Include role into position
                    return ObjectUtils.compare(role, aOther.getRole());
                case LINK_ROLE_AS_LABEL:
                    // Include target into position
                    if (linkTargetBegin != aOther.getLinkTargetBegin()) {
                        return linkTargetBegin - aOther.getLinkTargetBegin();
                    }
                    
                    return linkTargetEnd - aOther.getLinkTargetEnd();
                default:
                    throw new IllegalStateException("Unknown link target comparison mode ["
                            + linkCompareBehavior + "]");
                }
            }
            else {
                return linkCmpCmp;
            }
        }
        
        protected void toStringFragment(StringBuilder builder)
        {
            builder.append("cas=");
            builder.append(getCasId());
            if (getCollectionId() != null) {
                builder.append(", coll=");
                builder.append(getCollectionId());
            }
            if (getDocumentId() != null) {
                builder.append(", doc=");
                builder.append(getDocumentId());
            }
            builder.append(", type=");
            if (getType().contains(".")) {
                builder.append(StringUtils.substringAfterLast(getType(), "."));
            }
            else {
                builder.append(getType());
            }
            if (getFeature() != null) {
                builder.append(", linkFeature=");
                builder.append(getFeature());
                switch (getLinkCompareBehavior()) {
                case LINK_TARGET_AS_LABEL:
                    builder.append(", role=");
                    builder.append(getRole());
                    break;
                case LINK_ROLE_AS_LABEL:
                    builder.append(", linkTarget=(");
                    builder.append(getLinkTargetBegin()).append('-').append(getLinkTargetEnd());
                    builder.append(')');
                    builder.append('[').append(linkTargetText).append(']');
                    break;
                default:
                    builder.append(", BAD LINK BEHAVIOR");
                }
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
        private final String text;

        public SpanPosition(String aCollectionId, String aDocumentId, int aCasId, String aType,
                int aBegin, int aEnd, String aText, String aFeature, String aRole,
                int aLinkTargetBegin, int aLinkTargetEnd, String aLinkTargetText,
                LinkCompareBehavior aLinkCompareBehavior)
        {
            super(aCollectionId, aDocumentId, aCasId, aType, aFeature, aRole, aLinkTargetBegin,
                    aLinkTargetEnd, aLinkTargetText, aLinkCompareBehavior);
            begin = aBegin;
            end = aEnd;
            text = aText;
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
            StringBuilder builder = new StringBuilder();
            builder.append("Span [");
            toStringFragment(builder);
            builder.append(", span=(").append(begin).append('-').append(end).append(')');
            builder.append('[').append(text).append(']');
            builder.append(']');
            return builder.toString();
        }

        @Override
        public String toMinimalString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append(begin).append('-').append(end).append(" [").append(text).append(']');
            LinkCompareBehavior linkCompareBehavior = getLinkCompareBehavior();
            if (linkCompareBehavior != null) {
                switch (linkCompareBehavior) {
                case LINK_TARGET_AS_LABEL:
                    builder.append(" role: [").append(getRole()).append(']');
                    break;
                case LINK_ROLE_AS_LABEL:
                    builder.append(" -> [").append(getLinkTargetBegin()).append('-')
                            .append(getLinkTargetEnd()).append(" [").append(getLinkTargetText())
                            .append(']');
                    break;
                default:
                    throw new IllegalStateException("Unknown link target comparison mode ["
                            + linkCompareBehavior + "]");
                }
            }
            return builder.toString();
        }
    }
    
    /**
     * Represents a span position in the text.
     */
    public static class ArcPosition extends Position_ImplBase
    {
        private final int sourceBegin;
        private final int sourceEnd;
        private final String sourceText;
        private final int targetBegin;
        private final int targetEnd;
        private final String targetText;

        public ArcPosition(String aCollectionId, String aDocumentId, int aCasId, String aType,
                int aSourceBegin, int aSourceEnd, String aSourceText, int aTargetBegin,
                int aTargetEnd, String aTargetText, String aFeature, String aRole,
                int aLinkTargetBegin, int aLinkTargetEnd, String aLinkTargetText,
                LinkCompareBehavior aLinkCompareBehavior)
        {
            super(aCollectionId, aDocumentId, aCasId, aType, aFeature, aRole, aLinkTargetBegin,
                    aLinkTargetEnd, aLinkTargetText, aLinkCompareBehavior);
            sourceBegin = aSourceBegin;
            sourceEnd = aSourceEnd;
            sourceText = aSourceText;
            targetBegin = aTargetBegin;
            targetEnd = aTargetEnd;
            targetText = aTargetText;
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
            StringBuilder builder = new StringBuilder();
            builder.append("Arc [");
            toStringFragment(builder);
            builder.append(", source=(").append(sourceBegin).append('-').append(sourceEnd)
                    .append(')');
            builder.append('[').append(sourceText).append(']');
            builder.append(", target=(").append(targetBegin).append('-').append(targetEnd)
                    .append(')');
            builder.append('[').append(targetText).append(']');
            builder.append("]");
            return builder.toString();
        }
        
        @Override
        public String toMinimalString()
        {
            return "(" + sourceBegin + '-' + sourceEnd + ')' + '[' + sourceText + ']' +
                " -> (" + targetBegin + '-' + targetEnd + ')' + " [" + targetText + ']';
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
            
            if (position.getFeature() == null) {
                // Check if this configuration is already present
                Configuration configuration = null;
                for (Configuration cfg : configurations) {
                    // Handle main positions
                    if (equalsFS(cfg.getRepresentative(), aFS)) {
                        configuration = cfg;
                        break;
                    }
                }
    
                // Not found, add new one
                if (configuration == null) {
                    configuration = new Configuration(position);
                    configurations.add(configuration);
                }
                
                configuration.add(aCasGroupId, aFS);
            }
            else {
                // For each slot at the given position in the FS-to-be-added, we need find a
                // corresponding configuration
                ArrayFS links = (ArrayFS) aFS.getFeatureValue(aFS.getType().getFeatureByBaseName(
                        position.getFeature()));
                for (int i = 0; i < links.size(); i++) {
                    FeatureStructure link = links.get(i);
                    DiffAdapter adapter = getAdapter(aFS.getType().getName());
                    LinkFeatureDecl decl = adapter.getLinkFeature(position.getFeature());
                    
                    // Check if this configuration is already present
                    Configuration configuration = null;
                    switch (position.getLinkCompareBehavior()) {
                    case LINK_TARGET_AS_LABEL: {
                        String role = link.getStringValue(
                                link.getType().getFeatureByBaseName(decl.roleFeature));
                        if (!role.equals(position.getRole())) {
                            continue;
                        }
                        
                        AnnotationFS target = (AnnotationFS) link.getFeatureValue(link.getType()
                                .getFeatureByBaseName(decl.targetFeature));
                        
                        cfgLoop: for (Configuration cfg : configurations) {
                            FeatureStructure repFS = cfg.getRepresentative();
                            AID repAID = cfg.getRepresentativeAID();
                            FeatureStructure repLink = ((ArrayFS) repFS.getFeatureValue(
                                    repFS.getType().getFeatureByBaseName(decl.name)))
                                            .get(repAID.index);
                            AnnotationFS repTarget = (AnnotationFS) repLink.getFeatureValue(
                                    repLink.getType().getFeatureByBaseName(decl.targetFeature));
                            
                            // Compare targets
                            if (equalsAnnotationFS(repTarget, target)) {
                                configuration = cfg;
                                break cfgLoop;
                            }
                        }
                        break;
                    }
                    case LINK_ROLE_AS_LABEL: {
                        AnnotationFS target = (AnnotationFS) link.getFeatureValue(link.getType()
                                .getFeatureByBaseName(decl.targetFeature));
                        if (!(target.getBegin() == position.getLinkTargetBegin() && 
                                target.getEnd() == position.getLinkTargetEnd())) {
                            continue;
                        }
                        
                        String role = link.getStringValue(link.getType().getFeatureByBaseName(
                                decl.roleFeature));
                        
                        cfgLoop: for (Configuration cfg : configurations) {
                            FeatureStructure repFS = cfg.getRepresentative();
                            AID repAID = cfg.getRepresentativeAID();
                            FeatureStructure repLink = ((ArrayFS) repFS.getFeatureValue(
                                    repFS.getType().getFeatureByBaseName(decl.name)))
                                            .get(repAID.index);
                            String linkRole = repLink.getStringValue(repLink.getType()
                                    .getFeatureByBaseName(decl.roleFeature));
                            
                            // Compare roles
                            if (role.equals(linkRole)) {
                                configuration = cfg;
                                break cfgLoop;
                            }
                        }
                        break;
                    }
                    default:
                        throw new IllegalStateException("Unknown link target comparison mode ["
                                + linkCompareBehavior + "]");
                    }
                    
                    // Not found, add new one
                    if (configuration == null) {
                        configuration = new Configuration(position);
                        configurations.add(configuration);
                    }
                    
                    configuration.add(aCasGroupId, aFS, position.getFeature(), i);
                }
            }

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
        String[] cachedSortedFeatures = sortedFeaturesCache.get(type1.getName());
        if (cachedSortedFeatures == null) {
            cachedSortedFeatures = new String[type1.getNumberOfFeatures()];
            int i = 0;
            for (Feature f : aFS1.getType().getFeatures()) {
                cachedSortedFeatures[i] = f.getShortName();
                i++;
            }
            sortedFeaturesCache.put(type1.getName(), cachedSortedFeatures);
        }
        
        DiffAdapter adapter = typeAdapters.get(type1.getName());

        if (adapter == null) {
            LOG.warn("No diff adapter for type [" + type1.getName() + "] -- ignoring!");
            return true;
        }

        // Only consider label features. In particular these must not include position features
        // such as begin, end, etc.
        List<String> sortedFeatures = new ArrayList<>(asList(cachedSortedFeatures));
        Set<String> labelFeatures = adapter.getLabelFeatures();
        sortedFeatures.removeIf(f -> !labelFeatures.contains(f));

        if (!recurseIntoLinkFeatures ) {
            // #1795 Chili REC: We can/should change CasDiff2 such that it does not recurse into
            // link features (or rather into any features that are covered by their own
            // sub-positions). So when when comparing two spans that differ only in their slots
            // (sub-positions) the main position could still exhibit agreement.
            sortedFeatures.removeIf(f -> adapter.getLinkFeature(f) != null);
        }
        
        for (String feature : sortedFeatures) {
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
                    if (!equalsAnnotationFS((AnnotationFS) aFS1, (AnnotationFS) aFS2)) {
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
    
    private boolean equalsAnnotationFS(AnnotationFS aFS1, AnnotationFS aFS2)
    {
        // Null check
        if (aFS1 == null || aFS2 == null) {
            return false;
        }

        // Position check
        DiffAdapter adapter = getAdapter(aFS1.getType().getName());
        Position pos1 = adapter.getPosition(0, aFS1);
        Position pos2 = adapter.getPosition(0, aFS2);
        
        return pos1.compareTo(pos2) == 0;
    }
    
    /**
     * A single configuration seen at a particular position. The configuration may have been
     * observed in multiple CASes. 
     */
    public class Configuration
    {
        private final Position position;
        private final Map<String, AID> fsAddresses = new TreeMap<>();

        public Set<String> getCasGroupIds()
        {
            return fsAddresses.keySet();
        } 
        
        public Configuration(Position aPosition)
        {
            position = aPosition;
        }
        
        public Position getPosition()
        {
            return position;
        }

        private void add(String aCasGroupId, FeatureStructure aFS) {
            fsAddresses.put(aCasGroupId, new AID(getAddr(aFS)));            
        }

        private void add(String aCasGroupId, FeatureStructure aFS, String aFeature, int aSlot) {
            fsAddresses.put(aCasGroupId, new AID(getAddr(aFS), aFeature, aSlot));            
        }

        private FeatureStructure getRepresentative()
        {
            Entry<String, AID> e = fsAddresses.entrySet().iterator().next();
            return selectByAddr(cases.get(e.getKey()).get(position.getCasId()), e.getValue().addr);
        }

        private AID getRepresentativeAID()
        {
            Entry<String, AID> e = fsAddresses.entrySet().iterator().next();
            return e.getValue();
        }

        private Map<String, AID> getAddressByCasId()
        {
            return fsAddresses;
        }

        public AID getAID(String aCasGroupId)
        {
            return fsAddresses.get(aCasGroupId);
        }

        public <T extends FeatureStructure> T getFs(String aCasGroupId, int aCasId,
                Class<T> aClass, Map<String, List<JCas>> aCasMap)
        {
            AID aid = fsAddresses.get(aCasGroupId);
            if (aid == null) {
                return null;
            }
            
            List<JCas> cases = aCasMap.get(aCasGroupId);
            if (cases == null) {
                return null;
            }
            
            JCas cas = cases.get(aCasId);
            if (cas == null) {
                return null;
            }
            
            return selectByAddr(cas, aClass, aid.addr);
        }

        // FIXME aCasId parameter should not be required as we can get it from the position
        public FeatureStructure getFs(String aCasGroupId, int aCasId,
                Map<String, List<JCas>> aCasMap)
        {
            return getFs(aCasGroupId, aCasId, FeatureStructure.class, aCasMap);
        }

        public FeatureStructure getFs(String aCasGroupId, Map<String, JCas> aCasMap)
        {
            Map<String, List<JCas>> casMap = new LinkedHashMap<>();
            for (Entry<String, JCas> e : aCasMap.entrySet()) {
                casMap.put(e.getKey(), asList(e.getValue()));
            }
            return getFs(aCasGroupId, 0, FeatureStructure.class, casMap);
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (Entry<String, AID> e : fsAddresses.entrySet()) {
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
        private final boolean cachedHasDifferences;
        private final Map<String, DiffAdapter> typeDiffAdapters;
        
        private DiffResult(CasDiff2 aDiff)
        {
            data = Collections.unmodifiableMap(aDiff.configSets);
            casGroupIds = new LinkedHashSet<>(aDiff.cases.keySet());
            cachedHasDifferences = !getDifferingConfigurationSets().isEmpty();
            typeDiffAdapters = aDiff.typeAdapters;
        }
        
        public DiffAdapter getDiffAdapter(String aType)
        {
            return typeDiffAdapters.get(aType);
        }
        
        public boolean hasDifferences()
        {
            return cachedHasDifferences;
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
            
            // If there is only a single configuration in the set, we call it an agreement
            if (aConfigurationSet.configurations.size() == 1) {
                return true;
            }

//          Issue 21 GitHub - REC - not really sure if we should call this an agreement            
//            // If there are multiple configurations in the set, we only call it an agreement if
//            // at least one of these configurations has been made by all annotators
//            for (Configuration cfg : aConfigurationSet.configurations) {
//                HashSet<String> unseenGroupCasIDs = new HashSet<>(casGroupIds);
//                unseenGroupCasIDs.removeAll(cfg.fsAddresses.keySet());
//                if (unseenGroupCasIDs.isEmpty()) {
//                    return true;
//                }
//            }
            
            return false;
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
        
        public Map<Position, ConfigurationSet> getDifferingConfigurationSets()
        {
            Map<Position, ConfigurationSet> diffs = new LinkedHashMap<>();
            for (Entry<Position, ConfigurationSet> e : data.entrySet()) {
                if (!isAgreement(e.getValue())) {
                    diffs.put(e.getKey(), e.getValue());
                }
            }
            
            return diffs;
        }

        public Map<Position, ConfigurationSet> getIncompleteConfigurationSets()
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
                    for (Configuration cfg : configurationSet.getConfigurations()) {
                        aOut.println();
                        aOut.println(cfg);
                    }
                }
            }
        }
    }
    
    public static class AID {
        public final int addr;
        public final String feature;
        public final int index;

        public AID(int aAddr)
        {
            this(aAddr, null, -1);
        }
        
        public AID(int aAddr, String aFeature, int aIndex)
        {
            addr = aAddr;
            feature = aFeature;
            index = aIndex;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("AID [addr=");
            builder.append(addr);
            if (feature != null) {
                builder.append(", feature=");
                builder.append(feature);
                builder.append(", index=");
                builder.append(index);
            }
            builder.append("]");
            return builder.toString();
        }
    }
    
    public static class LinkFeatureDecl {
        public final String name;
        public final String roleFeature;
        public final String targetFeature;
        
        public LinkFeatureDecl(String aName, String aRoleFeature, String aTargetFeature)
        {
            name = aName;
            roleFeature = aRoleFeature;
            targetFeature = aTargetFeature;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("LinkFeatureDecl [name=");
            builder.append(name);
            if (roleFeature != null) {
                builder.append(", roleFeature=");
                builder.append(roleFeature);
            }
            if (targetFeature != null) {
                builder.append(", targetFeature=");
                builder.append(targetFeature);
            }
            builder.append("]");
            return builder.toString();
        }
    }
    
    public interface DiffAdapter
    {
        String getType();
        
        Collection<? extends Position> generateSubPositions(int aCasId, AnnotationFS aFs,
                LinkCompareBehavior aLinkCompareBehavior);

        LinkFeatureDecl getLinkFeature(String aFeature);
        
        Set<String> getLabelFeatures();
        
        Position getPosition(int aCasId, FeatureStructure aFS);

        Position getPosition(int aCasId, FeatureStructure aFS, String aFeature, String aRole,
                int aLinkTargetBegin, int aLinkTargetEnd, LinkCompareBehavior aLinkCompareBehavior);
    }
    
    public static abstract class DiffAdapter_ImplBase implements DiffAdapter
    {
        private final String type;
        
        private final Set<String> labelFeatures;
        
        private final List<LinkFeatureDecl> linkFeatures = new ArrayList<>();
        
        public DiffAdapter_ImplBase(String aType, Set<String> aLabelFeatures)
        {
            type = aType;
            labelFeatures = Collections.unmodifiableSet(new HashSet<>(aLabelFeatures));
        }
        
        public void addLinkFeature(String aName, String aRoleFeature, String aTargetFeature)
        {
            linkFeatures.add(new LinkFeatureDecl(aName, aRoleFeature, aTargetFeature));
        }
        
        @Override
        public String getType()
        {
            return type;
        }
        
        @Override
        public Set<String> getLabelFeatures()
        {
            return labelFeatures;
        }
        
        @Override
        public LinkFeatureDecl getLinkFeature(String aFeature)
        {
            for (LinkFeatureDecl decl : linkFeatures) {
                if (decl.name.equals(aFeature)) {
                    return decl;
                }
            }
            return null;
        }

        @Override
        public Position getPosition(int aCasId, FeatureStructure aFS)
        {
            return getPosition(aCasId, aFS, null, null, -1, -1, null);
        }
        
        @Override
        public List<? extends Position> generateSubPositions(int aCasId, AnnotationFS aFs,
                LinkCompareBehavior aLinkCompareBehavior)
        {
            List<Position> subPositions = new ArrayList<>();
            
            for (LinkFeatureDecl decl : linkFeatures) {
                Feature linkFeature = aFs.getType().getFeatureByBaseName(decl.name);
                ArrayFS array = (ArrayFS) aFs.getFeatureValue(linkFeature);
                if (array == null) {
                    continue;
                }
                for (FeatureStructure linkFS : array.toArray()) {
                    String role = linkFS.getStringValue(linkFS.getType().getFeatureByBaseName(
                            decl.roleFeature));
                    AnnotationFS target = (AnnotationFS) linkFS.getFeatureValue(linkFS.getType()
                            .getFeatureByBaseName(decl.targetFeature));
                    Position pos = getPosition(aCasId, aFs, decl.name, role, target.getBegin(),
                            target.getEnd(), aLinkCompareBehavior);
                    subPositions.add(pos);
                }
            }
            
            return subPositions;
        }
    }

    public static class SpanDiffAdapter extends DiffAdapter_ImplBase
    {
        public static final SpanDiffAdapter POS = new SpanDiffAdapter(POS.class.getName(),
                "PosValue");
        
        public static final SpanDiffAdapter NER = new SpanDiffAdapter(NamedEntity.class.getName(),
                "value");
        
        public <T extends TOP> SpanDiffAdapter(Class<T> aType, String... aLabelFeatures)
        {
            this(aType.getName(), new HashSet<>(asList(aLabelFeatures)));
        }
        
        public SpanDiffAdapter(String aType, String... aLabelFeatures)
        {
            this(aType, new HashSet<>(asList(aLabelFeatures)));
        }
        
        public SpanDiffAdapter(String aType, Set<String> aLabelFeatures)
        {
            super(aType, aLabelFeatures);
        }
        
        @Override
        public Position getPosition(int aCasId, FeatureStructure aFS, String aFeature, String aRole,
                int aLinkTargetBegin, int aLinkTargetEnd, LinkCompareBehavior aLinkCompareBehavior)
        {
            AnnotationFS annoFS = (AnnotationFS) aFS;
            
            String collectionId = null;
            String documentId = null;
            try {
                DocumentMetaData dmd = DocumentMetaData.get(aFS.getCAS());
                collectionId = dmd.getCollectionId();
                documentId = dmd.getDocumentId();
            }
            catch (IllegalArgumentException e) {
                // We use this information only for debugging - so we can ignore if the information
                // is missing.
            }
            
            String linkTargetText = null;
            if (aLinkTargetBegin != -1 && aFS.getCAS().getDocumentText() != null) {
                linkTargetText = aFS.getCAS().getDocumentText()
                        .substring(aLinkTargetBegin, aLinkTargetEnd);
            }
            
            return new SpanPosition(collectionId, documentId, aCasId, getType(), annoFS.getBegin(),
                    annoFS.getEnd(), annoFS.getCoveredText(), aFeature, aRole, aLinkTargetBegin,
                    aLinkTargetEnd, linkTargetText, aLinkCompareBehavior);
        }
    }

    public static class ArcDiffAdapter extends DiffAdapter_ImplBase
    {
        public static final ArcDiffAdapter DEPENDENCY = new ArcDiffAdapter(
                Dependency.class.getName(), WebAnnoConst.FEAT_REL_TARGET,
                WebAnnoConst.FEAT_REL_SOURCE, "DependencyType");
        
        private String sourceFeature;
        private String targetFeature;
        
        public <T extends TOP> ArcDiffAdapter(Class<T> aType, String aSourceFeature,
                String aTargetFeature, String... aLabelFeatures)
        {
            this(aType.getName(), aSourceFeature, aTargetFeature,
                    new HashSet<>(asList(aLabelFeatures)));
        }
        
        public ArcDiffAdapter(String aType, String aSourceFeature, String aTargetFeature,
                String... aLabelFeatures)
        {
            this(aType, aSourceFeature, aTargetFeature, new HashSet<>(asList(aLabelFeatures)));
        }
        
        public ArcDiffAdapter(String aType, String aSourceFeature, String aTargetFeature,
                Set<String> aLabelFeatures)
        {
            super(aType, aLabelFeatures);
            sourceFeature = aSourceFeature;
            targetFeature = aTargetFeature;
        }
        
        public String getSourceFeature()
        {
            return sourceFeature;
        }
        
        public String getTargetFeature()
        {
            return targetFeature;
        }
        
        @Override
        public Position getPosition(int aCasId, FeatureStructure aFS, String aFeature, String aRole,
                int aLinkTargetBegin, int aLinkTargetEnd, LinkCompareBehavior aLinkCompareBehavior)
        {
            Type type = aFS.getType();
            AnnotationFS sourceFS = (AnnotationFS) aFS.getFeatureValue(type
                    .getFeatureByBaseName(sourceFeature));
            AnnotationFS targetFS = (AnnotationFS) aFS.getFeatureValue(type
                    .getFeatureByBaseName(targetFeature));
            
            String collectionId = null;
            String documentId = null;
            try {
                DocumentMetaData dmd = DocumentMetaData.get(aFS.getCAS());
                collectionId = dmd.getCollectionId();
                documentId = dmd.getDocumentId();
            }
            catch (IllegalArgumentException e) {
                // We use this information only for debugging - so we can ignore if the information
                // is missing.
            }
            
            String linkTargetText = null;
            if (aLinkTargetBegin != -1 && aFS.getCAS().getDocumentText() != null) {
                linkTargetText = aFS.getCAS().getDocumentText()
                        .substring(aLinkTargetBegin, aLinkTargetEnd);
            }
            
            return new ArcPosition(collectionId, documentId, aCasId, getType(), 
                    sourceFS != null ? sourceFS.getBegin() : -1,
                    sourceFS != null ? sourceFS.getEnd() : -1,
                    sourceFS != null ? sourceFS.getCoveredText() : null,
                    targetFS != null ? targetFS.getBegin() : -1,
                    targetFS != null ? targetFS.getEnd() : -1,
                    targetFS != null ? targetFS.getCoveredText() : null,
                    aFeature, aRole, aLinkTargetBegin, aLinkTargetEnd, linkTargetText,
                    aLinkCompareBehavior);
        }
    }

    public static List<DiffAdapter> getAdapters(AnnotationSchemaService annotationService,
            Project project)
    {
        List<DiffAdapter> adapters = new ArrayList<>();
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(project)) {
            Set<String> labelFeatures = new LinkedHashSet<>();
            for (AnnotationFeature f : annotationService.listAnnotationFeature(layer)) {
                if (!f.isEnabled()) {
                    continue;
                }
                
                // Link features are treated separately from primitive label features
                if (!LinkMode.NONE.equals(f.getLinkMode())) {
                    continue;
                }
                
                labelFeatures.add(f.getName());
            }
            
            DiffAdapter_ImplBase adpt;
            switch (layer.getType()) {
            case SPAN_TYPE: {
                adpt = new SpanDiffAdapter(layer.getName(), labelFeatures);
                break;
            }
            case RELATION_TYPE: {
                RelationAdapter typeAdpt = (RelationAdapter) annotationService.getAdapter(layer);
                adpt = new ArcDiffAdapter(layer.getName(),
                        typeAdpt.getSourceFeatureName(), typeAdpt.getTargetFeatureName(),
                        labelFeatures);
                break;
            }
            case CHAIN_TYPE:
                // FIXME Currently, these are ignored.
                continue;
            default:
                throw new IllegalStateException("Unknown layer type [" + layer.getType() + "]");
            }

            adapters.add(adpt);

            for (AnnotationFeature f : annotationService.listAnnotationFeature(layer)) {
                if (!f.isEnabled()) {
                    continue;
                }
                
                switch (f.getLinkMode()) {
                case NONE:
                    // Nothing to do here
                    break;
                case SIMPLE:
                    adpt.addLinkFeature(f.getName(), f.getLinkTypeRoleFeatureName(), null);
                    break;
                case WITH_ROLE:
                    adpt.addLinkFeature(f.getName(), f.getLinkTypeRoleFeatureName(),
                            f.getLinkTypeTargetFeatureName());
                    break;
                default:
                    throw new IllegalStateException("Unknown link mode [" + f.getLinkMode() + "]");
                }
                
                labelFeatures.add(f.getName());
            }
        }
        return adapters;
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

