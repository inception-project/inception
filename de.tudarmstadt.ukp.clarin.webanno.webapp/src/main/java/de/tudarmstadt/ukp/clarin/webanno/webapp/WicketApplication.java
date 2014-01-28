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
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.odlabs.wiquery.core.WiQuerySettings;
import org.odlabs.wiquery.ui.themes.IThemableApplication;
import org.odlabs.wiquery.ui.themes.WiQueryCoreThemeResourceReference;

import de.tudarmstadt.ukp.clarin.webanno.brat.WebAnnoResources;
import de.tudarmstadt.ukp.clarin.webanno.project.page.ProjectPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.automation.AutomationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.correction.CorrectionPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.crowdsource.CrowdSourcePage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.CurationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.login.LoginPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.monitoring.MonitoringPage;
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
    implements IThemableApplication
{
    boolean isInitialized = false;

    private ResourceReference theme;

    public WicketApplication()
    {
        theme = new WiQueryCoreThemeResourceReference("redlion");
    }

    @Override
    protected void validateInit()
    {
        super.validateInit();
        final WiQuerySettings wqs = WiQuerySettings.get();
        wqs.setAutoImportJQueryResource(false);
    }

    @Override
    public void init()
    {
        if (!isInitialized) {
            super.init();
            getComponentInstantiationListeners().add(new SpringComponentInjector(this));
            setListeners();

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

            mountResource("/client/lib/head.load.min.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/lib/head.load.min.js"));
            mountResource("/client/lib/jquery.min.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/lib/jquery.min.js"));
            mountResource("/client/lib/jquery-ui.min.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/lib/jquery-ui.min.js"));
            mountResource("/client/lib/jquery.svg.min.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/lib/jquery.svg.min.js"));
            mountResource("/client/lib/jquery.svgdom.min.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/lib/jquery.svgdom.min.js"));
            mountResource("/client/lib/jquery.ba-bbq.min.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/lib/jquery.ba-bbq.min.js"));
            mountResource("/client/lib/jquery.sprintf.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/lib/jquery.sprintf.js"));
            mountResource("/client/lib/jquery.json.min.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/lib/jquery.json.min.js"));
            mountResource("/client/lib/webfont.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/lib/webfont.js"));

            mountResource("/client/src/configuration.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/src/configuration.js"));
            mountResource("/client/src/util.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/src/util.js"));
            mountResource("/client/src/annotation_log.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/src/annotation_log.js"));

            mountResource("/client/src/dispatcher.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/src/dispatcher.js"));
            mountResource("/client/src/url_monitor.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/src/url_monitor.js"));
            mountResource("/client/src/ajax.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/src/ajax.js"));
            mountResource("/client/src/visualizer.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/src/visualizer.js"));
            mountResource("/client/src/visualizer_ui.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/src/visualizer_ui.js"));
            mountResource("/client/src/annotator_ui.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/src/annotator_ui.js"));
            mountResource("/client/src/spinner.js",
                    new JavaScriptResourceReference(WebAnnoResources.class, "client/src/spinner.js"));

            mountResource("/client/src/curation_mod.js",
            		new JavaScriptResourceReference(WebAnnoResources.class, "client/src/curation_mod.js"));

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

    public void setTheme(ResourceReference theme)
    {
        this.theme = theme;
    }

    @Override
    public ResourceReference getTheme(Session session)
    {
        return theme;
    }
}
