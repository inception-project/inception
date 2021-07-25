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
package de.tudarmstadt.ukp.inception.recogitojseditor.model;

import static com.fasterxml.jackson.annotation.JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.annotation.JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder({ "@context", "id", "type", "motivation", "body", "target" })
public class WebAnnotation
{
    @JsonProperty("@context")
    private String context;

    private String id;

    private String type;

    private String motivation;

    private List<WebAnnotationBodyItem> body;

    @JsonFormat(with = { ACCEPT_SINGLE_VALUE_AS_ARRAY, WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
    private List<WebAnnotationTarget> target;

    public String getContext()
    {
        return context;
    }

    public void setContext(String aContext)
    {
        context = aContext;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String aId)
    {
        id = aId;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
    }

    public List<WebAnnotationBodyItem> getBody()
    {
        return body;
    }

    public void setBody(List<WebAnnotationBodyItem> aBody)
    {
        body = aBody;
    }

    public List<WebAnnotationTarget> getTarget()
    {
        return target;
    }

    public void setTarget(List<WebAnnotationTarget> aTarget)
    {
        target = aTarget;
    }

    public String getMotivation()
    {
        return motivation;
    }

    public void setMotivation(String aMotivation)
    {
        motivation = aMotivation;
    }
}
