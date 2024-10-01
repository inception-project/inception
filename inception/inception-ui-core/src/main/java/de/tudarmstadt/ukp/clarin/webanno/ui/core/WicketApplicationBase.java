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

import static de.tudarmstadt.ukp.inception.support.SettingsUtil.getApplicationHome;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;
import static org.apache.wicket.coep.CrossOriginEmbedderPolicyConfiguration.CoepMode.ENFORCING;
import static org.apache.wicket.coop.CrossOriginOpenerPolicyConfiguration.CoopMode.SAME_ORIGIN;
import static org.apache.wicket.csp.CSPDirective.BASE_URI;
import static org.apache.wicket.csp.CSPDirective.CHILD_SRC;
import static org.apache.wicket.csp.CSPDirective.CONNECT_SRC;
import static org.apache.wicket.csp.CSPDirective.DEFAULT_SRC;
import static org.apache.wicket.csp.CSPDirective.FONT_SRC;
import static org.apache.wicket.csp.CSPDirective.FORM_ACTION;
import static org.apache.wicket.csp.CSPDirective.FRAME_SRC;
import static org.apache.wicket.csp.CSPDirective.IMG_SRC;
import static org.apache.wicket.csp.CSPDirective.MANIFEST_SRC;
import static org.apache.wicket.csp.CSPDirective.MEDIA_SRC;
import static org.apache.wicket.csp.CSPDirective.SCRIPT_SRC;
import static org.apache.wicket.csp.CSPDirective.STYLE_SRC;
import static org.apache.wicket.csp.CSPDirectiveSrcValue.NONCE;
import static org.apache.wicket.csp.CSPDirectiveSrcValue.NONE;
import static org.apache.wicket.csp.CSPDirectiveSrcValue.SELF;
import static org.apache.wicket.csp.CSPDirectiveSrcValue.STRICT_DYNAMIC;
import static org.apache.wicket.csp.CSPDirectiveSrcValue.UNSAFE_EVAL;
import static org.apache.wicket.csp.CSPDirectiveSrcValue.UNSAFE_INLINE;
import static org.apache.wicket.settings.ExceptionSettings.SHOW_INTERNAL_ERROR_PAGE;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.wicket.authorization.strategies.CompoundAuthorizationStrategy;
import org.apache.wicket.authroles.authorization.strategies.role.RoleAuthorizationStrategy;
import org.apache.wicket.coep.CrossOriginEmbedderPolicyConfiguration;
import org.apache.wicket.coep.CrossOriginEmbedderPolicyRequestCycleListener;
import org.apache.wicket.csp.CSPDirective;
import org.apache.wicket.csp.CSPRenderable;
import org.apache.wicket.csp.FixedCSPValue;
import org.apache.wicket.devutils.stateless.StatelessChecker;
import org.apache.wicket.protocol.http.ResourceIsolationRequestCycleListener;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.wicketstuff.kendo.ui.form.TextField;
import org.wicketstuff.kendo.ui.form.autocomplete.AutoCompleteTextField;
import org.wicketstuff.kendo.ui.form.combobox.ComboBox;
import org.wicketstuff.kendo.ui.form.multiselect.MultiSelect;

import com.giffing.wicket.spring.boot.starter.app.WicketBootSecuredWebApplication;

