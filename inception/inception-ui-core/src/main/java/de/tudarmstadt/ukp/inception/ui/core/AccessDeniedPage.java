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

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toPrettyJsonString;
import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.parseMediaTypes;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.protocol.http.servlet.ErrorAttributes;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.request.http.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.wicketstuff.annotation.mount.MountPath;

import com.giffing.wicket.spring.boot.context.scan.WicketAccessDeniedPage;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

@WicketAccessDeniedPage
@MountPath("/nowhere")
public class AccessDeniedPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 7848496813044538495L;
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public AccessDeniedPage()
    {
        if (isJsonResponseRequested() || StringUtils.startsWith(getRequestUri(), "/api")) {
            produceJsonResponseIfRequested();
        }
    }

    private void produceJsonResponseIfRequested()
    {
        String response;
        try {
            response = toPrettyJsonString(Map.of( //
                    "messages", asList( //
                            Map.of( //
                                    "level", "ERROR", //
                                    "message", getStatusReasonPhrase()))));
        }
        catch (IOException e) {
            response = "{\"messages\": [{\"level\": \"ERROR\", \"message\": \"Unable to render error.\"}] }";
            LOG.error("Unable to render error message", e);
        }

        getRequestCycle().scheduleRequestHandlerAfterCurrent(
                new TextRequestHandler(APPLICATION_JSON.toString(), null, response));
    }

    private boolean isJsonResponseRequested()
    {
        if (!(getRequestCycle().getRequest() instanceof WebRequest)) {
            return false;
        }

        WebRequest request = (WebRequest) getRequestCycle().getRequest();
        if (!APPLICATION_JSON.isPresentIn(parseMediaTypes(request.getHeader("Accept")))) {
            return false;
        }

        return true;
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
