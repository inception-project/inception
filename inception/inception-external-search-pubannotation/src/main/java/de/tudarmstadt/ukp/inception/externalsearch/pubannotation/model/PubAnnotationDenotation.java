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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PubAnnotationDenotation
{
    private String id;

    private List<PubAnnotationSpan> spans = Collections.emptyList();

    @JsonProperty("obj")
    private String object;

    public String getId()
    {
        return id;
    }

    public void setId(String aId)
    {
        id = aId;
    }

    public List<PubAnnotationSpan> getSpans()
    {
        return spans;
    }

    public void setSpans(List<PubAnnotationSpan> aSpans)
    {
        spans = aSpans != null ? aSpans : Collections.emptyList();
    }

    /**
     * PubAnnotation's bagging model allows {@code "span"} to be either a single
     * <code>{begin, end}</code> object or an array of such objects (discontinuous span). The
     * single-object form is normalized to a one-element list.
     */
    @JsonProperty("span")
    public void setSpan(Object aValue)
    {
        if (aValue == null) {
            spans = Collections.emptyList();
            return;
        }
        var result = new ArrayList<PubAnnotationSpan>();
        if (aValue instanceof List<?> list) {
            for (var element : list) {
                result.add(toSpan(element));
            }
        }
        else {
            result.add(toSpan(aValue));
        }
        spans = result;
    }

    private static PubAnnotationSpan toSpan(Object aValue)
    {
        if (!(aValue instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(
                    "Expecting span as object with begin/end, got: " + aValue);
        }
        var span = new PubAnnotationSpan();
        span.setBegin(((Number) map.get("begin")).intValue());
        span.setEnd(((Number) map.get("end")).intValue());
        return span;
    }

    public String getObject()
    {
        return object;
    }

    public void setObject(String aObject)
    {
        object = aObject;
    }
}
