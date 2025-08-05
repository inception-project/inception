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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff;

import static de.tudarmstadt.ukp.inception.schema.api.feature.MaterializedLink.toMaterializedLinks;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Stream.concat;
import static org.apache.commons.collections4.CollectionUtils.disjunction;
import static org.apache.uima.cas.CAS.TYPE_NAME_BOOLEAN;
import static org.apache.uima.cas.CAS.TYPE_NAME_BYTE;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOUBLE;
import static org.apache.uima.cas.CAS.TYPE_NAME_FLOAT;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER;
import static org.apache.uima.cas.CAS.TYPE_NAME_LONG;
import static org.apache.uima.cas.CAS.TYPE_NAME_SHORT;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;
import static org.apache.uima.fit.util.FSUtil.getFeature;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.curation.api.DiffAdapter;
import de.tudarmstadt.ukp.inception.curation.api.Position;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public class CasDiff
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    Map<String, CAS> casses = new LinkedHashMap<>();

    final Map<Position, ConfigurationSet> configSets = new TreeMap<>();

    private int begin;

    private int end;

    private final Map<String, DiffAdapter> diffAdapters = new HashMap<>();

    private CasDiff(int aBegin, int aEnd, Iterable<? extends DiffAdapter> aAdapters)
    {
        begin = aBegin;
        end = aEnd;

        if (aAdapters != null) {
            for (var adapter : aAdapters) {
                diffAdapters.put(adapter.getType(), adapter);
            }
        }
    }

    /**
     * Calculate the differences between CASes.
     * 
     * @param aAdapters
     *            a set of diff adapters how the diff algorithm should handle different features
     * @param aCasMap
     *            a set of CASes, each associated with an ID
     * @return a diff result.
     */
    public static CasDiff doDiff(Iterable<? extends DiffAdapter> aAdapters,
            Map<String, CAS> aCasMap)
    {
        return doDiff(aAdapters, aCasMap, -1, -1);
    }

    /**
     * Calculate the differences between CASes. This method scopes the calculation of differences to
     * a span instead of calculating them on the whole text.
     * 
     * @param aAdapters
     *            a set of diff adapters telling how the diff algorithm should handle different
     *            features
     * @param aCasMap
     *            a set of CASes, each associated with an ID
     * @param aBegin
     *            begin of the span for which differences should be calculated.
     * @param aEnd
     *            end of the span for which differences should be calculated.
     * @return a diff.
     */
    public static CasDiff doDiff(Iterable<? extends DiffAdapter> aAdapters,
            Map<String, CAS> aCasMap, int aBegin, int aEnd)
    {
        if (aCasMap.isEmpty()) {
            return new CasDiff(0, 0, aAdapters);
        }

        var startTime = currentTimeMillis();

        var diff = new CasDiff(aBegin, aEnd, aAdapters);

        for (var e : aCasMap.entrySet()) {
            var cas = e.getValue();
            var casGroup = e.getKey();
            for (var adapter : aAdapters) {
                // null elements in the list can occur if a user has never worked on a CAS
                diff.addCas(casGroup, cas != null ? cas : null, adapter.getType());
            }
        }

        LOG.trace("Completed in {} ms", currentTimeMillis() - startTime);

        return diff;
    }

    private DiffAdapter getAdapter(String aType)
    {
        var adapter = diffAdapters.get(aType);
        if (adapter == null) {
            LOG.warn("No diff adapter for type [{}] -- treating as without features", aType);
            adapter = new AnnotationDiffAdapter(aType, emptySet());
            diffAdapters.put(aType, adapter);
        }
        return adapter;
    }

    public Map<String, DiffAdapter> getAdapters()
    {
        return diffAdapters;
    }

    public Map<String, CAS> getCasMap()
    {
        return casses;
    }

    public Collection<Position> getPositions()
    {
        return configSets.keySet();
    }

    /**
     * @param aPosition
     *            a position.
     * @return the configuration set for the given position.
     */
    public ConfigurationSet getConfigurationSet(Position aPosition)
    {
        return configSets.get(aPosition);
    }

    public DiffResult toResult()
    {
        return new DiffResult(this);
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
    private void addCas(String aCasGroupId, CAS aCas, String aType)
    {
        // Remember that we have already seen this CAS.
        casses.put(aCasGroupId, aCas);

        // null elements in the list can occur if a user has never worked on a CAS
        // We add these to the internal list above, but then we bail out here.
        if (aCas == null) {
            LOG.debug("CAS group [{}] does not contain a CAS", aCasGroupId);
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Analyzing CAS group [{}]", aCasGroupId);

            String collectionId = null;
            String documentId = null;
            try {
                var dmd = WebAnnoCasUtil.getDocumentMetadata(aCas);
                collectionId = getFeature(dmd, "collectionId", String.class);
                documentId = getFeature(dmd, "documentId", String.class);
                LOG.debug("User [{}] - Document [{}]", collectionId, documentId);
            }
            catch (IllegalArgumentException e) {
                // We use this information only for debugging - so we can ignore if the information
                // is missing.
            }
        }

        var type = aCas.getTypeSystem().getType(aType);
        if (type == null) {
            LOG.debug("CAS group [{}] contains no annotations of type [{}]", aCasGroupId, aType);
            return;
        }

        var adapter = getAdapter(aType);

        Collection<? extends AnnotationBase> annotations;
        if (begin == -1 && end == -1) {
            annotations = aCas.<Annotation> select(type).asList();
        }
        else {
            annotations = adapter.selectAnnotationsInWindow(aCas, begin, end);
        }

        if (annotations.isEmpty()) {
            LOG.debug("CAS group [{}] contains no annotations of type [{}]", aCasGroupId, aType);
            return;
        }

        LOG.debug("CAS group [{}] contains [{}] annotations of type [{}]", aCasGroupId,
                annotations.size(), aType);

        var posBefore = configSets.keySet().size();
        LOG.debug("Positions before: [{}]", posBefore);

        for (var ann : annotations) {
            var positions = new ArrayList<Position>();

            // Get/create configuration set at the current position
            positions.add(adapter.getPosition(ann));

            // Generate secondary positions for multi-link features
            positions.addAll(adapter.generateSubPositions(ann));

            for (var pos : positions) {
                LOG.trace("Analyzing {}", pos);

                var configSet = configSets.get(pos);
                if (configSet == null) {
                    configSet = new ConfigurationSet(pos);
                    configSets.put(pos, configSet);
                }

                assert pos.getClass() == configSet.getPosition()
                        .getClass() : "Position type mismatch [" + pos.getClass() + "] vs ["
                                + configSet.getPosition().getClass() + "]";

                // Merge FS into current set
                addConfiguration(configSet, aCasGroupId, ann);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Positions after: [{}] (delta: {})", configSets.keySet().size(),
                    (configSets.keySet().size() - posBefore));
        }
    }

    private void addConfiguration(ConfigurationSet aSet, String aCasGroupId, AnnotationBase aFS)
    {
        if (aFS instanceof SofaFS) {
            return;
        }

        var position = aSet.getPosition();
        if (position.isLinkFeaturePosition()) {
            addLinkConfiguration(aSet, aCasGroupId, aFS);
            return;
        }

        addBaseConfiguration(aSet, aCasGroupId, aFS);
    }

    private void addLinkConfiguration(ConfigurationSet aSet, String aCasGroupId,
            AnnotationBase aHost)
    {
        var position = aSet.getPosition();
        var feat = aHost.getType().getFeatureByBaseName(position.getLinkFeature());

        // If the CAS has not been upgraded yet to include the feature, then there are no
        // configurations for it.
        if (feat == null) {
            return;
        }

        // For each slot at the given position in the FS-to-be-added, we need find a
        // corresponding configuration
        var links = getFeature(aHost, feat, ArrayFS.class);
        linkLoop: for (var i = 0; i < links.size(); i++) {
            var link = links.get(i);
            var adapter = getAdapter(aHost.getType().getName());
            var decl = adapter.getLinkFeature(position.getLinkFeature());

            LOG.trace("`-> link {}", decl);

            // Check if this configuration is already present
            Configuration configuration = null;
            switch (position.getLinkFeatureMultiplicityMode()) {
            case ONE_TARGET_MULTIPLE_ROLES: {
                var role = link
                        .getStringValue(link.getType().getFeatureByBaseName(decl.getRoleFeature()));
                if (!Objects.equals(role, position.getLinkRole())) {
                    LOG.trace("    `-> role mismatch", decl);
                    continue linkLoop;
                }

                var target = (Annotation) link.getFeatureValue(
                        link.getType().getFeatureByBaseName(decl.getTargetFeature()));

                cfgLoop: for (var cfg : aSet.getConfigurations()) {
                    var repFS = cfg.getRepresentative(casses);
                    var repAID = cfg.getRepresentativeAID();
                    var repLink = getFeature(repFS,
                            repFS.getType().getFeatureByBaseName(decl.getName()), ArrayFS.class)
                                    .get(repAID.index);
                    var repTarget = (Annotation) repLink.getFeatureValue(
                            repLink.getType().getFeatureByBaseName(decl.getTargetFeature()));

                    // Compare targets
                    if (samePosition(repTarget, target) && equalsFS(aHost, repFS)) {
                        configuration = cfg;
                        LOG.trace("    `-> target position match");
                        LOG.trace("    `-> host match");
                        break cfgLoop;
                    }
                }
                break;
            }
            case MULTIPLE_TARGETS_ONE_ROLE: {
                var target = (AnnotationFS) link.getFeatureValue(
                        link.getType().getFeatureByBaseName(decl.getTargetFeature()));
                if (!(target.getBegin() == position.getLinkTargetBegin()
                        && target.getEnd() == position.getLinkTargetEnd())) {
                    LOG.trace("    `-> target offset mismatch", decl);
                    continue linkLoop;
                }

                var role = link
                        .getStringValue(link.getType().getFeatureByBaseName(decl.getRoleFeature()));

                cfgLoop: for (var cfg : aSet.getConfigurations()) {
                    var repFS = cfg.getRepresentative(casses);
                    var repAID = cfg.getRepresentativeAID();
                    var repLink = getFeature(repFS,
                            repFS.getType().getFeatureByBaseName(decl.getName()), ArrayFS.class)
                                    .get(repAID.index);
                    var linkRole = repLink.getStringValue(
                            repLink.getType().getFeatureByBaseName(decl.getRoleFeature()));

                    // Compare roles
                    if (Objects.equals(role, linkRole) && equalsFS(aHost, repFS)) {
                        configuration = cfg;
                        LOG.trace("    `-> role match: [{}]", linkRole);
                        LOG.trace("    `-> host match");
                        break cfgLoop;
                    }
                }
                break;
            }
            case MULTIPLE_TARGETS_MULTIPLE_ROLES: {
                var target = (Annotation) link.getFeatureValue(
                        link.getType().getFeatureByBaseName(decl.getTargetFeature()));
                if (!(target.getBegin() == position.getLinkTargetBegin()
                        && target.getEnd() == position.getLinkTargetEnd())) {
                    LOG.trace("    `-> target offset mismatch", decl);
                    continue linkLoop;
                }

                var role = link
                        .getStringValue(link.getType().getFeatureByBaseName(decl.getRoleFeature()));
                if (!Objects.equals(role, position.getLinkRole())) {
                    LOG.trace("    `-> role mismatch", decl);
                    continue linkLoop;
                }

                cfgLoop: for (var cfg : aSet.getConfigurations()) {
                    var repFS = cfg.getRepresentative(casses);
                    var repAID = cfg.getRepresentativeAID();
                    var repLink = getFeature(repFS,
                            repFS.getType().getFeatureByBaseName(decl.getName()), ArrayFS.class)
                                    .get(repAID.index);
                    var linkRole = repLink.getStringValue(
                            repLink.getType().getFeatureByBaseName(decl.getRoleFeature()));
                    var repTarget = (Annotation) repLink.getFeatureValue(
                            repLink.getType().getFeatureByBaseName(decl.getTargetFeature()));

                    // Compare role and target
                    if (Objects.equals(role, linkRole) && samePosition(repTarget, target)
                            && equalsFS(aHost, repFS)) {
                        configuration = cfg;
                        LOG.trace("    `-> role match: [{}]", linkRole);
                        LOG.trace("    `-> target position match");
                        LOG.trace("    `-> host match");
                        break cfgLoop;
                    }
                }
                break;
            }
            default:
                throw new IllegalStateException("Unknown link feature multiplicity mode ["
                        + position.getLinkFeatureMultiplicityMode() + "]");
            }

            // Not found, add new one
            if (configuration == null) {
                configuration = new Configuration(position);
                aSet.addConfiguration(configuration);
                LOG.trace("    `-> Link configuration created: {}", configuration);
            }
            else {
                LOG.trace("    `-> Link configuration found  : {}", configuration);
            }

            configuration.add(aCasGroupId, aHost, position.getLinkFeature(), i);
            aSet.addCasGroupId(aCasGroupId);
        }
    }

    private void addBaseConfiguration(ConfigurationSet aSet, String aCasGroupId,
            FeatureStructure aFS)
    {
        // Check if this configuration is already present
        Configuration configuration = null;
        for (var cfg : aSet.getConfigurations()) {
            if (equalsFS(cfg.getRepresentative(casses), aFS)) {
                configuration = cfg;
                break;
            }
        }

        // Not found, add new one
        if (configuration == null) {
            configuration = new Configuration(aSet.getPosition());
            aSet.addConfiguration(configuration);
            LOG.trace("`-> Base configuration created: {}", configuration);
        }
        else {
            LOG.trace("`-> Base configuration found  : {}", configuration);
        }

        configuration.add(aCasGroupId, aFS);
        aSet.addCasGroupId(aCasGroupId);
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
    private boolean equalsFS(FeatureStructure aFS1, FeatureStructure aFS2)
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
        if (aFS1.getCAS() == aFS2.getCAS() && ICasUtil.getAddr(aFS1) == ICasUtil.getAddr(aFS2)) {
            return true;
        }

        var type1 = aFS1.getType();
        var type2 = aFS2.getType();

        // Types must be the same
        if (!type1.getName().equals(type2.getName())) {
            return false;
        }

        var adapter = diffAdapters.get(type1.getName());

        if (adapter == null) {
            LOG.warn("No diff adapter for type [" + type1.getName() + "] -- ignoring!");
            return true;
        }

        var type1Features = type1.getFeatures().stream().map(Feature::getShortName);
        var type2Features = type2.getFeatures().stream().map(Feature::getShortName);

        // Only consider label features. In particular these must not include position features
        // such as begin, end, etc. Mind that the types may come from different CASes at different
        // levels of upgrading, so it could be that the types actually have slightly different
        // features.
        var labelFeatures = adapter.getFeatures();
        var linkFeatures = adapter.getLinkFeatures();
        var sortedFeatures = concat(type1Features, type2Features) //
                .filter(f -> labelFeatures.contains(f) || linkFeatures.contains(f)) //
                .sorted() //
                .distinct() //
                .collect(toCollection(ArrayList::new));

        nextFeature: for (var feature : sortedFeatures) {
            var featureValueEqual = equalsPrimitiveOrMultiValueFeature(aFS1, aFS2, feature);
            if (featureValueEqual != null) {
                if (!featureValueEqual) {
                    return false;
                }

                continue nextFeature;
            }

            if (adapter.isIncludeInDiff(feature)) {
                var lfd = adapter.getLinkFeature(feature);
                var links1 = toMaterializedLinks(aFS1, feature, lfd.getRoleFeature(),
                        lfd.getTargetFeature());
                var links2 = toMaterializedLinks(aFS2, feature, lfd.getRoleFeature(),
                        lfd.getTargetFeature());
                var linksEqual = disjunction(links1, links2).isEmpty();
                if (!linksEqual) {
                    return false;
                }

                continue nextFeature;

                // // #1795 Chili REC: We can/should change CasDiff2 such that it does not recurse
                // into
                // // link features (or rather into any features that are covered by their own
                // // sub-positions). So when when comparing two spans that differ only in their
                // slots
                // // (sub-positions) the main position could still exhibit agreement.
                // if (!equalsNonPrimitiveFeature(aFS1, aFS2, feature)) {
                // return false;
                // }
                //
                // continue nextFeature;
            }
        }

        return true;
    }

    private boolean equalsNonPrimitiveFeature(FeatureStructure aFS1, FeatureStructure aFS2,
            String feature)
    {
        // Must be some kind of feature structure then
        var f1 = aFS1.getType().getFeatureByBaseName(feature);
        var f2 = aFS2.getType().getFeatureByBaseName(feature);
        var valueFS1 = f1 != null ? aFS1.getFeatureValue(f1) : null;
        var valueFS2 = f2 != null ? aFS2.getFeatureValue(f2) : null;

        if (valueFS1 == null && valueFS2 == null) {
            return true;
        }

        if (valueFS1 == null || valueFS2 == null) {
            return false;
        }

        // Ignore the SofaFS - we already checked that the CAS is the same.
        if (valueFS1 instanceof SofaFS) {
            return true;
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
        // single annotation, but we want to consider each link as an annotation
        if (valueFS1 instanceof Annotation ann1 && valueFS1 instanceof Annotation ann2) {
            if (!samePosition(ann1, ann2)) {
                return false;
            }
        }

        // If the feature type is not an annotation we are still in the "feature tier"
        // just dealing with structured features. It is ok to check these deeply.
        if (!equalsFS(valueFS1, valueFS2)) {
            return false;
        }

        return true;
    }

    private Boolean equalsPrimitiveOrMultiValueFeature(FeatureStructure aFS1, FeatureStructure aFS2,
            String feature)
    {
        var f1 = aFS1.getType().getFeatureByBaseName(feature);
        var f2 = aFS2.getType().getFeatureByBaseName(feature);

        var range = (f1 != null) ? f1.getRange() : (f2 != null ? f2.getRange() : null);

        // If none of the feature structures declare the feature, then they must have the same
        // value (no value)
        if (range == null) {
            return true;
        }

        // If both features are declared but their range differs, then the comparison is false
        if (f1 != null && f2 != null && !f1.getRange().equals(f2.getRange())) {
            return false;
        }

        // When we get here, f1 or f2 can still be null
        switch (range.getName()) {
        case TYPE_NAME_STRING_ARRAY: {
            var value1 = f1 != null ? getFeature(aFS1, f1, Set.class) : null;
            if (value1 == null) {
                value1 = emptySet();
            }
            var value2 = f2 != null ? getFeature(aFS2, f2, Set.class) : null;
            if (value2 == null) {
                value2 = emptySet();
            }
            return value1.equals(value2);
        }
        case TYPE_NAME_BOOLEAN: {
            boolean value1 = f1 != null ? aFS1.getBooleanValue(f1) : false;
            boolean value2 = f2 != null ? aFS2.getBooleanValue(f2) : false;
            return value1 == value2;
        }
        case TYPE_NAME_BYTE: {
            byte value1 = f1 != null ? aFS1.getByteValue(f1) : 0;
            byte value2 = f2 != null ? aFS2.getByteValue(f2) : 0;
            return value1 == value2;
        }
        case TYPE_NAME_DOUBLE: {
            double value1 = f1 != null ? aFS1.getDoubleValue(f1) : 0.0d;
            double value2 = f2 != null ? aFS2.getDoubleValue(f2) : 0.0d;
            return value1 == value2;
        }
        case TYPE_NAME_FLOAT: {
            float value1 = f1 != null ? aFS1.getFloatValue(f1) : 0.0f;
            float value2 = f2 != null ? aFS2.getFloatValue(f2) : 0.0f;
            return value1 == value2;
        }
        case TYPE_NAME_INTEGER: {
            int value1 = f1 != null ? aFS1.getIntValue(f1) : 0;
            int value2 = f2 != null ? aFS2.getIntValue(f2) : 0;
            return value1 == value2;
        }
        case TYPE_NAME_LONG: {
            long value1 = f1 != null ? aFS1.getLongValue(f1) : 0l;
            long value2 = f2 != null ? aFS2.getLongValue(f2) : 0l;
            return value1 == value2;
        }
        case TYPE_NAME_SHORT: {
            short value1 = f1 != null ? aFS1.getShortValue(f1) : 0;
            short value2 = f2 != null ? aFS2.getShortValue(f2) : 0;
            return value1 == value2;
        }
        case TYPE_NAME_STRING: {
            var value1 = f1 != null ? aFS1.getStringValue(f1) : null;
            var value2 = f2 != null ? aFS2.getStringValue(f2) : null;

            return Objects.equals(value1, value2);
        }
        }
        return null;
    }

    private boolean samePosition(AnnotationBase aFS1, AnnotationBase aFS2)
    {
        // Null check
        if (aFS1 == null || aFS2 == null) {
            return false;
        }

        // Position check
        var adapter = getAdapter(aFS1.getType().getName());
        var pos1 = adapter.getPosition(aFS1);
        var pos2 = adapter.getPosition(aFS2);

        return pos1.compareTo(pos2) == 0;
    }
}
