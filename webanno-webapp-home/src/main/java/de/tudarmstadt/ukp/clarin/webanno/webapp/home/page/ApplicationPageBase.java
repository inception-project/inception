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
package de.tudarmstadt.ukp.clarin.webanno.webapp.home.page;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.caching.NoOpResourceCachingStrategy;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.googlecode.wicket.jquery.ui.settings.JQueryUILibrarySettings;
import com.googlecode.wicket.kendo.ui.settings.KendoUILibrarySettings;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.webapp.home.security.LogoutPanel;

public abstract class ApplicationPageBase
    extends WebPage
{
    private final static Log LOG = LogFactory.getLog(ApplicationPageBase.class);

    private static final long serialVersionUID = -1690130604031181803L;

    private LogoutPanel logoutPanel;
    private FeedbackPanel feedbackPanel;
    private Label versionLabel;
    private Label embeddedDbWarning;
    private Label browserWarning;
    private ExternalLink helpLink;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    protected ApplicationPageBase()
    {
        commonInit();
    }

    protected ApplicationPageBase(final PageParameters parameters)
    {
        super(parameters);
        commonInit();
    }

    @SuppressWarnings({ "serial" })
    private void commonInit()
    {
        Properties settings = SettingsUtil.getSettings();
        
        // Override locale to be used by application
        String locale = settings.getProperty("locale", "en");
        switch (locale) {
        case "auto":
            // Do nothing - locale is picked up from browser
            break;
        default:
            // Override the locale in the session
            getSession().setLocale(Locale.forLanguageTag(locale));
            break;
        }

        logoutPanel = new LogoutPanel("logoutPanel");
        feedbackPanel = new FeedbackPanel("feedbackPanel");
        feedbackPanel.setOutputMarkupId(true);
        feedbackPanel.add(new AttributeModifier("class", "error"));
        feedbackPanel.setFilter(new IFeedbackMessageFilter()
        {
            @Override
            public boolean accept(FeedbackMessage aMessage)
            {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String username = auth != null ? auth.getName() : "SYSTEM";
                if (aMessage.isFatal()) {
                    LOG.fatal(username + ": " + aMessage.getMessage());
                }
                else if (aMessage.isError()) {
                    LOG.error(username + ": " + aMessage.getMessage());
                }
                else if (aMessage.isWarning()) {
                    LOG.warn(username + ": " + aMessage.getMessage());
                }
                else if (aMessage.isInfo()) {
                    LOG.info(username + ": " + aMessage.getMessage());
                }
                else if (aMessage.isDebug()) {
                    LOG.debug(username + ": " + aMessage.getMessage());
                }
                return true;
            }
        });
        
        versionLabel = new Label("version", SettingsUtil.getVersionString());

        embeddedDbWarning = new Label("embeddedDbWarning",
                "USE THIS INSTALLATION FOR TESTING ONLY -- "
                + "AN EMBEDDED DATABASE IS NOT RECOMMENDED FOR PRODUCTION USE");
        embeddedDbWarning.setVisible(false);
        try {
            String driver = repository.getDatabaseDriverName();
            embeddedDbWarning.setVisible(StringUtils.contains(driver.toLowerCase(Locale.US),
                    "hsql"));
        }
        catch (Throwable e) {
            LOG.warn("Unable to determine which database is being used", e);
        }

        // Override warning about embedded database.
        if ("false".equalsIgnoreCase(settings.getProperty("warnings.embeddedDatabase"))) {
            embeddedDbWarning.setVisible(false);
        }
        
        // Display a warning when using an unsupported browser
        RequestCycle requestCycle = RequestCycle.get();
        WebClientInfo clientInfo;
        if (Session.exists()) {
            WebSession session = WebSession.get();
            clientInfo = session.getClientInfo();
        }
        else {
            clientInfo = new WebClientInfo(requestCycle);
        }
        ClientProperties clientProperties = clientInfo.getProperties();

        browserWarning = new Label("browserWarning", "THIS BROWSER IS NOT SUPPORTED -- "
                + "PLEASE USE CHROME OR SAFARI");
        browserWarning.setVisible(!clientProperties.isBrowserSafari()
                && !clientProperties.isBrowserChrome());

        // Override warning about browser.
        if ("false".equalsIgnoreCase(settings.getProperty("warnings.unsupportedBrowser"))) {
            browserWarning.setVisible(false);
        }
        
        boolean helpAvailable;
        try {
            Application.get().getResourceSettings().getLocalizer().getString("page.help.link", this);
            Application.get().getResourceSettings().getLocalizer().getString("page.help", this);
            helpAvailable = true;
        }
        catch (MissingResourceException e) {
            helpAvailable = false;
        }
        
        add(helpLink = new ExternalLink("helpLink", new ResourceModel("page.help.link", ""),
                new ResourceModel("page.help", "")));
        helpLink.setPopupSettings(new PopupSettings("_blank"));
        helpLink.setVisible(helpAvailable);
        
        add(logoutPanel);
        add(feedbackPanel);
        add(versionLabel);
        add(embeddedDbWarning);
        add(browserWarning);
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        logoutPanel.setVisible(AuthenticatedWebSession.get().isSignedIn());
        
        // Do not cache pages in development mode - allows us to make changes to the HMTL without
        // having to reload the application
        if (RuntimeConfigurationType.DEVELOPMENT.equals(getApplication().getConfigurationType())) {
            getApplication().getMarkupSettings().getMarkupFactory().getMarkupCache().clear();
            getApplication().getResourceSettings().setCachingStrategy(NoOpResourceCachingStrategy.INSTANCE);
        }
   }

    public FeedbackPanel getFeedbackPanel()
    {
        return feedbackPanel;
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        // We also load the JQuery CSS always just to get a consistent look across the app
        JQueryUILibrarySettings jqueryCfg = JQueryUILibrarySettings.get();

        if (jqueryCfg.getStyleSheetReference() != null) {
            aResponse.render(CssHeaderItem.forReference(jqueryCfg.getStyleSheetReference()));
        }

        // We use Kendo TextFields, but they do not automatically load the Kendo JS/CSS, so
        // we do it manually here and for all the pages.
        KendoUILibrarySettings kendoCfg = KendoUILibrarySettings.get();

        if (kendoCfg.getCommonStyleSheetReference() != null) {
            aResponse.render(CssHeaderItem.forReference(kendoCfg.getCommonStyleSheetReference()));
        }

        if (kendoCfg.getThemeStyleSheetReference() != null) {
            aResponse.render(CssHeaderItem.forReference(kendoCfg.getThemeStyleSheetReference()));
        }

        if (kendoCfg.getJavaScriptReference() != null) {
            aResponse.render(JavaScriptHeaderItem.forReference(kendoCfg.getJavaScriptReference()));
        }

        // Loading WebAnno CSS here so it can override JQuery/Kendo CSS
        aResponse.render(CssHeaderItem.forReference(WebAnnoCssReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(WebAnnoJavascriptReference.get()));
    }
}
