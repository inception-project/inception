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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.jquery.ui.settings.JQueryUILibrarySettings;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratRequestUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratCurationResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.CurationPage;
import de.tudarmstadt.ukp.inception.diam.editor.actions.LazyDetailsHandler;
import de.tudarmstadt.ukp.inception.diam.editor.lazydetails.LazyDetailsLookupService;

/**
 * Wicket panel for visualizing an annotated sentence in brat. When a user clicks on a span or an
 * arc, the Method onSelectAnnotationForMerge() is called. Override that method to receive the
 * result in another Wicket panel.
 */
public abstract class BratSuggestionVisualizer
    extends BratVisualizer
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = 6653508018500736430L;

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userService;
    private @SpringBean DocumentService documentService;
    private @SpringBean LazyDetailsLookupService lazyDetailsLookupService;

    private final ConfirmationDialog stateChangeConfirmationDialog;
    private final AbstractDefaultAjaxBehavior controller;

    private final int position;

    public BratSuggestionVisualizer(String aId, IModel<AnnotatorSegment> aModel, int aPosition)
    {
        super(aId, aModel);

        position = aPosition;

        add(new Label("username", getModel().map(this::maybeAnonymizeUsername)));

        add(stateChangeConfirmationDialog = new ConfirmationDialog("stateChangeConfirmationDialog",
                new StringResourceModel("StateChangeConfirmationDialog.title", this, null),
                new StringResourceModel("StateChangeConfirmationDialog.text", this, null)));
        stateChangeConfirmationDialog.setConfirmAction(this::actionToggleAnnotationDocumentState);

        LambdaAjaxLink stateToggle = new LambdaAjaxLink("stateToggle",
                stateChangeConfirmationDialog::show);
        stateToggle.setOutputMarkupId(true);
        stateToggle.add(new Label("state",
                LoadableDetachableModel.of(() -> getAnnotationDocumentState().symbol()))
                        .setEscapeModelStrings(false));
        add(stateToggle);

        controller = new AbstractDefaultAjaxBehavior()
        {
            private static final long serialVersionUID = 1133593826878553307L;

            @Override
            protected void respond(AjaxRequestTarget aTarget)
            {
                try {
                    final IRequestParameters request = getRequest().getPostParameters();
                    String action = BratRequestUtils.getActionFromRequest(request);
                    final VID paramId = BratRequestUtils.getVidFromRequest(request);

                    if (LazyDetailsHandler.COMMAND.equals(action)) {
                        AnnotatorSegment segment = getModelObject();
                        AnnotatorState state = segment.getAnnotatorState();
                        CasProvider casProvider = () -> documentService.readAnnotationCas(
                                segment.getAnnotatorState().getDocument(),
                                segment.getUser().getUsername());
                        var result = lazyDetailsLookupService.actionLookupNormData(request, paramId,
                                casProvider, state.getDocument(), segment.getUser(),
                                state.getWindowBeginOffset(), state.getWindowEndOffset());

                        try {
                            BratRequestUtils.attachResponse(aTarget, vis, result);
                        }
                        catch (IOException e) {
                            handleError("Unable to produce JSON response", e);
                        }
                    }
                    else {
                        onClientEvent(aTarget);
                    }
                }
                catch (Exception e) {
                    aTarget.addChildren(getPage(), IFeedback.class);
                    error("Error: " + e.getMessage());
                }
            }

        };
        add(controller);
    }

    private void actionToggleAnnotationDocumentState(AjaxRequestTarget aTarget)
    {
        var username = getModelObject().getUser().getUsername();
        var doc = getModelObject().getAnnotatorState().getDocument();
        var annDoc = documentService.getAnnotationDocument(doc, username);
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

        ((CurationPage) getPage()).actionLoadDocument(aTarget);
    }

    private AnnotationDocumentState getAnnotationDocumentState()
    {
        var username = getModelObject().getUser().getUsername();
        var doc = getModelObject().getAnnotatorState().getDocument();
        return documentService.getAnnotationDocument(doc, username).getState();
    }

    private String maybeAnonymizeUsername(AnnotatorSegment aSegment)
    {
        Project project = aSegment.getAnnotatorState().getProject();
        if (project.isAnonymousCuration()
                && !projectService.isManager(project, userService.getCurrentUser())) {
            return "Anonymized annotator " + (position + 1);
        }

        return aSegment.getUser().getUiName();
    }

    public void setModel(IModel<AnnotatorSegment> aModel)
    {
        setDefaultModel(aModel);
    }

    public void setModelObject(AnnotatorSegment aModel)
    {
        setDefaultModelObject(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorSegment> getModel()
    {
        return (IModel<AnnotatorSegment>) getDefaultModel();
    }

    public AnnotatorSegment getModelObject()
    {
        return (AnnotatorSegment) getDefaultModelObject();
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        // MUST NOT CALL super.renderHead here because that would call Util.embedByUrl again!
        // super.renderHead(aResponse);

        aResponse.render(forReference(JQueryUILibrarySettings.get().getJavaScriptReference()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratCurationResourceReference.get()));

        // BRAT call to load the BRAT JSON from our collProvider and docProvider.
        String script = "BratCuration('" + vis.getMarkupId() + "', '" + controller.getCallbackUrl()
                + "', '" + collProvider.getCallbackUrl() + "', '" + docProvider.getCallbackUrl()
                + "')";
        aResponse.render(OnLoadHeaderItem.forScript("\n" + script));
    }

    @Override
    public String getDocumentData()
    {
        return getModelObject().getDocumentResponse() == null ? "{}"
                : getModelObject().getDocumentResponse();
    }

    @Override
    protected String getCollectionData()
    {
        return getModelObject().getCollectionData();
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

    protected abstract void onClientEvent(AjaxRequestTarget aTarget) throws Exception;
}
