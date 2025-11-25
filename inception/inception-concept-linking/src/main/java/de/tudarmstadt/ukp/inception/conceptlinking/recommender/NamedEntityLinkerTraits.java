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
package de.tudarmstadt.ukp.inception.conceptlinking.recommender;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize
public class NamedEntityLinkerTraits
    implements Serializable
{
    private static final long serialVersionUID = 4379021097577126023L;

    private boolean emptyCandidateFeatureRequired = true;
    private boolean synchronous = true;
    private boolean includeLinkTargetsInQuery = true;

    public boolean isEmptyCandidateFeatureRequired()
    {
        return emptyCandidateFeatureRequired;
    }

    public void setEmptyCandidateFeatureRequired(boolean aEmptyCandidateFeatureRequired)
    {
        emptyCandidateFeatureRequired = aEmptyCandidateFeatureRequired;
    }

    public boolean isSynchronous()
    {
        return synchronous;
    }

    public void setSynchronous(boolean aSynchronous)
    {
        synchronous = aSynchronous;
    }

    public boolean isIncludeLinkTargetsInQuery()
    {
        return includeLinkTargetsInQuery;
    }

    public void setIncludeLinkTargetsInQuery(boolean aIncludeLinkTargetsInQuery)
    {
        includeLinkTargetsInQuery = aIncludeLinkTargetsInQuery;
    }
}
