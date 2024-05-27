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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.brat.annotation.RenderType.DIFFERENTIAL;
import static de.tudarmstadt.ukp.clarin.webanno.brat.annotation.RenderType.FULL;
import static de.tudarmstadt.ukp.clarin.webanno.brat.annotation.RenderType.SKIP;
import static java.lang.Math.abs;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flipkart.zjsonpatch.JsonDiff;

import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics;
import de.tudarmstadt.ukp.inception.support.http.ServerTimingWatch;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

class DifferentialRenderingSupport
    implements Serializable
{
    private static final long serialVersionUID = -7029198496087580165L;

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final BratMetrics metrics;

    private transient JsonNode lastRenderedJsonParsed;
    private String lastRenderedJson;
    private int lastRenderedWindowStart = -1;

    public DifferentialRenderingSupport(BratMetrics aMetrics)
    {
        metrics = aMetrics;
    }

    public RenderResult fullRendering(GetDocumentResponse aBratDoc) throws IOException
    {
        try (var watch = new ServerTimingWatch("brat-json", "brat JSON generation (FULL)")) {
            lastRenderedJson = JSONUtil.toInterpretableJsonString(aBratDoc);
            lastRenderedJsonParsed = null;
            metrics.renderComplete(FULL, watch.stop(), lastRenderedJson, null);
            return new RenderResult(FULL, lastRenderedJson);
        }
    }

    public Optional<RenderResult> differentialRendering(GetDocumentResponse aBratDoc)
        throws IOException
    {
        try (var watch = new ServerTimingWatch("brat-json")) {
            ObjectMapper mapper = JSONUtil.getObjectMapper();
            JsonNode current = mapper.valueToTree(aBratDoc);
            String json = JSONUtil.toInterpretableJsonString(current);

            // By default, we do a full rendering...
            RenderType renderType = FULL;
            String responseJson = json;
            JsonNode diff;
            String diffJsonStr = null;

            // Here, we try to balance server CPU load against network load. So if we have a
            // chance of significantly reducing the data sent to the client via a differential
            // update, then we try that. However, if it is pretty obvious that we won't save a
            // lot, then we will not even try. I.e. we apply some heuristics to see if large
            // parts of the editor have changed.
            var windowDisplacement = abs(lastRenderedWindowStart - aBratDoc.getWindowBegin());
            var windowSize = aBratDoc.getWindowEnd() - aBratDoc.getWindowBegin();
            boolean tryDifferentialUpdate = lastRenderedWindowStart >= 0
                    // Check if we did a far scroll or switch pages
                    && windowDisplacement < windowSize / 3;

            if (tryDifferentialUpdate) {
                // ... try to render diff
                JsonNode previous = null;
                try {
                    if (lastRenderedJsonParsed != null) {
                        previous = lastRenderedJsonParsed;
                    }
                    else {
                        previous = lastRenderedJson != null ? mapper.readTree(lastRenderedJson)
                                : null;
                    }
                }
                catch (IOException e) {
                    LOG.error("Unable to generate diff, falling back to full render.", e);
                    // Fall-through
                }

                if (previous != null && current != null) {
                    diff = JsonDiff.asJson(previous, current);
                    diffJsonStr = diff.toString();

                    if (diff instanceof ArrayNode && ((ArrayNode) diff).isEmpty()) {
                        // No difference? Well, don't render at all :)
                        renderType = SKIP;
                    }
                    else if (diffJsonStr.length() < json.length()) {
                        // Only sent a patch if it is smaller than sending the full data. E.g.
                        // when switching pages, the patch usually ends up being twice as large
                        // as the full data.
                        responseJson = diffJsonStr;
                        renderType = DIFFERENTIAL;
                    }

                    // LOG.info("Diff: " + diff);
                    // LOG.info("Full: {} Patch: {} Diff time: {}", json.length(),
                    // diff.length(),
                    // timer);
                }
            }

            // Storing the last rendered JSON as string because JsonNodes are not serializable.
            lastRenderedJson = json;
            lastRenderedJsonParsed = current;
            lastRenderedWindowStart = aBratDoc.getWindowBegin();

            watch.setDescription("brat JSON generation (" + renderType + ")");
            metrics.renderComplete(renderType, watch.stop(), json, diffJsonStr);

            if (SKIP.equals(renderType)) {
                return Optional.empty();
            }

            return Optional.of(new RenderResult(renderType, responseJson));
        }
    }
}
