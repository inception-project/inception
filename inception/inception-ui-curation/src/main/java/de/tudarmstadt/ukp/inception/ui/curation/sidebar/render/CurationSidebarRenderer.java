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
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.getDiffAdapters;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_ROLE_AS_LABEL;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.internal.AID;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.span.SpanPosition;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderStep;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VComment;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VCommentType;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
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

    public CurationSidebarRenderer(CurationSidebarService aCurationService,
            LayerSupportRegistry aLayerSupportRegistry, DocumentService aDocumentService,
            UserDao aUserRepository, AnnotationSchemaService aAnnotationService)
    {
        curationService = aCurationService;
        layerSupportRegistry = aLayerSupportRegistry;
        documentService = aDocumentService;
        userRepository = aUserRepository;
        annotationService = aAnnotationService;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public boolean accepts(RenderRequest aRequest)
    {
        var state = aRequest.getState();

        // do not show predictions on the decicated curation page
        if (state != null && state.getMode() != ANNOTATION) {
            return false;
        }

        if (aRequest.getCas() == null) {
            return false;
        }

        return true;
    }

    @Override
    public void render(VDocument aVdoc, RenderRequest aRequest)
    {
        var sessionOwner = userRepository.getCurrentUsername();
        var project = aRequest.getProject();

        if (!curationService.existsSession(sessionOwner, project.getId())) {
            return;
        }

        var selectedUsers = curationService.listUsersReadyForCuration(sessionOwner, project,
                aRequest.getSourceDocument());
        if (selectedUsers.isEmpty()) {
            return;
        }

        var targetUser = aRequest.getAnnotationUser().getUsername();

        var casDiff = createDiff(aRequest, selectedUsers);
        var diff = casDiff.toResult();

        // Listing the features once is faster than repeatedly hitting the DB to list features for
        // every layer.
        var supportedFeatures = annotationService.listSupportedFeatures(project);
        var allFeatures = annotationService.listAnnotationFeature(project);

        // Set up a cache for resolving type to layer to avoid hammering the DB as we process each
        // position
        var type2layer = diff.getPositions().stream().map(Position::getType).distinct()
                .map(type -> annotationService.findLayer(project, type))
                .collect(toMap(AnnotationLayer::getName, identity()));

        var generatedCurationVids = new HashSet<VID>();
        var showAll = curationService.isShowAll(sessionOwner, project.getId());
        var curationTarget = curationService.getCurationTarget(sessionOwner, project.getId());
        for (var cfgSet : diff.getConfigurationSets()) {
            if (!showAll && cfgSet.getCasGroupIds().contains(curationTarget)) {
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
                var fs = cfg.getRepresentative(casDiff.getCasMap());
                if (!(fs instanceof Annotation)) {
                    continue;
                }

                var user = cfg.getRepresentativeCasGroupId();

                // We need to pass in *all* the annotation features here because we also to that in
                // other places where we create renderers - and the set of features must always be
                // the same because otherwise the IDs of armed slots would be inconsistent
                var layerSupport = layerSupportRegistry.getLayerSupport(layer);
                var renderer = layerSupport.createRenderer(layer, () -> layerAllFeatures);

                var objects = renderer.render(aRequest, layerSupportedFeatures, aVdoc,
                        (AnnotationFS) fs);

                for (var object : objects) {
                    if (cfg.getPosition() instanceof SpanPosition spanPosition) {
                        if (spanPosition.getFeature() != null && object instanceof VSpan) {
                            // When processing a slot position, do not render the origin span
                            // because that should be already rendered by a separate position
                            continue;
                        }
                    }

                    if (!showAll && object instanceof VSpan) {
                        var sourceConfiguration = diff.findConfiguration(
                                cfg.getRepresentativeCasGroupId(), new AID(object.getVid()));
                        if (sourceConfiguration.map($ -> $.getAID(targetUser) != null)
                                .orElse(false)) {
                            continue;
                        }
                    }

                    var curationVid = new CurationVID(user, object.getVid());
                    if (generatedCurationVids.contains(curationVid)) {
                        continue;
                    }

                    generatedCurationVids.add(curationVid);

                    object.setVid(curationVid);
                    object.setColorHint(COLOR);
                    aVdoc.add(object);

                    aVdoc.add(new VComment(object.getVid(), VCommentType.INFO,
                            "Annotators: " + cfg.getCasGroupIds().stream().collect(joining(", "))));

                    if (object instanceof VArc arc) {
                        // Currently works for relations but not for slots
                        arc.setSource(getCurationVid(targetUser, diff, cfg, arc.getSource()));
                        arc.setTarget(getCurationVid(targetUser, diff, cfg, arc.getTarget()));
                        LOG.trace("Rendering curation vid: {} source: {} target: {}", arc.getVid(),
                                arc.getSource(), arc.getTarget());
                    }
                    else {
                        LOG.trace("Rendering curation vid: {}", object.getVid());
                    }
                }
            }
        }
    }

    private CasDiff createDiff(RenderRequest aRequest, List<User> selectedUsers)
    {
        var casses = new LinkedHashMap<String, CAS>();

        // This is the CAS that the user can actively edit
        casses.put(aRequest.getAnnotationUser().getUsername(), aRequest.getCas());

        for (var user : selectedUsers) {
            try {
                var userCas = documentService.readAnnotationCas(aRequest.getSourceDocument(),
                        user.getUsername());
                casses.put(user.getUsername(), userCas);
            }
            catch (IOException e) {
                LOG.error("Could not retrieve CAS for user [{}] and project {}", user.getUsername(),
                        aRequest.getProject(), e);
            }
        }

        var adapters = getDiffAdapters(annotationService, aRequest.getVisibleLayers());
        return doDiff(adapters, LINK_ROLE_AS_LABEL, casses, aRequest.getWindowBeginOffset(),
                aRequest.getWindowEndOffset());
    }

    /**
     * Find and return the rendered VID which is equivalent to the given VID. E.g. if the given VID
     * belongs to an already curated annotation, then locate the VID for the rendered annotation of
     * the curation user by searching the diff for configuration set that contains the annotators
     * annotation and then switching over to the configuration containing the curators annotation.
     */
    private VID getCurationVid(String aTargetUser, DiffResult aDiff, Configuration aCfg, VID aVid)
    {
        var representativeCasGroupId = aCfg.getRepresentativeCasGroupId();
        var sourceConfiguration = aDiff.findConfiguration(representativeCasGroupId, new AID(aVid));

        if (sourceConfiguration.isPresent()) {
            var curatedAID = sourceConfiguration.get().getAID(aTargetUser);
            if (curatedAID != null) {
                return new VID(curatedAID.addr);
            }

            return new CurationVID(representativeCasGroupId,
                    new VID(sourceConfiguration.get().getAID(representativeCasGroupId).addr));
        }

        return new CurationVID(representativeCasGroupId, aVid);
    }
}
