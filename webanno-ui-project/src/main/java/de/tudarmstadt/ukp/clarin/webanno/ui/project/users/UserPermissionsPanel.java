/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.users;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static java.util.Arrays.asList;

import java.util.Collection;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractChoice.LabelPosition;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;

public class UserPermissionsPanel
    extends Panel
{
    private static final long serialVersionUID = -5278078988218713188L;

    private @SpringBean ProjectService projectRepository;
    
    private IModel<Project> project;
    private IModel<User> user;

    public UserPermissionsPanel(String aId, IModel<Project> aProject, IModel<User> aUser)
    {
        super(aId);
        
        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
        
        project = aProject;
        user = aUser;
        
        Form<Void> form = new Form<>("form");
        add(form);

        CheckBoxMultipleChoice<PermissionLevel> levels = new CheckBoxMultipleChoice<>("permissions");
        levels.setPrefix("<div class=\"checkbox\">");
        levels.setSuffix("</div>");
        levels.setLabelPosition(LabelPosition.WRAP_AFTER);
        // This model adapter handles loading/saving permissions directly to the DB
        levels.setModel(new LambdaModelAdapter<Collection<PermissionLevel>>(() -> {
            return projectRepository.getProjectPermissionLevels(user.getObject(),
                    project.getObject());
        }, (lvls) -> {
            projectRepository.setProjectPermissionLevels(user.getObject(), project.getObject(),
                    lvls);
        }));
        levels.setChoices(asList(MANAGER, CURATOR, ANNOTATOR));
        levels.setChoiceRenderer(new EnumChoiceRenderer<>(levels));
        form.add(levels);
        
        form.add(new Label("username", PropertyModel.of(aUser, "username")));
        form.add(new LambdaAjaxButton<>("save", this::actionSave));
        form.add(new LambdaAjaxLink("cancel", this::actionCancel));
    }
    
    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        
        setVisible(user.getObject() != null);
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<Void> aForm) {
        // The model adapter already commits the changes for us as part of the normal form
        // processing cycle. So nothing special to do here.
        
        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(getPage());
    }
    
    
    private void actionCancel(AjaxRequestTarget aTarget) {
        user.setObject(null);
        
        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(getPage());
    }
}
