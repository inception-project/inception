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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation;

import java.lang.invoke.MethodHandles;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.documents.AnnotationSessionService;

/**
 * Tracks active annotation time per browser tab. Each tab instance stores its own
 * {@link #currentSessionId} so timer reports and close signals are always routed to the correct
 * {@link de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSession}.
 */
class AnnotationTimeTrackingBehavior
    extends AbstractDefaultAjaxBehavior
{
    private static final long serialVersionUID = -3928254639249427891L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long IDLE_TIMEOUT_MS = 2 * 60 * 1000;
    private static final long REPORT_INTERVAL_MS = 30 * 1000;
    private static final long MAX_DELTA_MS = 60 * 1000;

    private final AnnotationSessionService annotationSessionService;

    /** Session ID for this specific tab. Null until a document is opened. */
    private Long currentSessionId;

    AnnotationTimeTrackingBehavior(AnnotationSessionService aAnnotationSessionService)
    {
        annotationSessionService = aAnnotationSessionService;
    }

    void setCurrentSessionId(Long aSessionId)
    {
        currentSessionId = aSessionId;
    }

    @Override
    public void renderHead(Component aComponent, IHeaderResponse aResponse)
    {
        super.renderHead(aComponent, aResponse);

        // Always update the URL so it stays fresh after re-renders.
        aResponse.render(JavaScriptHeaderItem.forScript(
                "window.__annTimeTrackerUrl = '" + getCallbackUrl() + "';", null));

        // @formatter:off
        var js = String.join("\n",
            "(function() {",
            "  if (window.__annTimeTracker) return;",
            "  window.__annTimeTracker = true;",
            "",
            "  var IDLE_TIMEOUT = " + IDLE_TIMEOUT_MS + ";",
            "  var REPORT_INTERVAL = " + REPORT_INTERVAL_MS + ";",
            "  var active = true;",
            "  var accumulatedMs = 0;",
            "  var lastTick = Date.now();",
            "  var idleTimer = null;",
            "",
            "  function resetIdle() {",
            "    if (!active) { active = true; lastTick = Date.now(); }",
            "    clearTimeout(idleTimer);",
            "    idleTimer = setTimeout(function() { active = false; }, IDLE_TIMEOUT);",
            "  }",
            "",
            "  function tick() {",
            "    if (active) { var now = Date.now(); accumulatedMs += (now - lastTick); lastTick = now; }",
            "    else { lastTick = Date.now(); }",
            "  }",
            "",
            "  function report() {",
            "    tick();",
            "    if (accumulatedMs > 0) {",
            "      var delta = accumulatedMs; accumulatedMs = 0;",
            "      Wicket.Ajax.get({ u: window.__annTimeTrackerUrl + '&elapsedMs=' + delta });",
            "    }",
            "  }",
            "",
            "  document.addEventListener('visibilitychange', function() {",
            "    if (document.hidden) { tick(); active = false; }",
            "    else { active = true; lastTick = Date.now(); resetIdle(); }",
            "  });",
            "",
            "  window.addEventListener('beforeunload', function() {",
            "    tick();",
            "    var url = window.__annTimeTrackerUrl + '&close=1'",
            "      + (accumulatedMs > 0 ? '&elapsedMs=' + accumulatedMs : '');",
            "    navigator.sendBeacon(url);",
            "    accumulatedMs = 0;",
            "  });",
            "",
            "  ['mousemove','keydown','scroll','touchstart'].forEach(function(evt) {",
            "    document.addEventListener(evt, resetIdle, { passive: true });",
            "  });",
            "",
            "  resetIdle();",
            "  setInterval(report, REPORT_INTERVAL);",
            "})();"
        );
        // @formatter:on

        aResponse.render(JavaScriptHeaderItem.forScript(js, "ann-time-tracker"));
    }

    @Override
    protected void respond(AjaxRequestTarget aTarget)
    {
        if (currentSessionId == null) {
            return;
        }

        var params = RequestCycle.get().getRequest().getRequestParameters();

        var elapsedParam = params.getParameterValue("elapsedMs");
        if (!elapsedParam.isNull() && !elapsedParam.isEmpty()) {
            try {
                var delta = Math.min(elapsedParam.toLong(), MAX_DELTA_MS);
                if (delta > 0) {
                    annotationSessionService.addActiveTime(currentSessionId, delta);
                }
            }
            catch (Exception e) {
                LOG.warn("Failed to record active time for session [{}]", currentSessionId, e);
            }
        }

        var closeParam = params.getParameterValue("close");
        if ("1".equals(closeParam.toString())) {
            try {
                annotationSessionService.closeSession(currentSessionId);
            }
            catch (Exception e) {
                LOG.warn("Failed to close session [{}]", currentSessionId, e);
            }
        }
    }
}
