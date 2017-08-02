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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.page;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.Session;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.caching.NoOpResourceCachingStrategy;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.db.DatabaseDriverService;

public abstract class ApplicationPageBase
    extends WebPage
{
    private final static Logger LOG = LoggerFactory.getLogger(ApplicationPageBase.class);

    private static final long serialVersionUID = -1690130604031181803L;

    public static final MetaDataKey<Class<? extends Component>> MENUBAR_CLASS = 
            new MetaDataKey<Class<? extends Component>>()
    {
        private static final long serialVersionUID = 1L;
    };
    
    private FeedbackPanel feedbackPanel;
    private Label versionLabel;
    private Label embeddedDbWarning;
    private Label browserWarning;

    private @SpringBean DatabaseDriverService dbDriverService;

    protected ApplicationPageBase()
    {
        commonInit();
    }

    protected ApplicationPageBase(final PageParameters parameters)
    {
        super(parameters);
        commonInit();
    }

    private void commonInit()
    {
        Properties settings = SettingsUtil.getSettings();
        
        // Override locale to be used by application
        String locale = settings.getProperty(SettingsUtil.CFG_LOCALE, "en");
        switch (locale) {
        case "auto":
            // Do nothing - locale is picked up from browser
            break;
        default:
            // Override the locale in the session
            getSession().setLocale(Locale.forLanguageTag(locale));
            break;
        }
        
        // Add menubar
        try {
            Class<? extends Component> menubarClass = getApplication().getMetaData(MENUBAR_CLASS);
            if (menubarClass == null) {
                menubarClass = MenuBar.class;
            }
            add(ConstructorUtils.invokeConstructor(menubarClass, "menubar"));
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                | InstantiationException e1) {
            throw new RuntimeException(e1);
        }

        feedbackPanel = new BootstrapFeedbackPanel("feedbackPanel");
        feedbackPanel.setOutputMarkupId(true);
        feedbackPanel.setFilter((IFeedbackMessageFilter) aMessage -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : "SYSTEM";
            if (aMessage.isFatal()) {
                LOG.error("{}: {}", username, aMessage.getMessage());
            }
            else if (aMessage.isError()) {
                LOG.error("{}: {}", username, aMessage.getMessage());
            }
            else if (aMessage.isWarning()) {
                LOG.warn("{}: {}", username, aMessage.getMessage());
            }
            else if (aMessage.isInfo()) {
                LOG.info("{}: {}", username, aMessage.getMessage());
            }
            else if (aMessage.isDebug()) {
                LOG.debug("{}: {}", username, aMessage.getMessage());
            }
            return true;
        });
        add(feedbackPanel);
        
        versionLabel = new Label("version", SettingsUtil.getVersionString());
        add(versionLabel);
        
        // set up warnings shown when using an embedded DB or some unsupported browser
        boolean isBrowserWarningVisible = isBrowserWarningVisible(settings);
        boolean isDatabaseWarningVisible = isDatabaseWarningVisible(settings);
        
        embeddedDbWarning = new Label("embeddedDbWarning", new ResourceModel("warning.database"));
        embeddedDbWarning.setVisible(isDatabaseWarningVisible);
        add(embeddedDbWarning);
        browserWarning = new Label("browserWarning", new ResourceModel("warning.browser"));
        browserWarning.setVisible(isBrowserWarningVisible);
        add(browserWarning);
        
        WebMarkupContainer warningsContainer = new WebMarkupContainer("warnings");
        warningsContainer.setVisible(isBrowserWarningVisible || isDatabaseWarningVisible);  
        add(warningsContainer);
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        // Do not cache pages in development mode - allows us to make changes to the HMTL without
        // having to reload the application
        if (RuntimeConfigurationType.DEVELOPMENT.equals(getApplication().getConfigurationType())) {
            getApplication().getMarkupSettings().getMarkupFactory().getMarkupCache().clear();
            getApplication().getResourceSettings()
                    .setCachingStrategy(NoOpResourceCachingStrategy.INSTANCE);
        }
    }

    public FeedbackPanel getFeedbackPanel()
    {
        return feedbackPanel;
    }
    
    private boolean isDatabaseWarningVisible(Properties settings) {
        boolean isUsingEmbeddedDatabase;
        try {
            String driver = dbDriverService.getDatabaseDriverName();
            isUsingEmbeddedDatabase = StringUtils.contains(driver.toLowerCase(Locale.US), "hsql");
        } catch (Throwable e) {
            LOG.warn("Unable to determine which database is being used", e);
            isUsingEmbeddedDatabase = false;
        }
        boolean ignoreWarning = "false".equalsIgnoreCase(
                settings.getProperty(SettingsUtil.CFG_WARNINGS_EMBEDDED_DATABASE));

        return isUsingEmbeddedDatabase && !ignoreWarning;
    }
    
    private boolean isBrowserWarningVisible(Properties settings) { 
        RequestCycle requestCycle = RequestCycle.get();
        WebClientInfo clientInfo;
        if (Session.exists()) {
            WebSession session = WebSession.get();
            clientInfo = session.getClientInfo();
        } else {
            clientInfo = new WebClientInfo(requestCycle);
        }
        ClientProperties clientProperties = clientInfo.getProperties();
        boolean isUsingUnsupportedBrowser = !clientProperties.isBrowserSafari()
                && !clientProperties.isBrowserChrome();
        
        boolean ignoreWarning = "false".equalsIgnoreCase(
                settings.getProperty(SettingsUtil.CFG_WARNINGS_UNSUPPORTED_BROWSER));

        return isUsingUnsupportedBrowser && !ignoreWarning;
    }
}
