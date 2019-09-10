/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.footer;

import java.util.Locale;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.db.DatabaseDriverService;

public class WarningsFooterPanel
    extends Panel
{
    private static final long serialVersionUID = 2586844743503672765L;

    private final static Logger LOG = LoggerFactory.getLogger(WarningsFooterPanel.class);
    
    private @SpringBean DatabaseDriverService dbDriverService;
    
    private Label embeddedDbWarning;
    private Label browserWarning;

    public WarningsFooterPanel(String aId)
    {
        super(aId);
        
        Properties settings = SettingsUtil.getSettings();
        
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
