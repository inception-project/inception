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
package de.tudarmstadt.ukp.inception.ui.kb;

import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.ui.kb.event.KnowledgeBaseEventHandler;

/**
 * Wicket panel with a built-in {@link KnowledgeBaseEventHandler}.
 */
public class EventListeningPanel extends Panel {
   
    private static final long serialVersionUID = 4489553724413208199L;
    
    protected KnowledgeBaseEventHandler eventHandler;
   
    public EventListeningPanel(String id) {
        super(id);
        commonInit();
    }

    public EventListeningPanel(String id, IModel<?> model) {
        super(id, model);
        commonInit();
    }
    
    private void commonInit() {
        this.eventHandler = new KnowledgeBaseEventHandler();
    }
    
    @Override
    public void onEvent(IEvent<?> event) {
        super.onEvent(event);
        eventHandler.onEvent(event);
    }

}
