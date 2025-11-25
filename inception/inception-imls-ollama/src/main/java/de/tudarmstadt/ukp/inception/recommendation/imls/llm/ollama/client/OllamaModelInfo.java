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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaModelInfo
{
    private @JsonProperty("general.architecture") String architecture;
    private @JsonProperty("general.file_type") Integer fileType;
    private @JsonProperty("general.parameter_count") Long parameterCount;
    private @JsonProperty("general.quantization_version") Integer quantizationVersion;

    private Map<String, Object> properties = new HashMap<>();

    @JsonAnySetter
    public void setProperty(String name, Object value)
    {
        properties.put(name, value);
    }

    public String getArchitecture()
    {
        return architecture;
    }

    public void setArchitecture(String aArchitecture)
    {
        architecture = aArchitecture;
    }

    public Integer getFileType()
    {
        return fileType;
    }

    public void setFileType(Integer aFileType)
    {
        fileType = aFileType;
    }

    public Long getParameterCount()
    {
        return parameterCount;
    }

    public void setParameterCount(Long aParameterCount)
    {
        parameterCount = aParameterCount;
    }

    public Integer getQuantizationVersion()
    {
        return quantizationVersion;
    }

    public void setQuantizationVersion(Integer aQuantizationVersion)
    {
        quantizationVersion = aQuantizationVersion;
    }

    public Integer getContextLength()
    {
        for (var entry : properties.entrySet()) {
            if (entry.getKey().endsWith("context_length")
                    && entry.getValue() instanceof Number val) {
                return val.intValue();
            }
        }

        return null;
    }
}
