/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.webapp;

import java.io.File;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.resource.DynamicJQueryResourceReference;
import org.apache.wicket.settings.ExceptionSettings;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.wicketstuff.annotation.scan.AnnotatedMountScanner;

import de.tudarmstadt.ukp.clarin.webanno.brat.WebAnnoResources;
import de.tudarmstadt.ukp.clarin.webanno.support.FileSystemResource;
import de.tudarmstadt.ukp.clarin.webanno.webapp.home.page.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.login.LoginPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.welcome.WelcomePage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.security.SpringAuthenticatedWebSession;

/**
 * The wicket application class. Sets up pages, authentication, theme, and other application-wide
 * configuration.
 *
 */
public class WicketApplication
    extends AuthenticatedWebApplication
{
    boolean isInitialized = false;

    @Override
    protected void init()
    {
        super.init();
        getComponentInstantiationListeners().add(new SpringComponentInjector(this));
        
        if (!isInitialized) {
            // Enable dynamic switching between JQuery 1 and JQuery 2 based on the browser
            // identification. 
            getJavaScriptLibrarySettings().setJQueryReference(
                    new DynamicJQueryResourceReference());

            mountPage("/login.html", getSignInPageClass());
            mountPage("/welcome.html", getHomePage());

            // Mount the other pages via @MountPath annotation on the page classes
            new AnnotatedMountScanner().scanPackage("de.tudarmstadt.ukp.clarin.webanno").mount(this);

            // FIXME Handling brat font/css resources should be moved to brat module
            mountResource("/style-vis.css",
                    new CssResourceReference(WebAnnoResources.class, "client/css/style-vis.css"));
            mountResource("/style-ui.css",
                    new CssResourceReference(WebAnnoResources.class, "client/css/style-ui.css"));

            Properties settings = SettingsUtil.getSettings();
            String logoValue = settings.getProperty("style.logo");
            if (StringUtils.isNotBlank(logoValue) && new File(logoValue).canRead()) {
                getSharedResources().add("logo", new FileSystemResource(new File(logoValue)));
                mountResource("/images/logo.png", new SharedResourceReference("logo"));
            }
            else {
                mountResource("/images/logo.png", new ContextRelativeResourceReference(
                        "images/logo.png", false));
            }
            
            // Display stack trace instead of internal error
            if ("true".equalsIgnoreCase(settings.getProperty("debug.showExceptionPage"))) {
                getExceptionSettings().setUnexpectedExceptionDisplay(
                        ExceptionSettings.SHOW_EXCEPTION_PAGE);
            }

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
