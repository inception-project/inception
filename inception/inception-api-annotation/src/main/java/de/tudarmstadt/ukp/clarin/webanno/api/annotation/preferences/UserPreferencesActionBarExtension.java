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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationEditorManagerPrefs.KEY_ANNOTATION_EDITOR_MANAGER_PREFS;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationEditorManagerPrefs;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;

@Order(ActionBarExtension.ORDER_SETTINGS)
@Component
public class UserPreferencesActionBarExtension
    implements ActionBarExtension
{
    private final PreferencesService preferencesService;

    public UserPreferencesActionBarExtension(PreferencesService aPreferencesService)
    {
        super();
        preferencesService = aPreferencesService;
    }

    @Override
    public boolean accepts(AnnotationPageBase aPage)
    {
        AnnotationEditorManagerPrefs editorState = preferencesService.loadDefaultTraitsForProject(
                KEY_ANNOTATION_EDITOR_MANAGER_PREFS, aPage.getProject());

        return editorState.isPreferencesAccessAllowed()
                || aPage.getModelObject().getMode() == CURATION;
    }

    @Override
    public Panel createActionBarItem(String aId, AnnotationPageBase aPage)
    {
        return new PreferencesActionBarItem(aId, aPage);
    }
}
