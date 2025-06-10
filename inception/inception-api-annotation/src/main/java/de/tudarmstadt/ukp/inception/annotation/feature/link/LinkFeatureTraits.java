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

import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureDiffMode.DEFAULT_LINK_DIFF_MODE;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode.DEFAULT_LINK_MULTIPLICITY_MODE;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode.MULTIPLE_TARGETS_ONE_ROLE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Traits for link features.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkFeatureTraits
    implements Serializable
{
    private static final long serialVersionUID = -8450181605003189055L;

    @JsonProperty("defaultSlots")
    private List<Long> defaultSlots = new ArrayList<>();

    @JsonProperty("enableRoleLabels")
    private boolean enableRoleLabels = true;

    @JsonProperty("compareMode")
    private LinkFeatureMultiplicityMode compareMode = DEFAULT_LINK_MULTIPLICITY_MODE;

    @JsonProperty("diffMode")
    private LinkFeatureDiffMode diffMode = DEFAULT_LINK_DIFF_MODE;

    public LinkFeatureTraits()
    {
        // Nothing to do
    }

    public List<Long> getDefaultSlots()
    {
        return defaultSlots;
    }

    public void setDefaultSlots(List<Long> aDefaultSlots)
    {
        defaultSlots = aDefaultSlots;
    }

    public boolean isEnableRoleLabels()
    {
        return enableRoleLabels;
    }

    public void setEnableRoleLabels(boolean aEnableRoleLabels)
    {
        enableRoleLabels = aEnableRoleLabels;
    }

    public LinkFeatureMultiplicityMode getMultiplicityMode()
    {
        if (!enableRoleLabels) {
            return MULTIPLE_TARGETS_ONE_ROLE;
        }

        return compareMode;
    }

    public void setMultiplicityMode(LinkFeatureMultiplicityMode aCompareMode)
    {
        compareMode = aCompareMode;
    }

    public LinkFeatureDiffMode getDiffMode()
    {
        return diffMode;
    }

    public void setDiffMode(LinkFeatureDiffMode aDiffMode)
    {
        diffMode = aDiffMode;
    }
}