import de.agilecoders.wicket.core.Bootstrap;
import de.agilecoders.wicket.core.settings.IBootstrapSettings;
import de.tudarmstadt.ukp.clarin.webanno.security.SpringAuthenticatedWebSession;
import de.tudarmstadt.ukp.clarin.webanno.ui.config.FontAwesomeResourceBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.WebAnnoJavascriptBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.theme.CustomThemeCssResourceBehavior;
import de.tudarmstadt.ukp.inception.bootstrap.InceptionBootstrapCssReference;
import de.tudarmstadt.ukp.inception.bootstrap.InceptionBootstrapResourceReference;
import de.tudarmstadt.ukp.inception.security.config.CspProperties;
import de.tudarmstadt.ukp.inception.support.SettingsUtil;
import de.tudarmstadt.ukp.inception.support.jquery.JQueryJavascriptBehavior;
import de.tudarmstadt.ukp.inception.support.jquery.JQueryUIResourceBehavior;
import de.tudarmstadt.ukp.inception.support.kendo.KendoFixDisabledInputComponentStylingBehavior;
import de.tudarmstadt.ukp.inception.support.kendo.KendoResourceBehavior;
import de.tudarmstadt.ukp.inception.support.kendo.WicketJQueryFocusPatchBehavior;
import de.tudarmstadt.ukp.inception.support.wicket.PatternMatchingCrossOriginEmbedderPolicyRequestCycleListener;
import de.tudarmstadt.ukp.inception.support.wicket.WicketUtil;
import de.tudarmstadt.ukp.inception.support.wicket.resource.ContextSensitivePackageStringResourceLoader;
import de.tudarmstadt.ukp.inception.ui.core.ErrorListener;
import de.tudarmstadt.ukp.inception.ui.core.ErrorTestPage;

/**
 * The Wicket application class. Sets up pages, authentication, theme, and other application-wide
 * configuration.
 */
