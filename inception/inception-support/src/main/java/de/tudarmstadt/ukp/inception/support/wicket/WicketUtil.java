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

import static java.lang.String.format;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.IMetadataContext;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Page;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.PageRequestHandlerTracker;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.response.filter.IResponseFilter;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WicketUtil
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private WicketUtil()
    {
        // No instances
    }

    public static <M extends Serializable> M getMetaData(IMetadataContext<M, ?> aContext,
            MetaDataKey<M> aKey, Supplier<M> aSupplier)
    {
        var value = aContext.getMetaData(aKey);
        if (value == null) {
            value = aSupplier.get();
            aContext.setMetaData(aKey, value);
        }
        return value;
    }

    public static void ajaxFallbackScript(IHeaderResponse aResponse, String aScript)
    {
        RequestCycle.get().find(AjaxRequestTarget.class).ifPresentOrElse(
                target -> target.appendJavaScript(aScript),
                () -> aResponse.render(OnDomReadyHeaderItem.forScript(aScript)));

    }

    /**
     * Focus a component either via the current AJAX request or by adding a focus script as a header
     * item.
     * 
     * @param aResponse
     *            a header response
     * @param aComponent
     *            the component to focus
     */
    public static void ajaxFallbackFocus(IHeaderResponse aResponse, Component aComponent)
    {
        var script = "setTimeout(() => document.getElementById('" + aComponent.getMarkupId()
                + "')?.focus(), 100)";
        ajaxFallbackScript(aResponse, script);
    }

    public static Optional<Page> getPage()
    {
        try {
            var requestCycle = RequestCycle.get();
            var handler = PageRequestHandlerTracker.getLastHandler(requestCycle);
            var page = (Page) handler.getPage();
            return Optional.of(page);
        }
        catch (Exception e) {
            LOG.debug("Unable to get page", e);
        }
        return Optional.empty();
    }

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

        var requestCycle = RequestCycle.get();
        if (requestCycle == null) {
            return;
        }

        var thl = getTimingListener(requestCycle);
        if (thl != null) {
            thl.add(aKey, aDescription, aTime);
        }
    }

    public static void installTimingListener(RequestCycle requestCycle)
    {
        TimingHeaderListener thl = null;
        var i = requestCycle.getListeners().iterator();
        while (i.hasNext()) {
            var listener = i.next();
            if (listener instanceof TimingHeaderListener foundThl) {
                thl = foundThl;
            }
        }

        if (thl == null) {
            thl = new TimingHeaderListener();
            requestCycle.getListeners().add(thl);
        }
    }

    private static TimingHeaderListener getTimingListener(RequestCycle requestCycle)
    {
        TimingHeaderListener thl = null;
        var i = requestCycle.getListeners().iterator();
        while (i.hasNext()) {
            var listener = i.next();
            if (listener instanceof TimingHeaderListener foundThl) {
                thl = foundThl;
            }
        }

        return thl;
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

    private record TimingRecord(String key, String description, long time) {}

    public static final class TimingHeaderListener
        implements IRequestCycleListener
    {
        private List<TimingRecord> records = new ArrayList<>();

        void add(String aKey, String aDescription, long aTime)
        {
            records.add(new TimingRecord(aKey, aDescription, aTime));
        }

        @Override
        public void onRequestHandlerExecuted(RequestCycle aCycle, IRequestHandler aHandler)
        {
            renderTimingHeaders(aCycle);
        }

        private void renderTimingHeaders(RequestCycle aCycle)
        {
            Response response = aCycle.getResponse();
            if (response instanceof WebResponse) {
                var webResponse = (WebResponse) response;

                for (var rec : records) {
                    var sb = new StringBuilder();
                    sb.append(rec.key);
                    if (rec.description != null) {
                        sb.append(";desc=\"");
                        sb.append(rec.description);
                        sb.append("\"");
                    }
                    sb.append(";dur=");
                    sb.append(rec.time);

                    webResponse.addHeader("Server-Timing", sb.toString());
                }
            }
            records.clear();
        }
    }

    public static final class TimingResponseFilter
        implements IResponseFilter
    {

        @Override
        public AppendingStringBuffer filter(AppendingStringBuffer aResponseBuffer)
        {
            var requestCycle = RequestCycle.get();
            if (requestCycle != null) {
                var thl = getTimingListener(requestCycle);
                if (thl != null) {
                    thl.renderTimingHeaders(requestCycle);

                }
            }

            return aResponseBuffer;
        }
    }

    public static void installTimingListeners(Application aApplication)
    {
        // Register a timing listener early in the request cycle so we can buffer timing information
        // inside that listener
        aApplication.getRequestCycleListeners().add(new IRequestCycleListener()
        {
            @Override
            public void onBeginRequest(RequestCycle aCycle)
            {
                WicketUtil.installTimingListener(aCycle);
            }
        });

        // Register a timing listener late in the rendering process such that we can render the
        // timing headers latest then
        aApplication.getRequestCycleSettings()
                .addResponseFilter(new WicketUtil.TimingResponseFilter());
    }

    public static String constructEndpointUrl(String aUrl)
    {
        var contextPath = WebApplication.get().getServletContext().getContextPath();
        var endPointUrl = Url.parse(format("%s%s", contextPath, aUrl));
        return RequestCycle.get().getUrlRenderer().renderFullUrl(endPointUrl);
    }

    public static String constructWsEndpointUrl(String aUrl)
    {
        var contextPath = WebApplication.get().getServletContext().getContextPath();
        var endPointUrl = Url.parse(format("%s%s", contextPath, aUrl));
        endPointUrl.setProtocol("ws");
        return RequestCycle.get().getUrlRenderer().renderFullUrl(endPointUrl);
    }
}
