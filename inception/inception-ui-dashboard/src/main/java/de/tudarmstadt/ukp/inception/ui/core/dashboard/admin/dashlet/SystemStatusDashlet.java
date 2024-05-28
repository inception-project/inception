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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.dashlet;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.catalina.valves.RemoteIpValve;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Url;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.security.core.session.SessionRegistry;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.support.markdown.MarkdownLabel;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.dashlet.ClientUrlAjaxBehavior.ClientUrlChangedEvent;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.Dashlet_ImplBase;

public class SystemStatusDashlet
    extends Dashlet_ImplBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = 1276835215161570732L;

    private @SpringBean SessionRegistry sessionRegistry;
    private @SpringBean UserDao userRepository;
    private @SpringBean SystemStatusService systemStatusService;

    private String clientUrl;

    public SystemStatusDashlet(String aId)
    {
        super(aId);
        setOutputMarkupId(true);

        var devMode = getApplication().getConfigurationType() == DEVELOPMENT;

        add(new ClientUrlAjaxBehavior());

        queue(new Label("activeUsers", LoadableDetachableModel.of(() -> getActiveUsers().size())));
        queue(new Label("activeUsersDetail", LoadableDetachableModel.of(
                () -> getActiveUsers().stream().map(Objects::toString).collect(joining(", ")))));

        queue(new WebMarkupContainer("reverseProxyInfo")
                .add(visibleWhen(LoadableDetachableModel.of(this::isRunningBehindReverseProxy))));

        var isProxyOk = isProxyTrusted();
        queue(new Fragment("isProxyTrusted", isProxyOk ? "proxyTrusted" : "proxyNotTrusted", this));
        queue(new Label("remoteIp", LoadableDetachableModel.of(this::getRemoteIp))
                .setVisible(!isProxyOk || devMode));
        queue(new Label("trustedProxies",
                LoadableDetachableModel.of(this::getTrustedProxies).orElse("-- not set --"))
                        .setVisible(!isProxyOk || devMode));
        queue(new Label("internalProxies",
                LoadableDetachableModel.of(this::getInternalProxies).orElse("-- not set --"))
                        .setVisible(!isProxyOk || devMode));

        queue(new MarkdownLabel("isProtocolOk", LoadableDetachableModel
                .of(() -> getString(isProtocolOk() ? "protocolOk" : "protocolNotOk"))));
        queue(new Label("clientUrl", LoadableDetachableModel.of(() -> clientUrl))
                .add(visibleWhen(() -> isProtocolOk() || devMode)));
        queue(new Label("serverUrl", LoadableDetachableModel.of(this::getServerUrl))
                .add(visibleWhen(() -> isProtocolOk() || devMode)));

        queue(new MarkdownLabel("hasCsrfWithProtocol", LoadableDetachableModel.of(() -> getString(
                hasCsrfWithProtocol() ? "csrfWithProtocolOk" : "csrfWithProtocolNotOk"))));

        queue(new MarkdownLabel("hasCsrfWithoutProtocol",
                LoadableDetachableModel
                        .of(() -> getString(hasCsrfWithoutProtocol() ? "csrfWithoutProtocolOk"
                                : "csrfWithoutProtocolNotOk"))));

        queue(new WebMarkupContainer("requestDetails").add(visibleWhen(() -> devMode)));

        queue(new Label("headers", LoadableDetachableModel.of(this::getHeaders)));

        var reverseProxyHeaders = LoadableDetachableModel.of(this::getReverseProxyHeaders);
        queue(new Label("reverseProxyHeaders", reverseProxyHeaders)
                .add(visibleWhen(reverseProxyHeaders.map(StringUtils::isNotBlank))));

        var csrfInfo = LoadableDetachableModel.of(this::getCsrfAcceptedOrigins);
        queue(new Label("csrfInfo", csrfInfo).add(
                visibleWhen(reverseProxyHeaders.map(StringUtils::isNotBlank).orElse(devMode))));
    }

    @OnEvent
    public void onClientUrlChangedEvent(ClientUrlChangedEvent aEvent)
    {
        clientUrl = aEvent.getUrl();
        aEvent.getTarget().add(this);
    }

    private List<Object> getActiveUsers()
    {
        return sessionRegistry.getAllPrincipals().stream() //
                .filter($ -> !"anonymousUser".equals($.toString())) //
                .toList();
    }

    private boolean hasCsrfWithProtocol()
    {
        try {
            var acceptedOrigins = systemStatusService.getCsrfAttacksPreventionProperties()
                    .getAcceptedOrigins();

            if (clientUrl == null) {
                return false;
            }

            var url = new URL(clientUrl);

            return acceptedOrigins.contains(url.getProtocol() + "://" + url.getHost());
        }
        catch (Exception e) {
            LOG.error("Cannot parse client URL", e);
        }

        return false;
    }

    private boolean hasCsrfWithoutProtocol()
    {
        try {
            var acceptedOrigins = systemStatusService.getCsrfAttacksPreventionProperties()
                    .getAcceptedOrigins();

            if (clientUrl == null) {
                return false;
            }

            var url = new URL(clientUrl);

            return acceptedOrigins.contains(url.getHost());
        }
        catch (Exception e) {
            LOG.error("Cannot parse client URL", e);
        }

        return false;
    }

    private boolean isProtocolOk()
    {
        try {
            var clientProtocol = getClientUrlProtocol();
            var serverProtocol = getServerUrlProtocol();

            return Objects.equals(clientProtocol, serverProtocol);
        }
        catch (Exception e) {
            LOG.error("Cannot compare protocols", e);
        }

        return false;
    }

    private String getServerUrlProtocol()
    {
        var serverUrl = getServerUrl();
        return substringBefore(serverUrl, "://");
    }

    private String getClientUrlProtocol()
    {
        return substringBefore(clientUrl, "://");
    }

    private boolean isProxyTrusted()
    {
        var remoteIp = getRemoteIp();
        if (getRequest() instanceof ServletWebRequest request) {
            for (var header : asList("x-forwarded-for", "x-real-ip")) {
                var headerValue = request.getHeader(header);
                if (headerValue != null && headerValue.equals(remoteIp)) {
                    LOG.debug(
                            "Proxy seems trusted as remoteIp [{}] seems to have been picked up from header [{}]",
                            remoteIp, header);
                    return true;
                }
            }
        }

        if (clientUrl != null && startsWith(clientUrl, getServerUrl())) {
            // It seems that the URL the client requested is known to us, so we seem to have
            // trusted the proxy
            LOG.debug(
                    "Proxy seems trusted as clientUrl starts with serverUrl: [{}] starts with [{}]",
                    clientUrl, getServerUrl());
            return true;
        }

        // If the server and client URLs mismatch, then let's look at the remote IP - maybe that
        // is not trusted or internal.
        var trustedProxies = getTrustedProxies();
        if (isNotBlank(trustedProxies)) {
            try {
                if (Pattern.matches(trustedProxies, remoteIp)) {
                    LOG.debug("Proxy seems trusted by trustedProxies: [{}] matches [{}]", remoteIp,
                            trustedProxies);
                    return true;
                }
            }
            catch (Exception e) {
                LOG.error("Cannot check trusted proxies expression [" + trustedProxies + "]", e);
            }
        }

        var internalProxies = getInternalProxies();
        if (isNotBlank(internalProxies)) {
            try {
                if (Pattern.matches(internalProxies, remoteIp)) {
                    LOG.debug("Proxy seems trusted by internalProxies: [{}] matches [{}]", remoteIp,
                            internalProxies);
                    return true;
                }
            }
            catch (Exception e) {
                LOG.error("Cannot check internal proxies expression [" + internalProxies + "]", e);
            }
        }

        LOG.debug(
                "Proxy seems not to be trusted: clientUrl [{}] and serverUrl [{}] do not match and remoteIP [] does"
                        + "not match trustedProxies [{}] or internalProxies [{}]",
                clientUrl, getServerUrl(), remoteIp, trustedProxies, internalProxies);
        return false;
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setVisible(userRepository.isAdministrator(userRepository.getCurrentUser()));
    }

    private String getCsrfAcceptedOrigins()
    {
        var buf = new StringBuilder();
        var acceptedOrigins = systemStatusService.getCsrfAttacksPreventionProperties()
                .getAcceptedOrigins();
        if (isNotEmpty(acceptedOrigins)) {
            for (var acceptedOrigin : acceptedOrigins) {
                buf.append(acceptedOrigin);
                buf.append("\n");
            }
        }
        return buf.toString();
    }

    private boolean isRunningBehindReverseProxy()
    {
        if (getRequest() instanceof ServletWebRequest request) {
            var xForwardHeaders = list(request.getContainerRequest().getHeaderNames()).stream() //
                    .filter(h -> StringUtils.startsWithAny(h.toLowerCase(ROOT), "x-forwarded-",
                            "forwarded")) //
                    .toList();

            if (!xForwardHeaders.isEmpty()) {
                // Found headers related to the present of a reverse proxy
                return true;
            }
        }

        if (clientUrl != null && !startsWith(clientUrl, getServerUrl())) {
            // Probably running behind a reverse proxy, but the URL the server sees from the client
            // does not match the URL that the client actually tried to access
            return true;
        }

        return false;
    }

    private String getReverseProxyHeaders()
    {
        if (getRequest() instanceof ServletWebRequest request) {
            var xForwardHeaders = list(request.getContainerRequest().getHeaderNames()).stream() //
                    .filter(h -> StringUtils.startsWithAny(h.toLowerCase(ROOT), "x-forwarded-",
                            "forwarded", "x-real-ip")) //
                    .toList();
            if (xForwardHeaders.isEmpty()) {
                return null;
            }

            // https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto.webserver.use-behind-a-proxy-server
            // If you are using Tomcat and terminating SSL at the proxy,
            // server.tomcat.redirect-context-root should be
            // set to false. This allows the X-Forwarded-Proto header to be honored before any
            // redirects are performed.

            // server.tomcat.remoteip.remote-ip-header=x-your-remote-ip-header
            // server.tomcat.remoteip.protocol-header=x-your-protocol-header
            // server.tomcat.remoteip.internal-proxies=192\\.168\\.\\d{1,3}\\.\\d{1,3}

            var buf = new StringBuilder();
            List<String> displayHeaderNames = new ArrayList<>();
            displayHeaderNames.addAll(xForwardHeaders);
            // The X-Forwarded-For (XFF) HTTP header field is a common method for identifying the
            // originating IP address of a client connecting to a web server through an HTTP proxy
            // or load balancer.
            displayHeaderNames.add("x-forwarded-for");
            // The X-Forwarded-Proto (XFP) header is a de-facto standard header for identifying the
            // protocol (HTTP or HTTPS) that a client used to connect to your proxy or load
            // balancer.
            displayHeaderNames.add("x-forwarded-proto");
            // X-Forwarded-Host The original host requested by the client in the Host HTTP request
            // header.
            displayHeaderNames.add("x-forwarded-host");
            // The X-Forwarded-Port request header helps you identify the destination port that the
            // client used to connect to the load balancer.
            displayHeaderNames.add("x-forwarded-port");
            // X-Forwarded-Server is the hostname of the proxy server. It gets overwritten by each
            // proxy, which is involved in the communication, with the current proxy's hostname.
            displayHeaderNames.add("x-forwarded-server");
            // X-Forwarded-By is the proxy IP address)
            displayHeaderNames.add("x-forwarded-by");
            // Alternative to x-forwarded-proto with the same function
            displayHeaderNames.add("x-forwarded-scheme");
            displayHeaderNames = displayHeaderNames.stream().distinct().sorted().toList();

            for (var headerName : displayHeaderNames) {
                buf.append(headerName);
                buf.append(": ");
                var values = request.getHeaders(headerName);
                if (CollectionUtils.isEmpty(values)) {
                    buf.append("-- not set --");
                }
                else {
                    buf.append(String.join(", ", request.getHeaders(headerName)));
                }
                buf.append("\n");
            }
            return buf.toString();
        }

        return null;
    }

    private String getHeaders()
    {
        var buf = new StringBuilder();
        if (getRequest() instanceof ServletWebRequest request) {
            for (var headerName : list(request.getContainerRequest().getHeaderNames())) {
                buf.append(headerName);
                buf.append(": ");
                buf.append(String.join(", ", request.getHeaders(headerName)));
                buf.append("\n");
            }
        }
        return buf.toString();
    }

    private String getServerUrl()
    {
        var urlRenderer = getRequestCycle().getUrlRenderer();
        var homePageUrl = urlFor(getApplication().getHomePage(), null);
        return urlRenderer.renderFullUrl(Url.parse(homePageUrl));
    }

    private String getRemoteIp()
    {
        if (getRequest() instanceof ServletWebRequest request) {
            return request.getContainerRequest().getRemoteAddr();
        }

        return null;
    }

    private String getInternalProxies()
    {
        var maybeValve = getRemoteIpValve();
        return maybeValve.map(RemoteIpValve::getInternalProxies).orElse(null);
    }

    private String getTrustedProxies()
    {
        var maybeValve = getRemoteIpValve();
        return maybeValve.map(RemoteIpValve::getTrustedProxies).orElse(null);
    }

    private Optional<RemoteIpValve> getRemoteIpValve()
    {
        var context = ApplicationContextProvider.getApplicationContext();
        var factory = context.getBean(TomcatServletWebServerFactory.class);
        var maybeValve = factory.getEngineValves().stream() //
                .filter(RemoteIpValve.class::isInstance) //
                .map(v -> (RemoteIpValve) v).findFirst();
        return maybeValve;
    }
}
