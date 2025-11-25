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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.brat.preferences.BratAnnotationEditorManagerPrefPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.diam.sidebar.preferences.DiamSidebarManagerPrefPanel;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaForm;

public class AnnotationPreferencesProjectSettingsPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 4618192360418016955L;

    private static final String CID_FORM = "form";
    private static final String CID_SAVE = "save";
    private static final String CID_ANNOTATION_SIDEBAR = "annotationSidebar";
    private static final String CID_ANNOTATION_EDITOR_MANAGER_PREFS = "annotationEditorManagerPrefsPanel";
    private static final String CID_NAVIGATION_PREFS = "navigationPrefs";
    private static final String CID_BRAT_ANNOTATION_EDITOR_MANAGER_PREFS = "bratAnnotationEditorManagerPrefs";
    private static final String CID_DIAM_ANNOTATION_SIDEBAR_MANAGER_PREFS = "diamAnnotationSidebarManagerPrefs";
    private static final String CID_ANNOTATION_SEARCH = "annotationSearch";

    public AnnotationPreferencesProjectSettingsPanel(String aId, IModel<Project> aProject)
    {
        super(aId, aProject);
        setOutputMarkupPlaceholderTag(true);

        queue(new LambdaForm<>(CID_FORM));
        queue(new LambdaAjaxButton<>(CID_SAVE, this::actionSave));

        queue(new DefaultAnnotationSidebarStatePanel(CID_ANNOTATION_SIDEBAR, aProject));
        queue(new DiamSidebarManagerPrefPanel(CID_DIAM_ANNOTATION_SIDEBAR_MANAGER_PREFS, aProject));
        queue(new AnnotationEditorManagerPrefsPanel(CID_ANNOTATION_EDITOR_MANAGER_PREFS, aProject));
        queue(new AnnotationNavigationUserPrefsPanel(CID_NAVIGATION_PREFS, aProject));
        queue(new BratAnnotationEditorManagerPrefPanel(CID_BRAT_ANNOTATION_EDITOR_MANAGER_PREFS,
                aProject));
        queue(new AnnotationSearchStatePanel(CID_ANNOTATION_SEARCH, aProject));
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<Void> aDummy)
    {
        success("Settings updated");
        aTarget.addChildren(getPage(), IFeedback.class);
    }
}
