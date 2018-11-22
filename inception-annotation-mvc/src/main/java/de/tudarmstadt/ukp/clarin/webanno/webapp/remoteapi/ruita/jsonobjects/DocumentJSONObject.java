/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.RemoteApiController2;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class DocumentJSONObject
    extends JSONOutput
{
    public long id;
    public String name;
    public String state;
    public int tokenCount;

    public DocumentJSONObject(SourceDocument aDocument, JCas aJCas)
    {
        id = aDocument.getId();
        name = aDocument.getName();
        state = RemoteApiController2.sourceDocumentStateToString(aDocument.getState());
        tokenCount = JCasUtil.select(aJCas, Token.class).size();
    }

    public DocumentJSONObject(long aId, String aName, SourceDocumentState aState, int aTokenCount)
    {
        super();
        id = aId;
        name = aName;
        state = RemoteApiController2.sourceDocumentStateToString(aState);
        tokenCount = aTokenCount;
    }
}
