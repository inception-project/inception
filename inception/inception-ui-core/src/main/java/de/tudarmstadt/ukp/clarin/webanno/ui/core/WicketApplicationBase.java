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
package de.tudarmstadt.ukp.clarin.webanno.ui.core;

import static de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil.getApplicationHome;
import static java.lang.System.currentTimeMillis;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;
import static org.apache.wicket.coep.CrossOriginEmbedderPolicyConfiguration.CoepMode.ENFORCING;
import static org.apache.wicket.coop.CrossOriginOpenerPolicyConfiguration.CoopMode.SAME_ORIGIN;
import static org.apache.wicket.settings.ExceptionSettings.SHOW_INTERNAL_ERROR_PAGE;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.wicket.Page;
import org.apache.wicket.authorization.strategies.CompoundAuthorizationStrategy;
import org.apache.wicket.authroles.authorization.strategies.role.RoleAuthorizationStrategy;
import org.apache.wicket.coep.CrossOriginEmbedderPolicyConfiguration;
import org.apache.wicket.coep.CrossOriginEmbedderPolicyRequestCycleListener;
import org.apache.wicket.devutils.stateless.StatelessChecker;
import org.apache.wicket.markup.html.IPackageResourceGuard;
import org.apache.wicket.markup.html.SecurePackageResourceGuard;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.PageRequestHandlerTracker;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.caching.NoOpResourceCachingStrategy;
import org.apache.wicket.resource.FileSystemResourceReference;
import org.apache.wicket.resource.JQueryResourceReference;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.resource.loader.NestedStringResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.giffing.wicket.spring.boot.starter.app.WicketBootSecuredWebApplication;

import de.agilecoders.wicket.core.Bootstrap;
import de.agilecoders.wicket.core.settings.IBootstrapSettings;
import de.agilecoders.wicket.webjars.WicketWebjars;
import de.tudarmstadt.ukp.clarin.webanno.security.SpringAuthenticatedWebSession;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.PatternMatchingCrossOriginEmbedderPolicyRequestCycleListener;
import de.tudarmstadt.ukp.clarin.webanno.ui.config.FontAwesomeResourceBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.config.JQueryJavascriptBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.config.JQueryUIResourceBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.config.KendoResourceBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.kendo.WicketJQueryFocusPatchBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.WebAnnoJavascriptBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.theme.CustomThemeCssResourceBehavior;
import de.tudarmstadt.ukp.inception.bootstrap.InceptionBootstrapCssReference;
import de.tudarmstadt.ukp.inception.bootstrap.InceptionBootstrapResourceReference;
import de.tudarmstadt.ukp.inception.ui.core.ErrorListener;
import de.tudarmstadt.ukp.inception.ui.core.ErrorTestPage;

/**
 * The Wicket application class. Sets up pages, authentication, theme, and other application-wide
 * configuration.
 */
