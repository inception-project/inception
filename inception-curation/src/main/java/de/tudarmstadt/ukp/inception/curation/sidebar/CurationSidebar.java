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
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

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
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.value.AttributeMap;
import org.apache.wicket.util.value.IValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import de.tudarmstadt.ukp.inception.curation.CurationService;
import de.tudarmstadt.ukp.inception.curation.merge.ManualMergeStrategy;
import de.tudarmstadt.ukp.inception.curation.merge.MergeStrategy;


public class CurationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -4195790451286055737L;
    private static final String DEFAULT_CURATION_TARGET = "  my document";
    
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean CurationService curationService;
    private @SpringBean DocumentService documentService;
    
    private @SpringBean List<MergeStrategy> mergeStrategies;
    private @SpringBean ManualMergeStrategy defaultMergeStrategy;
    private MergeStrategy selectedMergeStrategy;
    
    private CheckGroup<User> selectedUsers;
    private Form<List<User>> usersForm;
    private Form<Void> settingsForm ;
    private WebMarkupContainer mainContainer;
    private DropDownChoice<MergeStrategy> mergeChoice;

    private final List<String> curationTargets = Arrays
            .asList(new String[] { "  curation document", DEFAULT_CURATION_TARGET});
    private String selectedCurationTarget = DEFAULT_CURATION_TARGET;
    
    private AnnotatorState state;
    private AnnotationPage annoPage;
    
    // TODO: only show to people who are curators
    public CurationSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aAnnotationPage);
        selectedMergeStrategy = defaultMergeStrategy;
        state = aModel.getObject();
        annoPage = aAnnotationPage;
        mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.setOutputMarkupId(true);
        add(mainContainer);
        
        // set up user-checklist
        usersForm = createUserSelection();
        usersForm.setOutputMarkupId(true);
        mainContainer.add(usersForm);
        
        // set up settings form for curation target, merge op selection
        Form<Void> settingsForm = createSettingsForm("settingsForm");
        settingsForm.setOutputMarkupId(true);
        settingsForm.setVisible(false);
        mainContainer.add(settingsForm);
    }

    private Form<Void> createSettingsForm(String aId)
    {
        settingsForm = new Form<Void>(aId);
        LambdaAjaxButton<Void> applyBtn = new LambdaAjaxButton<>("apply", this::merge);
        settingsForm.add(applyBtn);
        
        // set up selection for merge strategy
        mergeChoice = createMergeDropDownChoice("mergeChoice");
        settingsForm.add(mergeChoice);
        
        // set up curation target selection as radio button
        RadioChoice<String> curationTargetChoice = new RadioChoice<String>("curationTargetRadioBtn",
                new PropertyModel<String>(this, "selectedCurationTarget"), curationTargets)
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
        curationTargetChoice.setPrefix("<br/>");
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
    
    private DropDownChoice<MergeStrategy> createMergeDropDownChoice(String aId)
    {
        DropDownChoice<MergeStrategy> mergeChoice = new DropDownChoice<MergeStrategy>(aId,
                new PropertyModel<MergeStrategy>(this, "selectedMergeStrategy"), 
                new ListModel<MergeStrategy>(mergeStrategies),
                new ChoiceRenderer<MergeStrategy>("uiName"));
        return mergeChoice;
    }
    
    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        setEnabled(!documentService
                .isAnnotationFinished(state.getDocument(), state.getUser()));
    }

    private void merge(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        long project = state.getProject().getId();
        User curator = userRepository.getCurrentUser();
        String currentUsername = curator.getUsername();
        if (selectedCurationTarget.equals(DEFAULT_CURATION_TARGET)) {
            curationService.updateCurationName(currentUsername,
                    project, currentUsername);
        }
        else {
            curator = new User(CURATION_USER, Role.ROLE_USER);
            curationService.updateCurationName(currentUsername, project, CURATION_USER);
        }
        
        state.setUser(curator);
        state.getSelection().clear();
        // update selected users
        Collection<User> users = selectedUsers.getModelObject();
        curationService.updateUsersSelectedForCuration(
                state.getUser().getUsername(), state.getProject().getId(), users);
        // merge cases
        try {
            SourceDocument doc = state.getDocument();
            Map<String, CAS> userCases = curationService.retrieveUserCases(users, doc);
            Optional<CAS> targetCas = curationService.retrieveCurationCAS(curator.getUsername(), 
                    state.getProject().getId(), doc);
            if (targetCas.isPresent()) {
                MergeStrategy mergeStrat = ((MergeStrategy) mergeChoice.getDefaultModelObject());
                mergeStrat.merge(state, targetCas.get(), userCases);
                log.info("{} merge done", mergeStrat.getUiName());  //TODO change to debug
            }
        }
        catch (IOException e) {
            log.error(String.format("Could not retrieve CAS for user %s and project %d",
                        curator.getUsername(), state.getProject().getId()));
            e.printStackTrace();
        }
        //open curation doc
        annoPage.actionLoadDocument(aTarget);
    }
    
    private Form<List<User>> createUserSelection()
    {
        Form<List<User>> usersForm = new Form<List<User>>("usersForm",
                LoadableDetachableModel.of(this::listSelectedUsers));
        LambdaAjaxButton<Void> clearButton = new LambdaAjaxButton<>("clear", this::clearUsers);
        LambdaAjaxButton<Void> showButton = new LambdaAjaxButton<>("show", this::updateUsers);
        usersForm.add(clearButton);
        usersForm.add(showButton);
        selectedUsers = new CheckGroup<User>("selectedUsers", usersForm.getModelObject());
        ListView<User> users = new ListView<User>("users",
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
        usersForm.add(visibleWhen(() -> !users.getModelObject().isEmpty()));
        return usersForm;
    }
    
    private List<User> listSelectedUsers()
    {
        Optional<List<User>> users = curationService.listUsersSelectedForCuration(
                userRepository.getCurrentUser().getUsername(), state.getProject().getId());
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
                .listProjectUsersWithPermissions(state.getProject(), PermissionLevel.ANNOTATOR)
                .stream().filter(user -> !user.equals(userRepository.getCurrentUser()) 
                        && hasFinishedDoc(user))
                .collect(Collectors.toList());
    }

    private boolean hasFinishedDoc(User aUser)
    {
        SourceDocument doc = state.getDocument();
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

    private void updateUsers(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        Collection<User> users = selectedUsers.getModelObject();
        curationService.updateUsersSelectedForCuration(
                state.getUser().getUsername(), state.getProject().getId(), users);
        aTarget.add(usersForm);
        annoPage.actionRefreshDocument(aTarget);
    }
    
    private void clearUsers(AjaxRequestTarget aTarget, Form<Void> aForm) {
        selectedUsers.setModelObject(new ArrayList<>());
        curationService.clearUsersSelectedForCuration(
                state.getUser().getUsername(), state.getProject().getId());
        aTarget.add(usersForm);
        annoPage.actionRefreshDocument(aTarget);
    }

    //getters, setters needed for property-models
    public MergeStrategy getSelectedMergeStrategy()
    {
        return selectedMergeStrategy;
    }

    public void setSelectedMergeStrategy(MergeStrategy aSelectedMergeStrategy)
    {
        selectedMergeStrategy = aSelectedMergeStrategy;
    }

    public String getSelectedCurationTarget()
    {
        return selectedCurationTarget;
    }

    public void setSelectedCurationTarget(String aSelectedCurationTarget)
    {
        selectedCurationTarget = aSelectedCurationTarget;
    }

}
