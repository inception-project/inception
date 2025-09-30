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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.NONE;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.NotEditableException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.curation.merge.CasMerge;
import de.tudarmstadt.ukp.inception.diam.editor.actions.ScrollToHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.SelectAnnotationHandler;
import de.tudarmstadt.ukp.inception.diam.editor.lazydetails.LazyDetailsLookupService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtension;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionImplBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetail;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.config.CurationSidebarAutoConfiguration;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.render.CurationVID;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationSidebarAutoConfiguration#curationEditorExtension}.
 * </p>
 */
public class CurationEditorExtension
    extends AnnotationEditorExtensionImplBase
    implements AnnotationEditorExtension
{
    public static final String EXTENSION_ID = "cur";

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AnnotationSchemaService annotationService;
    private final DocumentService documentService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserDao userRepository;
    private final CurationSidebarService curationSidebarService;
    private final FeatureSupportRegistry featureSupportRegistry;
    private final LazyDetailsLookupService detailsLookupService;
    private final DiffAdapterRegistry diffAdapterRegistry;

    public CurationEditorExtension(AnnotationSchemaService aAnnotationService,
            DocumentService aDocumentService, ApplicationEventPublisher aApplicationEventPublisher,
            UserDao aUserRepository, CurationSidebarService aCurationSidebarService,
            FeatureSupportRegistry aFeatureSupportRegistry,
            LazyDetailsLookupService aDetailsLookupService,
            DiffAdapterRegistry aDiffAdapterRegistry)
    {
        annotationService = aAnnotationService;
        documentService = aDocumentService;
        applicationEventPublisher = aApplicationEventPublisher;
        userRepository = aUserRepository;
        curationSidebarService = aCurationSidebarService;
        featureSupportRegistry = aFeatureSupportRegistry;
        detailsLookupService = aDetailsLookupService;
        diffAdapterRegistry = aDiffAdapterRegistry;
    }

    @Override
    public String getBeanName()
    {
        return EXTENSION_ID;
    }

    @Override
    public void handleAction(AnnotationActionHandler aActionHandler, AnnotatorState aState,
            AjaxRequestTarget aTarget, CAS aCas, VID aParamId, String aAction)
        throws AnnotationException, IOException
    {
        // only process actions relevant to curation
        if (!aParamId.getExtensionId().equals(EXTENSION_ID)) {
            return;
        }

        var curationVid = CurationVID.parse(aParamId.getExtensionPayload());
        if (curationVid == null) {
            return;
        }

        var doc = aState.getDocument();
        var srcUser = curationVid.getUsername();

        if (!documentService.existsAnnotationDocument(doc, AnnotationSet.forUser(srcUser))) {
            LOG.error("Source CAS of [{}] for curation not found", srcUser);
            return;
        }

        if (SelectAnnotationHandler.COMMAND.equals(aAction)) {
            actionCurationSuggestionSelected(aActionHandler, aState, aTarget, aCas, aAction,
                    curationVid);
        }
        else if (ScrollToHandler.COMMAND.equals(aAction)) {
            actionJumpTo(aActionHandler, aTarget, curationVid, doc, srcUser);
        }
    }

    private void actionJumpTo(AnnotationActionHandler aActionHandler, AjaxRequestTarget aTarget,
            CurationVID curationVid, SourceDocument doc, String srcUser)
        throws IOException, AnnotationException
    {
        // get user CAS and annotation (to be merged into curator's)
        var vid = VID.parse(curationVid.getExtensionPayload());

        var srcCas = documentService.readAnnotationCas(doc, AnnotationSet.forUser(srcUser));
        var sourceAnnotation = selectAnnotationByAddr(srcCas, vid.getId());

        aActionHandler.actionJump(aTarget, sourceAnnotation.getBegin(), sourceAnnotation.getEnd());
    }

    private void actionCurationSuggestionSelected(AnnotationActionHandler aActionHandler,
            AnnotatorState aState, AjaxRequestTarget aTarget, CAS aCas, String aAction,
            CurationVID curationVid)
        throws NotEditableException, IOException, AnnotationException
    {
        if (curationSidebarService.isCurationFinished(aState,
                userRepository.getCurrentUsername())) {
            throw new NotEditableException("Curation is already finished. You can put it back "
                    + "into progress via the monitoring page.");
        }

        var page = (AnnotationPageBase) aTarget.getPage();
        page.ensureIsEditable();

        mergeAnnotation(aAction, aActionHandler, aState, aTarget, aCas, curationVid);
    }

    @Override
    public <V> V getFeatureValue(SourceDocument aDocument, User aUser, CAS aCas, VID aVid,
            AnnotationFeature aFeature)
        throws IOException
    {
        // only process actions relevant to curation
        if (!aVid.getExtensionId().equals(EXTENSION_ID)) {
            return null;
        }

        var curationVid = CurationVID.parse(aVid.getExtensionPayload());
        if (curationVid == null) {
            return null;
        }

        var srcSet = AnnotationSet.forUser(curationVid.getUsername());

        if (!documentService.existsAnnotationDocument(aDocument, srcSet)) {
            LOG.error("Source CAS of [{}] for curation not found", srcSet);
            return null;
        }

        var vid = VID.parse(curationVid.getExtensionPayload());
        var cas = documentService.readAnnotationCas(aDocument, srcSet);
        var fs = selectAnnotationByAddr(cas, vid.getId());
        var ext = featureSupportRegistry.findExtension(aFeature).orElseThrow();
        return ext.getFeatureValue(aFeature, fs);
    }

    /**
     * Save annotation identified by aVID from user CAS to given curator's CAS
     */
    private void mergeAnnotation(String aAction, AnnotationActionHandler aActionHandler,
            AnnotatorState aState, AjaxRequestTarget aTarget, CAS aTargetCas,
            CurationVID aCurationVid)
        throws IOException, AnnotationException
    {

        // get user CAS and annotation (to be merged into curator's)
        var doc = aState.getDocument();
        var srcUser = aCurationVid.getUsername();

        var vid = VID.parse(aCurationVid.getExtensionPayload());

        var srcCas = documentService.readAnnotationCas(doc, AnnotationSet.forUser(srcUser));
        var sourceAnnotation = selectAnnotationByAddr(srcCas, vid.getId());
        var layer = annotationService.findLayer(aState.getProject(), sourceAnnotation);

        if (vid.isSlotSet()) {
            mergeSlot(aState, aTargetCas, vid, srcUser, sourceAnnotation, layer);
        }
        else if (RelationLayerSupport.TYPE.equals(layer.getType())) {
            mergeRelation(aState, aTargetCas, vid, srcUser, sourceAnnotation, layer);
        }
        else if (SpanLayerSupport.TYPE.equals(layer.getType())) {
            mergeSpan(aState, aTargetCas, vid, srcUser, sourceAnnotation, layer);
        }

        aActionHandler.actionSelect(aTarget);
        aActionHandler.writeEditorCas();
    }

    private void mergeSlot(AnnotatorState aState, CAS aTargetCas, VID aVid, String aSrcUser,
            AnnotationFS sourceAnnotation, AnnotationLayer layer)
        throws AnnotationException
    {
        var doc = aState.getDocument();
        var casMerge = new CasMerge(annotationService, applicationEventPublisher);

        var adapter = annotationService.getAdapter(layer);
        var feature = adapter.listFeatures().stream().sequential().skip(aVid.getAttribute())
                .findFirst().get();

        var mergeResult = casMerge.mergeSlotFeature(doc, aSrcUser, layer, aTargetCas,
                sourceAnnotation, feature.getName(), aVid.getSlot());

        // open created/updates FS in annotation detail editor panel
        var mergedAnno = selectAnnotationByAddr(aTargetCas, mergeResult.targetAddress());
        aState.getSelection().selectSpan(mergedAnno);
    }

    private void mergeRelation(AnnotatorState aState, CAS aTargetCas, VID aVid, String aSrcUser,
            AnnotationFS sourceAnnotation, AnnotationLayer layer)
        throws AnnotationException
    {
        var doc = aState.getDocument();
        var casMerge = new CasMerge(annotationService, applicationEventPublisher);
        var mergeResult = casMerge.mergeRelationAnnotation(doc, aSrcUser, layer, aTargetCas,
                sourceAnnotation);

        // open created/updates FS in annotation detail editor panel
        var mergedAnno = selectAnnotationByAddr(aTargetCas, mergeResult.targetAddress());
        aState.getSelection().selectArc(mergedAnno);
    }

    private void mergeSpan(AnnotatorState aState, CAS aTargetCas, VID aVid, String aSrcUser,
            AnnotationFS sourceAnnotation, AnnotationLayer layer)
        throws AnnotationException
    {
        var doc = aState.getDocument();
        var casMerge = new CasMerge(annotationService, applicationEventPublisher);
        var mergeResult = casMerge.mergeSpanAnnotation(doc, aSrcUser, layer, aTargetCas,
                sourceAnnotation);

        // open created/updates FS in annotation detail editor panel
        var mergedAnno = selectAnnotationByAddr(aTargetCas, mergeResult.targetAddress());
        aState.getSelection().selectSpan(mergedAnno);
    }

    @Override
    public List<VLazyDetailGroup> lookupLazyDetails(SourceDocument aDocument, User aDataOwner,
            CAS aCas, VID aVid, AnnotationLayer aLayer)
    {
        var detailGroups = new ArrayList<VLazyDetailGroup>();

        try {
            var curationVid = CurationVID.parse(aVid.getExtensionPayload());
            var vid = VID.parse(curationVid.getExtensionPayload());
            var srcUser = curationVid.getUsername();
            var srcCas = documentService.readAnnotationCas(aDocument,
                    AnnotationSet.forUser(srcUser));
            var features = annotationService.listEnabledFeatures(aLayer).stream() //
                    .filter(f -> f.isIncludeInHover()) //
                    .filter(f -> NONE == f.getMultiValueMode()) //
                    .toList();

            // This is where we get the "show on hover" stuff...
            detailsLookupService.lookupLayerLevelDetails(vid, srcCas, aLayer)
                    .forEach(detailGroups::add);

            // The curatable features need to be all the same across the users for the position
            var curatableFeatures = features.stream() //
                    .filter(f -> f.isCuratable()) //
                    .toList();
            for (var feature : curatableFeatures) {
                detailsLookupService.lookupFeatureLevelDetails(vid, srcCas, feature)
                        .forEach(detailGroups::add);
            }

            // The non-curatable features (e.g. comments) may differ, so we need to collect them
            var nonCuratableFeatures = features.stream() //
                    .filter(f -> !f.isCuratable()) //
                    .toList();
            lookupFeaturesAcrossAnnotators(aDocument, aDataOwner, aCas, aLayer, vid, srcUser,
                    srcCas, nonCuratableFeatures).forEach(detailGroups::add);
        }
        catch (IOException e) {
            LOG.error("Unable to load lazy details", e);
            var detailGroup = new VLazyDetailGroup();
            detailGroup.addDetail(new VLazyDetail("Error", "Unable to load annotator details"));
            detailGroups.add(detailGroup);
        }

        return detailGroups;
    }

    private List<VLazyDetailGroup> lookupFeaturesAcrossAnnotators(SourceDocument aDocument,
            User aDataOwner, CAS aCas, AnnotationLayer aLayer, VID vid, String aSrcUser,
            CAS aSrcCas, List<AnnotationFeature> aNonCuratableFeatures)
    {
        var sessionOwner = userRepository.getCurrentUsername();
        var selectedUsers = curationSidebarService.listUsersReadyForCuration(sessionOwner,
                aDocument.getProject(), aDocument);

        if (selectedUsers.isEmpty()) {
            return emptyList();
        }

        var casses = collectCasses(aDocument, aDataOwner, aCas, selectedUsers);

        var srcAnnotation = selectAnnotationByAddr(aSrcCas, vid.getId());
        var casDiff = createDiff(casses, aLayer, srcAnnotation.getBegin(), srcAnnotation.getEnd());

        var maybeConfiguration = casDiff.toResult().findConfiguration(aSrcUser, srcAnnotation);
        if (maybeConfiguration.isEmpty()) {
            return emptyList();
        }

        var detailGroups = new ArrayList<VLazyDetailGroup>();

        var configuration = maybeConfiguration.get();
        for (var user : configuration.getCasGroupIds()) {
            var group = new VLazyDetailGroup(user);
            var fs = configuration.getFs(user, casses);
            for (var f : aNonCuratableFeatures) {
                featureSupportRegistry.findExtension(f).ifPresent(support -> {
                    var label = support.renderFeatureValue(f, fs);
                    if (isNotBlank(label)) {
                        group.addDetail(new VLazyDetail(f.getUiName(), label));
                    }
                });
            }
            if (!group.getDetails().isEmpty()) {
                detailGroups.add(group);
            }
        }

        return detailGroups;
    }

    private Map<String, CAS> collectCasses(SourceDocument aDocument, User aDataOwner, CAS aCas,
            List<User> selectedUsers)
    {
        var casses = new LinkedHashMap<String, CAS>();

        // This is the CAS that the user can actively edit
        casses.put(aDataOwner.getUsername(), aCas);

        for (var user : selectedUsers) {
            try {
                var userCas = documentService.readAnnotationCas(aDocument,
                        AnnotationSet.forUser(user.getUsername()));
                casses.put(user.getUsername(), userCas);
            }
            catch (IOException e) {
                LOG.error("Could not retrieve CAS for user [{}] and project {}", user.getUsername(),
                        aDocument.getProject(), e);
            }
        }

        return casses;
    }

    private CasDiff createDiff(Map<String, CAS> aCasses, AnnotationLayer aLayer, int aBegin,
            int aEnd)
    {
        var adapters = diffAdapterRegistry.getDiffAdapters(asList(aLayer));
        return doDiff(adapters, aCasses, aBegin, aEnd);
    }
}
