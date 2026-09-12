/*
 * Licensed to the Technische UniversitÃ¤t Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische UniversitÃ¤t Darmstadt
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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.actionbar;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.security.config.LoginProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.config.PreauthenticationProperties;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.logout.LogoutPanel;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

/**
 * @deprecated Only used by the deprecated {@link CloseSessionActionBarExtension}.
 */
@Deprecated
public class CloseSessionPanel
    extends Panel
{
    private static final long serialVersionUID = -9213541738534665790L;

    private @SpringBean PreauthenticationProperties preauthenticationProperties;
    private @SpringBean LoginProperties securityProperties;

    public CloseSessionPanel(String aId)
    {
        super(aId);
        queue(new LambdaAjaxLink("logoutButton", this::actionLogout));
    }

    private void actionLogout(AjaxRequestTarget aTarget)
    {
        LogoutPanel.actionLogout(this, preauthenticationProperties, securityProperties);
    }
}
