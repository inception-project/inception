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
package de.tudarmstadt.ukp.inception.diam.editor.actions;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.preferences.ClientSidePreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.ClientSidePreferenceMapValue;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link DiamAutoConfig#loadPreferences}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_RENDER_HANDLER)
public class LoadPreferences
    extends EditorAjaxRequestHandlerBase
{
    public static final String COMMAND = "loadPreferences";

    public static final String PARAM_KEY = "key";

    private final PreferencesService preferencesService;
    private final UserDao userService;

    public LoadPreferences(UserDao aUserService, PreferencesService aPreferencesService)
    {
        userService = aUserService;
        preferencesService = aPreferencesService;
    }

    @Override
    public String getCommand()
    {
        return COMMAND;
    }

    @Override
    public AjaxResponse handle(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget,
            Request aRequest)
    {
        try {
            var key = new ClientSidePreferenceKey<ClientSidePreferenceMapValue>(
                    ClientSidePreferenceMapValue.class,
                    aRequest.getRequestParameters().getParameterValue(PARAM_KEY).toString());
            var project = getAnnotatorState().getProject();
            var sessionOwner = userService.getCurrentUser();
            var prefs = preferencesService.loadTraitsForUserAndProject(key, sessionOwner, project);
            var json = JSONUtil.toInterpretableJsonString(prefs);
            attachResponse(aTarget, aRequest, json);
            return new DefaultAjaxResponse();
        }
        catch (Exception e) {
            return handleError("Unable to load preferences", e);
        }
    }
}
