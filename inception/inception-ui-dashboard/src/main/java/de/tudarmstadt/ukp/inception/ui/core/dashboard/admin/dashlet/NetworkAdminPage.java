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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.dashlet;

import static java.lang.String.format;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.AdminSettingsDashboardPageBase;

@MountPath("/admin/network")
public class NetworkAdminPage
    extends AdminSettingsDashboardPageBase
{
    private static final long serialVersionUID = -3184701055919509115L;

    private @SpringBean UserDao userService;

    public NetworkAdminPage(final PageParameters aParameters)
    {
        super(aParameters);

        if (!userService.isCurrentUserAdmin()) {
            denyAccess();
        }

        add(new NetworkInfoPanel("networkInfoPanel"));
    }

    private void denyAccess()
    {
        getSession().error(format("Access to [%s] denied.", getClass().getSimpleName()));
        throw new RestartResponseException(getApplication().getHomePage());
    }
}
