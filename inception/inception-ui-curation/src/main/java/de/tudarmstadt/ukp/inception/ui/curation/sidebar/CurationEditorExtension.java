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

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.SPAN_TYPE;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.NotEditableException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.merge.CasMerge;
import de.tudarmstadt.ukp.inception.curation.merge.CasMergeOperationResult;
import de.tudarmstadt.ukp.inception.diam.editor.actions.ScrollToHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.SelectAnnotationHandler;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtension;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionImplBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
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

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AnnotationSchemaService annotationService;
    private final DocumentService documentService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserDao userRepository;
    private final CurationSidebarService curationSidebarService;
    private final FeatureSupportRegistry featureSupportRegistry;

    public CurationEditorExtension(AnnotationSchemaService aAnnotationService,
            DocumentService aDocumentService, ApplicationEventPublisher aApplicationEventPublisher,
            UserDao aUserRepository, CurationSidebarService aCurationSidebarService,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        annotationService = aAnnotationService;
        documentService = aDocumentService;
        applicationEventPublisher = aApplicationEventPublisher;
        userRepository = aUserRepository;
        curationSidebarService = aCurationSidebarService;
        featureSupportRegistry = aFeatureSupportRegistry;
    }

    @Override
    public String getBeanName()
    {
        return EXTENSION_ID;
    }

    @Override
    public void handleAction(AnnotationActionHandler aPanel, AnnotatorState aState,
            AjaxRequestTarget aTarget, CAS aCas, VID aParamId, String aAction)
        throws AnnotationException, IOException
    {
        // only process actions relevant to curation
        if (!aParamId.getExtensionId().equals(EXTENSION_ID)) {
            return;
        }

        CurationVID curationVid = CurationVID.parse(aParamId.getExtensionPayload());
        if (curationVid == null) {
            return;
        }

        SourceDocument doc = aState.getDocument();
        String srcUser = curationVid.getUsername();

        if (!documentService.existsAnnotationDocument(doc, srcUser)) {
            log.error("Source CAS of [{}] for curation not found", srcUser);
            return;
        }

        if (SelectAnnotationHandler.COMMAND.equals(aAction)) {
            if (curationSidebarService.isCurationFinished(aState,
                    userRepository.getCurrentUsername())) {
                throw new NotEditableException("Curation is already finished. You can put it back "
                        + "into progress via the monitoring page.");
            }

            mergeAnnotation(aAction, aPanel, aState, aTarget, aCas, curationVid);
        }
        else if (ScrollToHandler.COMMAND.equals(aAction)) {
            // get user CAS and annotation (to be merged into curator's)
            VID vid = VID.parse(curationVid.getExtensionPayload());

            CAS srcCas = documentService.readAnnotationCas(doc, srcUser);
            AnnotationFS sourceAnnotation = ICasUtil.selectAnnotationByAddr(srcCas, vid.getId());

            var page = (AnnotationPageBase) aTarget.getPage();
            page.getAnnotationActionHandler().actionJump(aTarget, sourceAnnotation.getBegin(),
                    sourceAnnotation.getEnd());
        }
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

        var srcUser = curationVid.getUsername();

        if (!documentService.existsAnnotationDocument(aDocument, srcUser)) {
            log.error("Source CAS of [{}] for curation not found", srcUser);
            return null;
        }

        var vid = VID.parse(curationVid.getExtensionPayload());
        var cas = documentService.readAnnotationCas(aDocument, srcUser);
        var fs = ICasUtil.selectAnnotationByAddr(cas, vid.getId());
        var ext = featureSupportRegistry.findExtension(aFeature).orElseThrow();
        return ext.getFeatureValue(aFeature, fs);
    }

    /**
     * Save annotation identified by aVID from user CAS to given curator's CAS
     */
    private void mergeAnnotation(String aAction, AnnotationActionHandler aPanel,
            AnnotatorState aState, AjaxRequestTarget aTarget, CAS aTargetCas,
            CurationVID aCurationVid)
        throws IOException, AnnotationException
    {
        // get user CAS and annotation (to be merged into curator's)
        SourceDocument doc = aState.getDocument();
        String srcUser = aCurationVid.getUsername();

        VID vid = VID.parse(aCurationVid.getExtensionPayload());

        CAS srcCas = documentService.readAnnotationCas(doc, srcUser);
        AnnotationFS sourceAnnotation = ICasUtil.selectAnnotationByAddr(srcCas, vid.getId());
        AnnotationLayer layer = annotationService.findLayer(aState.getProject(), sourceAnnotation);

        if (vid.isSlotSet()) {
            mergeSlot(aState, aTargetCas, vid, srcUser, sourceAnnotation, layer);
        }
        else if (RELATION_TYPE.equals(layer.getType())) {
            mergeRelation(aState, aTargetCas, vid, srcUser, sourceAnnotation, layer);
        }
        else if (SPAN_TYPE.equals(layer.getType())) {
            mergeSpan(aState, aTargetCas, vid, srcUser, sourceAnnotation, layer);
        }

        aPanel.actionSelect(aTarget);
        aPanel.actionCreateOrUpdate(aTarget, aTargetCas); // should also update timestamps
    }

    private void mergeSlot(AnnotatorState aState, CAS aTargetCas, VID aVid, String aSrcUser,
            AnnotationFS sourceAnnotation, AnnotationLayer layer)
        throws AnnotationException
    {
        SourceDocument doc = aState.getDocument();
        CasMerge casMerge = new CasMerge(annotationService, applicationEventPublisher);

        TypeAdapter adapter = annotationService.getAdapter(layer);
        AnnotationFeature feature = adapter.listFeatures().stream().sequential()
                .skip(aVid.getAttribute()).findFirst().get();

        CasMergeOperationResult mergeResult = casMerge.mergeSlotFeature(doc, aSrcUser, layer,
                aTargetCas, sourceAnnotation, feature.getName(), aVid.getSlot());

        // open created/updates FS in annotation detail editorpanel
        AnnotationFS mergedAnno = ICasUtil.selectAnnotationByAddr(aTargetCas,
                mergeResult.getResultFSAddress());
        aState.getSelection().selectSpan(mergedAnno);
    }

    private void mergeRelation(AnnotatorState aState, CAS aTargetCas, VID aVid, String aSrcUser,
            AnnotationFS sourceAnnotation, AnnotationLayer layer)
        throws AnnotationException
    {
        SourceDocument doc = aState.getDocument();
        CasMerge casMerge = new CasMerge(annotationService, applicationEventPublisher);
        CasMergeOperationResult mergeResult = casMerge.mergeRelationAnnotation(doc, aSrcUser, layer,
                aTargetCas, sourceAnnotation, layer.isAllowStacking());

        // open created/updates FS in annotation detail editorpanel
        AnnotationFS mergedAnno = ICasUtil.selectAnnotationByAddr(aTargetCas,
                mergeResult.getResultFSAddress());
        aState.getSelection().selectArc(mergedAnno);
    }

    private void mergeSpan(AnnotatorState aState, CAS aTargetCas, VID aVid, String aSrcUser,
            AnnotationFS sourceAnnotation, AnnotationLayer layer)
        throws AnnotationException
    {
        SourceDocument doc = aState.getDocument();
        CasMerge casMerge = new CasMerge(annotationService, applicationEventPublisher);
        CasMergeOperationResult mergeResult = casMerge.mergeSpanAnnotation(doc, aSrcUser, layer,
                aTargetCas, sourceAnnotation, layer.isAllowStacking());

        // open created/updates FS in annotation detail editor panel
        AnnotationFS mergedAnno = ICasUtil.selectAnnotationByAddr(aTargetCas,
                mergeResult.getResultFSAddress());
        aState.getSelection().selectSpan(mergedAnno);
    }
}
