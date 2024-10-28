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
package de.tudarmstadt.ukp.inception.annotation.feature.link;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum LinkFeatureDiffMode
{
    /**
     * The link target is considered to be part of the position. Two links that have the same target
     * will be considered to be at the same position. Thus, linking the same target in multiple
     * roles will be considered stacking. Linking different targets in the same role is viable.
     */
    @JsonProperty("include")
    INCLUDE,

    /**
     * The link role is considered to be part of the position. Two links that have the same role but
     * different targets it will be considered stacking.
     */
    @JsonProperty("exclude")
    EXCLUDE;

    public static final LinkFeatureDiffMode DEFAULT_LINK_DIFF_MODE = EXCLUDE;
}
