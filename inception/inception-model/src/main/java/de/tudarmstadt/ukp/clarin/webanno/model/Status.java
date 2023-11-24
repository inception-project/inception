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
package de.tudarmstadt.ukp.clarin.webanno.model;

import de.tudarmstadt.ukp.inception.support.db.PersistentEnum;

/**
 * State of Automation, either, {NOT_STARTED, INPROGRESS, COMPLETED}
 *
 *
 */
public enum Status
    implements PersistentEnum
{
    /**
     * Automation layer settings are created, training documents uploaded but automation is not yet
     * started
     *
     */
    NOT_STARTED("not started"),
    /**
     * Generating training document adding appropriate features, including from other train layer
     */
    GENERATE_TRAIN_DOC("processing train documents..."),
    /**
     * Generating classifier
     */
    GENERATE_CLASSIFIER("generating classifier..."),
    /**
     * predicting annotation documents
     */
    PREDICTION("suggesting annotations..."),
    /**
     * automation process is interrupted due to error
     */
    INTERRUPTED("Process interrupted"),
    /**
     * Automation completed
     */
    COMPLETED("completed");

    public String getName()
    {
        return getId();
    }

    @Override
    public String toString()
    {
        return getId();
    }

    Status(String aId)
    {
        this.id = aId;
    }

    private final String id;

    @Override
    public String getId()
    {
        return id;
    }
}
