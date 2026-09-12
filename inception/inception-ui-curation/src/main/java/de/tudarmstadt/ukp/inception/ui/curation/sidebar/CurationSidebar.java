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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static de.tudarmstadt.ukp.inception.support.wicket.WicketUtil.refreshPage;
import static java.util.Collections.emptyList;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.Form;
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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.MergeDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.MergeDialog.State;
import de.tudarmstadt.ukp.inception.curation.api.CurationSessionService;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormChoiceComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class CurationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -4195790451286055737L;

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean CurationSessionService curationSessionService;
    private @SpringBean CurationSidebarService curationSidebarService;
    private @SpringBean CurationService curationService;
    private @SpringBean CurationMergeService curationMergeService;
    private @SpringBean AnnotationSchemaService annotationService;

    private CheckGroup<AnnotationSet> selectedUsers;
    private ListView<AnnotationSet> users;
    private final Form<Void> usersForm;
    private CheckBox showMerged;
    private CheckBox showScore;
    private final IModel<CurationWorkflow> curationWorkflowModel;
    private final IModel<Boolean> isTargetFinished;

    private final Label noDocsLabel;

    private final MergeDialog mergeConfirm;

    public CurationSidebar(String aId, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aAnnotationPage);

        var state = aAnnotationPage.getModelObject();

        isTargetFinished = LambdaModel.of(() -> {
            return curationSidebarService.isCurationFinished(state);
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
        IModel<String> documentNameModel = PropertyModel.of(getModel(), "document.name");
        queue(mergeConfirm = new MergeDialog("mergeConfirmDialog",
                new ResourceModel("mergeConfirmTitle"), new ResourceModel("mergeConfirmText"),
                documentNameModel, curationWorkflowModel));
    }

    private void actionToggleShowMerged(AjaxRequestTarget aTarget)
    {
        var sessionOwner = userRepository.getCurrentUsername();
        curationSidebarService.setShowAll(sessionOwner, getModelObject().getProject().getId(),
                showMerged.getModelObject());
        getActiveContext().orElseThrow().actionRefreshDocument(aTarget);
    }

    private void actionToggleShowScore(AjaxRequestTarget aTarget)
    {
        var sessionOwner = userRepository.getCurrentUsername();
        curationSidebarService.setShowScore(sessionOwner, getModelObject().getProject().getId(),
                showScore.getModelObject());
        getActiveContext().orElseThrow().actionRefreshDocument(aTarget);
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
                    selectedUsers.getModelObject(), aForm.getModelObject().isClearTargetCas());
            success("Re-merge using [" + mergeStrategyFactory.getLabel() + "] finished!");
            refreshPage(aTarget, getPage());
        }
        catch (Exception e) {
            error("Unable to merge: " + e.getMessage());
            LOG.error("Unable to merge document {} to user {}", dataOwner, state.getDocument(), e);
        }
    }

    private boolean isSessionActive()
    {
        return curationSessionService.existsSession(userRepository.getCurrentUsername(),
                getModelObject().getProject().getId());
    }

    private Form<Void> createUserSelection(String aId)
    {
        var sessionOwner = userRepository.getCurrentUsername();
        var project = getModelObject().getProject();

        var form = new Form<Void>(aId);
        form.setOutputMarkupPlaceholderTag(true);
        form.add(visibleWhen(() -> isSessionActive() && !users.getModelObject().isEmpty()));

        form.add(new LambdaAjaxLink("selectAll", this::actionSelectAll));
        form.add(new LambdaAjaxLink("selectNone", this::actionSelectNone));
        form.add(new LambdaAjaxLink("invertSelection", this::actionInvertSelection));

        form.add(new LambdaAjaxButton<>("merge", this::actionOpenMergeDialog)
                .add(visibleWhenNot(isTargetFinished)));

        users = new ListView<AnnotationSet>("users",
                LoadableDetachableModel.of(this::listCuratableDataOwners))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<AnnotationSet> aItem)
            {
                aItem.add(new Check<AnnotationSet>("user", aItem.getModel()));
                aItem.add(new Label("name", maybeAnonymizeUsername(aItem)));
            }
        };

        selectedUsers = new CheckGroup<AnnotationSet>("selectedUsers");
        // Wicket reads the CheckGroup model once per rendered Check (i.e. once per annotator
        // row). A LoadableDetachableModel resolves the selected set a single time per request
        // and caches it, instead of re-entering the curation session service - and its lock -
        // for every row and rebuilding the set each time.
        selectedUsers.setModel(new LoadableDetachableModel<Collection<AnnotationSet>>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected Collection<AnnotationSet> load()
            {
                return new ArrayList<>(curationSessionService.getSelectedDataOwners(sessionOwner,
                        project.getId(), users.getModelObject()));
            }

            @Override
            public void setObject(Collection<AnnotationSet> aSelected)
            {
                curationSessionService.setSelectedDataOwners(sessionOwner, project.getId(),
                        users.getModelObject(), aSelected);
                super.setObject(aSelected);
            }
        });
        selectedUsers.add(
                new LambdaAjaxFormChoiceComponentUpdatingBehavior(this::actionChangeVisibleUsers));
        selectedUsers.add(users);
        form.add(selectedUsers);

        return form;
    }

    private IModel<String> maybeAnonymizeUsername(ListItem<AnnotationSet> aDataOwnerListItem)
    {
        var project = getModelObject().getProject();
        if (project.isAnonymousCuration()
                && !projectService.hasRole(userRepository.getCurrentUser(), project, MANAGER)) {
            return Model.of("Anonymized annotator " + (aDataOwnerListItem.getIndex() + 1));
        }

        return Model.of(aDataOwnerListItem.getModelObject().displayName());
    }

    /**
     * retrieve annotators of this document which finished annotating
     */
    private List<AnnotationSet> listCuratableDataOwners()
    {
        var context = getActiveContext();
        if (context.isEmpty()) {
            return emptyList();
        }

        var doc = context.get().getAnnotatorState().getDocument();
        if (doc == null) {
            return emptyList();
        }

        return curationSessionService.listCuratableDataOwners(doc);
    }

    private void actionChangeVisibleUsers(AjaxRequestTarget aTarget)
    {
        aTarget.add(usersForm);
        getActiveContext().orElseThrow().actionRefreshDocument(aTarget);
    }

    private void actionSelectAll(AjaxRequestTarget aTarget)
    {
        var candidates = users.getModelObject();
        setSelectedDataOwners(candidates);
        actionChangeVisibleUsers(aTarget);
    }

    private void actionSelectNone(AjaxRequestTarget aTarget)
    {
        setSelectedDataOwners(emptyList());
        actionChangeVisibleUsers(aTarget);
    }

    private void actionInvertSelection(AjaxRequestTarget aTarget)
    {
        var sessionOwner = userRepository.getCurrentUsername();
        var projectId = getModelObject().getProject().getId();
        var candidates = users.getModelObject();
        var selected = curationSessionService.getSelectedDataOwners(sessionOwner, projectId,
                candidates);
        var inverted = candidates.stream().filter(candidate -> !selected.contains(candidate))
                .toList();
        setSelectedDataOwners(inverted);
        actionChangeVisibleUsers(aTarget);
    }

    private void setSelectedDataOwners(Collection<AnnotationSet> aSelected)
    {
        var sessionOwner = userRepository.getCurrentUsername();
        var projectId = getModelObject().getProject().getId();
        curationSessionService.setSelectedDataOwners(sessionOwner, projectId,
                users.getModelObject(), aSelected);
    }
}
