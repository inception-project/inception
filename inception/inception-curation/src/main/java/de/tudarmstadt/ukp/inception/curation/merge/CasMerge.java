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

import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.copyDocumentMetadata;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createDocumentMetadata;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createSentence;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createToken;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.exists;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectSentences;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectTokens;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.events.BulkAnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentPosition;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationPosition;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanPosition;
import de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils;
import de.tudarmstadt.ukp.inception.curation.api.Position;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.DefaultMergeStrategy;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategy;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.IllegalFeatureValueException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public class CasMerge
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AnnotationSchemaService schemaService;
    private final ApplicationEventPublisher eventPublisher;

    private final CasMergeContext context;
    private MergeStrategy mergeStrategy = new DefaultMergeStrategy();

    public CasMerge(AnnotationSchemaService aSchemaService,
            ApplicationEventPublisher aEventPublisher)
    {
        schemaService = aSchemaService;
        eventPublisher = aEventPublisher;

        context = new CasMergeContext(schemaService);
    }

    public void setSilenceEvents(boolean aSilenceEvents)
    {
        context.setSilenceEvents(aSilenceEvents);
    }

    public boolean isSilenceEvents()
    {
        return context.isSilenceEvents();
    }

    public void setMergeStrategy(MergeStrategy aMergeStrategy)
    {
        mergeStrategy = aMergeStrategy;
    }

    public MergeStrategy getMergeStrategy()
    {
        return mergeStrategy;
    }

    /**
     * Using {@code DiffResult}, determine the annotations to be deleted from the randomly generated
     * MergeCase. The initial Merge CAS is stored under a name {@code CurationPanel#CURATION_USER}.
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
    public PerCasMergeContext clearAndMergeCas(DiffResult aDiff, SourceDocument aTargetDocument,
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
    public PerCasMergeContext mergeCas(DiffResult aDiff, SourceDocument aTargetDocument,
            String aTargetUsername, CAS aTargetCas, Map<String, CAS> aCasMap)
        throws UIMAException
    {
        context.setSilenceEvents(true);

        var localContext = new PerCasMergeContext();

        // If there is nothing to merge, bail out
        if (aCasMap.isEmpty()) {
            return localContext;
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
        mergeSpanLayers(aDiff, aTargetDocument, aTargetUsername, aTargetCas, aCasMap, localContext,
                type2layer, layerNames);

        // Next we process document metadata
        mergeDocumentMetadataLayers(aDiff, aTargetDocument, aTargetUsername, aTargetCas, aCasMap,
                localContext, type2layer, layerNames);

        // After the spans and document metadata are in place, we can merge the link features
        mergeLinkFeatures(aDiff, aTargetDocument, aTargetUsername, aTargetCas, aCasMap,
                localContext, type2layer, layerNames);

        // Finally, we merge the relations
        mergeRelationLayers(aDiff, aTargetDocument, aTargetUsername, aTargetCas, aCasMap,
                localContext, type2layer, layerNames);

        LOG.trace("Merge complete. Created:  {} Updated: {}", localContext.created,
                localContext.updated);

        if (eventPublisher != null) {
            eventPublisher
                    .publishEvent(new BulkAnnotationEvent(this, aTargetDocument, aTargetUsername));
        }

        return localContext;
    }

    private void mergeDocumentMetadataLayers(DiffResult aDiff, SourceDocument aTargetDocument,
            String aTargetUsername, CAS aTargetCas, Map<String, CAS> aCasMap,
            PerCasMergeContext aLocalContext, Map<String, AnnotationLayer> aType2layer,
            ArrayList<String> aLayerNames)
    {
        for (var layerName : aLayerNames) {
            var positions = aDiff.getPositions().stream()
                    .filter(pos -> layerName.equals(pos.getType()))
                    .filter(pos -> pos instanceof DocumentPosition)
                    .map(pos -> (DocumentPosition) pos) //
                    .toList();

            if (positions.isEmpty()) {
                continue;
            }

            LOG.debug("Processing [{}] document metadata positions on layer [{}]", positions.size(),
                    layerName);

            for (var position : positions) {
                mergeDocumentMetadataPosition(aDiff, aTargetDocument, aTargetUsername, aTargetCas,
                        aCasMap, aLocalContext, aType2layer, position);
            }
        }
    }

    private void mergeDocumentMetadataPosition(DiffResult aDiff, SourceDocument aTargetDocument,
            String aTargetUsername, CAS aTargetCas, Map<String, CAS> aCasMap,
            PerCasMergeContext localContext, Map<String, AnnotationLayer> type2layer,
            DocumentPosition aPosition)
    {
        LOG.trace(" |   processing {}", aPosition);
        var layer = type2layer.get(aPosition.getType());
        var cfgs = aDiff.getConfigurationSet(aPosition);

        for (var cfgsToMerge : mergeStrategy.chooseConfigurationsToMerge(aDiff, cfgs, layer)) {
            var sourceFS = (AnnotationBase) cfgsToMerge.getRepresentative(aCasMap);
            try {
                var result = mergeDocumentAnnotation(aTargetDocument, aTargetUsername,
                        type2layer.get(aPosition.getType()), aTargetCas, sourceFS);

                switch (result.state()) {
                case CREATED:
                    LOG.trace(" `-> merged document annotation [{}] (created) -> [{}]",
                            sourceFS.getAddress(), result.targetAddress());
                    localContext.created++;
                    break;
                case UPDATED:
                    LOG.trace(" `-> merged document annotation [{}] (updated) -> [{}]",
                            sourceFS.getAddress(), result.targetAddress());
                    localContext.updated++;
                    break;
                }
            }
            catch (AnnotationException e) {
                LOG.trace(" `-> not merged document annotation [{}]: {}", sourceFS.getAddress(),
                        e.getMessage());
                localContext.notMerged++;
                localContext.messages.add(LogMessage.error(this, "%s", e.getMessage()));
            }
        }
    }

    private void mergeRelationLayers(DiffResult aDiff, SourceDocument aTargetDocument,
            String aTargetUsername, CAS aTargetCas, Map<String, CAS> aCasMap,
            PerCasMergeContext localContext, Map<String, AnnotationLayer> type2layer,
            List<String> layerNames)
    {
        for (var layerName : layerNames) {
            var relationPositions = aDiff.getPositions().stream()
                    .filter(pos -> layerName.equals(pos.getType()))
                    .filter(pos -> pos instanceof RelationPosition)
                    .map(pos -> (RelationPosition) pos) //
                    .toList();

            if (relationPositions.isEmpty()) {
                continue;
            }

            LOG.debug("Processing {} relation positions on layer [{}]", relationPositions.size(),
                    layerName);

            for (var relationPosition : relationPositions) {
                mergeRelationPosition(aDiff, aTargetDocument, aTargetUsername, aTargetCas, aCasMap,
                        localContext, type2layer, relationPosition);
            }
        }
    }

    private void mergeRelationPosition(DiffResult aDiff, SourceDocument aTargetDocument,
            String aTargetUsername, CAS aTargetCas, Map<String, CAS> aCasMap,
            PerCasMergeContext aLocalContext, Map<String, AnnotationLayer> aType2layer,
            RelationPosition aRelationPosition)
    {
        LOG.trace(" |   processing {}", aRelationPosition);
        var layer = aType2layer.get(aRelationPosition.getType());
        var cfgs = aDiff.getConfigurationSet(aRelationPosition);

        var cfgsToMerge = mergeStrategy.chooseConfigurationsToMerge(aDiff, cfgs, layer);

        for (var cfgToMerge : cfgsToMerge) {
            var sourceFS = (AnnotationFS) cfgToMerge.getRepresentative(aCasMap);
            try {
                var result = mergeRelationAnnotation(aTargetDocument, aTargetUsername,
                        aType2layer.get(aRelationPosition.getType()), aTargetCas, sourceFS);

                switch (result.state()) {
                case CREATED:
                    LOG.trace(" `-> merged relation annotation [{}] (created) -> [{}]",
                            sourceFS.getAddress(), result.targetAddress());
                    aLocalContext.created++;
                    break;
                case UPDATED:
                    LOG.trace(" `-> merged relation annotation [{}] (updated) -> [{}]",
                            sourceFS.getAddress(), result.targetAddress());
                    aLocalContext.updated++;
                    break;
                }
            }
            catch (AnnotationException e) {
                LOG.trace(" `-> not merged relation annotation [{}]: {}", sourceFS.getAddress(),
                        e.getMessage());
                aLocalContext.notMerged++;
                aLocalContext.messages.add(LogMessage.error(this, "%s", e.getMessage()));
            }
        }
    }

    private void mergeLinkFeatures(DiffResult aDiff, SourceDocument aTargetDocument,
            String aTargetUsername, CAS aTargetCas, Map<String, CAS> aCasMap,
            PerCasMergeContext aLocalContext, Map<String, AnnotationLayer> aType2layer,
            List<String> aLayerNames)
    {
        for (var layerName : aLayerNames) {
            var linkPositions = aDiff.getPositions().stream()
                    .filter(pos -> layerName.equals(pos.getType()))
                    .filter(pos -> pos instanceof SpanPosition) //
                    .map(pos -> (SpanPosition) pos)
                    // We only process slot features here
                    .filter(Position::isLinkFeaturePosition) //
                    .toList();

            if (linkPositions.isEmpty()) {
                continue;
            }

            LOG.debug("Processing {} link positions on layer [{}]", linkPositions.size(),
                    layerName);

            for (var slotPosition : linkPositions) {
                mergeLinkPosition(aDiff, aTargetDocument, aTargetUsername, aTargetCas, aCasMap,
                        aLocalContext, aType2layer, slotPosition);
            }
        }
    }

    private void mergeLinkPosition(DiffResult aDiff, SourceDocument aTargetDocument,
            String aTargetUsername, CAS aTargetCas, Map<String, CAS> aCasMap,
            PerCasMergeContext aLocalContext, Map<String, AnnotationLayer> aType2layer,
            SpanPosition aSlotPosition)
    {
        LOG.trace(" |   processing {}", aSlotPosition);
        var layer = aType2layer.get(aSlotPosition.getType());
        var cfgs = aDiff.getConfigurationSet(aSlotPosition);

        var cfgsToMerge = mergeStrategy.chooseConfigurationsToMerge(aDiff, cfgs, layer);
        for (var cfgToMerge : cfgsToMerge) {
            var sourceFS = (AnnotationFS) cfgToMerge.getRepresentative(aCasMap);
            try {
                var sourceFsAid = cfgToMerge.getRepresentativeAID();
                var result = mergeSlotFeature(aTargetDocument, aTargetUsername,
                        aType2layer.get(aSlotPosition.getType()), aTargetCas, sourceFS,
                        sourceFsAid.feature, sourceFsAid.index);

                switch (result.state()) {
                case CREATED:
                    // mergeSlotFeature only returns UPDATED
                    throw new AnnotationException(
                            "mergeSlotFeature should return UPDATED not CREATED");
                case UPDATED:
                    LOG.trace(" `-> merged link [{}] into span [{}]", sourceFS.getAddress(),
                            result.targetAddress());
                    aLocalContext.updated++;
                    break;
                }
            }
            catch (AnnotationException e) {
                LOG.trace(" `-> not merged link [{}]: {}", sourceFS.getAddress(), e.getMessage());
                aLocalContext.notMerged++;
                aLocalContext.messages.add(LogMessage.error(this, "%s", e.getMessage()));
            }
        }
    }

    private void mergeSpanLayers(DiffResult aDiff, SourceDocument aTargetDocument,
            String aTargetUsername, CAS aTargetCas, Map<String, CAS> aCasMap,
            PerCasMergeContext aLocalContext, Map<String, AnnotationLayer> aType2layer,
            List<String> aLayerNames)
    {
        for (var layerName : aLayerNames) {
            var spanPositions = aDiff.getPositions().stream()
                    .filter(pos -> layerName.equals(pos.getType()))
                    .filter(pos -> pos instanceof SpanPosition) //
                    .map(pos -> (SpanPosition) pos)
                    // We don't process slot features here (they are span sub-positions)
                    .filter(pos -> pos.getLinkFeature() == null) //
                    .toList();

            if (spanPositions.isEmpty()) {
                continue;
            }

            LOG.debug("Processing [{}] span positions on layer [{}]", spanPositions.size(),
                    layerName);

            // First we merge the spans so that we can attach the relations/links to them later.
            // Slots are also excluded for the moment
            for (var spanPosition : spanPositions) {
                mergeSpanPosition(aDiff, aTargetDocument, aTargetUsername, aTargetCas, aCasMap,
                        aLocalContext, aType2layer, spanPosition);
            }
        }
    }

    private void mergeSpanPosition(DiffResult aDiff, SourceDocument aTargetDocument,
            String aTargetUsername, CAS aTargetCas, Map<String, CAS> aCasMap,
            PerCasMergeContext localContext, Map<String, AnnotationLayer> type2layer,
            SpanPosition spanPosition)
    {
        LOG.trace(" |   processing {}", spanPosition);
        var layer = type2layer.get(spanPosition.getType());
        var cfgs = aDiff.getConfigurationSet(spanPosition);

        for (var cfgsToMerge : mergeStrategy.chooseConfigurationsToMerge(aDiff, cfgs, layer)) {
            var sourceFS = (AnnotationFS) cfgsToMerge.getRepresentative(aCasMap);
            try {
                var result = mergeSpanAnnotation(aTargetDocument, aTargetUsername,
                        type2layer.get(spanPosition.getType()), aTargetCas, sourceFS);

                switch (result.state()) {
                case CREATED:
                    LOG.trace(" `-> merged span annotation [{}] (created) -> [{}]",
                            sourceFS.getAddress(), result.targetAddress());
                    localContext.created++;
                    break;
                case UPDATED:
                    LOG.trace(" `-> merged span annotation [{}] (updated) -> [{}]",
                            sourceFS.getAddress(), result.targetAddress());
                    localContext.updated++;
                    break;
                }
            }
            catch (AnnotationException e) {
                LOG.trace(" `-> not merged span annotation [{}]: {}", sourceFS.getAddress(),
                        e.getMessage());
                localContext.notMerged++;
                localContext.messages.add(LogMessage.error(this, "%s", e.getMessage()));
            }
        }
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
    private void transferSegmentation(Project aProject, CAS aTarget, CAS aSource)
    {
        if (!schemaService.isTokenLayerEditable(aProject)) {
            // Transfer token boundaries
            for (var t : selectTokens(aSource)) {
                aTarget.addFsToIndexes(createToken(aTarget, t.getBegin(), t.getEnd()));
            }
        }

        if (!schemaService.isSentenceLayerEditable(aProject)) {
            // Transfer sentence boundaries
            for (var s : selectSentences(aSource)) {
                aTarget.addFsToIndexes(createSentence(aTarget, s.getBegin(), s.getEnd()));
            }
        }
    }

    static void copyFeatures(CasMergeContext aContext, SourceDocument aDocument, String aUsername,
            TypeAdapter aAdapter, FeatureStructure aTargetFS, FeatureStructure aSourceFs)
        throws AnnotationException
    {
        // Cache the feature list instead of hammering the database
        var features = aContext.listSupportedFeatures(aAdapter.getLayer());
        for (var feature : features) {
            if (!feature.isCuratable()) {
                continue;
            }

            var sourceFsType = aAdapter.getAnnotationType(aSourceFs.getCAS()).get();
            var sourceFeature = sourceFsType.getFeatureByBaseName(feature.getName());

            if (sourceFeature == null) {
                throw new IllegalStateException("Target CAS type [" + sourceFsType.getName()
                        + "] does not define a feature named [" + feature.getName() + "]");
            }

            if (!aAdapter.getFeatureSupport(feature.getName())
                    .map(fs -> fs.isCopyOnCurationMerge(feature)).orElse(false)) {
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

    public CasMergeOperationResult mergeSlotFeature(SourceDocument aDoc, String aSrcUser,
            AnnotationLayer aLayer, CAS aTargetCas, AnnotationFS aSourceAnnotation, String aFeature,
            int aSlot)
        throws AnnotationException
    {
        return CasMergeLinkFeature.mergeSlotFeature(context, aDoc, aSrcUser, aLayer, aTargetCas,
                aSourceAnnotation, aFeature, aSlot);
    }

    public CasMergeOperationResult mergeRelationAnnotation(SourceDocument aDoc, String aSrcUser,
            AnnotationLayer aLayer, CAS aTargetCas, AnnotationFS aSourceAnnotation)
        throws AnnotationException
    {
        return CasMergeRelation.mergeRelationAnnotation(context, aDoc, aSrcUser, aLayer, aTargetCas,
                aSourceAnnotation, aLayer.isAllowStacking());
    }

    public CasMergeOperationResult mergeSpanAnnotation(SourceDocument aDoc, String aSrcUser,
            AnnotationLayer aLayer, CAS aTargetCas, AnnotationFS aSourceAnnotation)
        throws AnnotationException
    {
        return CasMergeSpan.mergeSpanAnnotation(context, aDoc, aSrcUser, aLayer, aTargetCas,
                aSourceAnnotation, aLayer.isAllowStacking());
    }

    public CasMergeOperationResult mergeDocumentAnnotation(SourceDocument aDoc, String aSrcUser,
            AnnotationLayer aLayer, CAS aTargetCas, AnnotationBase aSourceAnnotation)
        throws AnnotationException
    {
        return CasMergeDocument.mergeDocumentAnnotation(context, aDoc, aSrcUser, aLayer, aTargetCas,
                aSourceAnnotation);
    }
}
