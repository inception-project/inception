/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp;

import org.apache.wicket.Page;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.DynamicJQueryResourceReference;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;

import de.tudarmstadt.ukp.clarin.webanno.brat.WebAnnoResources;
import de.tudarmstadt.ukp.clarin.webanno.monitoring.page.MonitoringPage;
import de.tudarmstadt.ukp.clarin.webanno.project.page.ProjectPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.automation.AutomationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.correction.CorrectionPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.crowdsource.CrowdSourcePage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.CurationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.login.LoginPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.welcome.WelcomePage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.security.SpringAuthenticatedWebSession;
import de.tudarmstadt.ukp.clarin.webanno.webapp.security.page.ManageUsersPage;

/**
 * The wicket application class. Sets up pages, authentication, theme, and other application-wide
 * configuration.
 *
 * @author Richard Eckart de Castilho
 */
public class WicketApplication
    extends AuthenticatedWebApplication
{
    boolean isInitialized = false;

    @Override
    protected void init()
    {
        if (!isInitialized) {
            super.init();
            getComponentInstantiationListeners().add(new SpringComponentInjector(this));
            setListeners();

            // Enable dynamic switching between JQuery 1 and JQuery 2 based on the browser
            // identification. 
            getJavaScriptLibrarySettings().setJQueryReference(
                    new DynamicJQueryResourceReference());

            mountPage("/login.html", getSignInPageClass());
            mountPage("/welcome.html", getHomePage());
            mountPage("/annotation.html", AnnotationPage.class);

            mountPage("/curation.html", CurationPage.class);
            mountPage("/projectsetting.html", ProjectPage.class);
            mountPage("/monitoring.html", MonitoringPage.class);
            mountPage("/users.html", ManageUsersPage.class);
            mountPage("/crowdsource.html", CrowdSourcePage.class);

            mountPage("/correction.html", CorrectionPage.class);
            mountPage("/automation.html", AutomationPage.class);

            mountResource("/static/jquery-theme/jquery-ui-redmond.css",
                    new CssResourceReference(WebAnnoResources.class, "client/css/jquery-ui-redmond.css"));
            mountResource("/style-vis.css",
                    new CssResourceReference(WebAnnoResources.class, "client/css/style-vis.css"));
            mountResource("/style-ui.css",
                    new CssResourceReference(WebAnnoResources.class, "client/css/style-ui.css"));

/*            // mount fonts
            mountResource("/static/fonts/Astloch-Bold.ttf",
                    new CssResourceReference(Myresources.class, "fonts/Astloch-Bold.ttf"));
            mountResource("/static/fonts/Liberation_Sans-Regular.ttf",
                    new CssResourceReference(Myresources.class, "fonts/Liberation_Sans-Regular.ttf"));
            mountResource("/static/fonts/PT_Sans-Caption-Web-Regular.ttf",
                    new CssResourceReference(Myresources.class, "fonts/PT_Sans-Caption-Web-Regular.ttf"));
*/
            isInitialized = true;
        }
    }

    /**
     * @see org.apache.wicket.Application#getHomePage()
     */
    @Override
    public Class<? extends Page> getHomePage()
    {
        return WelcomePage.class;
    }

    private void setListeners()
    {
        getComponentInstantiationListeners().add(new SpringComponentInjector(this));
    }

    @Override
    public Class<? extends WebPage> getSignInPageClass()
    {
        return LoginPage.class;
    }

    @Override
    protected Class<? extends AuthenticatedWebSession> getWebSessionClass()
    {
        return SpringAuthenticatedWebSession.class;
    }
}
