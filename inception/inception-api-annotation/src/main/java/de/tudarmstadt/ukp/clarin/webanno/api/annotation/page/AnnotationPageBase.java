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

import static de.tudarmstadt.ukp.clarin.webanno.model.ValidationMode.NEVER;
import static de.tudarmstadt.ukp.inception.rendering.model.Range.rangeClippedToDocument;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.CENTERED;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.wicketstuff.urlfragment.UrlFragment;
import org.wicketstuff.urlfragment.UrlParametersReceivingBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.NotEditableException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.ValidationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ConstraintsEvaluator;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.ContextMenuLookup;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.wicket.DecoratedObject;
import jakarta.persistence.NoResultException;

public abstract class AnnotationPageBase
    extends ProjectPageBase
{
    private static final long serialVersionUID = -1133219266479577443L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String PAGE_PARAM_DOCUMENT = "d";
    public static final String PAGE_PARAM_DATA_OWNER = "u";
    public static final String PAGE_PARAM_FOCUS = "f";
    public static final String PAGE_PARAM_TEXT_HIGHLIGHT = "hl";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean DocumentAccess documentAccess;
    private @SpringBean UserPreferencesService userPreferenceService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;

    private LoadableDetachableModel<Boolean> annotationFinished = LoadableDetachableModel
            .of(this::loadAnnotationFinished);

    private LoadableDetachableModel<String> annotationNotEditableReason = LoadableDetachableModel
            .of(this::loadAnnotationNotEditableReason);

    protected AnnotationPageBase(PageParameters aParameters)
    {
        super(aParameters);

        var params = getPageParameters();
        var documentParameter = params.get(PAGE_PARAM_DOCUMENT);
        var userParameter = params.get(PAGE_PARAM_DATA_OWNER);

        // If the page was accessed using an URL form ending in a document ID, let's move
        // the document ID into the fragment and redirect to the form without the document ID.
        // This ensures that any links on the page do not carry the document ID, so that we can
        // happily switch between documents using AJAX without having to worry about links with
        // a document ID potentially sending us back to a specific document.
        if (!documentParameter.isEmpty()) {
            var requestCycle = getRequestCycle();

            var fragmentParams = new ArrayList<String>();
            fragmentParams.add(format("%s=%s", PAGE_PARAM_DOCUMENT, documentParameter.toString()));
            params.remove(PAGE_PARAM_DOCUMENT);

            if (!userParameter.isEmpty()) {
                fragmentParams
                        .add(format("%s=%s", PAGE_PARAM_DATA_OWNER, userParameter.toString()));
                params.remove(PAGE_PARAM_DATA_OWNER);
            }

            var url = Url.parse(requestCycle.urlFor(this.getClass(), params));
            var finalUrl = requestCycle.getUrlRenderer().renderFullUrl(url) + "#!"
                    + fragmentParams.stream().collect(joining("&"));
            LOG.trace(
                    "Pushing parameter for document [{}] and user [{}] into fragment: {} (URL redirect)",
                    documentParameter, userParameter, finalUrl);
            throw new RedirectToUrlException(finalUrl.toString());
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
            failWithDocumentNotFound("Document [" + aDocumentParameter
                    + "] does not exist in project [" + aProject.getName() + "]");
        }
        return null;
    }

    protected void failWithDocumentNotFound(String aDetails)
    {
        if (userRepository.isCurrentUserAdmin()) {
            getSession().error(aDetails);
        }
        else {
            getSession().error(
                    "Requested document does not exist or you have no permissions to access it.");
        }
        backToProjectPage();
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
                var document = aRequestParameters.getParameterValue(PAGE_PARAM_DOCUMENT);
                var focus = aRequestParameters.getParameterValue(PAGE_PARAM_FOCUS);
                var user = aRequestParameters.getParameterValue(PAGE_PARAM_DATA_OWNER);

                if (document.isEmpty() && focus.isEmpty()) {
                    return;
                }

                LOG.trace("URL fragment update: {}@{} focus {}", user, document, focus);

                var previousDoc = getModelObject().getDocument();
                var aPreviousUser = getModelObject().getUser();

                handleParameters(document, focus, user);

                updateDocumentView(aTarget, previousDoc, aPreviousUser, focus);
            }
        };
    }

    protected abstract void handleParameters(StringValue aDocumentParameter,
            StringValue aFocusParameter, StringValue aUser);

    /**
     * Switch between documents. Note that the document and data owner to switch to are obtained
     * from the {@link AnnotatorState}. The parameters indicate the the old document and data owner
     * before the switch!
     * 
     * @param aTarget
     *            a request target.
     * @param aPreviousDocument
     *            the document before the switch.
     * @param aPreviousUser
     *            the data owner before the switch.
     * @param aFocusParameter
     *            the focus before the switch.
     */
    protected abstract void updateDocumentView(AjaxRequestTarget aTarget,
            SourceDocument aPreviousDocument, User aPreviousUser, StringValue aFocusParameter);

    protected void updateUrlFragment(AjaxRequestTarget aTarget)
    {
        // No AJAX request - nothing to do
        if (aTarget == null) {
            return;
        }

        // Update URL for current document
        try {
            aTarget.registerRespondListener(new UrlFragmentUpdateListener());
        }
        catch (Exception e) {
            LOG.debug("Unable to request URL fragment update anymore", e);
        }
    }

    /**
     * Show the specified document.
     * 
     * @param aTarget
     *            the AJAX request target
     * @param aDocument
     *            the document to open
     * @return whether the document had to be switched or not.
     */
    public boolean actionShowDocument(AjaxRequestTarget aTarget, SourceDocument aDocument)
    {
        if (Objects.equals(aDocument.getId(), getModelObject().getDocument().getId())) {
            return false;
        }

        var docs = getListOfDocs();
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

    /**
     * Show the next document if it exists, starting in a certain begin offset
     * 
     * @param aTarget
     *            the AJAX request target
     * @param aDocument
     *            the document to open
     * @param aBegin
     *            the position in the document to scroll to
     * @param aEnd
     *            the position in the document to scroll to
     * @throws IOException
     *             if there was a problem retrieving the CAS
     */
    public void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            int aBegin, int aEnd)
        throws IOException
    {
        boolean switched = actionShowDocument(aTarget, aDocument);

        var state = getModelObject();

        var cas = getEditorCas();
        var range = rangeClippedToDocument(cas, aBegin, aEnd);
        state.getPagingStrategy().moveToOffset(state, cas, aBegin,
                new VRange(range.getBegin(), range.getEnd()), CENTERED);

        if (!switched && state.getPagingStrategy() instanceof NoPagingStrategy) {
            return;
        }

        actionRefreshDocument(aTarget);
    }

    protected void handleException(AjaxRequestTarget aTarget, Exception aException)
    {
        if (aException instanceof ReplaceHandlerException replaceHandlerException) {
            // Let Wicket redirects still work
            throw replaceHandlerException;
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

    public abstract Optional<ContextMenuLookup> getContextMenuLookup();

    public abstract void writeEditorCas(CAS aCas) throws IOException, AnnotationException;

    /**
     * Open a document or to a different document. This method should be used only the first time
     * that a document is accessed. It reset the annotator state and upgrades the CAS.
     * 
     * @param aTarget
     *            the AJAX request target
     */
    public abstract void actionLoadDocument(AjaxRequestTarget aTarget);

    /**
     * Re-render the document and update all related UI elements.
     * 
     * This method should be used while the editing process is ongoing. It does not upgrade the CAS
     * and it does not reset the annotator state.
     * 
     * @param aTarget
     *            the AJAX request target
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
        var editorCas = aCas;
        var layer = aAdapter.getLayer();
        var features = aAdapter.listFeatures();

        // If no feature is required, then we can skip the whole procedure
        if (features.stream().allMatch((f) -> !f.isRequired())) {
            return;
        }

        var evaluator = new ConstraintsEvaluator();
        var constraints = getModelObject().getConstraints();

        // Check each feature structure of this layer
        var layerType = aAdapter.getAnnotationType(editorCas).get();
        var annotationFsType = editorCas.getAnnotationType();
        try (var fses = editorCas.select(layerType)) {
            for (var fs : fses) {
                for (var f : features) {
                    if (!f.isRequired()) {
                        continue;
                    }
                    if (evaluator.isHiddenConditionalFeature(constraints, fs, f)) {
                        continue;
                    }

                    if (aAdapter.isFeatureValueValid(f, fs)) {
                        continue;
                    }

                    // Jump to invalid annotation if possible
                    if (editorCas.getTypeSystem().subsumes(annotationFsType, layerType)) {
                        getAnnotationActionHandler().actionSelectAndJump(aTarget, VID.of(fs));
                    }

                    // Inform the user
                    throw new ValidationException("Annotation with ID [" + ICasUtil.getAddr(fs)
                            + "] on layer [" + layer.getUiName()
                            + "] has invalid feature value in [" + f.getUiName() + "].");
                }
            }
        }
    }

    public void actionValidateDocument(AjaxRequestTarget aTarget, CAS aCas)
        throws ValidationException, IOException, AnnotationException
    {
        var state = getModelObject();
        for (var layer : annotationService.listAnnotationLayer(state.getProject())) {
            if (!layer.isEnabled()) {
                // No validation for disabled layers since there is nothing the annotator could do
                // about fixing annotations on disabled layers.
                continue;
            }

            if (layer.getValidationMode() == NEVER) {
                // If validation is disabled, then skip it
                continue;
            }

            var adapter = annotationService.getAdapter(layer);

            validateRequiredFeatures(aTarget, aCas, adapter);

            var messages = adapter.validate(aCas);
            if (!messages.isEmpty()) {
                var message = messages.get(0).getLeft();
                var fs = messages.get(0).getRight();

                getAnnotationActionHandler().actionSelectAndJump(aTarget, VID.of(fs));

                // Inform the user
                throw new ValidationException("Annotation with ID [" + VID.of(fs) + "] on layer ["
                        + layer.getUiName() + "] is invalid: " + message.getMessage());
            }
        }
    }

    /**
     * Load the user preferences. A side-effect of this method is that the active annotation layer
     * is refreshed based on the visibility preferences and based on the project to which the
     * document being edited belongs.
     */
    protected void loadPreferences() throws IOException
    {
        var state = getModelObject();
        userPreferenceService.loadPreferences(state, userRepository.getCurrentUsername());
    }

    private String loadAnnotationNotEditableReason()
    {
        try {
            var state = getModelObject();
            var sessionOwner = userRepository.getCurrentUser();
            documentAccess.assertCanEditAnnotationDocument(sessionOwner, state.getDocument(),
                    state.getUser().getUsername());
            return null;
        }
        catch (AccessDeniedException e) {
            return e.getMessage();
        }
    }

    public void ensureIsEditable() throws NotEditableException
    {
        var state = getModelObject();

        if (state.getDocument() == null) {
            throw new NotEditableException("No document selected");
        }

        var notEditableReason = annotationNotEditableReason.getObject();
        if (notEditableReason != null) {
            throw new NotEditableException(notEditableReason);
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
        var state = getModelObject();
        return documentService.isAnnotationFinished(state.getDocument(), state.getUser());
    }

    @Override
    public void detachModels()
    {
        super.detachModels();
        annotationFinished.detach();
        annotationNotEditableReason.detach();
    }

    public abstract IModel<List<DecoratedObject<Project>>> getAllowedProjects();

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
            var state = getModelObject();

            if (state.getDocument() == null) {
                return;
            }

            var currentDocumentId = state.getDocument().getId();
            var currentFocusUnitIndex = state.getFocusUnitIndex();

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

            var fragment = new UrlFragment(aTarget);

            fragment.putParameter(PAGE_PARAM_DOCUMENT, currentDocumentId);

            if (state.getFocusUnitIndex() > 0) {
                fragment.putParameter(PAGE_PARAM_FOCUS, currentFocusUnitIndex);
            }
            else {
                fragment.removeParameter(PAGE_PARAM_FOCUS);
            }

            // REC: We currently do not want that one can switch to the CURATION_USER directly via
            // the URL without having to activate sidebar curation mode as well, so we do not handle
            // the CURATION_USER here.
            if (Set.of(userRepository.getCurrentUsername(), CURATION_USER)
                    .contains(state.getUser().getUsername())) {
                fragment.removeParameter(PAGE_PARAM_DATA_OWNER);
            }
            else {
                fragment.putParameter(PAGE_PARAM_DATA_OWNER, state.getUser().getUsername());
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
