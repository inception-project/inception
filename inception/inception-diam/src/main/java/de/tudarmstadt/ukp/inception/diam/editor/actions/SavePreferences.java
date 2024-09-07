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

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromValidatedJsonString;
import static org.apache.commons.lang3.StringUtils.isBlank;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.preferences.ClientSidePreferenceMapValue;
import de.tudarmstadt.ukp.inception.preferences.ClientSiderUserPreferencesProviderRegistry;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link DiamAutoConfig#savePreferences}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_RENDER_HANDLER)
public class SavePreferences
    extends EditorAjaxRequestHandlerBase
{
    public static final String COMMAND = "savePreferences";

    public static final String PARAM_KEY = "key";
    public static final String PARAM_DATA = "data";

    private final PreferencesService preferencesService;
    private final UserDao userService;
    private final ClientSiderUserPreferencesProviderRegistry clientSiderUserPreferencesProviderRegistry;

    public SavePreferences(UserDao aUserService, PreferencesService aPreferencesService,
            ClientSiderUserPreferencesProviderRegistry aClientSiderUserPreferencesProviderRegistry)
    {
        userService = aUserService;
        preferencesService = aPreferencesService;
        clientSiderUserPreferencesProviderRegistry = aClientSiderUserPreferencesProviderRegistry;
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
            var keyParameter = aRequest.getRequestParameters().getParameterValue(PARAM_KEY)
                    .toOptionalString();
            if (isBlank(keyParameter)) {
                throw new IllegalArgumentException("No key specified");
            }

            var prefProvider = clientSiderUserPreferencesProviderRegistry
                    .getProviderForClientSideKey(keyParameter);

            if (prefProvider.isEmpty()) {
                throw new IllegalStateException(
                        "Client-side user preferences not allowed for given key");
            }

            var key = prefProvider.get().getUserPreferencesKey();
            var schema = prefProvider.get().getUserPreferencesSchema();

            if (key.isEmpty() || schema.isEmpty()) {
                throw new IllegalStateException(
                        "Client-side user preferences not allowed for given key");
            }

            var project = getAnnotatorState().getProject();
            var sessionOwner = userService.getCurrentUser();
            var dataString = aRequest.getRequestParameters().getParameterValue(PARAM_DATA);
            var data = fromValidatedJsonString(ClientSidePreferenceMapValue.class,
                    dataString.toString(), schema.get());
            preferencesService.saveTraitsForUserAndProject(key.get(), sessionOwner, project, data);
            return new DefaultAjaxResponse();
        }
        catch (Exception e) {
            return handleError("Unable to save client-side user preferences", e);
        }
    }
}
