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
package de.tudarmstadt.ukp.inception.curation.merge;

import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.ARRAY;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.copyDocumentMetadata;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createDocumentMetadata;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createSentence;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createToken;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.exists;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.isPrimitiveType;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectSentences;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectTokens;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectAt;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.FSUtil.getFeature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.relation.RelationPosition;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.span.SpanPosition;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.events.BulkAnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureTraits;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapter;
import de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.DefaultMergeStrategy;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategy;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationComparisonUtils;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.FeatureFilter;
import de.tudarmstadt.ukp.inception.schema.api.adapter.IllegalFeatureValueException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

/**
 * Do a merge CAS out of multiple user annotations
 */
public class CasMerge
{
    private static final Logger LOG = LoggerFactory.getLogger(CasMerge.class);

    private final AnnotationSchemaService schemaService;
    private final ApplicationEventPublisher eventPublisher;

    private MergeStrategy mergeStrategy = new DefaultMergeStrategy();
    private boolean silenceEvents = false;
    private Map<AnnotationLayer, List<AnnotationFeature>> featureCache = new HashMap<>();
    private Map<String, AnnotationLayer> layerCache;
    private LoadingCache<AnnotationLayer, TypeAdapter> adapterCache;
    private LoadingCache<AnnotationFeature, LinkFeatureTraits> linkTraitsCache;

    public CasMerge(AnnotationSchemaService aSchemaService,
            ApplicationEventPublisher aEventPublisher)
    {
        schemaService = aSchemaService;
        eventPublisher = aEventPublisher;

        layerCache = new HashMap<String, AnnotationLayer>();
        adapterCache = Caffeine.newBuilder() //
                .maximumSize(100) //
                .build(schemaService::getAdapter);
        linkTraitsCache = Caffeine.newBuilder() //
                .maximumSize(100) //
                .build(this::readTraits);
    }

    // Would be better to use this from the LinkFeatureSupport - but I do not want to change the
    // constructor at the moment to inject another dependency.
    private LinkFeatureTraits readTraits(AnnotationFeature aFeature)
    {
        LinkFeatureTraits traits = null;
        try {
            traits = JSONUtil.fromJsonString(LinkFeatureTraits.class, aFeature.getTraits());
        }
        catch (IOException e) {
            LOG.error("Unable to read traits", e);
        }

        if (traits == null) {
            traits = new LinkFeatureTraits();
        }

        return traits;
    }

    public void setSilenceEvents(boolean aSilenceEvents)
    {
        silenceEvents = aSilenceEvents;
    }

    public boolean isSilenceEvents()
    {
        return silenceEvents;
    }

    public void setMergeStrategy(MergeStrategy aMergeStrategy)
    {
        mergeStrategy = aMergeStrategy;
    }

    public MergeStrategy getMergeStrategy()
    {
        return mergeStrategy;
    }

    private List<Configuration> chooseConfigurationsToMerge(AnnotationLayer aLayer,
            DiffResult aDiff, ConfigurationSet cfgs)
    {
        return mergeStrategy.chooseConfigurationsToMerge(aDiff, cfgs, aLayer);
    }

    /**
     * Using {@code DiffResult}, determine the annotations to be deleted from the randomly generated
     * MergeCase. The initial Merge CAs is stored under a name {@code CurationPanel#CURATION_USER}.
     * <p>
     * Any similar annotations stacked in a {@code CasDiff2.Position} will be assumed a difference
     * <p>
     * Any two annotation with different value will be assumed a difference
     * <p>
     * Any non stacked empty/null annotations are assumed agreement
     * <p>
     * Any non stacked annotations with similar values for each of the features are assumed
     * agreement
     * <p>
     * Any two link mode / slotable annotations which agree on the base features are assumed
     * agreement
     *
     * @param aDiff
     *            the {@link DiffResult}
     * @param aTargetDocument
     *            the target document
     * @param aTargetUsername
     *            the annotator user owning the target annotation document
     * @param aTargetCas
     *            the target CAS for the annotation document
     * @param aCasMap
     *            a map of {@code CAS}s for each users and the random merge
     * @return a list of messages representing the result of the merge operation
     * @throws UIMAException
     *             if there was an UIMA-level exception
     */
    public Set<LogMessage> clearAndMergeCas(DiffResult aDiff, SourceDocument aTargetDocument,
            String aTargetUsername, CAS aTargetCas, Map<String, CAS> aCasMap)
        throws UIMAException
    {
        // Remove any annotations from the target CAS - keep type system, sentences and tokens
        clearAnnotations(aTargetDocument, aTargetCas);

        return mergeCas(aDiff, aTargetDocument, aTargetUsername, aTargetCas, aCasMap);
    }

