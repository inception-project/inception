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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.devutils.stateless.StatelessChecker;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.resource.DynamicJQueryResourceReference;
import org.apache.wicket.settings.ExceptionSettings;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.wicketstuff.annotation.scan.AnnotatedMountScanner;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.support.spring.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.SpringAuthenticatedWebSession;
import de.tudarmstadt.ukp.clarin.webanno.support.FileSystemResource;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.login.LoginPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.page.MainMenuPage;

/**
 * The Wicket application class. Sets up pages, authentication, theme, and other application-wide
 * configuration.
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
        if (RuntimeConfigurationType.DEVELOPMENT.equals(getConfigurationType())) {
            getComponentPostOnBeforeRenderListeners().add(new StatelessChecker());
        }
        
        if (!isInitialized) {
            // Enable dynamic switching between JQuery 1 and JQuery 2 based on the browser
            // identification. 
            getJavaScriptLibrarySettings().setJQueryReference(
                    new DynamicJQueryResourceReference());

            mountPage("/login.html", getSignInPageClass());
            mountPage("/welcome.html", getHomePage());

            // Mount the other pages via @MountPath annotation on the page classes
            new AnnotatedMountScanner().scanPackage("de.tudarmstadt.ukp").mount(this);

            Properties settings = SettingsUtil.getSettings();
            String logoValue = settings.getProperty(SettingsUtil.CFG_STYLE_LOGO);
            if (StringUtils.isNotBlank(logoValue) && new File(logoValue).canRead()) {
                getSharedResources().add("logo", new FileSystemResource(new File(logoValue)));
                mountResource("/assets/logo.png", new SharedResourceReference("logo"));
            }
            else {
                mountResource("/assets/logo.png", new PackageResourceReference(
                        "/de/tudarmstadt/ukp/clarin/webanno/ui/core/logo/logo.png"));
            }
            
            // Display stack trace instead of internal error
            if ("true".equalsIgnoreCase(settings.getProperty("debug.showExceptionPage"))) {
                getExceptionSettings().setUnexpectedExceptionDisplay(
                        ExceptionSettings.SHOW_EXCEPTION_PAGE);
            }

            getRequestCycleListeners().add(new AbstractRequestCycleListener()
            {
                @Override
                public void onBeginRequest(RequestCycle cycle)
                {
                    ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
                    DocumentService repo = ctx.getBean(DocumentService.class);
                    MDC.put(Logging.KEY_REPOSITORY_PATH, repo.getDir().getAbsolutePath());
                };

                @Override
                public void onEndRequest(RequestCycle cycle)
                {
                    MDC.remove(Logging.KEY_REPOSITORY_PATH);
                };
            });
            
            isInitialized = true;
        }
    }

    /**
     * @see org.apache.wicket.Application#getHomePage()
     */
    @Override
    public Class<? extends Page> getHomePage()
    {
        return MainMenuPage.class;
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