public abstract class WicketApplicationBase
    extends WicketBootSecuredWebApplication
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void init()
    {
        super.init();

        CompoundAuthorizationStrategy authorizationStrategy = new CompoundAuthorizationStrategy();
        authorizationStrategy.add(new RoleAuthorizationStrategy(this));
        getSecuritySettings().setAuthorizationStrategy(authorizationStrategy);

        getCspSettings().blocking().disabled();

        // if (DEVELOPMENT == getConfigurationType()) {
        // getCspSettings().reporting().strict().reportBack();
        // getCspSettings().reporting().unsafeInline().reportBack();
        // }

        // Enforce COEP while inheriting any exemptions that might already have been set e.g. via
        // WicketApplicationInitConfiguration beans
        getSecuritySettings().setCrossOriginEmbedderPolicyConfiguration(ENFORCING,
                getSecuritySettings().getCrossOriginEmbedderPolicyConfiguration().getExemptions()
                        .stream().toArray(String[]::new));
        getSecuritySettings().setCrossOriginOpenerPolicyConfiguration(SAME_ORIGIN);

        initStatelessChecker();

        initOnce();
    }

    @Override
    protected void validateInit()
    {
        super.validateInit();

        installPatternMatchingCrossOriginEmbedderPolicyRequestCycleListener();

        installSpringSecurityContextPropagationRequestCycleListener();
    }

    private void installSpringSecurityContextPropagationRequestCycleListener()
    {
        getRequestCycleListeners().add(new IRequestCycleListener()
        {
            @Override
            public void onBeginRequest(RequestCycle aCycle)
            {
                SpringAuthenticatedWebSession.get().syncSpringSecurityAuthenticationToWicket();
            }
        });
    }

    private void installPatternMatchingCrossOriginEmbedderPolicyRequestCycleListener()
    {
        CrossOriginEmbedderPolicyConfiguration coepConfig = getSecuritySettings()
                .getCrossOriginEmbedderPolicyConfiguration();
        if (coepConfig.isEnabled()) {
            // Remove the CrossOriginEmbedderPolicyRequestCycleListener that was configured
            // by Wicket
            List<IRequestCycleListener> toRemove = new ArrayList<>();
            for (IRequestCycleListener listener : getRequestCycleListeners()) {
                if (listener instanceof CrossOriginEmbedderPolicyRequestCycleListener) {
                    toRemove.add(listener);
                }
            }
            toRemove.forEach(listener -> getRequestCycleListeners().remove(listener));

            getRequestCycleListeners().add(
                    new PatternMatchingCrossOriginEmbedderPolicyRequestCycleListener(coepConfig));
        }
    }

    protected void initOnce()
    {
        // Allow nested string resource resolving using "#(key)"
        initNestedStringResourceLoader();

        initWebFrameworks();

        // Allow fetching the current page from non-Wicket code
        initPageRequestTracker();

        initServerTimeReporting();

        initNonCachingInDevEnvironment();

        initAccessToVueComponents();

        initErrorPage();
    }

    private void initNonCachingInDevEnvironment()
    {
        if (DEVELOPMENT.equals(getConfigurationType())) {
            // Do not cache pages in development mode - allows us to make changes to the HMTL
            // without
            // having to reload the application
            getMarkupSettings().getMarkupFactory().getMarkupCache().clear();
            getResourceSettings().setCachingStrategy(NoOpResourceCachingStrategy.INSTANCE);

            // Same for resources
            getResourceSettings().setDefaultCacheDuration(Duration.ZERO);
        }
    }

    private void initPageRequestTracker()
    {
        getRequestCycleListeners().add(new PageRequestHandlerTracker());
    }

    protected void initWebFrameworks()
    {
        initJQueryResourceReference();

        addJQueryJavascriptToAllPages();

        initBootstrap();

        addKendoResourcesToAllPages();

        addJQueryUIResourcesToAllPages();

        addFontAwesomeToAllPages();

        addWebAnnoJavascriptToAllPages();

        addCustomCssToAllPages();
    }

    protected void initBootstrap()
    {
        WicketWebjars.install(this);

        Bootstrap.install(this);

        IBootstrapSettings settings = Bootstrap.getSettings(this);

        File customBootstrap = new File(getApplicationHome(), "bootstrap.css");

        if (customBootstrap.exists()) {
            log.info("Using custom bootstrap at [{}]", customBootstrap);
            settings.setCssResourceReference(new FileSystemResourceReference(
                    "inception-bootstrap.css", customBootstrap.toPath()));
        }
        else {
            settings.setCssResourceReference(InceptionBootstrapCssReference.get());
        }

        settings.setJsResourceReference(InceptionBootstrapResourceReference.get());
    }

    protected void addCustomCssToAllPages()
    {
        File customCss = new File(getApplicationHome(), "theme.css");

        if (customCss.exists()) {
            log.info("Using custom CSS at [{}]", customCss);
            getComponentInstantiationListeners().add(component -> {
                if (component instanceof Page) {
                    component.add(new CustomThemeCssResourceBehavior());
                }
            });
        }
    }

    protected void addKendoResourcesToAllPages()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof Page) {
                component.add(new KendoResourceBehavior());
                component.add(new WicketJQueryFocusPatchBehavior());
            }
        });
    }

    protected void addFontAwesomeToAllPages()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof Page) {
                component.add(new FontAwesomeResourceBehavior());
            }
        });
    }

    protected void addJQueryUIResourcesToAllPages()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof Page) {
                component.add(new JQueryUIResourceBehavior());
            }
        });
    }

    protected void addJQueryJavascriptToAllPages()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof Page) {
                component.add(new JQueryJavascriptBehavior());
            }
        });
    }

    protected void addWebAnnoJavascriptToAllPages()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof Page) {
                component.add(new WebAnnoJavascriptBehavior());
            }
        });
    }

    protected void initJQueryResourceReference()
    {
        getJavaScriptLibrarySettings().setJQueryReference(JQueryResourceReference.getV3());
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
        if (DEVELOPMENT.equals(getConfigurationType())) {
            getComponentPostOnBeforeRenderListeners().add(new StatelessChecker());
        }
    }

    protected void initServerTimeReporting()
    {
        Properties settings = SettingsUtil.getSettings();
        if (DEVELOPMENT != getConfigurationType()
                && !"true".equalsIgnoreCase(settings.getProperty("debug.sendServerSideTimings"))) {
            return;
        }

        getRequestCycleListeners().add(new IRequestCycleListener()
        {
            @Override
            public void onEndRequest(RequestCycle cycle)
            {
                final Response response = cycle.getResponse();
                if (response instanceof WebResponse) {
                    final long serverTime = currentTimeMillis() - cycle.getStartTime();
                    ((WebResponse) response).addHeader("Server-Timing",
                            "Server-Side;desc=\"Server-side total\";dur=" + serverTime);
                }
            }
        });
    }

    @Override
    protected Class<? extends ApplicationSession> getWebSessionClass()
    {
        return ApplicationSession.class;
    }

    private void initErrorPage()
    {
        // Instead of configuring the different types of errors to refer to our error page, we
        // use @WicketInternalErrorPage and friends on our ErrorPage
        getExceptionSettings().setUnexpectedExceptionDisplay(SHOW_INTERNAL_ERROR_PAGE);
        getRequestCycleListeners().add(new ErrorListener());

        // When running in development mode, we mount the exception test page
        if (DEVELOPMENT.equals(getConfigurationType())) {
            mountPage("/whoops/test", ErrorTestPage.class);
        }
    }

    private void initAccessToVueComponents()
    {
        IPackageResourceGuard resourceGuard = getResourceSettings().getPackageResourceGuard();
        if (resourceGuard instanceof SecurePackageResourceGuard) {
            SecurePackageResourceGuard securePackageResourceGuard = (SecurePackageResourceGuard) resourceGuard;
            securePackageResourceGuard.addPattern("+*.vue");
        }
    }

    @Override
    public String getMimeType(String aFileName)
    {
        if (aFileName.endsWith(".vue")) {
            return "text/javascript";
        }

        return super.getMimeType(aFileName);
    }
}