    /**
     * Using {@code DiffResult}, determine the annotations to be deleted from the randomly generated
     * MergeCase. The initial Merge CAs is stored under a name {@code CurationPanel#CURATION_USER}.
     * <p>
     * Any similar annotations stacked in a {@code CasDiff2.Position} will be assumed a difference
     * <p>
     * Any two annotation with different value will be assumed a difference
     * <p>
     * Any non stacked empty/null annotations are assumed agreement
     * <p>
     * Any non stacked annotations with similar values for each of the features are assumed
     * agreement
     * <p>
     * Any two link mode / slotable annotations which agree on the base features are assumed
     * agreement
     *
     * @param aDiff
     *            the {@link DiffResult}
     * @param aTargetDocument
     *            the target document
     * @param aTargetUsername
     *            the annotator user owning the target annotation document
     * @param aTargetCas
     *            the target CAS for the annotation document
     * @param aCasMap
     *            a map of {@code CAS}s for each users and the random merge
     * @return a list of messages representing the result of the merge operation
     * @throws UIMAException
     *             if there was an UIMA-level exception
     */
    public Set<LogMessage> mergeCas(DiffResult aDiff, SourceDocument aTargetDocument,
            String aTargetUsername, CAS aTargetCas, Map<String, CAS> aCasMap)
        throws UIMAException
    {
        silenceEvents = true;

        var updated = 0;
        var created = 0;
        var messages = new LinkedHashSet<LogMessage>();

        // If there is nothing to merge, bail out
        if (aCasMap.isEmpty()) {
            return emptySet();
        }

        // Set up a cache for resolving type to layer to avoid hammering the DB as we process each
        // position
        var type2layer = aDiff.getPositions().stream().map(Position::getType) //
                .distinct() //
                .map(type -> schemaService.findLayer(aTargetDocument.getProject(), type))
                .collect(toMap(AnnotationLayer::getName, identity()));

        var layerNames = new ArrayList<>(type2layer.keySet());

        // Move token layer to front
        if (layerNames.contains(Token.class.getName())) {
            layerNames.remove(Token.class.getName());
            layerNames.add(0, Token.class.getName());
        }

        // Move sentence layer to front
        if (layerNames.contains(Sentence.class.getName())) {
            layerNames.remove(Sentence.class.getName());
            layerNames.add(0, Sentence.class.getName());
        }

        // First we process the SPAN layers since other layers can refer to them (via slot features
        // or as relation layers).
        // We process layer by layer so that we can order the layers (important to process tokens
        // and sentences before the others)
        for (var layerName : layerNames) {
            var spanPositions = aDiff.getPositions().stream()
                    .filter(pos -> layerName.equals(pos.getType()))
                    .filter(pos -> pos instanceof SpanPosition) //
                    .map(pos -> (SpanPosition) pos)
                    // We don't process slot features here (they are span sub-positions)
                    .filter(pos -> pos.getFeature() == null) //
                    .toList();

            if (spanPositions.isEmpty()) {
                continue;
            }

            LOG.debug("Processing [{}] span positions on layer [{}]", spanPositions.size(),
                    layerName);

            // First we merge the spans so that we can attach the relations to something later.
            // Slots are also excluded for the moment
            for (var spanPosition : spanPositions) {
                LOG.trace(" |   processing {}", spanPosition);
                var layer = type2layer.get(spanPosition.getType());
                var cfgs = aDiff.getConfigurationSet(spanPosition);

                for (var cfgToMerge : chooseConfigurationsToMerge(layer, aDiff, cfgs)) {
                    try {
                        var sourceFS = (AnnotationFS) cfgToMerge.getRepresentative(aCasMap);
                        var result = mergeSpanAnnotation(aTargetDocument, aTargetUsername,
                                type2layer.get(spanPosition.getType()), aTargetCas, sourceFS,
                                layer.isAllowStacking());
                        LOG.trace(" `-> merged annotation with agreement");

                        switch (result.getState()) {
                        case CREATED:
                            created++;
                            break;
                        case UPDATED:
                            updated++;
                            break;
                        }
                    }
                    catch (AnnotationException e) {
                        LOG.trace(" `-> not merged annotation: {}", e.getMessage());
                        messages.add(LogMessage.error(this, "%s", e.getMessage()));
                    }
                }
            }
        }

        // After the spans are in place, we can merge the slot features
        for (var layerName : layerNames) {
            var slotPositions = aDiff.getPositions().stream()
                    .filter(pos -> layerName.equals(pos.getType()))
                    .filter(pos -> pos instanceof SpanPosition) //
                    .map(pos -> (SpanPosition) pos)
                    // We only process slot features here
                    .filter(pos -> pos.getFeature() != null) //
                    .toList();

            if (slotPositions.isEmpty()) {
                continue;
            }

            LOG.debug("Processing {} slot positions on layer [{}]", slotPositions.size(),
                    layerName);

            for (var slotPosition : slotPositions) {
                LOG.trace(" |   processing {}", slotPosition);
                var layer = type2layer.get(slotPosition.getType());
                var cfgs = aDiff.getConfigurationSet(slotPosition);

                for (var cfgToMerge : chooseConfigurationsToMerge(layer, aDiff, cfgs)) {
                    try {
                        var sourceFS = (AnnotationFS) cfgToMerge.getRepresentative(aCasMap);
                        var sourceFsAid = cfgs.getConfigurations().get(0).getRepresentativeAID();
                        mergeSlotFeature(aTargetDocument, aTargetUsername,
                                type2layer.get(slotPosition.getType()), aTargetCas, sourceFS,
                                sourceFsAid.feature, sourceFsAid.index);
                        LOG.trace(" `-> merged annotation with agreement");
                    }
                    catch (AnnotationException e) {
                        LOG.trace(" `-> not merged annotation: {}", e.getMessage());
                        messages.add(LogMessage.error(this, "%s", e.getMessage()));
                    }
                }
            }
        }

        // Finally, we merge the relations
        for (var layerName : layerNames) {
            var relationPositions = aDiff.getPositions().stream()
                    .filter(pos -> layerName.equals(pos.getType()))
                    .filter(pos -> pos instanceof RelationPosition)
                    .map(pos -> (RelationPosition) pos) //
                    .collect(Collectors.toList());

            if (relationPositions.isEmpty()) {
                continue;
            }

            LOG.debug("Processing {} relation positions on layer [{}]", relationPositions.size(),
                    layerName);

            for (var relationPosition : relationPositions) {
                LOG.trace(" |   processing {}", relationPosition);
                var layer = type2layer.get(relationPosition.getType());
                var cfgs = aDiff.getConfigurationSet(relationPosition);

                var cfgsToMerge = chooseConfigurationsToMerge(layer, aDiff, cfgs);

                if (cfgsToMerge.isEmpty()) {
                    continue;
                }

                for (var cfgToMerge : cfgsToMerge) {
                    try {
                        var sourceFS = (AnnotationFS) cfgToMerge.getRepresentative(aCasMap);
                        var result = mergeRelationAnnotation(aTargetDocument, aTargetUsername,
                                type2layer.get(relationPosition.getType()), aTargetCas, sourceFS,
                                layer.isAllowStacking());
                        LOG.trace(" `-> merged annotation with agreement");

                        switch (result.getState()) {
                        case CREATED:
                            created++;
                            break;
                        case UPDATED:
                            updated++;
                            break;
                        }
                    }
                    catch (AnnotationException e) {
                        LOG.trace(" `-> not merged annotation: {}", e.getMessage());
                        messages.add(LogMessage.error(this, "%s", e.getMessage()));
                    }
                }
            }
        }

        LOG.trace("Merge complete. Created:  {} Updated: {}", created, updated);

        if (eventPublisher != null) {
            eventPublisher
                    .publishEvent(new BulkAnnotationEvent(this, aTargetDocument, aTargetUsername));
        }

        return messages;
    }

