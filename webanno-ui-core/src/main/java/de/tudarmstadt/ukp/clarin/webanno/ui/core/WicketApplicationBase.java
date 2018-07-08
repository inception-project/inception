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
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.authorization.strategies.CompoundAuthorizationStrategy;
import org.apache.wicket.authorization.strategies.page.SimplePageAuthorizationStrategy;
import org.apache.wicket.authroles.authorization.strategies.role.RoleAuthorizationStrategy;
import org.apache.wicket.core.request.mapper.HomePageMapper;
import org.apache.wicket.devutils.stateless.StatelessChecker;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.IRequestMapper;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.PageRequestHandlerTracker;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.resource.DynamicJQueryResourceReference;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.resource.loader.NestedStringResourceLoader;
import org.apache.wicket.settings.ExceptionSettings;
import org.slf4j.MDC;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.wicketstuff.annotation.scan.AnnotatedMountList;
import org.wicketstuff.annotation.scan.AnnotatedMountScanner;

import com.giffing.wicket.spring.boot.starter.app.WicketBootSecuredWebApplication;
import com.github.sommeri.less4j.LessCompiler.Configuration;
import com.googlecode.wicket.jquery.ui.settings.JQueryUILibrarySettings;

import de.agilecoders.wicket.core.Bootstrap;
import de.agilecoders.wicket.core.settings.IBootstrapSettings;
import de.agilecoders.wicket.less.BootstrapLess;
import de.agilecoders.wicket.less.LessCompilerConfigurationFactory;
import de.agilecoders.wicket.webjars.WicketWebjars;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.support.FileSystemResource;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.clarin.webanno.ui.config.BaseLayoutCssResourceBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.config.BootstrapAwareJQueryUIJavaScriptResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.ui.config.CssBrowserSelectorResourceBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.config.FontAwesomeResourceBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.config.JQueryUIResourceBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.config.KendoResourceBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.css.theme.CustomBootstrapLessReference;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.kendo.WicketJQueryFocusPatchBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.login.LoginPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.MenuBar;

/**
 * The Wicket application class. Sets up pages, authentication, theme, and other application-wide
 * configuration.
 */
public abstract class WicketApplicationBase
    extends WicketBootSecuredWebApplication
{
    protected boolean isInitialized = false;

    @Autowired
    private MenuItemRegistry menuItemRegistry;
    
    @Override
    protected void init()
    {
        super.init();

        CompoundAuthorizationStrategy authorizationStrategy = new CompoundAuthorizationStrategy();
        // Custom authorization strategy which prevents access to pages if the corresponding 
        // menu item does not permit it.
        authorizationStrategy.add(
                new SimplePageAuthorizationStrategy(Page.class, getHomePage())
                {
                    @Override
                    protected <T extends Page> boolean isPageAuthorized(Class<T> aPageClass)
                    {
                        return menuItemRegistry.getMenuItem(aPageClass).map(MenuItem::applies)
                                .orElse(true);
                    }

                    @Override
                    protected boolean isAuthorized()
                    {
                        throw new IllegalStateException("This should not be called");
                    }
                });
        authorizationStrategy.add(new RoleAuthorizationStrategy(this));
        getSecuritySettings().setAuthorizationStrategy(authorizationStrategy);        
        
//        initSpring();
        
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
        
        initWebFrameworks();
        
        initDefaultPageMounts();
        
        initLogoReference();

        // Display stack trace instead of internal error
        initShowExceptionPage();

        initMDCLifecycle();
        
        // Allow fetching the current page from non-Wicket code
        initPageRequestTracker();
    }
    
    private void initPageRequestTracker()
    {
        getRequestCycleListeners().add(new PageRequestHandlerTracker());
    }

    protected void initWebFrameworks()
    {
        // Enable dynamic switching between JQuery 1 and JQuery 2 based on the browser
        // identification. 
        initDynamicJQueryResourceReference();
 
        initBootstrap();
        
        initKendo();
        
        initJQueryUI();
        
        initFontAwesome();
        
        initCssBrowserSelector();
        
        // Loading base layout CSS here so it can override JQuery/Kendo CSS
        initBaseLayoutCss();
    }
    
    protected void initBootstrap()
    {
        LessCompilerConfigurationFactory lessConfigFactory = () -> {
            Configuration lessConfig = new Configuration();
            lessConfig.setCompressing(
                    RuntimeConfigurationType.DEPLOYMENT.equals(getConfigurationType()));
            return lessConfig;
        };
        
        WicketWebjars.install(this);
        BootstrapLess.install(this, lessConfigFactory);
        Bootstrap.install(this);
        
        IBootstrapSettings settings = Bootstrap.getSettings(this);
        settings.setCssResourceReference(CustomBootstrapLessReference.get());
    }

    protected void initBaseLayoutCss()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof Page) {
                component.add(BaseLayoutCssResourceBehavior.get());
            }
        });
    }
    protected void initKendo()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof Page) {
                component.add(new KendoResourceBehavior());
                component.add(new WicketJQueryFocusPatchBehavior());
            }
        });
    }

    protected void initFontAwesome()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof Page) {
                component.add(new FontAwesomeResourceBehavior());
            }
        });
    }
    
    protected void initCssBrowserSelector()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof Page) {
                component.add(new CssBrowserSelectorResourceBehavior());
            }
        });
    }
    
    protected void initJQueryUI()
    {
        JQueryUILibrarySettings jqueryUiCfg = JQueryUILibrarySettings.get();
        // Here we ensure that bootstrap is loaded before JQuery UI such that the
        // JQuery UI tooltip that we use e.g. on the annotation page takes precedence over
        // the less powerful Bootstrap tooltip (both are JQuery plugins using the same name!)
        jqueryUiCfg.setJavaScriptReference(BootstrapAwareJQueryUIJavaScriptResourceReference.get());
        
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof Page) {
                component.add(new JQueryUIResourceBehavior());
            }
        });
    }
    
    protected void initMDCLifecycle()
    {
        getRequestCycleListeners().add(new AbstractRequestCycleListener()
        {
            @Override
            public void onBeginRequest(RequestCycle cycle)
            {
                ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
                try {
                    DocumentService repo = ctx.getBean(DocumentService.class);
                    MDC.put(Logging.KEY_REPOSITORY_PATH, repo.getDir().getAbsolutePath());
                }
                catch (NoSuchBeanDefinitionException e) {
                    // Well, if the document service is not there, then we don't need to
                    // configure the logging placeholder
                }
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
                Pattern.compile("#\\(([^ ]*?)\\)"));
        getResourceSettings().getStringResourceLoaders().clear();
        getResourceSettings().getStringResourceLoaders().add(nestedLoader);
    }

    protected void initStatelessChecker()
    {
        if (RuntimeConfigurationType.DEVELOPMENT.equals(getConfigurationType())) {
            getComponentPostOnBeforeRenderListeners().add(new StatelessChecker());
        }
    }

//    protected void initSpring()
//    {
//        getComponentInstantiationListeners().add(new SpringComponentInjector(this));
//    }
    
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
    
    public Class<? extends Component> getMenubarClass()
    {
        return MenuBar.class;
    }
}
