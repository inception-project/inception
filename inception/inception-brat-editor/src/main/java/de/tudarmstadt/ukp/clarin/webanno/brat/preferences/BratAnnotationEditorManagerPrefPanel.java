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
package de.tudarmstadt.ukp.clarin.webanno.brat.preferences;

import static de.tudarmstadt.ukp.clarin.webanno.brat.preferences.BratAnnotationEditorManagerPrefs.KEY_BRAT_EDITOR_MANAGER_PREFS;
import static java.util.Arrays.asList;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaForm;

public class BratAnnotationEditorManagerPrefPanel
    extends Panel
{
    private static final long serialVersionUID = 3635729182772294488L;

    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;
    private @SpringBean PreferencesService preferencesService;
    private @SpringBean ProjectService projectService;

    private IModel<BratAnnotationEditorManagerPrefs> bratPreferences;

    public BratAnnotationEditorManagerPrefPanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);
        setOutputMarkupPlaceholderTag(true);

        bratPreferences = CompoundPropertyModel.of(actionLoad());

        LambdaForm<BratAnnotationEditorManagerPrefs> form = new LambdaForm<>("form",
                bratPreferences);

        NumberTextField<Integer> defaultPageSize = new NumberTextField<>("defaultPageSize",
                Integer.class);
        defaultPageSize.setMinimum(1);
        form.add(defaultPageSize);

        form.add(new CheckBox("changingScriptDirectionAllowed").setOutputMarkupId(true));

        // Historically, the script direction setting is on the project entity - maybe move it
        // one day...
        DropDownChoice<ScriptDirection> scriptDirection = new DropDownChoice<>("scriptDirection");
        scriptDirection.setModel(PropertyModel.of(aProjectModel, "scriptDirection"));
        scriptDirection.setChoiceRenderer(new EnumChoiceRenderer<>(this));
        scriptDirection.setChoices(asList(ScriptDirection.values()));
        form.add(scriptDirection);

        form.onSubmit(this::actionSave);

        add(form);
    }

    @SuppressWarnings("unchecked")
    public IModel<Project> getModel()
    {
        return (IModel<Project>) getDefaultModel();
    }

    private BratAnnotationEditorManagerPrefs actionLoad()
    {
        return preferencesService.loadDefaultTraitsForProject(KEY_BRAT_EDITOR_MANAGER_PREFS,
                getModel().getObject());
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<BratAnnotationEditorManagerPrefs> aForm)
    {
        preferencesService.saveDefaultTraitsForProject(KEY_BRAT_EDITOR_MANAGER_PREFS,
                getModel().getObject(), aForm.getModelObject());
        projectService.updateProject(getModel().getObject());
    }
}
