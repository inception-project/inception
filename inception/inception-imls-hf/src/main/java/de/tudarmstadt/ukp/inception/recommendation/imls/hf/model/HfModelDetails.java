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
package de.tudarmstadt.ukp.inception.recommendation.imls.hf.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonIgnoreProperties(ignoreUnknown = true)
public class HfModelDetails
{
    private @JsonProperty("private") boolean privateAccess;
    private @JsonProperty("modelId") String modelId;
    private @JsonProperty("pipeline_tag") String pipelineTag;
    private @JsonProperty("tags") List<String> tags;

    public boolean isPrivateAccess()
    {
        return privateAccess;
    }

    public void setPrivateAccess(boolean aPrivateAccess)
    {
        privateAccess = aPrivateAccess;
    }

    public String getModelId()
    {
        return modelId;
    }

    public void setModelId(String aModelId)
    {
        modelId = aModelId;
    }

    public String getPipelineTag()
    {
        return pipelineTag;
    }

    public void setPipelineTag(String aPipelineTag)
    {
        pipelineTag = aPipelineTag;
    }

    public List<String> getTags()
    {
        return tags;
    }

    public void setTags(List<String> aTags)
    {
        tags = aTags;
    }
}
