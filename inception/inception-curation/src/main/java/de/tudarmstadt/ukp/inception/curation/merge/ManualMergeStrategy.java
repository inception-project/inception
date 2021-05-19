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
package de.tudarmstadt.ukp.inception.curation.merge;

import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationServiceAutoConfiguration#manualMergeStrategy}.
 * </p>
 */
public class ManualMergeStrategy
    implements MergeStrategy
{
    public static final String BEAN_NAME = "manualStrategy";

    private final String UI_NAME = "Manual";

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ManualMergeStrategy)) {
            return false;
        }
        ManualMergeStrategy castOther = (ManualMergeStrategy) other;
        return new EqualsBuilder().append(UI_NAME, castOther.UI_NAME).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(UI_NAME).toHashCode();
    }

    @Override
    public void merge(AnnotatorState aState, CAS aCas, Map<String, CAS> aUserCases,
            boolean aMergeIncomplete)
    {
        // Do nothing
    }

    @Override
    public String getUiName()
    {
        return UI_NAME;
    }
}
