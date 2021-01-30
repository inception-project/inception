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
package de.tudarmstadt.ukp.inception.externalsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.support.annotation.OffsetSpan;

public class HighlightUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(HighlightUtils.class);

    private static final String HIGHLIGHT_START_TAG = "<em>";

    private static final String HIGHLIGHT_END_TAG = "</em>";

    public static Optional<ExternalSearchHighlight> parseHighlight(String highlight,
            String originalText)
    {
        // remove markers from the highlight
        String highlight_clean = highlight.replace(HIGHLIGHT_START_TAG, "")
                .replace(HIGHLIGHT_END_TAG, "");

        // find the matching highlight offset in the original text
        int highlight_start_index = originalText.indexOf(highlight_clean);

        // find offset to all keywords in the highlight
        // they are enclosed in <em> </em> tags in the highlight
        String highlightTemp = highlight;
        List<OffsetSpan> offsets = new ArrayList<>();
        while (highlightTemp.contains(HIGHLIGHT_START_TAG)) {
            int start = highlight_start_index + highlightTemp.indexOf(HIGHLIGHT_START_TAG);
            highlightTemp = highlightTemp.replaceFirst(HIGHLIGHT_START_TAG, "");
            int end = highlight_start_index + highlightTemp.indexOf(HIGHLIGHT_END_TAG);
            highlightTemp = highlightTemp.replaceFirst(HIGHLIGHT_END_TAG, "");
            offsets.add(new OffsetSpan(start, end));
        }

        if (!offsets.isEmpty()) {
            return Optional.of(new ExternalSearchHighlight(highlight, offsets));
        }
        else {
            LOG.warn("Refusing to create ExternalSearchHighlight for {} because it "
                    + "contains no keyword markers or it is not found in the document " + "text",
                    highlight);
            return Optional.empty();
        }
    }
}
