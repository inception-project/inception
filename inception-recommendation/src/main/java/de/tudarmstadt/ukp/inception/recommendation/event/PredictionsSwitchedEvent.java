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

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class PredictionsSwitchedEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = 8008652817225383181L;
    private final AnnotatorState annotatorState;

    public PredictionsSwitchedEvent(Object source, AnnotatorState aState)
    {
        super(source);
        annotatorState = aState;
    }

    public AnnotatorState getAnnotatorState()
    {
        return annotatorState;
    }
}
