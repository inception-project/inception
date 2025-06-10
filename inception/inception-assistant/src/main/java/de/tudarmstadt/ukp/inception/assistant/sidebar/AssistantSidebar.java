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
package de.tudarmstadt.ukp.inception.assistant.sidebar;

import static de.tudarmstadt.ukp.inception.assistant.sidebar.AssistantSidebarPrefs.KEY_ASSISTANT_SIDEBAR_PREFS;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome6IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.events.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.inception.assistant.AssistantService;
import de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryService;
import de.tudarmstadt.ukp.inception.bootstrap.IconToggleBox;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormSubmittingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;

public class AssistantSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -1585047099720374119L;

    private @SpringBean UserDao userService;
    private @SpringBean AssistantService assistantService;
    private @SpringBean DocumentQueryService documentQueryService;
    private @SpringBean SchedulingService schedulingService;
    private @SpringBean PreferencesService preferencesService;

    private AssistantPanel chat;

    private CompoundPropertyModel<AssistantSidebarPrefs> sidebarPrefs;
    private IModel<Boolean> debugMode;

    public AssistantSidebar(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);

        sidebarPrefs = new CompoundPropertyModel<>(Model.of(loadSidebarPrefs()));

        debugMode = new LambdaModelAdapter<>( //
                () -> assistantService.isDebugMode(userService.getCurrentUsername(),
                        getModelObject().getProject()), //
                onOff -> assistantService.setDebugMode(userService.getCurrentUsername(),
                        getModelObject().getProject(), onOff));

        chat = new AssistantPanel("chat");
        queue(chat);

        var form = new Form<>("form", sidebarPrefs);
        add(form);

        form.add(new LambdaAjaxLink("reindex", this::actionReindex));

        form.add(new LambdaAjaxLink("clear", this::actionClear));

        form.add(new IconToggleBox("watchMode") //
                .setCheckedIcon(FontAwesome6IconType.eye_s) //
                .setCheckedTitle(Model.of("Watching annotation actions and commenting")) //
                .setUncheckedIcon(FontAwesome6IconType.eye_slash_s) //
                .setUncheckedTitle(Model.of("Not watching annotation actions")) //
                .add(new LambdaAjaxFormSubmittingBehavior(CHANGE_EVENT,
                        _target -> saveSidebarPrefs())));

        form.add(new IconToggleBox("debugMode") //
                .setCheckedIcon(FontAwesome6IconType.bug_s) //
                .setCheckedTitle(Model.of("Recording and showing internal messages")) //
                .setUncheckedIcon(FontAwesome6IconType.bug_slash_s) //
                .setUncheckedTitle(Model.of("Not recoording and showing internal messages")) //
                .setModel(debugMode) //
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        _target -> _target.add(chat))));
    }

    private AssistantSidebarPrefs loadSidebarPrefs()
    {
        var sessionOwner = userService.getCurrentUser();
        return preferencesService.loadTraitsForUserAndProject(KEY_ASSISTANT_SIDEBAR_PREFS,
                sessionOwner, getModelObject().getProject());
    }

    private void saveSidebarPrefs()
    {
        var sessionOwner = userService.getCurrentUser();
        preferencesService.saveTraitsForUserAndProject(KEY_ASSISTANT_SIDEBAR_PREFS, sessionOwner,
                getModelObject().getProject(), sidebarPrefs.getObject());
    }

    private void actionReindex(AjaxRequestTarget aTarget)
    {
        documentQueryService.rebuildIndexAsync(getModelObject().getProject());
    }

    private void actionClear(AjaxRequestTarget aTarget)
    {
        var sessionOwner = userService.getCurrentUsername();
        var project = getModelObject().getProject();
        assistantService.clearConversation(sessionOwner, project);
    }

    @OnEvent
    public void onFeatureValueUpdated(FeatureValueUpdatedEvent aEvent)
    {
        if (!sidebarPrefs.map(AssistantSidebarPrefs::isWatchMode).orElse(false).getObject()
                && aEvent.getNewValue() == null) {
            return;
        }

        var sessionOwner = userService.getCurrentUser();

        schedulingService.enqueue(WatchAnnotationTask.builder() //
                .withTrigger("Assistant watching") //
                .withSessionOwner(sessionOwner) //
                .withProject(aEvent.getProject()) //
                .withDocument(aEvent.getDocument()) //
                .withDataOwner(aEvent.getDocumentOwner()) //
                .withAnnotation(VID.of(aEvent.getFS())) //
                .build());
    }
}
