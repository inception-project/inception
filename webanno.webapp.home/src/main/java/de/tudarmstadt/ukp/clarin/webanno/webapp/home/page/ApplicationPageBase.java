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

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.webapp.home.security.LogoutPanel;

/**
 *  The @WiQueryUIPlugin annotation and that the class implements IWiQueryPlugin makes sure that the
 * JQuery stylesheet is always present. - REC 2012-02-28
 * 
 * @author Richard Eckart de Castilho
 */
public abstract class ApplicationPageBase
    extends WebPage
{
    private final static Log LOG = LogFactory.getLog(ApplicationPageBase.class);

    private static final long serialVersionUID = -1690130604031181803L;

    private LogoutPanel logoutPanel;
    private FeedbackPanel feedbackPanel;
    private Label versionLabel;
    private Label embeddedDbWarning;

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
        getSession().setLocale(Locale.ENGLISH);

        logoutPanel = new LogoutPanel("logoutPanel");
        feedbackPanel = new FeedbackPanel("feedbackPanel");
        feedbackPanel.setOutputMarkupId(true);
        feedbackPanel.add(new AttributeModifier("class", "error"));
        feedbackPanel.setFilter(new IFeedbackMessageFilter()
        {
            @Override
            public boolean accept(FeedbackMessage aMessage)
            {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
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

        Properties props = getVersionProperties();
        String versionString = props.getProperty("version") + " (" + props.getProperty("timestamp")
                + ", build " + props.getProperty("buildNumber") + ")";
        versionLabel = new Label("version", versionString);

        embeddedDbWarning = new Label("embeddedDbWarning",
                "USE THIS INSTALLATION FOR TESTING ONLY -- "
                + "AN EMBEDDED DATABASE IS NOT SUPPORTED FOR PRODUCTION USE");
        embeddedDbWarning.setVisible(false);
        try {
            String driver = repository.getDatabaseDriverName();
            embeddedDbWarning.setVisible(StringUtils.contains(driver.toLowerCase(Locale.US),
                    "hsql"));
        }
        catch (Throwable e) {
            LOG.warn("Unable to determine which database is being used", e);
        }

        add(logoutPanel);
        add(feedbackPanel);
        add(versionLabel);
        add(embeddedDbWarning);
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        logoutPanel.setVisible(AuthenticatedWebSession.get().isSignedIn());
    }

    public FeedbackPanel getFeedbackPanel()
    {
        return feedbackPanel;
    }

    public Properties getVersionProperties()
    {
        try {
            return PropertiesLoaderUtils.loadAllProperties("/META-INF/version.properties");
        }
        catch (IOException e) {
            LOG.error("Unable to load version information", e);
            return new Properties();
        }
    }
}
