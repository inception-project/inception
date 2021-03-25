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
package de.tudarmstadt.ukp.inception.recommendation.event;

import org.apache.uima.cas.CAS;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;

public class PredictionsSwitchedEvent
{
    private final IPartialPageRequestHandler requestHandler;
    private final CAS cas;
    private final AnnotatorState state;
    private final VDocument vdoc;

    public PredictionsSwitchedEvent(IPartialPageRequestHandler aTarget, CAS aCas,
            AnnotatorState aState, VDocument aVDoc)
    {
        requestHandler = aTarget;
        cas = aCas;
        state = aState;
        vdoc = aVDoc;
    }

    public IPartialPageRequestHandler getRequestHandler()
    {
        return requestHandler;
    }

    public CAS getCas()
    {
        return cas;
    }

    public AnnotatorState getState()
    {
        return state;
    }

    public VDocument getVDocument()
    {
        return vdoc;
    }
}
