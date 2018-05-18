/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.ui.kb.event;

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;

/**
 * Event broadcast when a {@link KBProperty} is being selected.
 */
public class AjaxPropertySelectionEvent extends AjaxSelectionEvent {

    boolean redrawConceptandPropertyListPanels;
    
    public AjaxPropertySelectionEvent(AjaxRequestTarget aTarget, KBHandle newSelection)
    {
        this(aTarget, newSelection, false);
    }
    
    public AjaxPropertySelectionEvent(AjaxRequestTarget aTarget, KBHandle newSelection,
            boolean aRedrawConceptandPropertyListPanels)
    {
        super(aTarget, newSelection);
        redrawConceptandPropertyListPanels = aRedrawConceptandPropertyListPanels;
    }

    public boolean isRedrawConceptandPropertyListPanels()
    {
        return redrawConceptandPropertyListPanels;
    }

    public void setRedrawConceptandPropertyListPanels(boolean aRedrawConceptandPropertyListPanels)
    {
        redrawConceptandPropertyListPanels = aRedrawConceptandPropertyListPanels;
    }

    

    

    

}
