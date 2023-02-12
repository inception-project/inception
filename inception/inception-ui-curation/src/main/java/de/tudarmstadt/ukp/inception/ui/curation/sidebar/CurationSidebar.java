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
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhenNot;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhenNot;
import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil.refreshPage;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.LambdaChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormChoiceComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.MergeDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.MergeDialog.State;
import de.tudarmstadt.ukp.inception.curation.merge.MergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

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

    private CheckGroup<User> selectedUsers;
    private DropDownChoice<String> curationTargetChoice;
    private ListView<User> users;
    private final Form<Void> usersForm;
    private CheckBox showMerged;
    private final IModel<CurationWorkflow> curationWorkflowModel;

    private final Label noDocsLabel;
    private final Label finishedLabel;

    private final MergeDialog mergeConfirm;

    public CurationSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aAnnotationPage);

        // init. state with stored curation user
        AnnotatorState state = aModel.getObject();

        var notCuratableNotice = new WebMarkupContainer("notCuratableNotice");
        notCuratableNotice.setOutputMarkupId(true);
        notCuratableNotice.add(visibleWhen(() -> !isCuratable()));
        add(notCuratableNotice);

        queue(createSessionControlForm(CID_SESSION_CONTROL_FORM));

        var isTargetFinished = LambdaModel.of(() -> curationSidebarService.isCurationFinished(state,
                userRepository.getCurrentUsername()));

        finishedLabel = new Label("finishedLabel", new StringResourceModel("finished", this,
                LoadableDetachableModel.of(state::getUser)));
        finishedLabel.setOutputMarkupPlaceholderTag(true);
        finishedLabel.add(visibleWhen(() -> isSessionActive() && isTargetFinished.getObject()));
        queue(finishedLabel);

        noDocsLabel = new Label("noDocumentsLabel", new ResourceModel("noDocuments"));
        noDocsLabel.add(visibleWhen(() -> isSessionActive() && !isTargetFinished.getObject()
                && users.getModelObject().isEmpty()));
        queue(noDocsLabel);

        queue(usersForm = createUserSelection("usersForm"));

        showMerged = new CheckBox("showMerged", Model.of());
        showMerged.add(visibleWhen(this::isSessionActive));
        showMerged.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionToggleShowMerged));
        queue(showMerged);

        if (isSessionActive()) {
            showMerged.setModelObject(curationSidebarService
                    .isShowAll(userRepository.getCurrentUsername(), state.getProject().getId()));
        }

        curationWorkflowModel = Model
                .of(curationService.readOrCreateCurationWorkflow(state.getProject()));

        // confirmation dialog when using automatic merging (might change user's annos)
        IModel<String> documentNameModel = PropertyModel.of(getAnnotationPage().getModel(),
                "document.name");
        queue(mergeConfirm = new MergeDialog("mergeConfirmDialog",
                new ResourceModel("mergeConfirmTitle"), new ResourceModel("mergeConfirmText"),
                documentNameModel, curationWorkflowModel));

        // user started curating, extension can show suggestions
        state.setMetaData(CurationMetadata.CURATION_USER_PROJECT, true);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        if (!isCuratable()) {
            return;
        }

        if (!isSessionActive()) {
            return;
        }

        String sessionOwner = userRepository.getCurrentUsername();
        AnnotatorState state = getModelObject();

        // If curation is possible and the curation target user is different from the user set in
        // the annotation state, then we need to update the state and reload.
        User curationTarget = curationSidebarService.getCurationTargetUser(sessionOwner,
                state.getProject().getId());
        if (!state.getUser().equals(curationTarget)) {
            state.setUser(curationTarget);
            throw new RestartResponseException(getPage());
        }
    }

    private boolean isCuratable()
    {
        // Curation sidebar is not allowed when viewing another users annotations
        String currentUsername = userRepository.getCurrentUsername();
        AnnotatorState state = getModelObject();
        return asList(CURATION_USER, currentUsername).contains(state.getUser().getUsername());
    }

    private void actionToggleShowMerged(AjaxRequestTarget aTarget)
    {
        String sessionOwner = userRepository.getCurrentUsername();
        curationSidebarService.setShowAll(sessionOwner, getModelObject().getProject().getId(),
                showMerged.getModelObject());
        getAnnotationPage().actionRefreshDocument(aTarget);
    }

    private void actionOpenMergeDialog(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        actionChangeCurationTarget();
        writeSelectedUsers();
        mergeConfirm.setConfirmAction(this::actionMerge);
        mergeConfirm.show(aTarget);
    }

    private void actionMerge(AjaxRequestTarget aTarget, Form<State> aForm)
    {
        AnnotatorState state = getModelObject();

        if (aForm.getModelObject().isSaveSettingsAsDefault()) {
            curationService.createOrUpdateCurationWorkflow(curationWorkflowModel.getObject());
            success("Updated project merge strategy settings");
        }

        try {
            doMerge(state, state.getUser().getUsername(), selectedUsers.getModelObject());
        }
        catch (Exception e) {
            error("Unable to merge: " + e.getMessage());
            LOG.error("Unable to merge document {} to user {}", state.getUser(),
                    state.getDocument(), e);
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        MergeStrategyFactory<?> mergeStrategyFactory = curationService
                .getMergeStrategyFactory(curationWorkflowModel.getObject());
        success("Re-merge using [" + mergeStrategyFactory.getLabel() + "] finished!");
        refreshPage(aTarget, getPage());
    }

    private void actionChangeCurationTarget()
    {
        var aState = getModelObject();
        long project = aState.getProject().getId();
        User curationTargetUser;

        // update curation target
        String sessionOwner = userRepository.getCurrentUsername();
        if (curationTargetChoice.getModelObject().equals(sessionOwner)) {
            curationSidebarService.setCurationTarget(sessionOwner, project, sessionOwner);
            curationTargetUser = userRepository.get(sessionOwner);
        }
        else {
            curationTargetUser = new User(CURATION_USER, Role.ROLE_USER);
            curationSidebarService.setCurationTarget(sessionOwner, project, CURATION_USER);
        }

        aState.setUser(curationTargetUser);
        aState.getSelection().clear();
    }

    private void doMerge(AnnotatorState aState, String aCurator, Collection<User> aUsers)
        throws IOException, UIMAException
    {
        SourceDocument doc = aState.getDocument();
        CAS aTargetCas = curationSidebarService
                .retrieveCurationCAS(aCurator, aState.getProject().getId(), doc)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No target CAS configured in curation state"));

        Map<String, CAS> userCases = documentService.readAllCasesSharedNoUpgrade(doc, aUsers);

        // FIXME: should merging not overwrite the current users annos? (can result in
        // deleting the users annotations!!!), currently fixed by warn message to user
        // prepare merged CAS
        curationMergeService.mergeCasses(aState.getDocument(), aState.getUser().getUsername(),
                aTargetCas, userCases,
                curationService.getMergeStrategy(curationWorkflowModel.getObject()),
                aState.getAnnotationLayers());

        // write back and update timestamp
        curationSidebarService.writeCurationCas(aTargetCas, aState, aState.getProject().getId());

        LOG.debug("Merge done");
    }

    private Form<Void> createSessionControlForm(String aId)
    {
        var form = new Form<Void>(aId);

        form.setOutputMarkupId(true);

        IChoiceRenderer<String> targetChoiceRenderer = new LambdaChoiceRenderer<>(
                aUsername -> CURATION_USER.equals(aUsername) ? "curation document" : "my document");

        var curationTargets = new ArrayList<String>();
        curationTargets.add(CURATION_USER);
        if (projectService.hasRole(userRepository.getCurrentUsername(),
                getModelObject().getProject(), ANNOTATOR)) {
            curationTargets.add(userRepository.getCurrentUsername());
        }

        curationTargetChoice = new DropDownChoice<>(CID_SELECT_CURATION_TARGET);
        curationTargetChoice.setModel(Model.of(curationTargets.get(0)));
        curationTargetChoice.setChoices(curationTargets);
        curationTargetChoice.setChoiceRenderer(targetChoiceRenderer);
        curationTargetChoice.add(enabledWhenNot(this::isSessionActive));
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
        curationSidebarService.startSession(userRepository.getCurrentUsername(),
                getModelObject().getProject().getId());
        actionChangeCurationTarget();
        selectedUsers.setModelObject(listCuratableUsers());
        writeSelectedUsers();
        showMerged.setModelObject(curationSidebarService.isShowAll(
                userRepository.getCurrentUsername(), getModelObject().getProject().getId()));
        getAnnotationPage().actionLoadDocument(aTarget);
    }

    private void actionStopSession(AjaxRequestTarget aTarget)
    {
        actionChangeCurationTarget();

        // The CurationRenderer has no access to the state that is attached to the page - we need
        // to clear the selected users that are stored in the DB to shut it up
        selectedUsers.setModelObject(emptyList());
        writeSelectedUsers();

        getModelObject().setUser(userRepository.getCurrentUser());

        curationSidebarService.closeSession(userRepository.getCurrentUsername(),
                getModelObject().getProject().getId());

        getAnnotationPage().actionLoadDocument(aTarget);
    }

    private Form<Void> createUserSelection(String aId)
    {
        var form = new Form<Void>(aId);
        form.add(new LambdaAjaxButton<>("merge", this::actionOpenMergeDialog));

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

        selectedUsers = new CheckGroup<User>("selectedUsers", Model.ofList(new ArrayList<User>()));
        selectedUsers.add(
                new LambdaAjaxFormChoiceComponentUpdatingBehavior(this::actionChangeVisibleUsers));
        selectedUsers.add(users);
        form.add(selectedUsers);

        if (isSessionActive()) {
            // Restore session state
            selectedUsers.setModelObject(curationSidebarService.listUsersSelectedForCuration(
                    userRepository.getCurrentUsername(), getModelObject().getProject().getId()));
        }

        form.setOutputMarkupPlaceholderTag(true);
        form.add(
                visibleWhen(() -> isSessionActive()
                        && !curationSidebarService.isCurationFinished(getModelObject(),
                                userRepository.getCurrentUsername())
                        && !users.getModelObject().isEmpty()));
        return form;
    }

    private IModel<String> maybeAnonymizeUsername(ListItem<User> aUserListItem)
    {
        Project project = getModelObject().getProject();
        if (project.isAnonymousCuration()
                && !projectService.hasRole(userRepository.getCurrentUser(), project, MANAGER)) {
            return Model.of("Anonymized annotator " + (aUserListItem.getIndex() + 1));
        }

        return aUserListItem.getModel().map(User::getUiName);
    }

    private List<User> readSelectedUsers()
    {
        return curationSidebarService.listUsersSelectedForCuration(
                userRepository.getCurrentUsername(), getModelObject().getProject().getId());
    }

    private void writeSelectedUsers()
    {
        var state = getModelObject();
        curationSidebarService.setSelectedUsers(userRepository.getCurrentUsername(),
                state.getProject().getId(), selectedUsers.getModelObject());
    }

    /**
     * retrieve annotators of this document which finished annotating
     */
    private List<User> listCuratableUsers()
    {
        if (getModelObject().getDocument() == null) {
            return Collections.emptyList();
        }

        User currentUser = userRepository.getCurrentUser();
        String curationTarget = getModelObject().getUser().getUsername();
        return curationSidebarService
                .listCuratableUsers(getModelObject().getDocument())
                .stream()
                .filter(user -> !user.equals(currentUser) || curationTarget.equals(CURATION_USER))
                .collect(Collectors.toList());
    }

    private void actionChangeVisibleUsers(AjaxRequestTarget aTarget)
    {
        // switch to manual merge
        writeSelectedUsers();
        aTarget.add(usersForm);
        getAnnotationPage().actionRefreshDocument(aTarget);
    }
}
