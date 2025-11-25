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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationNavigationUserPrefs.KEY_ANNOTATION_NAVIGATION_USER_PREFS;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationNavigationUserPrefs;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaForm;

public class AnnotationNavigationUserPrefsPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = 4663693446465391162L;

    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;
    private @SpringBean PreferencesService preferencesService;

    private IModel<AnnotationNavigationUserPrefs> state;

    public AnnotationNavigationUserPrefsPanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);
        setOutputMarkupPlaceholderTag(true);

        state = CompoundPropertyModel.of(null);
        actionLoad();

        var form = new LambdaForm<AnnotationNavigationUserPrefs>("form", state);

        form.add(new CheckBox("finishedDocumentsSkippedByNavigation").setOutputMarkupId(true));

        form.onSubmit(this::actionSave);

        add(form);
    }

    private void actionLoad()
    {
        state.setObject(preferencesService.loadDefaultTraitsForProject(
                KEY_ANNOTATION_NAVIGATION_USER_PREFS, getModel().getObject()));
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<AnnotationNavigationUserPrefs> aDummy)
    {
        preferencesService.saveDefaultTraitsForProject(KEY_ANNOTATION_NAVIGATION_USER_PREFS,
                getModel().getObject(), state.getObject());
    }
}
