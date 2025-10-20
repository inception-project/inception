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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationEditorManagerPrefs.KEY_ANNOTATION_EDITOR_MANAGER_PREFS;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationEditorManagerPrefs;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaForm;

public class AnnotationEditorManagerPrefsPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = 4663693446465391162L;

    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;
    private @SpringBean PreferencesService preferencesService;

    private IModel<AnnotationEditorManagerPrefs> state;
    private IModel<Pair<String, String>> defaultEditor;

    public AnnotationEditorManagerPrefsPanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);
        setOutputMarkupPlaceholderTag(true);

        state = CompoundPropertyModel.of(null);
        defaultEditor = Model.of();

        var form = new LambdaForm<AnnotationEditorManagerPrefs>("form", state);

        var editor = new DropDownChoice<Pair<String, String>>("defaultEditor");
        editor.setModel(defaultEditor);
        editor.setChoiceRenderer(new ChoiceRenderer<>("value"));
        editor.setChoices(listAvailableEditors());
        editor.setNullValid(true);
        editor.add(visibleWhen(() -> editor.getChoices().size() > 1));
        form.add(editor);

        form.add(new CheckBox("preferencesAccessAllowed").setOutputMarkupId(true));

        form.add(new CheckBox("showDeleteAnnotationConfirmation").setOutputMarkupId(true));

        form.onSubmit(this::actionSave);

        add(form);

        actionLoad();
    }

    private List<Pair<String, String>> listAvailableEditors()
    {
        return annotationEditorRegistry.getEditorFactories().stream() //
                .map(f -> Pair.of(f.getBeanName(), f.getDisplayName())) //
                .toList();
    }

    private void actionLoad()
    {
        state.setObject(preferencesService.loadDefaultTraitsForProject(
                KEY_ANNOTATION_EDITOR_MANAGER_PREFS, getModel().getObject()));

        var defaultEditorId = state.getObject().getDefaultEditor();
        var factory = annotationEditorRegistry.getEditorFactory(defaultEditorId);

        if (factory != null) {
            defaultEditor.setObject(listAvailableEditors().stream() //
                    .filter(e -> Objects.equals(defaultEditorId, e.getKey())) //
                    .findFirst() //
                    .orElse(null));
        }
        else {
            defaultEditor.setObject(null);
        }
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<AnnotationEditorManagerPrefs> aDummy)
    {
        state.getObject()
                .setDefaultEditor(defaultEditor.map(Pair::getKey).orElse(null).getObject());

        preferencesService.saveDefaultTraitsForProject(KEY_ANNOTATION_EDITOR_MANAGER_PREFS,
                getModel().getObject(), state.getObject());
    }
}
