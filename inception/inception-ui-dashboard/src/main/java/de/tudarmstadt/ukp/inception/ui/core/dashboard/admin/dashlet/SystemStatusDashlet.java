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
import static java.util.Collections.list;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Url;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.session.SessionRegistry;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.Dashlet_ImplBase;

public class SystemStatusDashlet
    extends Dashlet_ImplBase
{
    private static final long serialVersionUID = 1276835215161570732L;

    private @SpringBean SessionRegistry sessionRegistry;
    private @SpringBean UserDao userRepository;
    private @SpringBean SystemStatusService systemStatusService;

    public SystemStatusDashlet(String aId)
    {
        super(aId);

        queue(new Label("clientUrl").add(new ClientUrlAjaxBehavior()));
        queue(new Label("activeUsers",
                LoadableDetachableModel.of(() -> sessionRegistry.getAllPrincipals().size())));
        queue(new Label("activeUsersDetail", LoadableDetachableModel.of(() -> sessionRegistry
                .getAllPrincipals().stream().map(Objects::toString).collect(joining(", ")))));

        queue(new Label("serverUrl", LoadableDetachableModel.of(this::getServerUrl)));
        queue(new Label("headers", LoadableDetachableModel.of(this::getHeaders))
                .add(visibleWhen(() -> getApplication().getConfigurationType() == DEVELOPMENT)));
        var reverseProxyInfo = LoadableDetachableModel.of(this::detectReverseProxy);
        queue(new Label("reverseProxyInfo", reverseProxyInfo)
                .add(visibleWhen(reverseProxyInfo.map(StringUtils::isNotBlank))));
        var csrfInfo = LoadableDetachableModel.of(this::getCsrfAcceptedOrigins);
        queue(new Label("csrfInfo", csrfInfo)
                .add(visibleWhen(reverseProxyInfo.map(StringUtils::isNotBlank))));
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

    private String detectReverseProxy()
    {
        if (getRequest() instanceof ServletWebRequest request) {
            var xForwardHeaders = list(request.getContainerRequest().getHeaderNames()).stream() //
                    .filter(h -> StringUtils.startsWithAny(h.toLowerCase(ROOT), "x-forwarded-",
                            "forwarded")) //
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
}