    /**
     * Removes all annotations except {@link Token} and {@link Sentence} annotations - but from
     * these also only the offsets are kept and all other features are cleared.
     * 
     * @param aDocument
     *            the document to which the CAS belongs.
     * @param aCas
     *            the CAS to clear.
     * @throws UIMAException
     *             if there was a problem clearing the CAS.
     */
    private void clearAnnotations(SourceDocument aDocument, CAS aCas) throws UIMAException
    {
        // Copy the CAS - basically we do this just to keep the full type system information
        var backup = WebAnnoCasUtil.createCasCopy(aCas);

        // Remove all annotations from the target CAS but we keep the type system!
        aCas.reset();

        // Copy over essential information
        if (exists(backup, getType(backup, DocumentMetaData.class))) {
            copyDocumentMetadata(backup, aCas);
        }
        else {
            createDocumentMetadata(aCas);
        }

        var casMetadataType = aCas.getTypeSystem().getType(CASMetadata._TypeName);
        if (casMetadataType != null && exists(backup, casMetadataType)) {
            var casMetadata = backup.select(CASMetadata.class).single();
            var timestamp = casMetadata.getLastChangedOnDisk();
            var username = casMetadata.getUsername();
            CasMetadataUtils.addOrUpdateCasMetadata(aCas, timestamp, aDocument, username);
        }

        aCas.setDocumentLanguage(backup.getDocumentLanguage()); // DKPro Core Issue 435
        aCas.setDocumentText(backup.getDocumentText());

        transferSegmentation(aDocument.getProject(), aCas, backup);
    }

