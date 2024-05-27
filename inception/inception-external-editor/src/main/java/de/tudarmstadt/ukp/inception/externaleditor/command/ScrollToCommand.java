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
package de.tudarmstadt.ukp.inception.externaleditor.command;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.springframework.core.annotation.Order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.inception.diam.model.compactv2.CompactRange;
import de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@Order(1000)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScrollToCommand
    implements EditorCommand
{
    private static final long serialVersionUID = 1779280309942407825L;

    @JsonProperty("offset")
    private final int offset;

    @JsonProperty("position")
    private final FocusPosition position;

    @JsonProperty("pingRanges")
    private List<CompactRange> pingRanges;

    public ScrollToCommand(int aOffset, FocusPosition aPosition)
    {
        offset = aOffset;
        position = aPosition;
    }

    public void setPingRange(VRange aRange)
    {
        if (aRange == null) {
            pingRanges = null;
            return;
        }

        pingRanges = asList(new CompactRange(aRange.getBegin(), aRange.getEnd()));
    }

    @Override
    public String command(String aEditorVariable)
    {
        try {
            var args = JSONUtil.toJsonString(this);
            return format("e.scrollTo(%s)", args);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
