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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.login;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhenNot;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;

import org.apache.wicket.NonResettingRestartException;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.parameter.UrlRequestParametersAdapter;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.SavedRequest;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.LoginProperties;
import de.tudarmstadt.ukp.inception.support.markdown.MarkdownLabel;

public class LocalLoginPanel
    extends Panel
{
    private static final long serialVersionUID = -4851128376067531994L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean UserDao userRepository;
    private @SpringBean LoginProperties loginProperties;

    private String username;
    private String password;
    private String urlfragment;

    private final IModel<Boolean> tooManyUsers;

    public LocalLoginPanel(String aId, IModel<Boolean> aTooManyUsers)
    {
        super(aId);

        tooManyUsers = aTooManyUsers;

        var form = new StatelessForm<LocalLoginPanel>("form", new CompoundPropertyModel<>(this))
        {
            private static final long serialVersionUID = 1050852689215968429L;

            @Override
            protected void onSubmit()
            {
                actionLogin();
            }
        };

        form.add(new RequiredTextField<String>("username").setOutputMarkupId(true));
        form.add(new PasswordTextField("password").setOutputMarkupId(true));
        form.add(new HiddenField<>("urlfragment"));
        form.add(new Button("signInBtn").add(enabledWhenNot(tooManyUsers)));
        form.add(new MarkdownLabel("loginMessage", loginProperties.getMessage()) //
                .add(visibleWhen(() -> isNotBlank(loginProperties.getMessage()))));

        add(form);

        redirectIfAlreadyLoggedIn(); // Must come after the localLoginPanel is initialized!
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        redirectIfAlreadyLoggedIn();
    }

    private void actionLogin()
    {
        // The redirect URL is stored in the session, so we have to pick it up before the
        // session is reset as part of the login.
        var redirectUrl = getRedirectUrl();

        // We only accept users that are not bound to a particular realm (e.g. to a project)
        var user = userRepository.get(username);
        if (user == null || user.getRealm() != null) {
            error("Login failed");
            return;
        }

        var session = AuthenticatedWebSession.get();
        if (!session.signIn(username, password)) {
            error("Login failed");
            return;
        }

        LOG.debug("Login successful");
        setDefaultResponsePageIfNecessary(redirectUrl);
    }

    private void redirectIfAlreadyLoggedIn()
    {
        // If we are already logged in, redirect to the welcome page. This tries to a void a
        // situation where the user tries to access the login page directly and thus the
        // application would redirect the user to the login page after a successful login
        if (!(SecurityContextHolder.getContext()
                .getAuthentication() instanceof AnonymousAuthenticationToken)) {
            LOG.debug("Already logged in, forwarding to home page");
            throw new RestartResponseException(getApplication().getHomePage());
        }

        var redirectUrl = getRedirectUrl();
        if (redirectUrl == null) {
            LOG.debug("Authentication required");
        }
        else {
            LOG.debug("Authentication required (original URL: [{}])", redirectUrl);
        }
    }

    private String getRedirectUrl()
    {
        String redirectUrl = null;

        var session = ((ServletWebRequest) RequestCycle.get().getRequest()).getContainerRequest()
                .getSession(false);
        if (session != null) {
            var savedRequest = (SavedRequest) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
            if (savedRequest != null) {
                redirectUrl = savedRequest.getRedirectUrl();
            }
        }

        // There is some kind of bug that logs the user out again if the redirect page is
        // the context root and if that does not end in a slash. To avoid this, we add a slash
        // here. This is rather a hack, but I have no idea why this problem occurs. Figured this
        // out through trial-and-error rather then by in-depth debugging.
        var baseUrl = RequestCycle.get().getUrlRenderer().renderFullUrl(Url.parse(""));
        if (baseUrl.equals(redirectUrl)) {
            redirectUrl += "/";
        }

        // In case there was a URL fragment in the original URL, append it again to the redirect
        // URL.
        if (redirectUrl != null && isNotBlank(urlfragment)) {
            redirectUrl += "#" + urlfragment;
        }

        return redirectUrl;
    }

    private void setDefaultResponsePageIfNecessary(String aRedirectUrl)
    {
        // This does not work because it was Spring Security that intercepted the access, not
        // Wicket continueToOriginalDestination();

        if (aRedirectUrl == null || aRedirectUrl.contains(".IBehaviorListener.")
                || aRedirectUrl.contains("-logoutPanel-") || aRedirectUrl.endsWith("/ws")) {
            LOG.debug("Redirecting to welcome page");
            setResponsePage(getApplication().getHomePage());
            return;
        }

        LOG.debug("Redirecting to saved URL: [{}]", aRedirectUrl);
        if (isNotBlank(urlfragment) && urlfragment.startsWith("!")) {
            var url = Url.parse("http://dummy?" + urlfragment.substring(1));
            var adapter = new UrlRequestParametersAdapter(url);
            var params = new LinkedHashMap<String, StringValue>();
            for (var name : adapter.getParameterNames()) {
                params.put(name, adapter.getParameterValue(name));
            }
            Session.get().setMetaData(SessionMetaData.LOGIN_URL_FRAGMENT_PARAMS, params);
        }
        throw new NonResettingRestartException(aRedirectUrl);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        // Capture the URL fragment into a hidden form field so we can use it later when
        // forwarding to the target page after login
        aResponse.render(OnDomReadyHeaderItem.forScript(
                "$('#urlfragment').attr('value', unescape(self.document.location.hash.substring(1)));"));
    }
}
