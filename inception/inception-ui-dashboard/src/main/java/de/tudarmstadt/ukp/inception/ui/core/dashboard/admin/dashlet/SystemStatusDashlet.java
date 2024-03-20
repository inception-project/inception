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
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        var buf = new StringBuilder();
        if (getRequest() instanceof ServletWebRequest request) {
            for (var headerName : list(request.getContainerRequest().getHeaderNames()).stream()
                    .sorted().toList()) {
                if (!headerName.toLowerCase(Locale.ROOT).startsWith("x-forwarded-")) {
                    continue;
                }

                renderHeader(buf, request, headerName);
            }
        }
        return buf.toString();
    }

    private void renderHeader(StringBuilder aBuf, ServletWebRequest request, String header)
    {
        var values = request.getHeaders(header);
        if (isNotEmpty(values)) {
            aBuf.append(header);
            aBuf.append(": ");
            aBuf.append(String.join(", ", values));
            aBuf.append("\n");
        }
    }

    private String getHeaders()
    {
        if (getRequest() instanceof ServletWebRequest request) {
            var headerNames = list(request.getContainerRequest().getHeaderNames());
            if (headerNames.stream().noneMatch(h -> h.startsWith("x-forwarded-"))) {
                return null;
            }

            var buf = new StringBuilder();
            List<String> displayHeaderNames = new ArrayList<>();
            displayHeaderNames.addAll(headerNames);
            displayHeaderNames.add("x-forwarded-for");
            displayHeaderNames.add("x-forwarded-proto");
            displayHeaderNames.add("x-forwarded-host");
            displayHeaderNames.add("x-forwarded-port");
            displayHeaderNames.add("x-forwarded-server");
            displayHeaderNames.add("x-forwarded-by");
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

    private String getOriginalUrl()
    {
        return getRequest().getOriginalUrl().toString();
    }

    private String getServerUrl()
    {
        var urlRenderer = getRequestCycle().getUrlRenderer();
        var homePageUrl = urlFor(getApplication().getHomePage(), null);
        return urlRenderer.renderFullUrl(Url.parse(homePageUrl));
    }
}
