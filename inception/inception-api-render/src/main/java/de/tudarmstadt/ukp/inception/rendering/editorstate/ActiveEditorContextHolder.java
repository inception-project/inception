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
package de.tudarmstadt.ukp.inception.rendering.editorstate;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * Holds the currently active {@link DiamContext}.
 */
public interface ActiveEditorContextHolder
{
    /**
     * @return the active context.
     */
    DiamContext getActiveContext();

    /**
     * Set the active context.
     *
     * @param aTarget
     *            the AJAX target, so consumers can be refreshed. May be {@code null} outside a
     *            partial page update.
     * @param aContext
     *            the editor context that became active, or {@code null} to clear.
     */
    void setActiveContext(AjaxRequestTarget aTarget, DiamContext aContext);
}
