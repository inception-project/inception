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
 * Subjects of interest, either annotation or curation.
 */
public enum Mode
    implements PersistentEnum
{
    /**
     * This mode is used when the user is in the annotation page of WebAnno and perform any of the
     * tasks such as creating annotation document, setting annotation preference, exporting file and
     * so on.
     *
     */
    ANNOTATION("annotation"),

    /**
     * This mode is used when the user is in the curation page of WebAnno and perform any of the
     * tasks such as creating curation document, setting annotation preference, exporting file and
     * so on.
     *
     */
    CURATION("curation");

    public String getName()
    {
        return getId();
    }

    @Override
    public String toString()
    {
        return getId();
    }

    Mode(String aId)
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
