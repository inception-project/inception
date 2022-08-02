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
package de.tudarmstadt.ukp.clarin.webanno.support.wicket;

import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.util.Properties;

import org.apache.wicket.Application;
import org.apache.wicket.Page;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;

public class WicketUtil
{
    public static void serverTiming(String aKey, long aTime)
    {
        serverTiming(aKey, null, aTime);
    }

    public static void serverTiming(String aKey, String aDescription, long aTime)
    {
        Application app;
        try {
            app = Application.get();
        }
        catch (WicketRuntimeException e) {
            // No application - ignore
            return;
        }

        if (app == null) {
            return;
        }

        Properties settings = SettingsUtil.getSettings();
        if (!DEVELOPMENT.equals(app.getConfigurationType())
                && !"true".equalsIgnoreCase(settings.getProperty("debug.sendServerSideTimings"))) {
            return;
        }

        RequestCycle requestCycle = RequestCycle.get();
        if (requestCycle == null) {
            return;
        }

        Response response = requestCycle.getResponse();
        if (response instanceof WebResponse) {
            WebResponse webResponse = (WebResponse) response;
            StringBuilder sb = new StringBuilder();
            sb.append(aKey);
            if (aDescription != null) {
                sb.append(";desc=\"");
                sb.append(aDescription);
                sb.append("\"");
            }
            sb.append(";dur=");
            sb.append(aTime);

            webResponse.addHeader("Server-Timing", sb.toString());
        }
    }

    public static void refreshPage(AjaxRequestTarget aTarget, Page aPage)
    {
        aPage.forEach(child -> {
            if (child.getOutputMarkupId() /* && child.isVisibleInHierarchy() */) {
                aTarget.add(child);
            }
        });
    }

    /**
     * @param aJsCall
     *            the script to wrap
     * @return given script wrapped in a try-catch block
     */
    public static String wrapInTryCatch(CharSequence aJsCall)
    {
        return " tryCatch(() => {" + aJsCall + "}); ";
    }
}
