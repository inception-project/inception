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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import de.tudarmstadt.ukp.inception.support.db.PersistentEnum;

public enum LearningRecordUserAction
    implements PersistentEnum
{
    /**
     * Rejected suggestion.
     */
    REJECTED("rejected"),
    /**
     * Accepted suggestion.
     */
    ACCEPTED("accepted"),
    /**
     * Skipped suggestion.
     */
    SKIPPED("skipped"),
    /**
     * Suggestion offered to the user by an AL strategy.
     * 
     * @deprecated Records of this type are no longer generated. Look for
     *             {@code ActiveLearningSuggestionOfferedEvent} in the action log instead.
     */
    SHOWN("shown"),
    /**
     * Suggestion corrected by the user via the AL sidebar.
     */
    CORRECTED("corrected");

    private final String id;

    LearningRecordUserAction(String aId)
    {
        id = aId;
    }

    public String getName()
    {
        return getId();
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public String toString()
    {
        return getId();
    }
}
