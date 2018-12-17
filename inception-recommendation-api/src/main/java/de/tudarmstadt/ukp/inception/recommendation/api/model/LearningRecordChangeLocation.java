/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import de.tudarmstadt.ukp.clarin.webanno.support.PersistentEnum;

public enum LearningRecordChangeLocation
    implements
    PersistentEnum
{

    /**
     * Triggered through the active learning sidebar.
     */
    AL_SIDEBAR("AL_SIDEBAR"),
    /**
     * Triggered through the main annotation editor.
     */
    MAIN_EDITOR("MAIN_EDITOR"),
    /**
     * Triggered through the annotation detail editor panel.
     */
    DETAIL_EDITOR("DETAIL_EDITOR");

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
