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

public enum LearningRecordChangeLocation
    implements PersistentEnum
{
    /**
     * Triggered through the active learning sidebar.
     */
    AL_SIDEBAR("AL_SIDEBAR"),
    /**
     * Triggered through the recommender sidebar.
     */
    REC_SIDEBAR("REC_SIDEBAR"),
    /**
     * Triggered through the main annotation editor.
     */
    MAIN_EDITOR("MAIN_EDITOR"),
    /**
     * Triggered through the annotation detail editor panel.
     */
    DETAIL_EDITOR("DETAIL_EDITOR"),
    /**
     * Triggered automatically by recommender.
     */
    AUTO_ACCEPT("AUTO_ACCEPT");

    private final String id;

    LearningRecordChangeLocation(String aId)
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
