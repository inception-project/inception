/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.login;

import de.tudarmstadt.ukp.clarin.webanno.webapp.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.security.LoginForm;
/**
 * A wicket page for the {@link LoginForm}
 * @author Richard Eckart de Castilho
 * @author Seid Muhie Yimam
 *
 */
public class LoginPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 4556072152990919319L;

    public LoginPage()
    {
        add(new LoginForm("loginForm"));
    }
}
