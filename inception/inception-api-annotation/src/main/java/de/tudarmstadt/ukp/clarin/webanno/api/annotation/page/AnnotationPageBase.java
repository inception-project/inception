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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.page;

import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.CENTERED;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SelectFSs;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.cas.TOP;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.wicketstuff.urlfragment.UrlFragment;
import org.wicketstuff.urlfragment.UrlParametersReceivingBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.NotEditableException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.ValidationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.ValidationMode;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.preferences.Key;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.validation.ValidationUtils;

public abstract class AnnotationPageBase
    extends ProjectPageBase
{
    private static final long serialVersionUID = -1133219266479577443L;

    public static final Key<AnnotationEditorState> KEY_EDITOR_STATE = new Key<>(
            AnnotationEditorState.class, "annotation/editor");

    public static final String PAGE_PARAM_DOCUMENT = "d";
    public static final String PAGE_PARAM_USER = "u";
    public static final String PAGE_PARAM_FOCUS = "f";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean UserPreferencesService userPreferenceService;
    private @SpringBean UserDao userRepository;

    private LoadableDetachableModel<Boolean> annotationFinished = LoadableDetachableModel
            .of(this::loadAnnotationFinished);

    protected AnnotationPageBase(PageParameters aParameters)
    {
        super(aParameters);

        StringValue documentParameter = getPageParameters().get(PAGE_PARAM_DOCUMENT);
        StringValue userParameter = getPageParameters().get(PAGE_PARAM_USER);

        // If the page was accessed using an URL form ending in a document ID, let's move
        // the document ID into the fragment and redirect to the form without the document ID.
        // This ensures that any links on the page do not carry the document ID, so that we can
        // happily switch between documents using AJAX without having to worry about links with
        // a document ID potentially sending us back to a specific document.
        if (!documentParameter.isEmpty()) {
            RequestCycle requestCycle = getRequestCycle();
            Url clientUrl = requestCycle.getRequest().getClientUrl();
            clientUrl.resolveRelative(Url.parse("./"));
            List<String> fragmentParams = new ArrayList<>();
            fragmentParams.add(format("%s=%s", PAGE_PARAM_DOCUMENT, documentParameter.toString()));
            if (!userParameter.isEmpty()) {
                fragmentParams.add(format("%s=%s", PAGE_PARAM_USER, userParameter.toString()));
            }
            clientUrl.setFragment("!" + fragmentParams.stream().collect(joining("&")));
            String url = requestCycle.getUrlRenderer().renderRelativeUrl(clientUrl);
            throw new RedirectToUrlException(url.toString());
        }
    }

    public void setModel(IModel<AnnotatorState> aModel)
    {
        setDefaultModel(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    public void setModelObject(AnnotatorState aModel)
    {
        setDefaultModelObject(aModel);
    }

    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    protected SourceDocument getDocumentFromParameters(Project aProject,
            StringValue aDocumentParameter)
    {
        if (aDocumentParameter.isEmpty()) {
            return null;
        }

        try {
            try {
                long documentId = aDocumentParameter.toLong();
                return documentService.getSourceDocument(aProject.getId(), documentId);
            }
            catch (StringValueConversionException e) {
                // If it is not a number, try interpreting it as a name
            }

            return documentService.getSourceDocument(aProject, aDocumentParameter.toString());
        }
        catch (NoResultException e) {
            error("Document [" + aDocumentParameter + "] does not exist in project ["
                    + aProject.getName() + "]");
        }

        return null;
    }

    protected UrlParametersReceivingBehavior createUrlFragmentBehavior()
    {
        return new UrlParametersReceivingBehavior()
        {
            private static final long serialVersionUID = -3860933016636718816L;

            @Override
            protected void onParameterArrival(IRequestParameters aRequestParameters,
                    AjaxRequestTarget aTarget)
            {
                StringValue document = aRequestParameters.getParameterValue(PAGE_PARAM_DOCUMENT);
                StringValue focus = aRequestParameters.getParameterValue(PAGE_PARAM_FOCUS);
                StringValue user = aRequestParameters.getParameterValue(PAGE_PARAM_USER);

                // nothing changed, do not check for project, because inception always opens
                // on a project
                if (document.isEmpty() && focus.isEmpty()) {
                    return;
                }

                SourceDocument previousDoc = getModelObject().getDocument();
                User aPreviousUser = getModelObject().getUser();
                handleParameters(document, focus, user);

                updateDocumentView(aTarget, previousDoc, aPreviousUser, focus);
            }
        };
    }

    protected abstract void handleParameters(StringValue aDocumentParameter,
            StringValue aFocusParameter, StringValue aUser);

    protected abstract void updateDocumentView(AjaxRequestTarget aTarget,
            SourceDocument aPreviousDocument, User aPreviousUser, StringValue aFocusParameter);

    protected void updateUrlFragment(AjaxRequestTarget aTarget)
    {
        // No AJAX request - nothing to do
        if (aTarget == null) {
            return;
        }

        aTarget.registerRespondListener(new UrlFragmentUpdateListener());
    }

    /**
     * Show the specified document.
     * 
     * @return whether the document had to be switched or not.
     */
    public boolean actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument)
    {
        if (!Objects.equals(aDocument.getId(), getModelObject().getDocument().getId())) {
            List<SourceDocument> docs = getListOfDocs();
            if (!docs.contains(aDocument)) {
                error("The document [" + aDocument.getName() + "] is not accessible");
                if (aTarget != null) {
                    aTarget.addChildren(getPage(), IFeedback.class);
                }
                return false;
            }

            getModelObject().setDocument(aDocument, docs);
            actionLoadDocument(aTarget);
            return true;
        }

        return false;
    }

    /**
     * Show the next document if it exists, starting in a certain begin offset
     */
    public void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            int aBegin, int aEnd)
        throws IOException
    {
        boolean switched = actionShowSelectedDocument(aTarget, aDocument);

        AnnotatorState state = getModelObject();

        CAS cas = getEditorCas();
        state.getPagingStrategy().moveToOffset(state, cas, aBegin, CENTERED);

        if (!switched && state.getPagingStrategy() instanceof NoPagingStrategy) {
            return;
        }

        actionRefreshDocument(aTarget);
    }

    protected void handleException(AjaxRequestTarget aTarget, Exception aException)
    {
        if (aException instanceof ReplaceHandlerException) {
            // Let Wicket redirects still work
            throw (ReplaceHandlerException) aException;
        }

        LoggerFactory.getLogger(getClass()).error("Error: " + aException.getMessage(), aException);
        error("Error: " + aException.getMessage());
        if (aTarget != null) {
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    public abstract List<SourceDocument> getListOfDocs();

    public abstract CAS getEditorCas() throws IOException;

    public abstract AnnotationActionHandler getAnnotationActionHandler();

    public abstract void writeEditorCas(CAS aCas) throws IOException, AnnotationException;

    /**
     * Open a document or to a different document. This method should be used only the first time
     * that a document is accessed. It reset the annotator state and upgrades the CAS.
     */
    public abstract void actionLoadDocument(AjaxRequestTarget aTarget);

    /**
     * Re-render the document and update all related UI elements.
     * 
     * This method should be used while the editing process is ongoing. It does not upgrade the CAS
     * and it does not reset the annotator state.
     */
    public abstract void actionRefreshDocument(AjaxRequestTarget aTarget);

    /**
     * Checks if all required features on all annotations are set. If a required feature value is
     * missing, then the method scrolls to that location and schedules a re-rendering. In such a
     * case, an {@link IllegalStateException} is thrown.
     */
    protected void validateRequiredFeatures(AjaxRequestTarget aTarget, CAS aCas,
            TypeAdapter aAdapter)
        throws ValidationException, IOException, AnnotationException
    {
        CAS editorCas = aCas;
        AnnotationLayer layer = aAdapter.getLayer();
        List<AnnotationFeature> features = annotationService.listAnnotationFeature(layer);

        // If no feature is required, then we can skip the whole procedure
        if (features.stream().allMatch((f) -> !f.isRequired())) {
            return;
        }

        // Check each feature structure of this layer
        Type layerType = aAdapter.getAnnotationType(editorCas);
        Type annotationFsType = editorCas.getAnnotationType();
        try (SelectFSs<TOP> fses = editorCas.select(layerType)) {
            for (FeatureStructure fs : fses) {
                for (AnnotationFeature f : features) {
                    if (ValidationUtils.isRequiredFeatureMissing(f, fs)) {
                        // If it is an annotation, then we jump to it if it has required empty
                        // features
                        if (editorCas.getTypeSystem().subsumes(annotationFsType, layerType)) {
                            getAnnotationActionHandler().actionSelectAndJump(aTarget, new VID(fs));
                        }

                        // Inform the user
                        throw new ValidationException("Annotation with ID [" + ICasUtil.getAddr(fs)
                                + "] on layer [" + layer.getUiName()
                                + "] is missing value for feature [" + f.getUiName() + "].");
                    }
                }
            }
        }
    }

    public void actionValidateDocument(AjaxRequestTarget aTarget, CAS aCas)
        throws ValidationException, IOException, AnnotationException
    {
        AnnotatorState state = getModelObject();
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(state.getProject())) {
            if (!layer.isEnabled()) {
                // No validation for disabled layers since there is nothing the annotator could do
                // about fixing annotations on disabled layers.
                continue;
            }

            if (ValidationMode.NEVER.equals(layer.getValidationMode())) {
                // If validation is disabled, then skip it
                continue;
            }

            TypeAdapter adapter = annotationService.getAdapter(layer);

            validateRequiredFeatures(aTarget, aCas, adapter);

            List<Pair<LogMessage, AnnotationFS>> messages = adapter.validate(aCas);
            if (!messages.isEmpty()) {
                LogMessage message = messages.get(0).getLeft();
                AnnotationFS fs = messages.get(0).getRight();

                getAnnotationActionHandler().actionSelectAndJump(aTarget, new VID(fs));

                // Inform the user
                throw new ValidationException(
                        "Annotation with ID [" + ICasUtil.getAddr(fs) + "] on layer ["
                                + layer.getUiName() + "] is invalid: " + message.getMessage());
            }
        }
    }

    /**
     * Load the user preferences. A side-effect of this method is that the active annotation layer
     * is refreshed based on the visibility preferences and based on the project to which the
     * document being edited belongs.
     */
    protected void loadPreferences() throws BeansException, IOException
    {
        AnnotatorState state = getModelObject();
        userPreferenceService.loadPreferences(state, userRepository.getCurrentUsername());
    }

    public void ensureIsEditable() throws NotEditableException
    {
        AnnotatorState state = getModelObject();

        if (state.getDocument() == null) {
            throw new NotEditableException("No document selected");
        }

        // If curating (check mode for curation page and user for curation sidebar),
        // then it is editable unless the curation is finished
        if (state.getMode().equals(CURATION)
                || state.getUser().getUsername().equals(CURATION_USER)) {
            if (state.getDocument().getState().equals(CURATION_FINISHED)) {
                throw new NotEditableException("Curation is already finished. You can put it back "
                        + "into progress via the monitoring page.");
            }

            return;
        }

        if (getModelObject().isUserViewingOthersWork(userRepository.getCurrentUsername())) {
            throw new NotEditableException(
                    "Viewing another users annotations - document is read-only!");
        }

        if (isAnnotationFinished()) {
            throw new NotEditableException("This document is already closed for user ["
                    + state.getUser().getUsername() + "]. Please ask your "
                    + "project manager to re-open it via the monitoring page.");
        }
    }

    public boolean isEditable()
    {
        try {
            ensureIsEditable();
            return true;
        }
        catch (NotEditableException e) {
            return false;
        }
    }

    public boolean isAnnotationFinished()
    {
        return annotationFinished.getObject();
    }

    private boolean loadAnnotationFinished()
    {
        AnnotatorState state = getModelObject();
        return documentService.isAnnotationFinished(state.getDocument(), state.getUser());
    }

    @Override
    public void detachModels()
    {
        super.detachModels();
        annotationFinished.detach();
    }

    public abstract IModel<List<DecoratedObject<Project>>> getAllowedProjects();

    public abstract List<DecoratedObject<SourceDocument>> listAccessibleDocuments(Project aProject,
            User aUser);

    /**
     * This is a special AJAX target response listener which implements hashCode and equals. It uses
     * the markup ID of its host component to identify itself. This enables us to add multiple
     * instances of this listener to an AJAX response without *actually* adding multiple instances
     * since the AJAX response internally keeps track of the listeners using a set.
     */
    private class UrlFragmentUpdateListener
        implements AjaxRequestTarget.ITargetRespondListener
    {
        // These are page state variables used by the UrlFragmentUpdateListener to determine whether
        // an update of the URL parameters is necessary at all
        private Long urlFragmentLastDocumentId;
        private int urlFragmentLastFocusUnitIndex;

        @Override
        public void onTargetRespond(AjaxRequestTarget aTarget)
        {
            AnnotatorState state = getModelObject();

            if (state.getDocument() == null) {
                return;
            }

            Long currentDocumentId = state.getDocument().getId();
            int currentFocusUnitIndex = state.getFocusUnitIndex();

            // Check if the relevant parameters have actually changed since the URL parameters were
            // last set - if this is not the case, then let's not set the parameters because that
            // triggers another AJAX request telling us that the parameters were updated (stupid,
            // right?)
            if (Objects.equals(urlFragmentLastDocumentId, currentDocumentId)
                    && urlFragmentLastFocusUnitIndex == currentFocusUnitIndex) {
                return;
            }

            urlFragmentLastDocumentId = currentDocumentId;
            urlFragmentLastFocusUnitIndex = currentFocusUnitIndex;

            UrlFragment fragment = new UrlFragment(aTarget);

            fragment.putParameter(PAGE_PARAM_DOCUMENT, currentDocumentId);

            if (state.getFocusUnitIndex() > 0) {
                fragment.putParameter(PAGE_PARAM_FOCUS, currentFocusUnitIndex);
            }
            else {
                fragment.removeParameter(PAGE_PARAM_FOCUS);
            }

            if (userRepository.getCurrentUsername().equals(state.getUser().getUsername())) {
                fragment.removeParameter(PAGE_PARAM_USER);
            }
            else {
                fragment.putParameter(PAGE_PARAM_USER, state.getUser().getUsername());
            }

            // If we do not manually set editedFragment to false, then changing the URL
            // manually or using the back/forward buttons in the browser only works every
            // second time. Might be a bug in wicketstuff urlfragment... not sure.
            aTarget.appendJavaScript(
                    "try{if(window.UrlUtil){window.UrlUtil.editedFragment = false;}}catch(e){}");
        }

        private AnnotationPageBase getOuterType()
        {
            return AnnotationPageBase.this;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            UrlFragmentUpdateListener other = (UrlFragmentUpdateListener) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            return true;
        }
    }
}
