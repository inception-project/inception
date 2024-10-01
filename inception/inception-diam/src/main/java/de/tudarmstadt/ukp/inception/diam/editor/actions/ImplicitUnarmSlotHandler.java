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
package de.tudarmstadt.ukp.inception.diam.editor.actions;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

/**
 * Any handlers with a later priority that this one should be called only after a potentially armed
 * slot has been cleared. The handler does the disarming in its {@link #accepts} method as a
 * side-effect.
 * <p>
 * This class is exposed as a Spring Component via {@link DiamAutoConfig#implicitUnarmSlotHandler}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_UNARM_SLOT_HANDLER)
public class ImplicitUnarmSlotHandler
    extends EditorAjaxRequestHandlerBase
{

    @Override
    public String getCommand()
    {
        return null;
    }

    @Override
    public boolean accepts(Request aRequest)
    {
        AnnotatorState state = getAnnotatorState();
        if (state.isSlotArmed()) {
            state.clearArmedSlot();
        }
        return false;
    }

    @Override
    public AjaxResponse handle(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget,
            Request aRequest)
    {
        throw new IllegalStateException("This handler should never handle a request!");
    }
}
