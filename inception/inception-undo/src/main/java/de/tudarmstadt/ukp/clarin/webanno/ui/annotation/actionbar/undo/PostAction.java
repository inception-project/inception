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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;

public interface PostAction
    extends Serializable
{
    /**
     * @param aHost
     *            the component driving the undo/redo, used for error reporting. This is <b>not</b>
     *            a handle on the editor: the page-wide shortcut panel hosts the action from outside
     *            any editor, so the context must be passed explicitly rather than looked up from
     *            the component hierarchy.
     * @param aContext
     *            the editor the undone/redone action belongs to.
     * @param aTarget
     *            the AJAX target.
     */
    void apply(Component aHost, DiamContext aContext, AjaxRequestTarget aTarget);
}
