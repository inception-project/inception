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
package de.tudarmstadt.ukp.clarin.webanno.brat.actionbar.script;

import static de.tudarmstadt.ukp.clarin.webanno.brat.preferences.BratAnnotationEditorManagerPrefs.KEY_BRAT_EDITOR_MANAGER_PREFS;
import static java.util.Locale.ROOT;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorAutoConfiguration;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link BratAnnotationEditorAutoConfiguration#scriptDirectionActionBarExtension}.
 * </p>
 */
@Order(ActionBarExtension.ORDER_SCRIPT_DIRECTION)
public class ScriptDirectionActionBarExtension
    implements ActionBarExtension
{
    private final PreferencesService preferencesService;

    @Autowired
    public ScriptDirectionActionBarExtension(PreferencesService aPreferencesService)
    {
        preferencesService = aPreferencesService;
    }

    @Override
    public boolean accepts(AnnotationPageBase aPage)
    {
        AnnotatorState state = aPage.getModelObject();

        if (state == null) {
            return false;
        }

        if (state.getEditorFactoryId() == null) {
            return false;
        }

        if (!state.getEditorFactoryId().toLowerCase(ROOT).endsWith("brateditor")) {
            return false;
        }

        return preferencesService
                .loadDefaultTraitsForProject(KEY_BRAT_EDITOR_MANAGER_PREFS, state.getProject())
                .isChangingScriptDirectionAllowed();
    }

    @Override
    public Panel createActionBarItem(String aId, AnnotationPageBase aPage)
    {
        return new ScriptDirectionActionBarItem(aId, aPage);
    }
}
