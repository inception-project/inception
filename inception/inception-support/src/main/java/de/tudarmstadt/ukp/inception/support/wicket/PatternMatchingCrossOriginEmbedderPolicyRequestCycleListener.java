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
package de.tudarmstadt.ukp.inception.support.wicket;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.wicket.coep.CrossOriginEmbedderPolicyConfiguration;
import org.apache.wicket.coep.CrossOriginEmbedderPolicyRequestCycleListener;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

public class PatternMatchingCrossOriginEmbedderPolicyRequestCycleListener
    extends CrossOriginEmbedderPolicyRequestCycleListener
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String REGEX_PREFIX = "regex:";

    private CrossOriginEmbedderPolicyConfiguration coepConfig;
    private List<Pattern> patterns;

    public PatternMatchingCrossOriginEmbedderPolicyRequestCycleListener(
            CrossOriginEmbedderPolicyConfiguration aCoepConfig)
    {
        super(aCoepConfig);
        coepConfig = aCoepConfig;
    }

    @Override
    public void onRequestHandlerResolved(RequestCycle aCycle, IRequestHandler aHandler)
    {
        if (!(aCycle.getResponse() instanceof WebResponse)) {
            return;
        }

        final Object containerRequest = aCycle.getRequest().getContainerRequest();
        if (!(containerRequest instanceof HttpServletRequest)) {
            return;
        }

        final String coepHeaderName = coepConfig.getCoepHeader();
        HttpServletRequest request = (HttpServletRequest) containerRequest;
        // CrossOriginEmbedderPolicyRequestCycleListener uses requesrt.getContextPath()
        // here which seems to make no sense at all!
        String path = request.getRequestURI();

        WebResponse webResponse = (WebResponse) aCycle.getResponse();
        if (webResponse.isHeaderSupported()) {
            if (coepConfig.getExemptions().contains(path) || matchesExemptionPatten(path)) {
                log.debug("Request path {} is exempted from COEP, no '{}' header added", path,
                        coepHeaderName);
                // We cannot un-set a header, so we set explicitly to disable COEP
                webResponse.setHeader(coepHeaderName, "unsafe-none");
            }
            else {
                webResponse.setHeader(coepHeaderName, "require-corp");
            }
        }
    };

    private boolean matchesExemptionPatten(String aPath)
    {
        initPatterns();

        for (Pattern p : patterns) {
            if (p.matcher(aPath).matches()) {
                return true;
            }
        }

        return false;
    }

    private synchronized void initPatterns()
    {
        if (patterns != null) {
            return;
        }

        patterns = new ArrayList<>();
        for (String exemption : coepConfig.getExemptions()) {
            if (exemption.startsWith(REGEX_PREFIX)) {
                patterns.add(Pattern.compile(exemption.substring(REGEX_PREFIX.length())));
            }
        }
    }
}
