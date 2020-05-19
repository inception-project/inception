/*
 * Copyright 2020
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

package de.tudarmstadt.ukp.inception.app.ui.monitoring.support;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

import java.io.Serializable;

//Helper class for the Filter
public class Filter implements Serializable
{
    //All filter attributes
    private long creationTime;
    private boolean isUserForDocument;
    private SourceDocument document;


    //Default constructor
    public Filter(SourceDocument document)
    {
        this.document = document;
        this.creationTime = document.getCreated().getTime();

    }

    //Getter only required
    public long getCreationTime() {
        return creationTime;
    }

    public boolean isUserForDocument() {
        return isUserForDocument;
    }

    public SourceDocument getDocument() {
        return document;
    }
}