public abstract class WicketApplicationBase
    extends WicketBootSecuredWebApplication
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired CspProperties cspProperties;

    @Override
    protected void init()
    {
        super.init();

        var authorizationStrategy = new CompoundAuthorizationStrategy();
        authorizationStrategy.add(new RoleAuthorizationStrategy(this));
        getSecuritySettings().setAuthorizationStrategy(authorizationStrategy);

        var imgSrcValue = new ArrayList<>(asList(SELF, new FixedCSPValue("data:")));
        cspProperties.getAllowedImageSources().stream() //
                .map(FixedCSPValue::new) //
                .forEachOrdered(imgSrcValue::add);

        var mediaSrcValue = new ArrayList<CSPRenderable>(asList(SELF));
        cspProperties.getAllowedMediaSources().stream() //
                .map(FixedCSPValue::new) //
                .forEachOrdered(mediaSrcValue::add);

        var frameAncestorsValue = new ArrayList<CSPRenderable>(asList(SELF));
        cspProperties.getAllowedFrameAncestors().stream() //
                .map(FixedCSPValue::new) //
                .forEachOrdered(frameAncestorsValue::add);

        getCspSettings().blocking().clear() //
                .add(DEFAULT_SRC, NONE) //
                .add(SCRIPT_SRC, NONCE, STRICT_DYNAMIC, UNSAFE_EVAL) //
                // .add(STYLE_SRC, NONCE) //
                .add(STYLE_SRC, SELF, UNSAFE_INLINE) //
                .add(IMG_SRC, imgSrcValue.toArray(CSPRenderable[]::new)) //
                .add(MEDIA_SRC, mediaSrcValue.toArray(CSPRenderable[]::new)) //
                .add(CONNECT_SRC, SELF) //
                .add(FONT_SRC, SELF) //
                .add(MANIFEST_SRC, SELF) //
                .add(CHILD_SRC, SELF) //
                .add(FRAME_SRC, SELF) //
                .add(FORM_ACTION, SELF) //
                .add(CSPDirective.FRAME_ANCESTORS,
                        frameAncestorsValue.toArray(CSPRenderable[]::new))
                .add(BASE_URI, SELF); //

        // CSRF
        getRequestCycleListeners().add(new ResourceIsolationRequestCycleListener());

        installSpringSecurityContextPropagationRequestCycleListener();

        installTimingListener();

        // Enforce COEP while inheriting any exemptions that might already have been set e.g. via
        // WicketApplicationInitConfiguration beans
        getSecuritySettings().setCrossOriginEmbedderPolicyConfiguration(ENFORCING,
                getSecuritySettings().getCrossOriginEmbedderPolicyConfiguration().getExemptions()
                        .stream().toArray(String[]::new));
        getSecuritySettings().setCrossOriginOpenerPolicyConfiguration(SAME_ORIGIN);

        initStatelessChecker();

        initOnce();
    }

    private void installTimingListener()
    {
        var settings = SettingsUtil.getSettings();
        if (!"true".equalsIgnoreCase(settings.getProperty("debug.sendServerSideTimings"))) {
            return;
        }

        WicketUtil.installTimingListeners(this);

    }

    @Override
    protected void validateInit()
    {
        super.validateInit();

        // COEP - Do this late so we can override the default set by Wicket
        installPatternMatchingCrossOriginEmbedderPolicyRequestCycleListener();
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
        initContextSensitivePackageStringResourceLoader();

        // Allow nested string resource resolving using "#(key)"
        initNestedStringResourceLoader();

        initWebFrameworks();

        // Allow fetching the current page from non-Wicket code
        initPageRequestTracker();

        initServerTimeReporting();

        initNonCachingInDevEnvironment();

        initErrorPage();
    }

    private void initNonCachingInDevEnvironment()
    {
        if (DEVELOPMENT.equals(getConfigurationType())) {
            // Do not cache pages in development mode - allows us to make changes to the HMTL
            // without having to reload the application
            getMarkupSettings().getMarkupFactory().getMarkupCache().clear();
            getResourceSettings().setCachingStrategy(NoOpResourceCachingStrategy.INSTANCE);

            // Same for resources
            getResourceSettings().setDefaultCacheDuration(Duration.ZERO);

            // Same for properties
            getResourceSettings().getLocalizer().setEnableCache(false);
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

        addKendoComponentsDisabledLookFix();

        addJQueryUIResourcesToAllPages();

        addFontAwesomeToAllPages();

        addWebAnnoJavascriptToAllPages();

        addCustomCssToAllPages();
    }

    protected void initBootstrap()
    {
        Bootstrap.install(this);

        IBootstrapSettings settings = Bootstrap.getSettings(this);

        File customBootstrap = new File(getApplicationHome(), "bootstrap.css");

        if (customBootstrap.exists()) {
            LOG.info("Using custom bootstrap at [{}]", customBootstrap);
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
            LOG.info("Using custom CSS at [{}]", customCss);
            getComponentInstantiationListeners().add(component -> {
                if (component instanceof ApplicationPageBase) {
                    component.add(new CustomThemeCssResourceBehavior());
                }
            });
        }
    }

    protected void addKendoResourcesToAllPages()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof ApplicationPageBase) {
                component.add(new KendoResourceBehavior());
                component.add(new WicketJQueryFocusPatchBehavior());
            }
        });
    }

    protected void addKendoComponentsDisabledLookFix()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof ComboBox || component instanceof AutoCompleteTextField
                    || component instanceof TextField || component instanceof MultiSelect
                    || component instanceof org.wicketstuff.kendo.ui.form.multiselect.lazy.MultiSelect) {
                component.add(new KendoFixDisabledInputComponentStylingBehavior());
            }
        });
    }

    protected void addFontAwesomeToAllPages()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof ApplicationPageBase) {
                component.add(new FontAwesomeResourceBehavior());
            }
        });
    }

    protected void addJQueryUIResourcesToAllPages()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof ApplicationPageBase) {
                component.add(new JQueryUIResourceBehavior());
            }
        });
    }

    protected void addJQueryJavascriptToAllPages()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof ApplicationPageBase) {
                component.add(new JQueryJavascriptBehavior());
            }
        });
    }

    protected void addWebAnnoJavascriptToAllPages()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof ApplicationPageBase) {
                component.add(new WebAnnoJavascriptBehavior());
            }
        });
    }

    protected void initJQueryResourceReference()
    {
        getJavaScriptLibrarySettings().setJQueryReference(JQueryResourceReference.getV3());
    }

    protected void initContextSensitivePackageStringResourceLoader()
    {
        getResourceSettings().getStringResourceLoaders()
                .add(new ContextSensitivePackageStringResourceLoader());
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
        if (!"true".equalsIgnoreCase(settings.getProperty("debug.sendServerSideTimings"))) {
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
}
