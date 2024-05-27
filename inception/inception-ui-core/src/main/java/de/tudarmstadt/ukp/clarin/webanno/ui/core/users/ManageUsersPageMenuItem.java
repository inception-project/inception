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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.users;

import org.apache.wicket.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.IconType;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;
import wicket.contrib.input.events.key.KeyType;

@Order(200)
@Component
public class ManageUsersPageMenuItem
    implements MenuItem
{
    private @Autowired UserDao userRepository;

    @Override
    public String getPath()
    {
        return "/admin/users";
    }

    @Override
    public IconType getIcon()
    {
        return FontAwesome5IconType.users_cog_s;
    }

    @Override
    public String getLabel()
    {
        return Strings.getString("manageusers.page.menuitem.label");
    }

    @Override
    public boolean applies()
    {
        return userRepository.isAdministrator(userRepository.getCurrentUser());
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return ManageUsersPage.class;
    }

    @Override
    public KeyType[] shortcut()
    {
        return new KeyType[] { KeyType.Alt, KeyType.u };
    }
}
