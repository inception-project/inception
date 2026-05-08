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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.settings;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import wicket.contrib.input.events.key.KeyType;

@Order(400)
public class SettingsPageMenuItem
    implements MenuItem
{
    private @Autowired UserDao userService;

    @Override
    public String getPath()
    {
        return "/admin/settings";
    }

    @Override
    public Component getIcon(String aId)
    {
        return new Icon(aId, FontAwesome5IconType.sliders_h_s);
    }

    @Override
    public String getLabel()
    {
        return "Settings";
    }

    @Override
    public boolean applies()
    {
        return userService.isAdministrator(userService.getCurrentUser());
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return SettingsPage.class;
    }

    @Override
    public KeyType[] shortcut()
    {
        return new KeyType[] { KeyType.Alt, KeyType.s };
    }
}