    /**
     * If tokens and/or sentences are not editable, then they are not part of the curation process
     * and we transfer them from the template CAS.
     */
    private void transferSegmentation(Project aProject, CAS aCas, CAS backup)
    {
        if (!schemaService.isTokenLayerEditable(aProject)) {
            // Transfer token boundaries
            for (var t : selectTokens(backup)) {
                aCas.addFsToIndexes(createToken(aCas, t.getBegin(), t.getEnd()));
            }
        }

        if (!schemaService.isSentenceLayerEditable(aProject)) {
            // Transfer sentence boundaries
            for (var s : selectSentences(backup)) {
                aCas.addFsToIndexes(createSentence(aCas, s.getBegin(), s.getEnd()));
            }
        }
    }

    private static boolean existsEquivalentAt(CAS aCas, TypeAdapter aAdapter, AnnotationFS aFs)
    {
        var targetType = aCas.getTypeSystem().getType(aFs.getType().getName());
        if (targetType == null) {
            return false;
        }

        return selectAt(aCas, targetType, aFs.getBegin(), aFs.getEnd()).stream() //
                .filter(cand -> aAdapter.equivalents(aFs, cand,
                        (_fs, _f) -> !shouldIgnoreFeatureOnMerge(_f))) //
                .findAny() //
                .isPresent();
    }

    private static List<AnnotationFS> selectCandidateRelationsAt(CAS aTargetCas,
            AnnotationFS aSourceFs, AnnotationFS aSourceOriginFs, AnnotationFS aSourceTargetFs)
    {
        var type = aSourceFs.getType();
        var targetType = aTargetCas.getTypeSystem().getType(aSourceFs.getType().getName());
        if (targetType == null) {
            return emptyList();
        }

        var sourceFeat = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        var targetFeat = type.getFeatureByBaseName(FEAT_REL_TARGET);
        return selectCovered(aTargetCas, targetType, aSourceFs.getBegin(), aSourceFs.getEnd())
                .stream().filter(fs -> fs.getFeatureValue(sourceFeat).equals(aSourceOriginFs)
                        && fs.getFeatureValue(targetFeat).equals(aSourceTargetFs))
                .toList();
    }

    private void copyFeatures(SourceDocument aDocument, String aUsername, TypeAdapter aAdapter,
            FeatureStructure aTargetFS, FeatureStructure aSourceFs)
        throws AnnotationException
    {
        // Cache the feature list instead of hammering the database
        var features = featureCache.computeIfAbsent(aAdapter.getLayer(),
                key -> schemaService.listSupportedFeatures(key));
        for (var feature : features) {
            if (!feature.isCuratable()) {
                continue;
            }

            var sourceFsType = aAdapter.getAnnotationType(aSourceFs.getCAS());
            var sourceFeature = sourceFsType.getFeatureByBaseName(feature.getName());

            if (sourceFeature == null) {
                throw new IllegalStateException("Target CAS type [" + sourceFsType.getName()
                        + "] does not define a feature named [" + feature.getName() + "]");
            }

            if (shouldIgnoreFeatureOnMerge(sourceFeature)) {
                continue;
            }

            var value = aAdapter.getFeatureValue(feature, aSourceFs);

            try {
                aAdapter.setFeatureValue(aDocument, aUsername, aTargetFS.getCAS(),
                        getAddr(aTargetFS), feature, value);
            }
            catch (IllegalArgumentException e) {
                // This happens e.g. if the value we try to set is not in the tagset and the tagset
                // cannot be extended.
                throw new IllegalFeatureValueException("Cannot set value of feature ["
                        + feature.getUiName() + "] to [" + value + "]: " + e.getMessage(), e);
            }
        }
    }

