/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.recommendation.event;

import org.apache.uima.jcas.JCas;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;

public class AjaxPredictionsSwitchedEvent
{
    private final IPartialPageRequestHandler requestHandler;
    private final JCas jcas;
    private final AnnotatorState state;
    private final VDocument vdoc;

    public AjaxPredictionsSwitchedEvent(IPartialPageRequestHandler aTarget, JCas aJCas,
            AnnotatorState aState, VDocument aVDoc)
    {
        requestHandler = aTarget;
        jcas = aJCas;
        state = aState;
        vdoc = aVDoc;
    }

    public IPartialPageRequestHandler getTarget()
    {
        return requestHandler;
    }

    public JCas getJCas()
    {
        return jcas;
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
