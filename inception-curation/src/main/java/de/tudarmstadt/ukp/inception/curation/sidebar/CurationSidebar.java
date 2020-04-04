/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.curation.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.value.AttributeMap;
import org.apache.wicket.util.value.IValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.form.BootstrapRadioChoice;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.MergeDialog;
import de.tudarmstadt.ukp.inception.curation.CurationMetadata;
import de.tudarmstadt.ukp.inception.curation.CurationService;
import de.tudarmstadt.ukp.inception.curation.merge.AutomaticMergeStrategy;
import de.tudarmstadt.ukp.inception.curation.merge.ManualMergeStrategy;
import de.tudarmstadt.ukp.inception.curation.merge.MergeStrategy;


public class CurationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -4195790451286055737L;
    
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean CurationService curationService;
    private @SpringBean DocumentService documentService;
    
    private @SpringBean ManualMergeStrategy manualMergeStrat;
    private @SpringBean AutomaticMergeStrategy autoMergeStrat;
    
    private CheckGroup<User> selectedUsers;
    private Form<List<User>> usersForm;
    private BootstrapRadioChoice<String> curationTargetChoice;
    private WebMarkupContainer mainContainer;
    private ListView<User> users;
    private Label noDocsLabel;
    
    private AnnotationPage annoPage;
    private final MergeDialog mergeConfirm;
    
    
    public CurationSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aAnnotationPage);
        annoPage = aAnnotationPage;
        
        mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.setOutputMarkupId(true);
        add(mainContainer);
        
        // set up user-checklist
        usersForm = createUserSelection();
        usersForm.setOutputMarkupId(true);
        mainContainer.add(usersForm);
        
        // set up settings form for curation target
        Form<Void> settingsForm = createSettingsForm("settingsForm");
        settingsForm.setOutputMarkupId(true);
        settingsForm.setVisible(false);
        mainContainer.add(settingsForm);
        // confirmation dialog when using automatic merging (might change user's annos)  
        IModel<String> documentNameModel = PropertyModel.of(annoPage.getModel(), "document.name");
        add(mergeConfirm = new MergeDialog("mergeConfirmDialog",
                new StringResourceModel("mergeConfirmTitle", this),
                new StringResourceModel("mergeConfirmText", this)
                        .setModel(annoPage.getModel()).setParameters(documentNameModel),
                documentNameModel));
        mainContainer.add(mergeConfirm);
        
        // Add empty space message
        noDocsLabel = new Label("noDocumentsLabel", new ResourceModel("noDocuments"));
        mainContainer.add(noDocsLabel);
        
        // if curation user changed we have to reload the document
        AnnotatorState state = aModel.getObject();
        String currentUser = userRepository.getCurrentUser().getUsername();
        long projectid = state.getProject().getId();
        User curationUser = curationService.retrieveCurationUser(currentUser, 
                projectid);
        if (currentUser != null && !currentUser.equals(curationUser.getUsername())) {
            state.setUser(curationUser);
            Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
            annoPage.actionLoadDocument(target.orElseGet(null));
        }
        
        // user started curating, extension can show suggestions
        state.setMetaData(CurationMetadata.CURATION_USER_PROJECT, true);
    }
    
    private Form<Void> createSettingsForm(String aId)
    {
        Form<Void> settingsForm = new Form<Void>(aId);
        LambdaAjaxButton<Void> applyBtn = new LambdaAjaxButton<>("apply", 
                this::updateForNewCurator);
        settingsForm.add(applyBtn);
        
        // set up curation target selection as radio button
        List<String> curationTargets = Arrays.asList(
                new String[] { CURATION_USER, userRepository.getCurrentUser().getUsername() });
        ChoiceRenderer<String> choiceRenderer = new ChoiceRenderer<String>()
        {
            private static final long serialVersionUID = -8165699251116827372L;

            @Override
            public Object getDisplayValue(String aUsername)
            {
                if (aUsername.equals(CURATION_USER)) {
                    return " curation document";
                }
                else {
                    return " my document";
                }
            }
        };
        curationTargetChoice = new BootstrapRadioChoice<String>("curationTargetRadioBtn",
                Model.of(curationService.retrieveCurationTarget(
                userRepository.getCurrentUser().getUsername(),
                getModelObject().getProject().getId())),
                curationTargets, choiceRenderer)
        {
            private static final long serialVersionUID = 1513847274470368949L;

            @Override
            protected IValueMap getAdditionalAttributesForLabel(int aIndex, String aChoice)
            {
                // use normal font for choices
                IValueMap attrValMap = super.getAdditionalAttributesForLabel(aIndex, aChoice);
                if (attrValMap == null) {
                    attrValMap = new AttributeMap();
                }
                attrValMap.put("style", "font-weight:normal");
                return attrValMap;
            }
        };
        settingsForm.add(curationTargetChoice);
        
        // toggle visibility of settings form
        usersForm.add(new AjaxButton("toggleOptionsVisibility") {
            
            private static final long serialVersionUID = -5535838955781542216L;

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                settingsForm.setVisible(!settingsForm.isVisible());
                aTarget.add(mainContainer);
            }     
        });
        return settingsForm;
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        AnnotatorState state = getModelObject();
        // check that document is not already finished 
        // and user is curating not just viewing doc as admin
        User user = state.getUser();
        setEnabled((user.equals(userRepository.getCurrentUser()) || 
                user.getUsername().equals(CURATION_USER)) &&
                !documentService.isAnnotationFinished(state.getDocument(), user));
        configureVisibility();
    }

    protected void configureVisibility()
    {
        if (users.getModelObject().isEmpty()) {
            usersForm.setVisible(false);
            noDocsLabel.setVisible(true);
        }
        else {
            usersForm.setVisible(true);
            noDocsLabel.setVisible(false);
        }
    }

    private void merge(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        AnnotatorState state = getModelObject();
        User currentUser = userRepository.getCurrentUser();
        updateCurator(state, currentUser);
        // update selected users
        updateUsers(state);
        long projectId = state.getProject().getId();
        Collection<User> users = selectedUsers.getModelObject();
        curationService.updateMergeStrategy(currentUser.getUsername(), projectId,
                autoMergeStrat);
        mergeConfirm.setConfirmAction((target, form) -> {
            boolean mergeIncomplete = form.getModelObject().isMergeIncompleteAnnotations();
            doMerge(state, state.getUser().getUsername(), users, mergeIncomplete);
            target.add(annoPage);
        });
        mergeConfirm.show(aTarget);
    }

    private void updateCurator(AnnotatorState aState, User aCurrentUser)
    {
        long project = aState.getProject().getId();
        User curator = aCurrentUser;
        String currentUsername = aCurrentUser.getUsername();
        // update curation target
        if (curationTargetChoice.getModelObject()
                .equals(currentUsername)) {
            curationService.updateCurationName(currentUsername,
                    project, currentUsername);
        }
        else {
            curator = new User(CURATION_USER, Role.ROLE_USER);
            curationService.updateCurationName(currentUsername, project, CURATION_USER);
        }
        
        aState.setUser(curator);
        aState.getSelection().clear();
    }

    private void doMerge(AnnotatorState aState, String aCurator,
            Collection<User> aUsers, boolean aMergeIncomplete)
    {
        // merge cases
        try {
            SourceDocument doc = aState.getDocument();
            Map<String, CAS> userCases = curationService.retrieveUserCases(aUsers, doc);
            Optional<CAS> targetCas = curationService.retrieveCurationCAS(aCurator, 
                    aState.getProject().getId(), doc);
            if (targetCas.isPresent()) {
                MergeStrategy mergeStrat = curationService.retrieveMergeStrategy(
                        userRepository.getCurrentUser().getUsername(), aState.getProject().getId());
                mergeStrat.merge(aState, targetCas.get(), userCases, aMergeIncomplete);
                log.debug("{} merge done", mergeStrat.getUiName()); 
            }
        }
        catch (IOException e) {
            log.error(String.format("Could not retrieve CAS for user %s and project %d",
                        aCurator, aState.getProject().getId()));
            e.printStackTrace();
        } 
    }
    
    private Form<List<User>> createUserSelection()
    {
        Form<List<User>> usersForm = new Form<List<User>>("usersForm",
                LoadableDetachableModel.of(this::listSelectedUsers));
        LambdaAjaxButton<Void> clearButton = new LambdaAjaxButton<>("clear", this::clearUsers);
        LambdaAjaxButton<Void> mergeButton = new LambdaAjaxButton<>("merge", this::merge);
        LambdaAjaxButton<Void> showButton = new LambdaAjaxButton<>("show", this::selectAndShow);
        usersForm.add(clearButton);
        usersForm.add(showButton);
        usersForm.add(mergeButton);
        selectedUsers = new CheckGroup<User>("selectedUsers", usersForm.getModelObject());
        users = new ListView<User>("users",
                LoadableDetachableModel.of(this::listUsers))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<User> aItem)
            {
                aItem.add(new Check<User>("user", aItem.getModel()));
                aItem.add(new Label("name", aItem.getModelObject().getUsername()));

            }
        };
        selectedUsers.add(users);
        usersForm.add(selectedUsers);
        return usersForm;
    }
    
    private List<User> listSelectedUsers()
    {
        Optional<List<User>> users = curationService.listUsersSelectedForCuration(
                userRepository.getCurrentUser().getUsername(), getModelObject().getProject()
                .getId());
        if (!users.isPresent()) {
            return new ArrayList<>();
        }
        return users.get();
    }
    
    /**
     * retrieve annotators of this document which finished annotating
     */
    private List<User> listUsers()
    {
        return projectService
                .listProjectUsersWithPermissions(getModelObject().getProject(), 
                        PermissionLevel.ANNOTATOR)
                .stream().filter(user -> !user.equals(userRepository.getCurrentUser()) 
                        && hasFinishedDoc(user))
                .collect(Collectors.toList());
    }

    private boolean hasFinishedDoc(User aUser)
    {
        SourceDocument doc = getModelObject().getDocument();
        String username = aUser.getUsername();
        if (documentService.existsAnnotationDocument(doc, username) && 
                documentService.getAnnotationDocument(doc, username).getState()
                .equals(AnnotationDocumentState.FINISHED)) {
            return true;
        }
        else {
            return false;
        }
    }
    
    private void selectAndShow(AjaxRequestTarget aTarget, Form<Void> aForm) {
        // switch to manual merge
        AnnotatorState state = getModelObject();
        curationService.updateMergeStrategy(userRepository.getCurrentUser().getUsername(), 
                state.getProject().getId(), manualMergeStrat);
        
        updateUsers(state);
        aTarget.add(usersForm);
        annoPage.actionRefreshDocument(aTarget);
    }
    
    private void updateForNewCurator(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        // updateCurator and merge strategy
        AnnotatorState state = getModelObject();
        User currentUser = userRepository.getCurrentUser();
        updateCurator(state, currentUser);
        curationService.updateMergeStrategy(currentUser.getUsername(), 
                state.getProject().getId(), manualMergeStrat);
        updateUsers(state);
        aTarget.add(mainContainer);
        // open curation doc
        annoPage.actionLoadDocument(aTarget);
    }
    
    private void updateUsers(AnnotatorState aState)
    {
        Collection<User> users = selectedUsers.getModelObject();
        curationService.updateUsersSelectedForCuration(
                userRepository.getCurrentUser().getUsername(), aState.getProject().getId(), users);
    }
    
    private void clearUsers(AjaxRequestTarget aTarget, Form<Void> aForm) 
    {
        AnnotatorState state = getModelObject();
        selectedUsers.setModelObject(new ArrayList<>());
        curationService.clearUsersSelectedForCuration(
                userRepository.getCurrentUser().getUsername(), state.getProject().getId());
        aTarget.add(usersForm);
        annoPage.actionRefreshDocument(aTarget);
    }

}