    private static List<AnnotationFS> getCandidateAnnotations(CAS aTargetCas, TypeAdapter aAdapter,
            AnnotationFS aSource)
    {
        var targetType = aTargetCas.getTypeSystem().getType(aSource.getType().getName());
        if (targetType == null) {
            return emptyList();
        }

        return selectCovered(aTargetCas, targetType, aSource.getBegin(), aSource.getEnd()).stream()
                .filter(fs -> aAdapter.equivalents(fs, aSource,
                        (_fs, _f) -> !shouldIgnoreFeatureOnMerge(_f)))
                .toList();
    }

    public CasMergeOperationResult mergeSpanAnnotation(SourceDocument aDocument, String aUsername,
            AnnotationLayer aAnnotationLayer, CAS aTargetCas, AnnotationFS aSourceFs,
            boolean aAllowStacking)
        throws AnnotationException
    {
        var adapter = (SpanAdapter) adapterCache.get(aAnnotationLayer);
        if (silenceEvents) {
            adapter.silenceEvents();
        }

        if (existsEquivalentAt(aTargetCas, adapter, aSourceFs)) {
            throw new AlreadyMergedException(
                    "The annotation already exists in the target document.");
        }

        // a) if stacking allowed add this new annotation to the merge view
        var targetType = CasUtil.getType(aTargetCas, adapter.getAnnotationTypeName());
        var existingAnnos = selectAt(aTargetCas, targetType, aSourceFs.getBegin(),
                aSourceFs.getEnd());
        if (existingAnnos.isEmpty() || aAllowStacking) {
            // Create the annotation via the adapter - this also takes care of attaching to an
            // annotation if necessary
            var mergedSpan = adapter.add(aDocument, aUsername, aTargetCas, aSourceFs.getBegin(),
                    aSourceFs.getEnd());

            var mergedSpanAddr = -1;
            try {
                copyFeatures(aDocument, aUsername, adapter, mergedSpan, aSourceFs);
                mergedSpanAddr = getAddr(mergedSpan);
            }
            catch (AnnotationException e) {
                // If there was an error while setting the features, then we skip the entire
                // annotation
                adapter.delete(aDocument, aUsername, aTargetCas, VID.of(mergedSpan));
                throw e;
            }
            return new CasMergeOperationResult(CasMergeOperationResult.ResultState.CREATED,
                    mergedSpanAddr);
        }
        // b) if stacking is not allowed, modify the existing annotation with this one
        else {
            var annoToUpdate = existingAnnos.get(0);
            copyFeatures(aDocument, aUsername, adapter, annoToUpdate, aSourceFs);
            var mergedSpanAddr = getAddr(annoToUpdate);
            return new CasMergeOperationResult(CasMergeOperationResult.ResultState.UPDATED,
                    mergedSpanAddr);
        }
    }

