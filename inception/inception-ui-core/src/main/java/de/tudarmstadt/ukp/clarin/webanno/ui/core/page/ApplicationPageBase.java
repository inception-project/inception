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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.page;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.MetaDataHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;

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

    public static final MetaDataKey<Class<? extends Component>> MENUBAR_CLASS = //
            new MetaDataKey<Class<? extends Component>>()
            {
                private static final long serialVersionUID = 1L;
            };

    private FeedbackPanel feedbackPanel;
    private WebMarkupContainer footer;

    private @SpringBean GlobalInterceptorsRegistry interceptorsRegistry;
    private @SpringBean FooterItemRegistry footerItemRegistry;

    private IModel<List<Component>> footerItems;

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

        footerItems = new ListModel<>(new ArrayList<>());

        footerItemRegistry.getFooterItems().stream().map(c -> c.create("item"))
                .forEach(c -> footerItems.getObject().add(c));

        footer = new WebMarkupContainer("footer");
        footer.setOutputMarkupId(true);
        add(footer);

        footer.add(new ListView<Component>("footerItems", footerItems)
        {
            private static final long serialVersionUID = 5912513189482015963L;

            {
                setReuseItems(true);
            }

            @Override
            protected void populateItem(ListItem<Component> aItem)
            {
                aItem.setOutputMarkupPlaceholderTag(true);
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
                menubarClass = EmptyPanel.class;
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
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        // Actually, this is pretty pointless because we disable Spring Security CSRF for the
        // Wicket URLs...
        var containerRequest = RequestCycle.get().getRequest().getContainerRequest();
        if (containerRequest instanceof HttpServletRequest) {
            var httpRequest = (HttpServletRequest) containerRequest;
            var csrfToken = (CsrfToken) httpRequest.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                aResponse.render(new PriorityHeaderItem(
                        MetaDataHeaderItem.forMetaTag("csrftoken", csrfToken.getToken())));
            }
        }
    }

    public FeedbackPanel getFeedbackPanel()
    {
        return feedbackPanel;
    }

    public IModel<List<Component>> getFooterItems()
    {
        return footerItems;
    }

    public void addToFooter(Component aComponent)
    {
        List<Component> items = footerItems.getObject();

        if (!items.contains(aComponent)) {
            items.add(aComponent);
        }

        RequestCycle.get().find(IPartialPageRequestHandler.class).ifPresent(handler -> {
            handler.add(footer);
        });
    }

    public void removeFromFooter(Component aComponent)
    {
        List<Component> items = footerItems.getObject();

        items.remove(aComponent);

        RequestCycle.get().find(IPartialPageRequestHandler.class).ifPresent(handler -> {
            handler.add(footer);
        });
    }
}
