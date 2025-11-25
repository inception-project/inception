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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toInterpretableJsonString;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.jquery.ui.settings.JQueryUILibrarySettings;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.comment.AnnotatorCommentDialogPanel;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratRequestUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratSerializer;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratCurationResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.schema.BratSchemaGenerator;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotatorSegmentState;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.render.CurationRenderer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.LegacyCurationPage;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandlerBase;
import de.tudarmstadt.ukp.inception.diam.editor.actions.LazyDetailsHandler;
import de.tudarmstadt.ukp.inception.diam.editor.lazydetails.LazyDetailsLookupService;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.SymbolLabel;
import de.tudarmstadt.ukp.inception.support.wicket.WicketUtil;

public abstract class BratSuggestionVisualizer
    extends Panel
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = 6653508018500736430L;

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userService;
    private @SpringBean DocumentService documentService;
    private @SpringBean LazyDetailsLookupService lazyDetailsLookupService;
    private @SpringBean BratSchemaGenerator bratSchemaGenerator;
    private @SpringBean CurationRenderer curationRenderer;
    private @SpringBean BratSerializer bratSerializer;

    private final WebMarkupContainer vis;
    private final ModalDialog modalDialog;
    private final BootstrapModalDialog confirmationDialog;
    private final AbstractDefaultAjaxBehavior controller;
    private final AbstractAjaxBehavior collProvider;
    private final AbstractAjaxBehavior docProvider;

    private final int position;

    public BratSuggestionVisualizer(String aId, IModel<AnnotatorSegmentState> aModel, int aPosition)
    {
        super(aId, aModel);

        position = aPosition;

        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);

        // Provides collection-level information like type definitions, styles, etc.
        collProvider = new AbstractAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onRequest()
            {
                getRequestCycle().scheduleRequestHandlerAfterCurrent(
                        new TextRequestHandler("application/json", "UTF-8", getCollectionData()));
            }
        };

        // Provides the actual document contents
        docProvider = new AbstractAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onRequest()
            {
                getRequestCycle().scheduleRequestHandlerAfterCurrent(
                        new TextRequestHandler("application/json", "UTF-8", getDocumentData()));
            }
        };

        modalDialog = new BootstrapModalDialog("modalDialog");
        queue(modalDialog);

        add(vis);
        add(collProvider, docProvider);

        add(new Label("username", getModel().map(this::maybeAnonymizeUsername)));

        confirmationDialog = new BootstrapModalDialog("stateChangeConfirmationDialog");
        confirmationDialog.trapFocus();
        add(confirmationDialog);

        var annDoc = LoadableDetachableModel.of(this::getAnnotationDocument);

        var stateToggle = new LambdaAjaxLink("stateToggle", _target -> {
            var dialogContent = new StateChangeConfirmationDialogPanel(
                    BootstrapModalDialog.CONTENT_ID, getModel().map(this::maybeAnonymizeUsername));
            dialogContent.setConfirmAction(this::actionToggleAnnotationDocumentState);
            confirmationDialog.open(dialogContent, _target);
        });
        stateToggle.setOutputMarkupId(true);
        stateToggle.add(new SymbolLabel("state", annDoc.map(AnnotationDocument::getState)));
        add(stateToggle);

        Icon commentSymbol = new Icon("commentSymbol", FontAwesome5IconType.comment_s);
        commentSymbol.add(visibleWhen(
                annDoc.map(AnnotationDocument::getAnnotatorComment).map(StringUtils::isNotBlank)));
        commentSymbol.add(
                AjaxEventBehavior.onEvent("click", _t -> actionShowAnnotatorComment(_t, annDoc)));
        queue(commentSymbol);

        controller = new DiamAjaxBehavior() //
                .setGlobalHandlersEnabled(false) //
                .addPriorityHandler(new CurationLazyDetailsHandler()) //
                .addPriorityHandler(new CurationActionAjaxRequestHandler());
        add(controller);
    }

    private void actionShowAnnotatorComment(AjaxRequestTarget aTarget,
            LoadableDetachableModel<AnnotationDocument> aAnnDoc)
    {
        modalDialog.open(new AnnotatorCommentDialogPanel(ModalDialog.CONTENT_ID, aAnnDoc), aTarget);
    }

    private void actionToggleAnnotationDocumentState(AjaxRequestTarget aTarget)
    {
        var username = getModelObject().getUser().getUsername();
        var doc = getModelObject().getAnnotatorState().getDocument();
        var annDoc = documentService.getAnnotationDocument(doc, AnnotationSet.forUser(username));
        var annDocState = annDoc.getState();

        switch (annDocState) {
        case IN_PROGRESS:
            documentService.setAnnotationDocumentState(annDoc, FINISHED);
            break;
        case FINISHED:
            documentService.setAnnotationDocumentState(annDoc, IN_PROGRESS);
            break;
        default:
            error("Can only change document state for documents that are finished or in progress, "
                    + "but document is in state [" + annDocState + "]");
            aTarget.addChildren(getPage(), IFeedback.class);
            break;
        }

        ((LegacyCurationPage) getPage()).actionLoadDocument(aTarget);
    }

    private AnnotationDocument getAnnotationDocument()
    {
        var username = getModelObject().getUser().getUsername();
        var doc = getModelObject().getAnnotatorState().getDocument();
        return documentService.getAnnotationDocument(doc, AnnotationSet.forUser(username));
    }

    private String maybeAnonymizeUsername(AnnotatorSegmentState aSegment)
    {
        Project project = aSegment.getAnnotatorState().getProject();
        if (project.isAnonymousCuration()
                && !projectService.hasRole(userService.getCurrentUser(), project, MANAGER)) {
            return "Anonymized annotator " + (position + 1);
        }

        return aSegment.getUser().getUiName();
    }

    public void setModel(IModel<AnnotatorSegmentState> aModel)
    {
        setDefaultModel(aModel);
    }

    public void setModelObject(AnnotatorSegmentState aModel)
    {
        setDefaultModelObject(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorSegmentState> getModel()
    {
        return (IModel<AnnotatorSegmentState>) getDefaultModel();
    }

    public AnnotatorSegmentState getModelObject()
    {
        return (AnnotatorSegmentState) getDefaultModelObject();
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        aResponse.render(forReference(JQueryUILibrarySettings.get().getJavaScriptReference()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratCurationResourceReference.get()));

        // BRAT call to load the BRAT JSON from our collProvider and docProvider.
        String script = "BratCuration('" + vis.getMarkupId() + "', '" + controller.getCallbackUrl()
                + "', '" + collProvider.getCallbackUrl() + "', '" + docProvider.getCallbackUrl()
                + "')";
        aResponse.render(OnLoadHeaderItem.forScript("\n" + script));
    }

    public String getDocumentData()
    {
        try {
            var state = getModelObject().getAnnotatorState();
            // FIXME: This is a minimalist render request containing only a view pieces of
            // information that the serializer needs. In particular, it does not contain the CAS
            // because even if we loaded it again now, its FS addresses could have changed over
            // what they were when the VDocument has first been created. Optimally, we wouldn't
            // need access to the request here at all and the important information would be
            // contained in the VDocument already.
            var request = RenderRequest.builder() //
                    .withState(state) //
                    .withSessionOwner(userService.getCurrentUser()) //
                    .withWindow(state.getWindowBeginOffset(), state.getWindowEndOffset()) //
                    .withClipArcs(true) //
                    .withClipSpans(true) //
                    .withLongArcs(true) //
                    .build();
            var response = bratSerializer.render(getModelObject().getVDocument(), request);

            return JSONUtil.toInterpretableJsonString(response);
        }
        catch (Exception e) {
            handleError("Unable to render annotatations", e);
            return "{}";
        }
    }

    private String getCollectionData()
    {
        try {
            var aState = getModelObject().getAnnotatorState();
            var info = new GetCollectionInformationResponse();
            info.setEntityTypes(bratSchemaGenerator.buildEntityTypes(aState.getProject(),
                    aState.getAnnotationLayers()));
            return JSONUtil.toInterpretableJsonString(info);
        }
        catch (IOException e) {
            handleError("Unablet to render collection information", e);
            return "{}";
        }
    }

    private void handleError(String aMessage, Exception e)
    {
        RequestCycle requestCycle = RequestCycle.get();
        requestCycle.find(AjaxRequestTarget.class)
                .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));

        if (e instanceof AnnotationException) {
            // These are common exceptions happening as part of the user interaction. We do
            // not really need to log their stack trace to the log.
            error(aMessage + ": " + e.getMessage());
            // If debug is enabled, we'll also write the error to the log just in case.
            if (LOG.isDebugEnabled()) {
                LOG.error("{}: {}", aMessage, e.getMessage(), e);
            }
            return;
        }

        LOG.error("{}", aMessage, e);
        error(aMessage);
    }

    private String bratRenderCommand(String aJson)
    {
        String str = WicketUtil.wrapInTryCatch("Wicket.$('" + vis.getMarkupId()
                + "').dispatcher.post('renderData', [" + aJson + "]);");
        return str;
    }

    public void render(AjaxRequestTarget aTarget)
    {
        LOG.debug("[{}][{}] render", getMarkupId(), vis.getMarkupId());

        // Controls whether rendering should happen within the AJAX request or after the AJAX
        // request. Doing it within the request has the benefit of the browser only having to
        // recalculate the layout once at the end of the AJAX request (at least theoretically)
        // while deferring the rendering causes the AJAX request to complete faster, but then
        // the browser needs to recalculate its layout twice - once of any Wicket components
        // being re-rendered and once for the brat view to re-render.
        final boolean deferredRendering = false;

        var js = new StringBuilder();

        if (deferredRendering) {
            js.append("setTimeout(function() {");
        }

        js.append(bratRenderCommand(getDocumentData()));

        if (deferredRendering) {
            js.append("}, 0);");
        }

        aTarget.appendJavaScript(js);
    }

    protected abstract void onClientEvent(AjaxRequestTarget aTarget) throws Exception;

    private final class CurationLazyDetailsHandler
        extends EditorAjaxRequestHandlerBase
        implements Serializable
    {
        private static final long serialVersionUID = -5651753631846550817L;

        @Override
        public String getCommand()
        {
            return LazyDetailsHandler.COMMAND;
        }

        @Override
        public AjaxResponse handle(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget,
                Request aRequest)
        {
            try {
                final IRequestParameters request = getRequest().getPostParameters();
                final VID paramId = BratRequestUtils.getVidFromRequest(request);

                AnnotatorSegmentState segment = getModelObject();
                AnnotatorState state = segment.getAnnotatorState();
                CasProvider casProvider = () -> documentService.readAnnotationCas(
                        segment.getAnnotatorState().getDocument(),
                        AnnotationSet.forUser(segment.getUser().getUsername()));
                var result = lazyDetailsLookupService.lookupLazyDetails(request, paramId,
                        casProvider, state.getDocument(), segment.getUser(),
                        state.getWindowBeginOffset(), state.getWindowEndOffset());
                attachResponse(aTarget, aRequest, toInterpretableJsonString(result));

                return new DefaultAjaxResponse(LazyDetailsHandler.COMMAND);
            }
            catch (Exception e) {
                return handleError("Unable to load lazy details", e);
            }
        }
    }

    private final class CurationActionAjaxRequestHandler
        extends EditorAjaxRequestHandlerBase
        implements Serializable
    {
        private static final long serialVersionUID = 8053988681869772378L;

        @Override
        public AjaxResponse handle(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget,
                Request aRequest)
        {
            try {
                onClientEvent(aTarget);
                return new DefaultAjaxResponse(getAction(aRequest));
            }
            catch (Exception e) {
                return handleError("Unable to merge", e);
            }
        }

        @Override
        public String getCommand()
        {
            return "clientEvent";
        }

        @Override
        public boolean accepts(Request aRequest)
        {
            return true;
        }
    }
}