    public CasMergeOperationResult mergeRelationAnnotation(SourceDocument aDocument,
            String aUsername, AnnotationLayer aAnnotationLayer, CAS aTargetCas,
            AnnotationFS aSourceFs, boolean aAllowStacking)
        throws AnnotationException
    {
        var relationAdapter = (RelationAdapter) adapterCache.get(aAnnotationLayer);
        if (silenceEvents) {
            relationAdapter.silenceEvents();
        }

        if (existsEquivalentAt(aTargetCas, relationAdapter, aSourceFs)) {
            throw new AlreadyMergedException(
                    "The annotation already exists in the target document.");
        }

        var originFsClicked = getFeature(aSourceFs, relationAdapter.getSourceFeatureName(),
                AnnotationFS.class);
        var sourceSpanLayer = layerCache.computeIfAbsent(originFsClicked.getType().getName(),
                typeName -> schemaService.findLayer(aDocument.getProject(), typeName));
        var sourceSpanAdapter = (SpanAdapter) adapterCache.get(sourceSpanLayer);
        var candidateOrigins = getCandidateAnnotations(aTargetCas, sourceSpanAdapter,
                originFsClicked);

        var targetFsClicked = getFeature(aSourceFs, relationAdapter.getTargetFeatureName(),
                AnnotationFS.class);
        var targetSpanLayer = layerCache.computeIfAbsent(targetFsClicked.getType().getName(),
                typeName -> schemaService.findLayer(aDocument.getProject(), typeName));
        var targetSpanAdapter = (SpanAdapter) adapterCache.get(targetSpanLayer);
        var candidateTargets = getCandidateAnnotations(aTargetCas, targetSpanAdapter,
                targetFsClicked);

        // check if target/source exists in the mergeview
        if (candidateOrigins.isEmpty() || candidateTargets.isEmpty()) {
            throw new UnfulfilledPrerequisitesException("Both the source and target annotation"
                    + " must exist in the target document. Please first merge/create them");
        }

        if (candidateOrigins.size() > 1) {
            throw new MergeConflictException(
                    "Stacked sources exist in the target document. Cannot merge this relation.");
        }

        if (candidateTargets.size() > 1) {
            throw new MergeConflictException(
                    "Stacked targets exist in the target document. Cannot merge this relation.");
        }

        var originFs = candidateOrigins.get(0);
        var targetFs = candidateTargets.get(0);

        if (relationAdapter.getAttachFeatureName() != null) {
            var originAttachAnnotation = FSUtil.getFeature(originFs,
                    relationAdapter.getAttachFeatureName(), AnnotationFS.class);
            var targetAttachAnnotation = FSUtil.getFeature(targetFs,
                    relationAdapter.getAttachFeatureName(), AnnotationFS.class);

            if (originAttachAnnotation == null || targetAttachAnnotation == null) {
                throw new UnfulfilledPrerequisitesException(
                        "No annotation to attach to. Cannot merge this relation.");
            }
        }

        var existingAnnos = selectCandidateRelationsAt(aTargetCas, aSourceFs, originFs, targetFs);
        if (existingAnnos.isEmpty() || aAllowStacking) {
            var mergedRelation = relationAdapter.add(aDocument, aUsername, originFs, targetFs,
                    aTargetCas);
            try {
                copyFeatures(aDocument, aUsername, relationAdapter, mergedRelation, aSourceFs);
            }
            catch (AnnotationException e) {
                // If there was an error while setting the features, then we skip the entire
                // annotation
                relationAdapter.delete(aDocument, aUsername, aTargetCas, VID.of(mergedRelation));
            }
            return new CasMergeOperationResult(CasMergeOperationResult.ResultState.CREATED,
                    getAddr(mergedRelation));
        }
        else {
            var mergeTargetFS = existingAnnos.get(0);
            copyFeatures(aDocument, aUsername, relationAdapter, mergeTargetFS, aSourceFs);
            return new CasMergeOperationResult(CasMergeOperationResult.ResultState.UPDATED,
                    getAddr(mergeTargetFS));
        }
    }

