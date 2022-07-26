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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase.KEY_EDITOR_STATE;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationEditorState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaForm;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorFactory;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;

public class DefaultAnnotationEditorStatePanel
    extends Panel
{
    private static final long serialVersionUID = 4663693446465391162L;

    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;
    private @SpringBean PreferencesService preferencesService;

    private IModel<Pair<String, String>> defaultEditor;

    public DefaultAnnotationEditorStatePanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);
        setOutputMarkupPlaceholderTag(true);

        defaultEditor = Model.of();

        LambdaForm<Void> form = new LambdaForm<>("form");

        DropDownChoice<Pair<String, String>> editor = new DropDownChoice<>("defaultEditor");
        editor.setModel(defaultEditor);
        editor.setChoiceRenderer(new ChoiceRenderer<>("value"));
        editor.setChoices(listAvailableEditors());
        editor.setNullValid(true);
        editor.add(visibleWhen(() -> editor.getChoices().size() > 1));
        form.add(editor);

        form.onSubmit(this::actionSave);

        add(form);

        actionLoad();
    }

    private List<Pair<String, String>> listAvailableEditors()
    {
        List<Pair<String, String>> editorChoices = annotationEditorRegistry.getEditorFactories()
                .stream().map(f -> Pair.of(f.getBeanName(), f.getDisplayName()))
                .collect(Collectors.toList());
        return editorChoices;
    }

    @SuppressWarnings("unchecked")
    public IModel<Project> getModel()
    {
        return (IModel<Project>) getDefaultModel();
    }

    private void actionLoad()
    {
        AnnotationEditorState state = preferencesService
                .loadDefaultTraitsForProject(KEY_EDITOR_STATE, getModel().getObject());

        AnnotationEditorFactory factory = annotationEditorRegistry
                .getEditorFactory(state.getDefaultEditor());

        if (factory != null) {
            defaultEditor.setObject(listAvailableEditors().stream() //
                    .filter(e -> Objects.equals(state.getDefaultEditor(), e.getKey())) //
                    .findFirst() //
                    .orElse(null));
        }
        else {
            defaultEditor.setObject(null);
        }
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<Void> aDummy)
    {
        AnnotationEditorState state = preferencesService
                .loadDefaultTraitsForProject(KEY_EDITOR_STATE, getModel().getObject());

        state.setDefaultEditor(defaultEditor.map(Pair::getKey).orElse(null).getObject());

        preferencesService.saveDefaultTraitsForProject(KEY_EDITOR_STATE, getModel().getObject(),
                state);
    }
}
