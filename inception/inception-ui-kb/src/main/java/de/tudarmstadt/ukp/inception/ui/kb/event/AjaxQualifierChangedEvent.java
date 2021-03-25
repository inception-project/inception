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
package de.tudarmstadt.ukp.inception.ui.kb.event;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.wicketstuff.event.annotation.AbstractAjaxAwareEvent;

import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;

public class AjaxQualifierChangedEvent
    extends AbstractAjaxAwareEvent
{
    private Component component;
    private KBQualifier qualifier;
    private boolean deleted;

    public AjaxQualifierChangedEvent(AjaxRequestTarget target, KBQualifier aQualifier,
            Component aComponent, boolean isDeleted)
    {
        super(target);
        qualifier = aQualifier;
        component = aComponent;
        deleted = isDeleted;
    }

    public AjaxQualifierChangedEvent(AjaxRequestTarget target, KBQualifier aQualifier)
    {
        this(target, aQualifier, null, false);
    }

    public KBQualifier getQualifier()
    {
        return qualifier;
    }

    public Component getComponent()
    {
        return component;
    }

    public boolean isDeleted()
    {
        return deleted;
    }
}
