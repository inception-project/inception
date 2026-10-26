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
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
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
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.curation.api.CurationSessionService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.selection.DocumentStateChangedInEditorEvent;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormChoiceComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarContext;

public class CurationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -4195790451286055737L;

    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean CurationSessionService curationSessionService;
    private @SpringBean CurationSidebarService curationSidebarService;

    private CheckGroup<AnnotationSet> selectedUsers;
    private ListView<AnnotationSet> users;
    private final Form<Void> usersForm;
    private CheckBox showMerged;
    private CheckBox showScore;
    private final IModel<Boolean> isTargetFinished;

    private final Label noDocsLabel;

    public CurationSidebar(String aId, SidebarContext aContext)
    {
        super(aId, aContext);

        isTargetFinished = LambdaModel.of(() -> {
            var state = getModelObject();
            return state != null && curationSidebarService.isCurationFinished(state);
        });

        noDocsLabel = new Label("noDocumentsLabel", new ResourceModel("noDocuments"));
        noDocsLabel.add(visibleWhen(
                () -> !isTargetFinished.getObject() && users.getModelObject().isEmpty()));
        queue(noDocsLabel);

        queue(usersForm = createUserSelection("usersForm"));

        var projectId = aContext.getProject().getId();
        var sessionOwner = userRepository.getSessionOwnerName();

        showMerged = new CheckBox("showMerged", LambdaModel.of( //
                () -> curationSidebarService.isShowAll(sessionOwner, projectId), //
                value -> curationSidebarService.setShowAll(sessionOwner, projectId, value)));
        showMerged.add(visibleWhen(this::isCurationEditorExtensionEnabled));
        showMerged.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionRefreshDocument));
        queue(showMerged);

        showScore = new CheckBox("showScore", LambdaModel.of( //
                () -> curationSidebarService.isShowScore(sessionOwner, projectId), //
                value -> curationSidebarService.setShowScore(sessionOwner, projectId, value)));
        showScore.add(visibleWhen(this::isCurationEditorExtensionEnabled));
        showScore.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionRefreshDocument));
        queue(showScore);
    }

    @Override
    public void onEvent(IEvent<?> aEvent)
    {
        super.onEvent(aEvent);

        if (aEvent.getPayload() instanceof DocumentStateChangedInEditorEvent stateChanged
                && stateChanged.isFor(getModelObject())
                && stateChanged.getRequestHandler() != null) {
            stateChanged.getRequestHandler().add(this);
        }
    }

    private void actionRefreshDocument(AjaxRequestTarget aTarget)
    {
        getActiveEditor().orElseThrow().actionRefreshDocument(aTarget);
    }

    /**
     * The "show all" and "show score" options only affect the curation suggestions rendered into
     * the editor, which only happens where the curation editor extension is enabled.
     */
    private boolean isCurationEditorExtensionEnabled()
    {
        var state = getModelObject();
        return state != null && state.isExtensionEnabled(CurationEditorExtension.EXTENSION_ID);
    }

    private Form<Void> createUserSelection(String aId)
    {
        var sessionOwner = userRepository.getSessionOwnerName();
        var project = getProject();

        var form = new Form<Void>(aId);
        form.setOutputMarkupPlaceholderTag(true);
        form.add(visibleWhen(() -> !users.getModelObject().isEmpty()));

        form.add(new LambdaAjaxLink("selectAll", this::actionSelectAll));
        form.add(new LambdaAjaxLink("selectNone", this::actionSelectNone));
        form.add(new LambdaAjaxLink("invertSelection", this::actionInvertSelection));

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
        var project = getProject();
        if (project.isAnonymousCuration()
                && !projectService.hasRole(userRepository.getSessionOwner(), project, MANAGER)) {
            return Model.of("Anonymized annotator " + (aDataOwnerListItem.getIndex() + 1));
        }

        return Model.of(aDataOwnerListItem.getModelObject().displayName());
    }

    /**
     * retrieve annotators of this document which finished annotating
     */
    private List<AnnotationSet> listCuratableDataOwners()
    {
        var context = getActiveEditor();
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
        getActiveEditor().orElseThrow().actionRefreshDocument(aTarget);
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
        var sessionOwner = userRepository.getSessionOwnerName();
        var projectId = getProject().getId();
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
        var sessionOwner = userRepository.getSessionOwnerName();
        var projectId = getProject().getId();
        curationSessionService.setSelectedDataOwners(sessionOwner, projectId,
                users.getModelObject(), aSelected);
    }
}
