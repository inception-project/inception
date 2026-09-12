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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.closesession;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.security.config.LoginProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.config.PreauthenticationProperties;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.logout.LogoutPanel;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class CloseSessionSidebarFooterItem
    extends Panel
{
    private static final long serialVersionUID = -4025477482483567938L;

    private @SpringBean PreauthenticationProperties preauthenticationProperties;
    private @SpringBean LoginProperties securityProperties;

    public CloseSessionSidebarFooterItem(String aId)
    {
        super(aId);

        add(new LambdaAjaxLink("logoutButton", this::actionLogout));
    }

    private void actionLogout(AjaxRequestTarget aTarget)
    {
        LogoutPanel.actionLogout(this, preauthenticationProperties, securityProperties);
    }
}
