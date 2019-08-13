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
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.caching.NoOpResourceCachingStrategy;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapFeedbackPanel;
import de.tudarmstadt.ukp.clarin.webanno.support.interceptors.GlobalInterceptor;
import de.tudarmstadt.ukp.clarin.webanno.support.interceptors.GlobalInterceptorsRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.footer.FooterItemRegistry;

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

    private @SpringBean GlobalInterceptorsRegistry interceptorsRegistry;
    private @SpringBean FooterItemRegistry footerItemRegistry;

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
        for (GlobalInterceptor interceptor : interceptorsRegistry.getInterceptors()) {
            interceptor.intercept(this);
        }

        List<Component> footerItems = footerItemRegistry.getFooterItems().stream()
                .map(c -> c.create("item"))
                .collect(Collectors.toList());
        
        add(new ListView<Component>("footerItems", footerItems)
        {
            private static final long serialVersionUID = 5912513189482015963L;

            @Override
            protected void populateItem(ListItem<Component> aItem)
            {
                aItem.add(aItem.getModelObject());
            }
        });
        
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
}
