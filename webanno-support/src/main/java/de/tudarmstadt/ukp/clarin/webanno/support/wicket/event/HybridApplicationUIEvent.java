/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.support.wicket.event;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.cycle.RequestCycle;

/**
 * Events which need to be sent to the UI as well as to the application.
 */
public interface HybridApplicationUIEvent
{
    /**
     * Returns the request target which can be used to update the UI (if the event was triggered
     * in the context of a UI action).
     */
    default AjaxRequestTarget getRequestTarget() {
        return RequestCycle.get().find(AjaxRequestTarget.class).get();
    }
}
