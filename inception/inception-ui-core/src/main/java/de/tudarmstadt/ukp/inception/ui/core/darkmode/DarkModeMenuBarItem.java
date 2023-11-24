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
package de.tudarmstadt.ukp.inception.ui.core.darkmode;

import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.UIState.KEY_UI;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.UIState;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class DarkModeMenuBarItem
    extends Panel
{
    private static final long serialVersionUID = 7486091139970717604L;

    private static final String CID_DARK_MODE_SWITCH = "darkModeSwitch";
    private static final String CID_ICON = "icon";

    private @SpringBean PreferencesService preferencesService;
    private @SpringBean UserDao userRepository;

    private IModel<User> user;

    public DarkModeMenuBarItem(String aId)
    {
        super(aId);

        user = LoadableDetachableModel.of(userRepository::getCurrentUser);

        queue(new LambdaAjaxLink(CID_DARK_MODE_SWITCH, this::actionToggleDarkMode));
        queue(new Icon(CID_ICON, LoadableDetachableModel.of( //
                () -> UIState.DEFAULT_THEME.equals(getTheme()) ? FontAwesome5IconType.sun_s
                        : FontAwesome5IconType.moon_s)));

        add(visibleWhen(user.isPresent()));
    }

    private String getTheme()
    {
        if (user.isPresent().getObject()) {
            return preferencesService.loadTraitsForUser(KEY_UI, user.getObject()).getTheme();
        }

        return UIState.DEFAULT_THEME;
    }

    private void actionToggleDarkMode(AjaxRequestTarget aTarget)
    {
        var traits = preferencesService.loadTraitsForUser(KEY_UI, user.getObject());
        if (UIState.LIGHT_THEME.equals(traits.getTheme())) {
            traits.setTheme(UIState.DARK_THEME);
        }
        else {
            traits.setTheme(UIState.LIGHT_THEME);
        }
        preferencesService.saveTraitsForUser(KEY_UI, user.getObject(), traits);
        aTarget.add(getPage());
    }
}
