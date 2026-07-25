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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.wicketstuff.event.annotation.AbstractAjaxAwareEvent;

/**
 * Sent when an operation has changed the repository or full-text index statistics of a knowledge
 * base (e.g. rebuilding the full-text index), so that the components displaying these statistics
 * can refresh themselves.
 */
public class KnowledgeBaseStatisticsChangedEvent
    extends AbstractAjaxAwareEvent
{
    public KnowledgeBaseStatisticsChangedEvent(AjaxRequestTarget aTarget)
    {
        super(aTarget);
    }
}
