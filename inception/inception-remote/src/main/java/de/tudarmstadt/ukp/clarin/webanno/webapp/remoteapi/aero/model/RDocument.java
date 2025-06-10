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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model;

import static de.tudarmstadt.ukp.inception.remoteapi.SourceDocumentStateUtils.sourceDocumentStateToString;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;

public class RDocument
{
    public long id;
    public String name;
    public String state;

    public RDocument(SourceDocument aDocument)
    {
        id = aDocument.getId();
        name = aDocument.getName();
        state = sourceDocumentStateToString(aDocument.getState());
    }

    public RDocument(long aId, String aName, SourceDocumentState aState)
    {
        super();
        id = aId;
        name = aName;
        state = sourceDocumentStateToString(aState);
    }
}
