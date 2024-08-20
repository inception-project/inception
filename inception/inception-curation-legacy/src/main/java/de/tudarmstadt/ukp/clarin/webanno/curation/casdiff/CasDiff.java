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

import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.NONE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Stream.concat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.docmeta.DocumentMetadataDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.internal.AID;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.relation.RelationDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.span.SpanDiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;

public class CasDiff
{
    private final static Logger LOG = LoggerFactory.getLogger(CasDiff.class);

    Map<String, CAS> cases = new LinkedHashMap<>();

    final Map<Position, ConfigurationSet> configSets = new TreeMap<>();

    private int begin;

    private int end;

    private final Map<String, DiffAdapter> diffAdapters = new HashMap<>();

    private final LinkCompareBehavior linkCompareBehavior;

    private boolean recurseIntoLinkFeatures = false;

    private CasDiff(int aBegin, int aEnd, Iterable<? extends DiffAdapter> aAdapters,
            LinkCompareBehavior aLinkCompareBehavior)
    {
        begin = aBegin;
        end = aEnd;
        linkCompareBehavior = aLinkCompareBehavior;

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
     * @param aLinkCompareBehavior
     *            the link comparison mode
     * @param aCasMap
     *            a set of CASes, each associated with an ID
     * @return a diff result.
     */
    public static CasDiff doDiff(Iterable<? extends DiffAdapter> aAdapters,
            LinkCompareBehavior aLinkCompareBehavior, Map<String, CAS> aCasMap)
    {
        return doDiff(aAdapters, aLinkCompareBehavior, aCasMap, -1, -1);
    }

    /**
     * Calculate the differences between CASes. This method scopes the calculation of differences to
     * a span instead of calculating them on the whole text.
     * 
     * @param aAdapters
     *            a set of diff adapters telling how the diff algorithm should handle different
     *            features
     * @param aLinkCompareBehavior
     *            the link comparison mode
     * @param aCasMap
     *            a set of CASes, each associated with an ID
     * @param aBegin
     *            begin of the span for which differences should be calculated.
     * @param aEnd
     *            end of the span for which differences should be calculated.
     * @return a diff.
     */
    public static CasDiff doDiff(Iterable<? extends DiffAdapter> aAdapters,
            LinkCompareBehavior aLinkCompareBehavior, Map<String, CAS> aCasMap, int aBegin,
            int aEnd)
    {
        if (aCasMap.isEmpty()) {
            return new CasDiff(0, 0, aAdapters, aLinkCompareBehavior);
        }

        var startTime = System.currentTimeMillis();

        var diff = new CasDiff(aBegin, aEnd, aAdapters, aLinkCompareBehavior);

        for (var e : aCasMap.entrySet()) {
            var cas = e.getValue();
            var casGroup = e.getKey();
            for (var adapter : aAdapters) {
                // null elements in the list can occur if a user has never worked on a CAS
                diff.addCas(casGroup, cas != null ? cas : null, adapter.getType());
            }
        }

        LOG.trace("CASDiff completed in {} ms", System.currentTimeMillis() - startTime);

        return diff;
    }

    private DiffAdapter getAdapter(String aType)
    {
        var adapter = diffAdapters.get(aType);
        if (adapter == null) {
            LOG.warn("No diff adapter for type [" + aType + "] -- treating as without features");
            adapter = new SpanDiffAdapter(aType, emptySet());
            diffAdapters.put(aType, adapter);
        }
        return adapter;
    }

    public Map<String, DiffAdapter> getTypeAdapters()
    {
        return diffAdapters;
    }

    public Map<String, CAS> getCasMap()
    {
        return cases;
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
        cases.put(aCasGroupId, aCas);

        // null elements in the list can occur if a user has never worked on a CAS
        // We add these to the internal list above, but then we bail out here.
        if (aCas == null) {
            LOG.debug("CAS group [" + aCasGroupId + "] does not contain a CAS");
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing CAS group [" + aCasGroupId + "]");

            String collectionId = null;
            String documentId = null;
            try {
                var dmd = WebAnnoCasUtil.getDocumentMetadata(aCas);
                collectionId = FSUtil.getFeature(dmd, "collectionId", String.class);
                documentId = FSUtil.getFeature(dmd, "documentId", String.class);
                LOG.debug("User [" + collectionId + "] - Document [" + documentId + "]");
            }
            catch (IllegalArgumentException e) {
                // We use this information only for debugging - so we can ignore if the information
                // is missing.
            }
        }

        var type = aCas.getTypeSystem().getType(aType);
        if (type == null) {
            LOG.debug("CAS group [" + aCasGroupId + "] contains no annotations of type [" + aType
                    + "]");
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
            LOG.debug("CAS group [" + aCasGroupId + "] contains no annotations of type [" + aType
                    + "]");
            return;
        }

        LOG.debug("CAS group [" + aCasGroupId + "] contains [" + annotations.size()
                + "] annotations of type [" + aType + "]");

        var posBefore = configSets.keySet().size();
        LOG.debug("Positions before: [{}]", posBefore);

        for (var fs : annotations) {
            var positions = new ArrayList<Position>();

            // Get/create configuration set at the current position
            positions.add(adapter.getPosition(fs));

            // Generate secondary positions for multi-link features
            positions.addAll(adapter.generateSubPositions(fs, linkCompareBehavior));

            for (var pos : positions) {
                var configSet = configSets.get(pos);
                if (configSet == null) {
                    configSet = new ConfigurationSet(pos);
                    configSets.put(pos, configSet);
                }

                assert pos.getClass() == configSet.getPosition()
                        .getClass() : "Position type mismatch [" + pos.getClass() + "] vs ["
                                + configSet.getPosition().getClass() + "]";

                // Merge FS into current set
                addConfiguration(configSet, aCasGroupId, fs);
            }
        }

        LOG.debug("Positions after: [{}] (delta: {})", configSets.keySet().size(),
                (configSets.keySet().size() - posBefore));
    }

    private void addConfiguration(ConfigurationSet aSet, String aCasGroupId, FeatureStructure aFS)
    {
        if (aFS instanceof SofaFS) {
            return;
        }

        var position = aSet.getPosition();
        if (position.getFeature() == null) {
            // Check if this configuration is already present
            Configuration configuration = null;
            for (var cfg : aSet.getConfigurations()) {
                // Handle main positions
                if (equalsFS(cfg.getRepresentative(cases), aFS)) {
                    configuration = cfg;
                    break;
                }
            }

            // Not found, add new one
            if (configuration == null) {
                configuration = new Configuration(position);
                aSet.addConfiguration(configuration);
            }

            configuration.add(aCasGroupId, aFS);
        }
        else {
            Feature feat = aFS.getType().getFeatureByBaseName(position.getFeature());

            // If the CAS has not been upgraded yet to include the feature, then there are no
            // configurations for it.
            if (feat == null) {
                return;
            }

            // For each slot at the given position in the FS-to-be-added, we need find a
            // corresponding configuration

            var links = FSUtil.getFeature(aFS, feat, ArrayFS.class);
            for (var i = 0; i < links.size(); i++) {
                var link = links.get(i);
                var adapter = getAdapter(aFS.getType().getName());
                var decl = adapter.getLinkFeature(position.getFeature());

                // Check if this configuration is already present
                Configuration configuration = null;
                switch (position.getLinkCompareBehavior()) {
                case LINK_TARGET_AS_LABEL: {
                    String role = link.getStringValue(
                            link.getType().getFeatureByBaseName(decl.getRoleFeature()));
                    if (!role.equals(position.getRole())) {
                        continue;
                    }

                    AnnotationFS target = (AnnotationFS) link.getFeatureValue(
                            link.getType().getFeatureByBaseName(decl.getTargetFeature()));

                    cfgLoop: for (Configuration cfg : aSet.getConfigurations()) {
                        FeatureStructure repFS = cfg.getRepresentative(cases);
                        AID repAID = cfg.getRepresentativeAID();
                        FeatureStructure repLink = FSUtil.getFeature(repFS,
                                repFS.getType().getFeatureByBaseName(decl.getName()), ArrayFS.class)
                                .get(repAID.index);
                        AnnotationFS repTarget = (AnnotationFS) repLink.getFeatureValue(
                                repLink.getType().getFeatureByBaseName(decl.getTargetFeature()));

                        // Compare targets
                        if (equalsAnnotationFS(repTarget, target)) {
                            configuration = cfg;
                            break cfgLoop;
                        }
                    }
                    break;
                }
                case LINK_ROLE_AS_LABEL: {
                    AnnotationFS target = (AnnotationFS) link.getFeatureValue(
                            link.getType().getFeatureByBaseName(decl.getTargetFeature()));
                    if (!(target.getBegin() == position.getLinkTargetBegin()
                            && target.getEnd() == position.getLinkTargetEnd())) {
                        continue;
                    }

                    String role = link.getStringValue(
                            link.getType().getFeatureByBaseName(decl.getRoleFeature()));

                    cfgLoop: for (Configuration cfg : aSet.getConfigurations()) {
                        FeatureStructure repFS = cfg.getRepresentative(cases);
                        AID repAID = cfg.getRepresentativeAID();
                        FeatureStructure repLink = FSUtil.getFeature(repFS,
                                repFS.getType().getFeatureByBaseName(decl.getName()), ArrayFS.class)
                                .get(repAID.index);
                        String linkRole = repLink.getStringValue(
                                repLink.getType().getFeatureByBaseName(decl.getRoleFeature()));

                        // Compare roles
                        if (role.equals(linkRole)) {
                            configuration = cfg;
                            break cfgLoop;
                        }
                    }
                    break;
                }
                default:
                    throw new IllegalStateException(
                            "Unknown link target comparison mode [" + linkCompareBehavior + "]");
                }

                // Not found, add new one
                if (configuration == null) {
                    configuration = new Configuration(position);
                    aSet.addConfiguration(configuration);
                }

                configuration.add(aCasGroupId, aFS, position.getFeature(), i);
            }
        }

        aSet.addCasGroupId(aCasGroupId);
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

        // Only consider label features. In particular these must not include position features
        // such as begin, end, etc. Mind that the types may come from different CASes at different
        // levels of upgrading, so it could be that the types actually have slightly different
        // features.
        var labelFeatures = adapter.getLabelFeatures();
        var sortedFeatures = concat(type1.getFeatures().stream().map(Feature::getShortName),
                type2.getFeatures().stream().map(Feature::getShortName)) //
                        .filter(labelFeatures::contains) //
                        .sorted() //
                        .distinct() //
                        .collect(toCollection(ArrayList::new));

        if (!recurseIntoLinkFeatures) {
            // #1795 Chili REC: We can/should change CasDiff2 such that it does not recurse into
            // link features (or rather into any features that are covered by their own
            // sub-positions). So when when comparing two spans that differ only in their slots
            // (sub-positions) the main position could still exhibit agreement.
            sortedFeatures.removeIf(f -> adapter.getLinkFeature(f) != null);
        }

        nextFeature: for (var feature : sortedFeatures) {
            var f1 = type1.getFeatureByBaseName(feature);
            var f2 = type2.getFeatureByBaseName(feature);

            Type range = (f1 != null) ? f1.getRange() : (f2 != null ? f2.getRange() : null);

            // If both feature structures do not declare the feature, then they must have the same
            // value (no value)
            if (range == null) {
                continue nextFeature;
            }

            // If both features are declared but their range differs, then the comparison is false
            if (f1 != null && f2 != null && !f1.getRange().equals(f2.getRange())) {
                return false;
            }

            // When we get here, f1 or f2 can still be null

            switch (range.getName()) {
            case CAS.TYPE_NAME_STRING_ARRAY: {
                var value1 = f1 != null ? FSUtil.getFeature(aFS1, f1, Set.class) : null;
                if (value1 == null) {
                    value1 = emptySet();
                }
                var value2 = f2 != null ? FSUtil.getFeature(aFS2, f2, Set.class) : null;
                if (value2 == null) {
                    value2 = emptySet();
                }
                if (!value1.equals(value2)) {
                    return false;
                }
                break;
            }
            case CAS.TYPE_NAME_BOOLEAN: {
                boolean value1 = f1 != null ? aFS1.getBooleanValue(f1) : false;
                boolean value2 = f2 != null ? aFS2.getBooleanValue(f2) : false;

                if (value1 != value2) {
                    return false;
                }
                break;
            }
            case CAS.TYPE_NAME_BYTE: {
                byte value1 = f1 != null ? aFS1.getByteValue(f1) : 0;
                byte value2 = f2 != null ? aFS2.getByteValue(f2) : 0;

                if (value1 != value2) {
                    return false;
                }
                break;
            }
            case CAS.TYPE_NAME_DOUBLE: {
                double value1 = f1 != null ? aFS1.getDoubleValue(f1) : 0.0d;
                double value2 = f2 != null ? aFS2.getDoubleValue(f2) : 0.0d;

                if (value1 != value2) {
                    return false;
                }
                break;
            }
            case CAS.TYPE_NAME_FLOAT: {
                float value1 = f1 != null ? aFS1.getFloatValue(f1) : 0.0f;
                float value2 = f2 != null ? aFS2.getFloatValue(f2) : 0.0f;

                if (value1 != value2) {
                    return false;
                }
                break;
            }
            case CAS.TYPE_NAME_INTEGER: {
                int value1 = f1 != null ? aFS1.getIntValue(f1) : 0;
                int value2 = f2 != null ? aFS2.getIntValue(f2) : 0;

                if (value1 != value2) {
                    return false;
                }
                break;
            }
            case CAS.TYPE_NAME_LONG: {
                long value1 = f1 != null ? aFS1.getLongValue(f1) : 0l;
                long value2 = f2 != null ? aFS2.getLongValue(f2) : 0l;

                if (value1 != value2) {
                    return false;
                }
                break;
            }
            case CAS.TYPE_NAME_SHORT: {
                short value1 = f1 != null ? aFS1.getShortValue(f1) : 0;
                short value2 = f2 != null ? aFS2.getShortValue(f2) : 0;

                if (value1 != value2) {
                    return false;
                }
                break;
            }
            case CAS.TYPE_NAME_STRING: {
                String value1 = f1 != null ? aFS1.getStringValue(f1) : null;
                String value2 = f2 != null ? aFS2.getStringValue(f2) : null;

                if (!StringUtils.equals(value1, value2)) {
                    return false;
                }
                break;
            }
            default: {
                // Must be some kind of feature structure then
                var valueFS1 = f1 != null ? aFS1.getFeatureValue(f1) : null;
                var valueFS2 = f2 != null ? aFS2.getFeatureValue(f2) : null;

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
                // single annotation, but we want to consider each link as an annotation
                var ts1 = aFS1.getCAS().getTypeSystem();
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
        var adapter = getAdapter(aFS1.getType().getName());
        var pos1 = adapter.getPosition(aFS1);
        var pos2 = adapter.getPosition(aFS2);

        return pos1.compareTo(pos2) == 0;
    }

    public static List<DiffAdapter> getDiffAdapters(AnnotationSchemaService schemaService,
            Collection<AnnotationLayer> aLayers)
    {
        if (aLayers.isEmpty()) {
            return emptyList();
        }

        var project = aLayers.iterator().next().getProject();

        var featuresByLayer = schemaService.listSupportedFeatures(project).stream() //
                .collect(groupingBy(AnnotationFeature::getLayer));

        var adapters = new ArrayList<DiffAdapter>();
        nextLayer: for (var layer : aLayers) {
            if (!layer.isEnabled()) {
                continue nextLayer;
            }

            var labelFeatures = new LinkedHashSet<String>();
            nextFeature: for (var f : featuresByLayer.getOrDefault(layer, emptyList())) {
                if (!f.isEnabled() || !f.isCuratable()) {
                    continue nextFeature;
                }

                // Link features are treated separately from primitive label features
                if (!NONE.equals(f.getLinkMode())) {
                    continue nextFeature;
                }

                labelFeatures.add(f.getName());
            }

            DiffAdapter_ImplBase adapter;
            switch (layer.getType()) {
            case SpanLayerSupport.TYPE: {
                adapter = new SpanDiffAdapter(layer.getName(), labelFeatures);
                break;
            }
            case RelationLayerSupport.TYPE: {
                var typeAdpt = (RelationAdapter) schemaService.getAdapter(layer);
                adapter = new RelationDiffAdapter(layer.getName(), typeAdpt.getSourceFeatureName(),
                        typeAdpt.getTargetFeatureName(), labelFeatures);
                break;
            }
            case DocumentMetadataLayerSupport.TYPE: {
                adapter = new DocumentMetadataDiffAdapter(layer.getName(), labelFeatures);
                break;
            }
            default:
                LOG.debug("Curation for layer type [{}] not supported - ignoring", layer.getType());
                continue nextLayer;
            }

            adapters.add(adapter);

            nextFeature: for (var f : featuresByLayer.getOrDefault(layer, emptyList())) {
                if (!f.isEnabled()) {
                    continue nextFeature;
                }

                switch (f.getLinkMode()) {
                case NONE:
                    // Nothing to do here
                    break;
                case SIMPLE:
                    adapter.addLinkFeature(f.getName(), f.getLinkTypeRoleFeatureName(), null);
                    break;
                case WITH_ROLE:
                    adapter.addLinkFeature(f.getName(), f.getLinkTypeRoleFeatureName(),
                            f.getLinkTypeTargetFeatureName());
                    break;
                default:
                    throw new IllegalStateException("Unknown link mode [" + f.getLinkMode() + "]");
                }

                labelFeatures.add(f.getName());
            }
        }

        // If the token/sentence layer is not editable, we do not offer curation of the tokens.
        // Instead the tokens are obtained from a random template CAS when initializing the CAS - we
        // assume here that the tokens have never been modified.
        if (!schemaService.isSentenceLayerEditable(project)) {
            adapters.removeIf(adapter -> Sentence._TypeName.equals(adapter.getType()));
        }

        if (!schemaService.isTokenLayerEditable(project)) {
            adapters.removeIf(adapter -> Token._TypeName.equals(adapter.getType()));
        }

        return adapters;
    }

    public DiffResult toResult()
    {
        return new DiffResult(this);
    }

    // private Set<String> entryTypes = new LinkedHashSet<>();

    // /**
    // * Clear the attachment to CASes allowing the class to be serialized.
    // */
    // public void detach()
    // {
    // if (cases != null) {
    // cases.clear();
    // }
    // }

    // /**
    // * Rebuilds the diff with the current offsets and entry types. This can be used to fix the
    // diff
    // * after reattaching to CASes that have changed. Mind that the diff results can be different
    // * due to the changes.
    // */
    // public void rebuild()
    // {
    // Map<String, CAS> oldCases = cases;
    // cases = new HashMap<>();
    //
    // for (String t : entryTypes) {
    // for (Entry<String, CAS> e : oldCases.entrySet()) {
    // addCas(e.getKey(), e.getValue(), t);
    // }
    // }
    // }

    // /**
    // * Attach CASes back so that representatives can be resolved. CASes must not have been changed
    // * or upgraded between detaching and reattaching - the CAS addresses of the feature structures
    // * must still be the same.
    // */
    // public void attach(Map<String, CAS> aCases)
    // {
    // cases = new HashMap<>(aCases);
    // }
}
