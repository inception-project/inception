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
package de.tudarmstadt.ukp.clarin.webanno.ui.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.core.request.mapper.HomePageMapper;
import org.apache.wicket.devutils.stateless.StatelessChecker;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.IRequestMapper;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.resource.DynamicJQueryResourceReference;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.resource.loader.NestedStringResourceLoader;
import org.apache.wicket.settings.ExceptionSettings;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.wicketstuff.annotation.scan.AnnotatedMountList;
import org.wicketstuff.annotation.scan.AnnotatedMountScanner;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.support.FileSystemResource;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.login.LoginPage;

/**
 * The Wicket application class. Sets up pages, authentication, theme, and other application-wide
 * configuration.
 */
public abstract class WicketApplicationBase
    extends AuthenticatedWebApplication
{
    protected boolean isInitialized = false;

    @Override
    protected void init()
    {
        super.init();
        
        initSpring();
        
        initStatelessChecker();
        
        if (!isInitialized) {
            initOnce();
            
            isInitialized = true;
        }
    }

    protected void initOnce()
    {
        // Allow nested string resource resolving using "#(key)"
        initNestedStringResourceLoader();
        
//        // This should avoid some application-reloading while working on I18N
//        getResourceSettings().setThrowExceptionOnMissingResource(false);
//        getResourceSettings().setCachingStrategy(new NoOpResourceCachingStrategy());
        
        // Enable dynamic switching between JQuery 1 and JQuery 2 based on the browser
        // identification. 
        initDynamicJQueryResourceReference();

        initDefaultPageMounts();
        
        initLogoReference();

        // Display stack trace instead of internal error
        initShowExceptionPage();

        initMDCLifecycle();
    }

    protected void initMDCLifecycle()
    {
        getRequestCycleListeners().add(new AbstractRequestCycleListener()
        {
            @Override
            public void onBeginRequest(RequestCycle cycle)
            {
                ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
                DocumentService repo = ctx.getBean(DocumentService.class);
                MDC.put(Logging.KEY_REPOSITORY_PATH, repo.getDir().getAbsolutePath());
            }

            @Override
            public void onEndRequest(RequestCycle cycle)
            {
                MDC.remove(Logging.KEY_REPOSITORY_PATH);
            }
        });
    }

    protected void initShowExceptionPage()
    {
        Properties settings = SettingsUtil.getSettings();
        if ("true".equalsIgnoreCase(settings.getProperty("debug.showExceptionPage"))) {
            getExceptionSettings().setUnexpectedExceptionDisplay(
                    ExceptionSettings.SHOW_EXCEPTION_PAGE);
        }
    }

    protected void initLogoReference()
    {
        Properties settings = SettingsUtil.getSettings();
        String logoValue = settings.getProperty(SettingsUtil.CFG_STYLE_LOGO);
        if (StringUtils.isNotBlank(logoValue) && new File(logoValue).canRead()) {
            getSharedResources().add("logo", new FileSystemResource(new File(logoValue)));
            mountResource("/assets/logo.png", new SharedResourceReference("logo"));
        }
        else {
            mountResource("/assets/logo.png", new PackageResourceReference(getLogoLocation()));
        }
    }
    
    protected String getLogoLocation()
    {
        return "/de/tudarmstadt/ukp/clarin/webanno/ui/core/logo/logo.png";
    }

    protected void initDefaultPageMounts()
    {
        mountPage("/login.html", getSignInPageClass());
        mountPage("/welcome.html", getHomePage());
        
        // Mount the other pages via @MountPath annotation on the page classes
        AnnotatedMountList mounts = new AnnotatedMountScanner().scanPackage("de.tudarmstadt.ukp");
        for (IRequestMapper mapper : mounts) {
            if (mapper instanceof HomePageMapper) {
                System.out.println(mapper);
            }
        }
        mounts.mount(this);
    }

    protected void initDynamicJQueryResourceReference()
    {
        getJavaScriptLibrarySettings().setJQueryReference(new DynamicJQueryResourceReference());
    }

    protected void initNestedStringResourceLoader()
    {
        List<IStringResourceLoader> loaders = new ArrayList<>(
                getResourceSettings().getStringResourceLoaders());
        NestedStringResourceLoader nestedLoader = new NestedStringResourceLoader(loaders,
                Pattern.compile("#\\(([^ ]*?)\\)")) {
            
            // Workaround for https://issues.apache.org/jira/browse/WICKET-6393
            // Can be remove with Wicket 7.8.0 or 8.0.0-M7
            @Override
            public String loadStringResource(Class<?> aClazz, String aKey, Locale aLocale,
                    String aStyle, String aVariation)
            {
                try {
                    return super.loadStringResource(aClazz, aKey, aLocale, aStyle, aVariation);
                }
                catch (MissingResourceException e) {
                    return null;
                }
            }
            
            // Workaround for https://issues.apache.org/jira/browse/WICKET-6393
            // Can be remove with Wicket 7.8.0 or 8.0.0-M7
            @Override
            public String loadStringResource(Component aComponent, String aKey,
                    Locale aLocale, String aStyle, String aVariation)
            {
                try {
                    return super.loadStringResource(aComponent, aKey, aLocale, aStyle, aVariation);
                }
                catch (MissingResourceException e) {
                    return null;
                }
            }
        };
        getResourceSettings().getStringResourceLoaders().clear();
        getResourceSettings().getStringResourceLoaders().add(nestedLoader);
    }

    protected void initStatelessChecker()
    {
        if (RuntimeConfigurationType.DEVELOPMENT.equals(getConfigurationType())) {
            getComponentPostOnBeforeRenderListeners().add(new StatelessChecker());
        }
    }

    protected void initSpring()
    {
        getComponentInstantiationListeners().add(new SpringComponentInjector(this));
    }
    
    @Override
    public Class<? extends WebPage> getSignInPageClass()
    {
        return LoginPage.class;
    }

    @Override
    protected Class<? extends ApplicationSession> getWebSessionClass()
    {
        return ApplicationSession.class;
    }
}
