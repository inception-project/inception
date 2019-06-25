/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;

/**
 * A panel used as Open dialog. It Lists all projects a user is member of for annotation/curation
 * and associated documents
 */
public class OpenDocumentDialogPanel
    extends Panel
{
    private static final long serialVersionUID = 1299869948010875439L;

    private @SpringBean ProjectService projectService;
    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean UserDao userRepository;

    private final ButtonsForm buttonsForm;

    private OverviewListChoice<DecoratedObject<Project>> projectListChoice;
    private IModel<List<DecoratedObject<Project>>> projects;
    
    private OverviewListChoice<DecoratedObject<SourceDocument>> docListChoice;

    private OverviewListChoice<DecoratedObject<User>> userListChoice;

    private final AnnotatorState state;
    
    private final ModalWindow modalWindow;
    
    
    
    public OpenDocumentDialogPanel(String aId, AnnotatorState aBModel, ModalWindow aModalWindow,
            IModel<List<DecoratedObject<Project>>> aProjects)
    {
        super(aId);
        
        modalWindow = aModalWindow;
        state = aBModel;
        projects = aProjects;
        
        projectListChoice = createProjectListChoice(aBModel);
        userListChoice = createUserListChoice();
        docListChoice = createDocListChoice();
        
        buttonsForm = new ButtonsForm("buttonsForm", aModalWindow);
       
        add(buttonsForm);
        add(projectListChoice);
        add(docListChoice);
        add(userListChoice);
    }

    private OverviewListChoice<DecoratedObject<SourceDocument>> createDocListChoice()
    {
        docListChoice = new OverviewListChoice<>("document", Model.of(), listDocuments());
        docListChoice.setChoiceRenderer(new ChoiceRenderer<DecoratedObject<SourceDocument>>()
        {

            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(DecoratedObject<SourceDocument> aDoc)
            {
                return defaultIfEmpty(aDoc.getLabel(), aDoc.get().getName());
            }
        });
        docListChoice.setOutputMarkupId(true);
        docListChoice.add(new OnChangeAjaxBehavior()
        {
            private static final long serialVersionUID = -8232688660762056913L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                aTarget.add(buttonsForm);
                aTarget.add(docListChoice);
            }
        }).add(AjaxEventBehavior.onEvent("dblclick",
                OpenDocumentDialogPanel.this::actionOpenDocument));;
        return docListChoice;
    }

    private OverviewListChoice<DecoratedObject<Project>> createProjectListChoice(
            AnnotatorState aBModel)
    {
        List<DecoratedObject<Project>> allowedProjects = projects.getObject();
        IModel<DecoratedObject<Project>> selectedProject = Model.of();
        if (!allowedProjects.isEmpty()) {
            selectedProject = Model.of(DecoratedObject.of(allowedProjects.get(0).get()));
        }
        if (aBModel.isProjectLocked()) {
            selectedProject = Model.of(DecoratedObject.of(aBModel.getProject()));
        }
        projectListChoice = new OverviewListChoice<>("project", selectedProject,
                projects.getObject());
        projectListChoice.setChoiceRenderer(new ChoiceRenderer<DecoratedObject<Project>>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(DecoratedObject<Project> aProject)
            {
                return defaultIfEmpty(aProject.getLabel(), aProject.get().getName());
            }
        });
        projectListChoice.setOutputMarkupId(true);
        projectListChoice.add(new OnChangeAjaxBehavior()
        {
            private static final long serialVersionUID = -2516735444707689106L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                if (userListChoice.isVisible()) {
                    userListChoice.setChoices(listUsers());
                    aTarget.add(userListChoice);
                }
                
                docListChoice.setChoices(listDocuments());
                docListChoice.setDefaultModel(Model.of());
                aTarget.add(buttonsForm);
                aTarget.add(docListChoice);
            }
        });
        
        if (aBModel.isProjectLocked()) {
            projectListChoice.setVisible(false);
        }
        
        return projectListChoice;
    }
    
    private OverviewListChoice<DecoratedObject<User>> createUserListChoice()
    {
        DecoratedObject<User> currentUser = DecoratedObject.of(userRepository.getCurrentUser());
        userListChoice = new OverviewListChoice<>("user", Model.of(currentUser), listUsers());
        userListChoice.setChoiceRenderer(new ChoiceRenderer<DecoratedObject<User>>()
        {

            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(DecoratedObject<User> aUser)
            {
                User user = aUser.get();
                String username = defaultIfEmpty(aUser.getLabel(), user.getUsername());
                if (user.equals(currentUser.get())) {
                    username += " (me)";
                }
                return username + (user.isEnabled() ? "" : " (login disabled)");
            }
        });
        userListChoice.setOutputMarkupId(true);
        userListChoice.add(new OnChangeAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                docListChoice.setChoices(listDocuments());
                docListChoice.setDefaultModel(Model.of());
                aTarget.add(buttonsForm);
                aTarget.add(docListChoice);
            }
        }).add(visibleWhen(() -> state.getMode().equals(Mode.ANNOTATION)
                && isManagerForListedProjects()));

        return userListChoice;
    }

    /**
     * Check if current user is manager for any of the listed projects
     */
    private boolean isManagerForListedProjects()
    {
        User currentUser = userRepository.getCurrentUser();
        return projectService.isManager(projectListChoice.getModelObject().get(),
                currentUser)
                || projects.getObject().stream().anyMatch(
                    p -> projectService.isManager(p.get(), currentUser));
    }

    private List<DecoratedObject<User>> listUsers()
    {
        if (projectListChoice.getModelObject() == null
                || !state.getMode().equals(Mode.ANNOTATION)) {
            return new ArrayList<>();
        }

        List<DecoratedObject<User>> users = new ArrayList<>();
        
        Project selectedProject = projectListChoice.getModelObject().get();
        User currentUser = userRepository.getCurrentUser();
        if (!projectService.isManager(selectedProject, currentUser)) {
            DecoratedObject<User> du = DecoratedObject.of(currentUser);
            du.setLabel(currentUser.getUsername());
            users.add(du);
            return users;
        }

        for (User user : projectService.listProjectUsersWithPermissions(
                selectedProject, PermissionLevel.ANNOTATOR)) {               
            DecoratedObject<User> du = DecoratedObject.of(user);
            du.setLabel(user.getUsername());
            if (user.equals(currentUser)) {
                users.add(0, du);
            }
            else {
                users.add(du);
            }
        }

        return users;
    }

    private List<DecoratedObject<SourceDocument>> listDocuments()
    {
        if (projectListChoice.getModelObject() == null || userListChoice.getModelObject() == null) {
            return new ArrayList<>();
        }
        
        final List<DecoratedObject<SourceDocument>> allSourceDocuments = new ArrayList<>();

        // Remove from the list source documents that are in IGNORE state OR
        // that do not have at least one annotation document marked as
        // finished for curation dialog
        switch (state.getMode()) {
        case ANNOTATION:
        case AUTOMATION:
        case CORRECTION: {
            Map<SourceDocument, AnnotationDocument> docs = documentService.listAllDocuments(
                    projectListChoice.getModelObject().get(),
                    userListChoice.getModelObject().get());

            for (Entry<SourceDocument, AnnotationDocument> e : docs.entrySet()) {
                
                DecoratedObject<SourceDocument> dsd = DecoratedObject.of(e.getKey());
                if (e.getValue() != null) {
                    AnnotationDocument adoc = e.getValue();
                    AnnotationDocumentState docState = adoc.getState();
                    dsd.setColor(docState.getColor());
                    
                    boolean userIsSelected = userListChoice.getModelObject().get()
                            .equals(userRepository.getCurrentUser());
                    // if current user is opening her own docs, don't let her see locked ones
                    if (userIsSelected && docState.equals(AnnotationDocumentState.IGNORE)) {
                        continue;
                    }
                }
                allSourceDocuments.add(dsd);
            }
            break;
        }
        case CURATION: {
            List<SourceDocument> sdocs = curationDocumentService
                    .listCuratableSourceDocuments(projectListChoice.getModelObject().get());
            
            for (SourceDocument sourceDocument : sdocs) {
                DecoratedObject<SourceDocument> dsd = DecoratedObject.of(sourceDocument);
                dsd.setLabel("%s (%s)", sourceDocument.getName(), sourceDocument.getState());
                dsd.setColor(sourceDocument.getState().getColor());
                allSourceDocuments.add(dsd);
            }

            break;
        }
        default:
            break;
        }
        
        return allSourceDocuments;
    }

    private class ButtonsForm
        extends Form<Void>
    {
        private static final long serialVersionUID = -1879323194964417564L;

        public ButtonsForm(String id, final ModalWindow modalWindow)
        {
            super(id);
            
            add(new LambdaAjaxLink("openButton", OpenDocumentDialogPanel.this::actionOpenDocument)
                    .add(enabledWhen(() -> docListChoice.getModelObject() != null)));

            add(new LambdaAjaxLink("cancelButton", OpenDocumentDialogPanel.this::actionCancel));
        }
    }

    private void actionOpenDocument(AjaxRequestTarget aTarget)
    {
        if (projectListChoice.getModelObject() != null && docListChoice.getModelObject()  != null) {
            state.setProject(projectListChoice.getModelObject().get());
            state.setDocument(docListChoice.getModelObject().get(), docListChoice.getChoices()
                    .stream().map(t -> t.get()).collect(Collectors.toList()));
            state.setUser(userListChoice.getModelObject().get());
  
            modalWindow.close(aTarget);
        }
    }
    
    private void actionCancel(AjaxRequestTarget aTarget)
    {
        projectListChoice.detach();
        userListChoice.detach();
        docListChoice.detach();
        if (Mode.CURATION.equals(state.getMode())) {
            state.setDocument(null, null); // on cancel, go welcomePage
        }
        onCancel(aTarget);
        modalWindow.close(aTarget);
    }

    protected void onCancel(AjaxRequestTarget aTarget)
    {
    }
}
