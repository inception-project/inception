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
package de.tudarmstadt.ukp.inception.workload.matrix.management.support;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;

public class CurationStateSymbolLabel
    extends Label
{
    private static final long serialVersionUID = -4797458000441245430L;

    public CurationStateSymbolLabel(String aId, IModel<SourceDocumentState> aModel)
    {
        super(aId, aModel.map(state -> stateSymbol(state)));
    }

    public CurationStateSymbolLabel(String aId, SourceDocumentState aLabel)
    {
        super(aId, stateSymbol(aLabel));
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
        setEscapeModelStrings(false); // SAFE AS WE ONLY RENDER A CONTROLLED SET OF ICONS
    }

    private static String stateSymbol(SourceDocumentState aDocState)
    {
        switch (aDocState) {
        case CURATION_IN_PROGRESS:
        case CURATION_FINISHED:
            return aDocState.symbol();
        default:
            return SourceDocumentState.NEW.symbol();
        }
    }
}
