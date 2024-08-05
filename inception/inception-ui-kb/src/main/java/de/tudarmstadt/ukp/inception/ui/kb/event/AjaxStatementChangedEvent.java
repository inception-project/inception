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

import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;

/**
 * An {@code AjaxStatementChangedEvent} serves as an event object sent around whenever a statement
 * is saved/deleted.
 */
public class AjaxStatementChangedEvent
    extends AbstractAjaxAwareEvent
{
    /**
     * Statement editor component of the changed statement.
     */
    private Component component;

    /**
     * The statement in the state that it was before the change.
     */
    private KBStatement statementBeforeChange;

    /**
     * The statement which was changed.
     */
    private KBStatement statement;

    /**
     * Whether the statement should be deleted.
     */
    private boolean deleted;

    public AjaxStatementChangedEvent(AjaxRequestTarget target, KBStatement statement)
    {
        this(target, statement, null, false);
    }

    public AjaxStatementChangedEvent(AjaxRequestTarget target, KBStatement statement,
            KBStatement statementBeforeChange)
    {
        this(target, statement, null, false, statementBeforeChange);
    }

    public AjaxStatementChangedEvent(AjaxRequestTarget target, KBStatement statement,
            Component component, boolean deleted)
    {
        this(target, statement, component, deleted, null);
    }

    public AjaxStatementChangedEvent(AjaxRequestTarget target, KBStatement statement,
            Component component, boolean deleted, KBStatement statementBeforeChange)
    {
        super(target);
        this.statement = statement;
        this.component = component;
        this.deleted = deleted;
        this.statementBeforeChange = statementBeforeChange;
    }

    public KBStatement getStatement()
    {
        return statement;
    }

    /**
     * @return the statement editor component of the changed statement.
     */
    public Component getComponent()
    {
        return component;
    }

    /**
     * @return {@code true} if the statement being changed has been deleted. If {@code false}, there
     *         were only value updates in the statement.
     */
    public boolean isDeleted()
    {
        return deleted;
    }

    /**
     * @return the old statement (before the change)
     */
    public KBStatement getStatementBeforeChange()
    {
        return statementBeforeChange;
    }
}
