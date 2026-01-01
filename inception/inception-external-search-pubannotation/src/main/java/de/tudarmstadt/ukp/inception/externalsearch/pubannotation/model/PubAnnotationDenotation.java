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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PubAnnotationDenotation
{
    private String id;

    private PubAnnotationSpan span;

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

    public PubAnnotationSpan getSpan()
    {
        return span;
    }

    public void setSpan(PubAnnotationSpan aSpan)
    {
        span = aSpan;
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
