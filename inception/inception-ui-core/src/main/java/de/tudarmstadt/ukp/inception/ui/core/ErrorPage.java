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
package de.tudarmstadt.ukp.inception.ui.core;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.protocol.http.servlet.ErrorAttributes;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import com.giffing.wicket.spring.boot.context.scan.WicketAccessDeniedPage;
import com.giffing.wicket.spring.boot.context.scan.WicketExpiredPage;
import com.giffing.wicket.spring.boot.context.scan.WicketInternalErrorPage;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

@WicketInternalErrorPage
@WicketExpiredPage
@WicketAccessDeniedPage
@MountPath("/whoops")
public class ErrorPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 7848496813044538495L;

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        Label statusCode = new Label("statusCode", LoadableDetachableModel.of(this::getStatusCode));
        statusCode.add(visibleWhen(() -> statusCode.getDefaultModelObject() != null));
        add(statusCode);

        Label statusReasonPhrase = new Label("statusReasonPhrase",
                LoadableDetachableModel.of(this::getStatusReasonPhrase));
        statusReasonPhrase
                .add(visibleWhen(() -> statusReasonPhrase.getDefaultModelObject() != null));
        add(statusReasonPhrase);

        Label requestUri = new Label("requestUri", LoadableDetachableModel.of(this::getRequestUri));
        requestUri.add(visibleWhen(() -> requestUri.getDefaultModelObject() != null));
        add(requestUri);

        Label servletName = new Label("servletName",
                LoadableDetachableModel.of(this::getServletName));
        servletName.add(visibleWhen(() -> servletName.getDefaultModelObject() != null));
        add(servletName);

        Label message = new Label("message", LoadableDetachableModel.of(this::getMessage));
        message.add(visibleWhen(() -> message.getDefaultModelObject() != null));
        add(message);

        Label exceptionMessage = new Label("exceptionMessage",
                LoadableDetachableModel.of(this::getExceptionMessage));
        exceptionMessage.add(visibleWhen(() -> exceptionMessage.getDefaultModelObject() != null));
        add(exceptionMessage);

        Label exceptionStackTrace = new Label("exceptionStackTrace",
                LoadableDetachableModel.of(this::getExceptionStackTrace));
        exceptionStackTrace
                .add(visibleWhen(() -> exceptionStackTrace.getDefaultModelObject() != null
                        && (isDeveloper() || isAdministrator())));
        add(exceptionStackTrace);

        Label appVersion = new Label("appVersion", SettingsUtil.getVersionString());
        add(appVersion);

        Label javaVendor = new Label("javaVendor", System.getProperty("java.vendor"));
        add(javaVendor);

        Label javaVersion = new Label("javaVersion", System.getProperty("java.version"));
        add(javaVersion);

        Label osName = new Label("osName", System.getProperty("os.name"));
        add(osName);

        Label osVersion = new Label("osVersion", System.getProperty("os.version"));
        add(osVersion);

        Label osArch = new Label("osArch", System.getProperty("os.arch"));
        add(osArch);
    }

    private boolean isDeveloper()
    {
        return DEVELOPMENT.equals(getApplication().getConfigurationType());
    }

    private boolean isAdministrator()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream() //
                .map(GrantedAuthority::getAuthority) //
                .anyMatch(a -> ROLE_ADMIN.toString().equals(a));
    }

    private Optional<ErrorAttributes> getErrorAttributes()
    {
        RequestCycle cycle = RequestCycle.get();

        Request req = cycle.getRequest();
        if (req instanceof ServletWebRequest) {
            ServletWebRequest webRequest = (ServletWebRequest) req;
            ErrorAttributes errorAttributes = ErrorAttributes.of(webRequest.getContainerRequest(),
                    webRequest.getFilterPrefix());

            return Optional.ofNullable(errorAttributes);
        }

        return Optional.empty();
    }

    private Integer getStatusCode()
    {
        return getErrorAttributes().map(ErrorAttributes::getStatusCode).orElse(null);
    }

    private String getStatusReasonPhrase()
    {
        return getErrorAttributes() //
                .map(ErrorAttributes::getStatusCode) //
                .map(code -> {
                    try {
                        return HttpStatus.valueOf(code).getReasonPhrase();
                    }
                    catch (IllegalArgumentException e) {
                        return null;
                    }
                }) //
                .orElse(null);
    }

    private String getRequestUri()
    {
        Optional<String> uri = getErrorAttributes().map(ErrorAttributes::getRequestUri);

        if (uri.isPresent()) {
            return uri.get();
        }

        Url url = RequestCycle.get().getRequest().getOriginalUrl();
        if (url != null) {
            return "/" + url.toString();
        }

        return null;
    }

    private String getMessage()
    {
        return getErrorAttributes().map(ErrorAttributes::getMessage).orElse(null);
    }

    private String getServletName()
    {
        return getErrorAttributes().map(ErrorAttributes::getServletName).orElse(null);
    }

    private String getExceptionMessage()
    {
        Throwable e = getException();

        if (e != null) {
            return ExceptionUtils.getRootCauseMessage(e);
        }

        return null;
    }

    private String getExceptionStackTrace()
    {
        Throwable e = getException();

        if (e != null) {
            return String.join("\n", ExceptionUtils.getRootCauseStackTrace(e));
        }

        return null;
    }

    private Throwable getException()
    {
        RequestCycle cycle = RequestCycle.get();

        Exception e = cycle.getMetaData(ErrorListener.EXCEPTION);
        if (e != null) {
            return e;
        }

        return getErrorAttributes().map(ErrorAttributes::getException).orElse(null);
    }

    @Override
    public boolean isErrorPage()
    {
        return true;
    }

    @Override
    public boolean isVersioned()
    {
        return false;
    }
}
