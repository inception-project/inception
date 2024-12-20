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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

//The @JsonSerialize annotation avoid the "InvalidDefinitionException: No serializer found"
//exception without having to set SerializationFeature.FAIL_ON_EMPTY_BEANS
@JsonSerialize
@JsonIgnoreProperties(ignoreUnknown = true)
public class StringMatchingRecommenderTraits
    implements Serializable
{
    private static final long serialVersionUID = -7329491581513178640L;

    private boolean ignoreCase;

    private String excludePattern;

    private int minLength = 3;

    // private int maxLength = 255;

    public boolean isIgnoreCase()
    {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean aIgnoreCase)
    {
        ignoreCase = aIgnoreCase;
    }

    public int getMinLength()
    {
        return minLength;
    }

    public void setMinLength(int aMinLength)
    {
        minLength = aMinLength;
    }

    public String getExcludePattern()
    {
        return excludePattern;
    }

    public void setExcludePattern(String aExcludePattern)
    {
        excludePattern = aExcludePattern;
    }

    // public int getMaxLength()
    // {
    // return maxLength;
    // }
    //
    // public void setMaxLength(int aMaxLength)
    // {
    // maxLength = aMaxLength;
    // }

}