    public CasMergeOperationResult mergeSlotFeature(SourceDocument aDocument, String aUsername,
            AnnotationLayer aAnnotationLayer, CAS aTargetCas, AnnotationFS aSourceFs,
            String aSourceFeature, int aSourceSlotIndex)
        throws AnnotationException
    {
        var adapter = adapterCache.get(aAnnotationLayer);
        if (silenceEvents) {
            adapter.silenceEvents();
        }

        var candidateHosts = getCandidateAnnotations(aTargetCas, adapter, aSourceFs);

        if (candidateHosts.size() == 0) {
            throw new UnfulfilledPrerequisitesException(
                    "There is no suitable [" + adapter.getLayer().getUiName() + "] annotation at ["
                            + aSourceFs.getBegin() + "," + aSourceFs.getEnd()
                            + "] into which the link could be merged. Please add one first.");
        }
        var mergeFs = candidateHosts.get(0);
        int liIndex = aSourceSlotIndex;

        var slotFeature = adapter.listFeatures().stream() //
                .filter(f -> aSourceFeature.equals(f.getName())) //
                .findFirst() //
                .orElseThrow(() -> new AnnotationException(
                        "Feature [" + aSourceFeature + "] not found"));

        if (slotFeature.getMultiValueMode() != ARRAY) {
            throw new AnnotationException("Feature [" + aSourceFeature + "] is not a slot feature");
        }

        List<LinkWithRoleModel> sourceLinks = adapter.getFeatureValue(slotFeature, aSourceFs);
        var targets = checkAndGetTargets(aTargetCas,
                selectAnnotationByAddr(aSourceFs.getCAS(), sourceLinks.get(liIndex).targetAddr));

        if (targets.isEmpty()) {
            throw new AnnotationException("No suitable merge target found");
        }

        var newLink = new LinkWithRoleModel(sourceLinks.get(liIndex));
        newLink.targetAddr = getAddr(targets.get(0));

        List<LinkWithRoleModel> links = adapter.getFeatureValue(slotFeature, mergeFs);
        // Override an existing link if no roles are used. If roles are used, then the user may want
        // to link the same target multiple times with different roles - hence we simply add.
        switch (slotFeature.getLinkMode()) {
        case WITH_ROLE:
            var traits = linkTraitsCache.get(slotFeature);
            if (traits.isEnableRoleLabels()) {
                if (links.stream().noneMatch(l -> l.targetAddr == newLink.targetAddr
                        && Objects.equals(l.role, newLink.role))) {
                    links.add(newLink);
                }
                else {
                    throw new AlreadyMergedException(
                            "The slot has already been filled with this annotation in the target document.");
                }
            }
            else {
                var existing = existingLinkWithTarget(newLink, links);
                if (existing != null && existing.equals(newLink)) {
                    throw new AlreadyMergedException(
                            "The slot has already been filled with this annotation in the target document.");
                }
                links.remove(existing);
                links.add(newLink);
            }
            break;
        default:
            throw new AnnotationException("Feature [" + aSourceFeature + "] is not a slot feature");
        }

        adapter.setFeatureValue(aDocument, aUsername, aTargetCas, getAddr(mergeFs), slotFeature,
                links);

        return new CasMergeOperationResult(CasMergeOperationResult.ResultState.UPDATED,
                getAddr(mergeFs));
    }

    private LinkWithRoleModel existingLinkWithTarget(LinkWithRoleModel aLink,
            List<LinkWithRoleModel> aLinks)
    {
        for (var lr : aLinks) {
            if (lr.targetAddr == aLink.targetAddr) {
                return lr;
            }
        }
        return null;
    }

    private static List<AnnotationFS> checkAndGetTargets(CAS aCas, AnnotationFS aOldTarget)
        throws UnfulfilledPrerequisitesException
    {
        var casType = CasUtil.getType(aCas, aOldTarget.getType().getName());
        var targets = selectCovered(aCas, casType, aOldTarget.getBegin(), aOldTarget.getEnd())
                .stream()
                .filter(fs -> AnnotationComparisonUtils.isEquivalentSpanAnnotation(fs, aOldTarget,
                        (FeatureFilter) (_fs, _f) -> !shouldIgnoreFeatureOnMerge(_f)))
                .collect(Collectors.toList());

        if (targets.size() == 0) {
            throw new UnfulfilledPrerequisitesException(
                    "This target annotation do not exist. Copy or create the target first ");
        }

        if (targets.size() > 1) {
            throw new UnfulfilledPrerequisitesException(
                    "There are multiple targets on the mergeview."
                            + " Can not copy this slot annotation.");
        }

        return targets;
    }

    public static boolean shouldIgnoreFeatureOnMerge(Feature aFeature)
    {
        if (aFeature.getRange().isArray()) {
            // Allow multi-value features as long as the value is a primitive (i.e. not a link
            // feature)
            return !aFeature.getRange().getComponentType().isPrimitive();
        }

        return !isPrimitiveType(aFeature.getRange()) || isBasicFeature(aFeature)
                || aFeature.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN)
                || aFeature.getName().equals(CAS.FEATURE_FULL_NAME_END);
    }

    /**
     * Do not check on agreement on Position and SOfa feature - already checked
     * 
     * @param aFeature
     *            a feature
     * 
     * @return if a feature is a basic feature
     */
    private static boolean isBasicFeature(Feature aFeature)
    {
        // FIXME The two parts of this OR statement seem to be redundant. Also the order
        // of the check should be changes such that equals is called on the constant.
        return aFeature.getName().equals(CAS.FEATURE_FULL_NAME_SOFA)
                || aFeature.toString().equals("uima.cas.AnnotationBase:sofa");
    }
}
