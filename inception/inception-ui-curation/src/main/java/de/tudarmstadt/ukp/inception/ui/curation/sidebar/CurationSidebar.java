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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhenNot;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static de.tudarmstadt.ukp.inception.support.wicket.WicketUtil.refreshPage;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.LambdaChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.MergeDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.MergeDialog.State;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarProperties;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormChoiceComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;

public class CurationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -4195790451286055737L;

    private static final String CID_SESSION_CONTROL_FORM = "sessionControlForm";
    private static final String CID_START_SESSION_BUTTON = "startSession";
    private static final String CID_STOP_SESSION_BUTTON = "stopSession";
    private static final String CID_SELECT_CURATION_TARGET = "curationTarget";

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean CurationSidebarService curationSidebarService;
    private @SpringBean CurationService curationService;
    private @SpringBean CurationMergeService curationMergeService;
    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean CurationSidebarProperties curationSidebarProperties;

    private CheckGroup<User> selectedUsers;
    private DropDownChoice<String> curationTargetChoice;
    private ListView<User> users;
    private final Form<Void> usersForm;
    private CheckBox showMerged;
    private CheckBox showScore;
    private final IModel<CurationWorkflow> curationWorkflowModel;
    private final IModel<Boolean> isTargetFinished;

    private final Label noDocsLabel;

    private final MergeDialog mergeConfirm;

    public CurationSidebar(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);

        var state = aAnnotationPage.getModelObject();

        queue(createSessionControlForm(CID_SESSION_CONTROL_FORM)
                .add(visibleWhen(() -> getPage() instanceof AnnotationPage)));

        isTargetFinished = LambdaModel.of(() -> {
            var sessionOwner = userRepository.getCurrentUsername();
            return curationSidebarService.isCurationFinished(state, sessionOwner);
        });

        noDocsLabel = new Label("noDocumentsLabel", new ResourceModel("noDocuments"));
        noDocsLabel.add(visibleWhen(() -> isSessionActive() && !isTargetFinished.getObject()
                && users.getModelObject().isEmpty()));
        queue(noDocsLabel);

        queue(usersForm = createUserSelection("usersForm"));

        showMerged = new CheckBox("showMerged", Model.of(false));
        showMerged.add(visibleWhen(this::isSessionActive));
        showMerged.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionToggleShowMerged));
        queue(showMerged);

        showScore = new CheckBox("showScore", Model.of(true));
        showScore.add(visibleWhen(this::isSessionActive));
        showScore.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionToggleShowScore));
        queue(showScore);

        if (isSessionActive()) {
            var sessionOwner = userRepository.getCurrentUsername();
            var project = state.getProject();
            showMerged.setModelObject(
                    curationSidebarService.isShowAll(sessionOwner, project.getId()));
            showScore.setModelObject(
                    curationSidebarService.isShowScore(sessionOwner, project.getId()));
        }

        curationWorkflowModel = Model
                .of(curationService.readOrCreateCurationWorkflow(state.getProject()));

        // confirmation dialog when using automatic merging (might change user's annos)
        IModel<String> documentNameModel = PropertyModel.of(getAnnotationPage().getModel(),
                "document.name");
        queue(mergeConfirm = new MergeDialog("mergeConfirmDialog",
                new ResourceModel("mergeConfirmTitle"), new ResourceModel("mergeConfirmText"),
                documentNameModel, curationWorkflowModel));
    }

    private void actionToggleShowMerged(AjaxRequestTarget aTarget)
    {
        var sessionOwner = userRepository.getCurrentUsername();
        curationSidebarService.setShowAll(sessionOwner, getModelObject().getProject().getId(),
                showMerged.getModelObject());
        getAnnotationPage().actionRefreshDocument(aTarget);
    }

    private void actionToggleShowScore(AjaxRequestTarget aTarget)
    {
        var sessionOwner = userRepository.getCurrentUsername();
        curationSidebarService.setShowScore(sessionOwner, getModelObject().getProject().getId(),
                showScore.getModelObject());
        getAnnotationPage().actionRefreshDocument(aTarget);
    }

    private void actionOpenMergeDialog(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        mergeConfirm.setConfirmAction(this::actionMerge);
        mergeConfirm.show(aTarget);
    }

    private void actionMerge(AjaxRequestTarget aTarget, Form<State> aForm)
    {
        var state = getModelObject();
        var dataOwner = state.getUser();
        var curationWorkflow = curationWorkflowModel.getObject();

        if (aForm.getModelObject().isSaveSettingsAsDefault()) {
            curationService.createOrUpdateCurationWorkflow(curationWorkflow);
            success("Updated project merge strategy settings");
        }

        aTarget.addChildren(getPage(), IFeedback.class);

        try {
            var mergeStrategyFactory = curationSidebarService.merge(state, curationWorkflow,
                    dataOwner.getUsername(), selectedUsers.getModelObject(),
                    aForm.getModelObject().isClearTargetCas());
            success("Re-merge using [" + mergeStrategyFactory.getLabel() + "] finished!");
            refreshPage(aTarget, getPage());
        }
        catch (Exception e) {
            error("Unable to merge: " + e.getMessage());
            LOG.error("Unable to merge document {} to user {}", dataOwner, state.getDocument(), e);
        }
    }

    private Form<Void> createSessionControlForm(String aId)
    {
        var form = new Form<Void>(aId);

        form.setOutputMarkupId(true);

        var targetChoiceRenderer = new LambdaChoiceRenderer<>(
                aUsername -> CURATION_USER.equals(aUsername) ? "curation document" : "my document");

        var curationTargets = new ArrayList<String>();
        curationTargets.add(CURATION_USER);
        if (curationSidebarProperties.isOwnUserCurationTargetEnabled() && projectService.hasRole(
                userRepository.getCurrentUsername(), getModelObject().getProject(), ANNOTATOR)) {
            curationTargets.add(userRepository.getCurrentUsername());
        }

        curationTargetChoice = new DropDownChoice<>(CID_SELECT_CURATION_TARGET);
        if (!isSessionActive()) {
            curationTargetChoice.setModel(Model.of(curationTargets.get(0)));
        }
        else {
            curationTargetChoice.setModel(Model.of(curationSidebarService.getCurationTarget(
                    userRepository.getCurrentUsername(), getModelObject().getProject().getId())));
        }
        curationTargetChoice.setChoices(curationTargets);
        curationTargetChoice.setChoiceRenderer(targetChoiceRenderer);
        curationTargetChoice.add(enabledWhenNot(this::isSessionActive));
        curationTargetChoice.add(visibleWhen(() -> curationTargets.size() > 1));
        curationTargetChoice.setOutputMarkupId(true);
        curationTargetChoice.setRequired(true);
        form.add(curationTargetChoice);

        form.add(new LambdaAjaxSubmitLink<>(CID_START_SESSION_BUTTON, this::actionStartSession)
                .add(visibleWhenNot(this::isSessionActive)));
        form.add(new LambdaAjaxLink(CID_STOP_SESSION_BUTTON, this::actionStopSession)
                .add(visibleWhen((this::isSessionActive))));

        return form;
    }

    private boolean isSessionActive()
    {
        return curationSidebarService.existsSession(userRepository.getCurrentUsername(),
                getModelObject().getProject().getId());
    }

    private void actionStartSession(AjaxRequestTarget aTarget, Form<?> form)
    {
        var sessionOwner = userRepository.getCurrentUsername();
        var state = getModelObject();
        var project = state.getProject().getId();

        curationSidebarService.startSession(sessionOwner, state.getProject(),
                !curationTargetChoice.getModelObject().equals(CURATION_USER));
        curationSidebarService.setDefaultSelectedUsersForDocument(sessionOwner,
                getModelObject().getDocument());

        showMerged.setModelObject(curationSidebarService.isShowAll(sessionOwner, project));

        state.setUser(curationSidebarService.getCurationTargetUser(sessionOwner, project));
        state.getSelection().clear();

        getAnnotationPage().actionLoadDocument(aTarget);
    }

    private void actionStopSession(AjaxRequestTarget aTarget)
    {
        var state = getModelObject();
        var sessionOwner = userRepository.getCurrentUser();

        state.setUser(userRepository.getCurrentUser());
        state.getSelection().clear();

        curationSidebarService.closeSession(sessionOwner.getUsername(), state.getProject().getId());

        getAnnotationPage().actionLoadDocument(aTarget);
    }

    private Form<Void> createUserSelection(String aId)
    {
        var sessionOwner = userRepository.getCurrentUsername();
        var project = getModelObject().getProject();

        var form = new Form<Void>(aId);
        form.setOutputMarkupPlaceholderTag(true);
        form.add(visibleWhen(() -> isSessionActive() && !users.getModelObject().isEmpty()));

        form.add(new LambdaAjaxButton<>("merge", this::actionOpenMergeDialog)
                .add(visibleWhenNot(isTargetFinished)));

        users = new ListView<User>("users", LoadableDetachableModel.of(this::listCuratableUsers))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<User> aItem)
            {
                aItem.add(new Check<User>("user", aItem.getModel()));
                aItem.add(new Label("name", maybeAnonymizeUsername(aItem)));
            }
        };

        selectedUsers = new CheckGroup<User>("selectedUsers");
        selectedUsers.setModel(new LambdaModelAdapter<>( //
                () -> curationSidebarService.getSelectedUsers(sessionOwner, project.getId()), //
                (_users) -> curationSidebarService.setSelectedUsers(sessionOwner, project.getId(),
                        _users)));
        selectedUsers.add(
                new LambdaAjaxFormChoiceComponentUpdatingBehavior(this::actionChangeVisibleUsers));
        selectedUsers.add(users);
        form.add(selectedUsers);

        return form;
    }

    private IModel<String> maybeAnonymizeUsername(ListItem<User> aUserListItem)
    {
        var project = getModelObject().getProject();
        if (project.isAnonymousCuration()
                && !projectService.hasRole(userRepository.getCurrentUser(), project, MANAGER)) {
            return Model.of("Anonymized annotator " + (aUserListItem.getIndex() + 1));
        }

        return aUserListItem.getModel().map(User::getUiName);
    }

    /**
     * retrieve annotators of this document which finished annotating
     */
    private List<User> listCuratableUsers()
    {
        var doc = getModelObject().getDocument();
        if (doc == null) {
            return Collections.emptyList();
        }

        return curationSidebarService.listCuratableUsers(userRepository.getCurrentUsername(), doc);
    }

    private void actionChangeVisibleUsers(AjaxRequestTarget aTarget)
    {
        aTarget.add(usersForm);
        getAnnotationPage().actionRefreshDocument(aTarget);
    }
}
