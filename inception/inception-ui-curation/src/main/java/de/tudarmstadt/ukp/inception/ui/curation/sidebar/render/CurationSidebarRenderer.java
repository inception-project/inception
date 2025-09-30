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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar.render;

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.inception.rendering.Renderer.REL_EXTENSION_ID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.apache.uima.cas.text.AnnotationPredicates.colocated;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.internal.AID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanPosition;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderStep;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VComment;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VCommentType;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VObject;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.CurationSidebarService;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.config.CurationSidebarAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationSidebarAutoConfiguration#curationSidebarRenderer}.
 * </p>
 */
@Order(RenderStep.RENDER_SYNTHETIC_STRUCTURE)
public class CurationSidebarRenderer
    implements RenderStep
{
    public static final String ID = "CurationRenderer";

    private static final String COLOR = "#ccccff";

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CurationSidebarService curationService;
    private final LayerSupportRegistry layerSupportRegistry;
    private final DocumentService documentService;
    private final UserDao userRepository;
    private final AnnotationSchemaService annotationService;
    private final DiffAdapterRegistry diffAdapterRegistry;

    public CurationSidebarRenderer(CurationSidebarService aCurationService,
            LayerSupportRegistry aLayerSupportRegistry, DocumentService aDocumentService,
            UserDao aUserRepository, AnnotationSchemaService aAnnotationService,
            DiffAdapterRegistry aDiffAdapterRegistry)
    {
        curationService = aCurationService;
        layerSupportRegistry = aLayerSupportRegistry;
        documentService = aDocumentService;
        userRepository = aUserRepository;
        annotationService = aAnnotationService;
        diffAdapterRegistry = aDiffAdapterRegistry;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public boolean accepts(RenderRequest aRequest)
    {
        var project = aRequest.getProject();
        var state = aRequest.getState();

        // do not show predictions on the decicated curation page
        if (state != null && state.getMode() != ANNOTATION) {
            return false;
        }

        if (aRequest.getCas() == null) {
            return false;
        }

        var sessionOwner = userRepository.getCurrentUsername();
        if (!curationService.existsSession(sessionOwner, project.getId())) {
            return false;
        }

        return true;
    }

    @Override
    public void render(VDocument aVdoc, RenderRequest aRequest)
    {
        var sessionOwner = userRepository.getCurrentUsername();

        var selectedUsers = curationService.listUsersReadyForCuration(sessionOwner,
                aRequest.getProject(), aRequest.getSourceDocument());
        if (selectedUsers.isEmpty()) {
            return;
        }

        var casDiff = createDiff(aRequest, selectedUsers);
        renderDiff(aVdoc, aRequest, casDiff);
    }

    private void renderDiff(VDocument aVdoc, RenderRequest aRequest, CasDiff aCasDiff)
    {
        var project = aRequest.getProject();
        var sessionOwner = userRepository.getCurrentUsername();
        var targetUser = aRequest.getAnnotationUser().getUsername();

        var diff = aCasDiff.toResult();
        var totalAnnotatorCount = diff.getCasGroupIds().stream() //
                .filter($ -> !$.equals(targetUser)) //
                .count();

        // Listing the features once is faster than repeatedly hitting the DB to list features for
        // every layer.
        var supportedFeatures = annotationService.listSupportedFeatures(project);
        var allFeatures = annotationService.listAnnotationFeature(project);

        // Set up a cache for resolving type to layer to avoid hammering the DB as we process each
        // position
        var type2layer = aRequest.getAllLayers().stream() //
                .collect(toMap(AnnotationLayer::getName, identity()));

        var generatedCurationVids = new HashSet<VID>();
        var showAll = curationService.isShowAll(sessionOwner, project.getId());
        var showScore = curationService.isShowScore(sessionOwner, project.getId());
        for (var cfgSet : diff.getConfigurationSets()) {
            LOG.trace("Processing set: {}", cfgSet);

            if (!showAll && cfgSet.getCasGroupIds().contains(targetUser)) {
                // Hide configuration sets where the curator has already curated (likely)
                continue;
            }

            var layer = type2layer.get(cfgSet.getPosition().getType());

            var layerSupportedFeatures = supportedFeatures.stream() //
                    .filter(feature -> feature.getLayer().equals(layer)) //
                    .toList();
            var layerAllFeatures = allFeatures.stream() //
                    .filter(feature -> feature.getLayer().equals(layer)) //
                    .toList();

            for (var cfg : cfgSet.getConfigurations()) {
                LOG.trace("Processing configuration: {}", cfg);
                var fs = cfg.getRepresentative(aCasDiff.getCasMap());
                if (!(fs instanceof Annotation)) {
                    continue;
                }

                var ann = (Annotation) fs;

                // Do not render configurations that belong only the the target user. Those are
                // already rendered by the normal annotation rendering mechanism
                if (cfg.getCasGroupIds().size() == 1 && cfg.getCasGroupIds().contains(targetUser)) {
                    LOG.trace(
                            "{} - skipping rendering because configuration induced only by target user",
                            cfg.getPosition());
                    continue;
                }

                var user = cfg.getRepresentativeCasGroupId();

                // We need to pass in *all* the annotation features here because we also to that in
                // other places where we create renderers - and the set of features must always be
                // the same because otherwise the IDs of armed slots would be inconsistent
                var layerSupport = layerSupportRegistry.getLayerSupport(layer);
                var renderer = layerSupport.createRenderer(layer, () -> layerAllFeatures);

                var objects = renderer.render(aRequest, layerSupportedFeatures, aVdoc, ann);

                for (var object : objects) {
                    if (!showAll && shouldBeHidden(aRequest, diff, cfg, layer, ann, object,
                            targetUser)) {
                        continue;
                    }

                    // Check if the object has already been rendered
                    var curationVid = new CurationVID(user, object.getVid());
                    if (generatedCurationVids.contains(curationVid)) {
                        continue;
                    }

                    generatedCurationVids.add(curationVid);

                    object.setVid(curationVid);
                    object.setColorHint(COLOR);
                    if (showScore) {
                        var localAnnotatorCount = cfg.getCasGroupIds().stream() //
                                .filter($ -> !$.equals(targetUser)) //
                                .count();

                        var score = (double) localAnnotatorCount / (double) totalAnnotatorCount;
                        object.setScore(score);
                    }
                    aVdoc.add(object);

                    aVdoc.add(new VComment(object.getVid(), VCommentType.INFO,
                            "Annotators: " + cfg.getCasGroupIds().stream()
                                    .filter(a -> !targetUser.equals(a)).collect(joining(", "))));

                    if (object instanceof VArc arc) {
                        resolveArcEndpoints(targetUser, diff, showAll, cfg, arc);
                        LOG.trace("Rendered arc: {} source: [{}] target: [{}]", arc,
                                arc.getSource(), arc.getTarget());
                    }
                    else {
                        LOG.trace("Rendered span: {}", object);
                    }
                }
            }
        }
    }

    private boolean shouldBeHidden(RenderRequest aRequest, DiffResult diff, Configuration cfg,
            AnnotationLayer aLayer, Annotation aAnn, VObject object, String targetUser)
    {
        // if (cfg.getPosition() instanceof SpanPosition spanPosition) {
        // if (spanPosition.isLinkFeaturePosition() && object instanceof VSpan) {
        // // When processing a slot position, do not render the origin span because that
        // // should be already when we encounter the base span position
        // LOG.trace("{} - skipping rendering of link origin on link position", object);
        // return true;
        // }
        //
        // if (!spanPosition.isLinkFeaturePosition() && object instanceof VArc) {
        // // When processing a span position, do not render slot links because these should be
        // // rendered when we encounter the slot position
        // LOG.trace("{} - skipping rendering of link arc on span position", object);
        // return true;
        // }
        // }

        // // Check if the target already contains an annotation from this configuration
        // if (object instanceof VSpan
        // && isCurationSuggestionHiddenByMergedAnnotation(targetUser, diff, cfg, object)) {
        // LOG.trace("{} - skipping rendering due to merged annotation", object);
        // return true;
        // }

        // Check if the position could be merged at all or if the merge is blocked by an overlapping
        // annotation (which may not be contained in the configuration)
        if (object instanceof VSpan) {
            var anns = aRequest.getCas().<Annotation> select(aAnn.getType().getName());

            var skip = switch (aLayer.getOverlapMode()) {
            case NO_OVERLAP -> anns.anyMatch(f -> aAnn.overlapping(f));
            case STACKING_ONLY -> anns.anyMatch(f -> aAnn.overlapping(f) && !colocated(f, aAnn));
            case OVERLAP_ONLY -> anns.anyMatch(f -> aAnn.overlapping(f) && colocated(f, aAnn));
            case ANY_OVERLAP -> false;
            };

            if (skip) {
                LOG.trace("{} - skipping rendering due to {}", object, aLayer.getOverlapMode());
            }

            return skip;
        }

        return false;
    }

    private boolean isCurationSuggestionHiddenByMergedAnnotation(String aTargetUser,
            DiffResult aDiff, Configuration aCfg, VObject aObject)
    {
        var sourceConfiguration = aDiff.findConfiguration(aCfg.getRepresentativeCasGroupId(),
                new AID(aObject.getVid()));
        return sourceConfiguration.map($ -> $.getAID(aTargetUser) != null).orElse(false);
    }

    private CasDiff createDiff(RenderRequest aRequest, List<User> selectedUsers)
    {
        var casses = new LinkedHashMap<String, CAS>();

        // This is the CAS that the user can actively edit
        casses.put(aRequest.getAnnotationUser().getUsername(), aRequest.getCas());

        for (var user : selectedUsers) {
            try {
                var userCas = documentService.readAnnotationCas(aRequest.getSourceDocument(),
                        AnnotationSet.forUser(user));
                casses.put(user.getUsername(), userCas);
            }
            catch (IOException e) {
                LOG.error("Could not retrieve CAS for user [{}] and project {}", user.getUsername(),
                        aRequest.getProject(), e);
            }
        }

        var adapters = diffAdapterRegistry.getDiffAdapters(aRequest.getVisibleLayers());
        return doDiff(adapters, casses, aRequest.getWindowBeginOffset(),
                aRequest.getWindowEndOffset());
    }

    private void resolveArcEndpoints(String aTargetUser, DiffResult aDiff, boolean showAll,
            Configuration cfg, VArc arc)
    {
        if (cfg.getPosition() instanceof SpanPosition spanPosition
                && spanPosition.isLinkFeaturePosition()) {
            arc.setSource(resolveVisibleLinkHost(aTargetUser, aDiff, cfg, arc.getSource()));
        }
        else {
            arc.setSource(
                    resolveVisibleEndpoint(aTargetUser, aDiff, cfg, arc.getSource(), showAll));
        }

        arc.setTarget(resolveVisibleEndpoint(aTargetUser, aDiff, cfg, arc.getTarget(), showAll));
    }

    private VID resolveVisibleLinkHost(String aTargetUser, DiffResult aDiff, Configuration aCfg,
            VID aVid)
    {
        if (REL_EXTENSION_ID.equals(aVid.getExtensionId())) {
            return aVid;
        }

        var representativeCasGroupId = aCfg.getRepresentativeCasGroupId();
        return new CurationVID(representativeCasGroupId, aVid);
    }

    /**
     * Find and return the rendered VID which is equivalent to the given VID. E.g. if the given VID
     * belongs to an already curated annotation, then locate the VID for the rendered annotation of
     * the curation user by searching the diff for configuration set that contains the annotators
     * annotation and then switching over to the configuration containing the curators annotation.
     */
    private VID resolveVisibleEndpoint(String aTargetUser, DiffResult aDiff, Configuration aCfg,
            VID aVid, boolean showAll)
    {
        if (REL_EXTENSION_ID.equals(aVid.getExtensionId())) {
            return aVid;
        }

        var sourceConfiguration = aDiff.findConfiguration(aCfg.getRepresentativeCasGroupId(),
                new AID(aVid));

        // This is for relation annotation endpoints and link targets. Here we can look for the
        // configuration which contains the annotators annotation and find the corresponding
        // curated version for it in the same configuration
        if (sourceConfiguration.isPresent()) {
            var curatedAID = sourceConfiguration.get().getAID(aTargetUser);

            // If showAll is not enabled, we use the merged annotation of the target user
            // If showAll is enabled though, we prefer to use the original un-merged annotation
            if (!showAll && curatedAID != null) {
                return new VID(curatedAID.addr);
            }

            var representativeCasGroupId = sourceConfiguration.get().getRepresentativeCasGroupId();
            return new CurationVID(representativeCasGroupId,
                    new VID(sourceConfiguration.get().getAID(representativeCasGroupId).addr));
        }

        return new CurationVID(aCfg.getRepresentativeCasGroupId(), aVid);
    }
}
