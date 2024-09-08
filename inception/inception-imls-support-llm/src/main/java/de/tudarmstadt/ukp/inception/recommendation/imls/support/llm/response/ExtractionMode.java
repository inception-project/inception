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
package de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;

public enum ExtractionMode
{
    @JsonProperty("response-as-label")
    RESPONSE_AS_LABEL, //

    @JsonProperty("mentions-from-json")
    MENTIONS_FROM_JSON;

    public boolean accepts(AnnotationLayer aLayer)
    {
        if (this == MENTIONS_FROM_JSON) {
            // Mention extraction only makes sense for span layers
            return SpanLayerSupport.TYPE.equals(aLayer.getType());
        }

        return true;
    }
}
