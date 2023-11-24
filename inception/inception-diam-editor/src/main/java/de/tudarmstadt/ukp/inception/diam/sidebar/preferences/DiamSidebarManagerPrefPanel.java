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
package de.tudarmstadt.ukp.inception.diam.sidebar.preferences;

import static de.tudarmstadt.ukp.inception.diam.sidebar.preferences.DiamSidebarManagerPrefs.KEY_DIAM_SIDEBAR_MANAGER_PREFS;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaForm;

public class DiamSidebarManagerPrefPanel
    extends Panel
{
    private static final long serialVersionUID = 3635729182772294488L;

    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;
    private @SpringBean PreferencesService preferencesService;
    private @SpringBean ProjectService projectService;

    private CompoundPropertyModel<DiamSidebarManagerPrefs> sidebarPreferences;

    public DiamSidebarManagerPrefPanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);
        setOutputMarkupPlaceholderTag(true);

        sidebarPreferences = CompoundPropertyModel.of(actionLoad());

        LambdaForm<DiamSidebarManagerPrefs> form = new LambdaForm<>("form", sidebarPreferences);

        form.add(new PinnedGroupsPanel("pinnedGroups", sidebarPreferences.bind("pinnedGroups")));

        form.onSubmit(this::actionSave);

        add(form);
    }

    @SuppressWarnings("unchecked")
    public IModel<Project> getModel()
    {
        return (IModel<Project>) getDefaultModel();
    }

    private DiamSidebarManagerPrefs actionLoad()
    {
        return preferencesService.loadDefaultTraitsForProject(KEY_DIAM_SIDEBAR_MANAGER_PREFS,
                getModel().getObject());
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<DiamSidebarManagerPrefs> aForm)
    {
        preferencesService.saveDefaultTraitsForProject(KEY_DIAM_SIDEBAR_MANAGER_PREFS,
                getModel().getObject(), aForm.getModelObject());
    }
}
