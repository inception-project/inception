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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.page;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.danekja.java.util.function.serializable.SerializableSupplier;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.settings.MergeStrategyPanel;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.AjaxCallback;
import de.tudarmstadt.ukp.inception.support.lambda.AjaxFormCallback;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class MergeDialog
    extends BootstrapModalDialog
{
    private static final long serialVersionUID = 5194857538069045172L;

    private final ResourceModel titleModel;
    private final ResourceModel challengeModel;
    private final IModel<String> expectedResponseModel;
    private final IModel<CurationWorkflow> curationWorkflowModel;

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userService;

    private AjaxFormCallback<State> confirmAction;
    private AjaxCallback cancelAction;
    private ContentPanel conentPanel;

    private SerializableSupplier<List<AnnotationSet>> dataOwnerSource;
    private SerializableSupplier<Collection<AnnotationSet>> sidebarSelectionSource;
    private List<AnnotationSet> candidateDataOwners = emptyList();

    public MergeDialog(String aId, ResourceModel aTitle, ResourceModel aChallenge,
            IModel<String> aExpectedResponse, IModel<CurationWorkflow> aCurationWorkflow)
    {
        super(aId);

        titleModel = aTitle;
        challengeModel = aChallenge;
        expectedResponseModel = aExpectedResponse;
        curationWorkflowModel = aCurationWorkflow;

        setOutputMarkupId(true);

        setModel(new CompoundPropertyModel<>(null));

        setContent(conentPanel = new ContentPanel(CONTENT_ID, getModel()));
    }

    public void setModel(IModel<State> aModel)
    {
        setDefaultModel(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<State> getModel()
    {
        return (IModel<State>) getDefaultModel();
    }

    public void setModelObject(State aModel)
    {
        setDefaultModelObject(aModel);
    }

    public State getModelObject()
    {
        return (State) getDefaultModelObject();
    }

    /**
     * @param aDataOwnerSource
     *            provides the annotators the curator can choose to merge from. Without it, the
     *            dialog offers no annotator choice and cannot be confirmed, since there is nobody
     *            to merge from.
     * @return the dialog itself for chaining.
     */
    public MergeDialog setDataOwnerSource(
            SerializableSupplier<List<AnnotationSet>> aDataOwnerSource)
    {
        dataOwnerSource = aDataOwnerSource;
        return this;
    }

    /**
     * @param aSidebarSelectionSource
     *            provides the annotators selected in the curation sidebar. Without it, the dialog
     *            offers no quick selection for them - e.g. on a page without such a selection.
     * @return the dialog itself for chaining.
     */
    public MergeDialog setSidebarSelectionSource(
            SerializableSupplier<Collection<AnnotationSet>> aSidebarSelectionSource)
    {
        sidebarSelectionSource = aSidebarSelectionSource;
        return this;
    }

    public void show(AjaxRequestTarget aTarget)
    {
        challengeModel.detach();
        expectedResponseModel.detach();

        candidateDataOwners = dataOwnerSource != null ? dataOwnerSource.get() : emptyList();

        // The content panel is reused across openings. Input left over from a submit that failed
        // validation would otherwise win over the fresh state - for the annotator checkboxes, it
        // would not even match the newly created list items, so they would all show as unticked.
        // Form.clearInput() would skip everything here, since it only clears visible components
        // and the dialog is still closed.
        conentPanel.form.visitFormComponents((formComponent, visit) -> formComponent.clearInput());

        var state = new State();
        // Like the initial merge, consider all annotators unless the curator decides otherwise
        state.dataOwners = new ArrayList<>(candidateDataOwners);
        setModelObject(state);

        // The response field lives inside the challenge container, which is only rendered when
        // discarding already curated annotations. Only focus it when it is actually visible.
        if (state.clearTargetCas) {
            aTarget.focusComponent(conentPanel.responseField);
        }

        open(aTarget);
    }

    public AjaxFormCallback<State> getConfirmAction()
    {
        return confirmAction;
    }

    public void setConfirmAction(AjaxFormCallback<State> aConfirmAction)
    {
        confirmAction = aConfirmAction;
    }

    public AjaxCallback getCancelAction()
    {
        return cancelAction;
    }

    public void setCancelAction(AjaxCallback aCancelAction)
    {
        cancelAction = aCancelAction;
    }

    protected void onConfirmInternal(AjaxRequestTarget aTarget, Form<State> aForm)
    {
        State state = aForm.getModelObject();

        // The challenge only guards the destructive case where already curated annotations are
        // discarded. When only filling in not-yet-curated positions, no challenge is required.
        if (state.clearTargetCas
                && !Objects.equals(expectedResponseModel.getObject(), state.response)) {
            info("Your response did not meet the challenge.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        // A merge without any annotator to merge from does nothing - or, when discarding, destroys
        // the curation results
        if (state.dataOwners.isEmpty()) {
            error(candidateDataOwners.isEmpty()
                    ? "There are no annotators whose annotations could be merged."
                    : "Select at least one annotator to merge from.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var closeOk = true;

        // Invoke callback if one is defined
        if (confirmAction != null) {
            try {
                confirmAction.accept(aTarget, aForm);
            }
            catch (ReplaceHandlerException e) {
                // Let Wicket redirects still work
                throw e;
            }
            catch (Exception e) {
                LoggerFactory.getLogger(getPage().getClass()).error("Error: " + e.getMessage(), e);
                info("Unable to perform merge: " + getRootCauseMessage(e));
                aTarget.addChildren(getPage(), IFeedback.class);
                closeOk = false;
            }
        }

        if (closeOk) {
            close(aTarget);
        }
    }

    protected void onCancelInternal(AjaxRequestTarget aTarget)
    {
        if (cancelAction != null) {
            try {
                cancelAction.accept(aTarget);
            }
            catch (Exception e) {
                LoggerFactory.getLogger(getPage().getClass()).error("Error: " + e.getMessage(), e);
                info("Unable to perform merge: " + getRootCauseMessage(e));
                aTarget.addChildren(getPage(), IFeedback.class);
            }
        }

        close(aTarget);
    }

    /**
     * Anonymizes the annotator names in the same way as the curation sidebar does.
     */
    private String dataOwnerLabel(ListItem<AnnotationSet> aItem)
    {
        var project = curationWorkflowModel.getObject().getProject();
        if (project.isAnonymousCuration()
                && !projectService.hasRole(userService.getCurrentUser(), project, MANAGER)) {
            return "Anonymized annotator " + (aItem.getIndex() + 1);
        }

        return aItem.getModelObject().displayName();
    }

    public static class State
        implements Serializable
    {
        private static final long serialVersionUID = 4483229579553569947L;

        String response;
        boolean saveSettingsAsDefault;
        boolean clearTargetCas = false;
        Collection<AnnotationSet> dataOwners = new ArrayList<>();

        public boolean isSaveSettingsAsDefault()
        {
            return saveSettingsAsDefault;
        }

        /**
         * @return the annotators to merge from.
         */
        public Collection<AnnotationSet> getDataOwners()
        {
            return dataOwners;
        }

        public boolean isClearTargetCas()
        {
            return clearTargetCas;
        }
    }

    private class ContentPanel
        extends Panel
    {
        private static final long serialVersionUID = 5202661827792148838L;

        private final LambdaAjaxLink cancelButton;

        private final Form<State> form;

        private CheckGroup<AnnotationSet> dataOwnersGroup;

        private TextField<Object> responseField;

        public ContentPanel(String aId, IModel<State> aModel)
        {
            super(aId, aModel);

            form = new Form<>("form", aModel);
            queue(form);
            queue(new Label("title", titleModel));

            // The challenge only needs to be met when already curated annotations are discarded.
            // It is therefore only shown when the discard option is enabled.
            var challengeContainer = new WebMarkupContainer("challengeContainer");
            challengeContainer.setOutputMarkupPlaceholderTag(true);
            challengeContainer.add(visibleWhen(() -> aModel.getObject().clearTargetCas));
            queue(challengeContainer);
            var challenge = new Label("challenge", challengeModel);
            challenge.setEscapeModelStrings(false); // SAFE - challengeModel is a ResourceModel
            // which can only come from a trusted resource bundle
            queue(challenge);
            queue(new Label("expectedResponse", expectedResponseModel));
            responseField = new TextField<>("response");
            responseField.setOutputMarkupId(true);
            queue(responseField);
            var dataOwnersContainer = new WebMarkupContainer("dataOwnersContainer");
            dataOwnersContainer.setOutputMarkupPlaceholderTag(true);
            dataOwnersContainer.add(visibleWhen(() -> !candidateDataOwners.isEmpty()));
            queue(dataOwnersContainer);
            // The quick selections set the model, so input left over from a submit that failed
            // validation must not override it
            queue(new LambdaAjaxLink("selectAllDataOwners", $ -> {
                aModel.getObject().dataOwners = new ArrayList<>(candidateDataOwners);
                dataOwnersGroup.clearInput();
                $.add(dataOwnersContainer);
            }));
            queue(new LambdaAjaxLink("selectSidebarDataOwners", $ -> {
                var selected = sidebarSelectionSource.get();
                aModel.getObject().dataOwners = candidateDataOwners.stream() //
                        .filter(selected::contains) //
                        .collect(toCollection(ArrayList::new));
                dataOwnersGroup.clearInput();
                $.add(dataOwnersContainer);
            }).add(visibleWhen(() -> sidebarSelectionSource != null)));
            dataOwnersGroup = new CheckGroup<>("dataOwners");
            queue(dataOwnersGroup);
            queue(new ListView<AnnotationSet>("dataOwnerChoices",
                    LambdaModel.of(() -> candidateDataOwners))
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<AnnotationSet> aItem)
                {
                    aItem.add(new Check<>("dataOwner", aItem.getModel()));
                    aItem.add(new Label("name", dataOwnerLabel(aItem)));
                }
            });
            queue(new MergeStrategyPanel("mergeStrategySettings", curationWorkflowModel));
            queue(new CheckBox("saveSettingsAsDefault").setOutputMarkupId(true));
            queue(new AjaxCheckBox("clearTargetCas")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    aTarget.add(challengeContainer);
                }
            }.setOutputMarkupId(true));
            queue(new LambdaAjaxButton<>("confirm", MergeDialog.this::onConfirmInternal)
                    .triggerAfterSubmit());
            queue(new LambdaAjaxLink("closeDialog", MergeDialog.this::onCancelInternal));
            cancelButton = new LambdaAjaxLink("cancel", MergeDialog.this::onCancelInternal);
            cancelButton.setOutputMarkupId(true);
            queue(cancelButton);
        }
    }
}
