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
import de.tudarmstadt.ukp.inception.diam.editor.DiamRequest;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DiamAutoConfig#fillSlotWithExistingAnnotationHandler}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_SLOT_FILLER_HANDLER)
public class FillSlotWithExistingAnnotationHandler
    extends EditorAjaxRequestHandlerBase
{
    public static final String COMMAND = SelectAnnotationHandler.COMMAND;

    @Override
    public String getCommand()
    {
        return COMMAND;
    }

    @Override
    public DefaultAjaxResponse handle(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget,
            Request aRequest)
    {
        try {
            var context = aBehavior.getContext();
            context.getActionHandler().ensureIsEditable();

            var cas = context.getEditorCas();
            var slotFillerId = getVid(aRequest);
            // When filling a slot, the current selection is *NOT* changed. The Span annotation
            // which owns the slot that is being filled remains selected!
            context.getActionHandler().actionFillSlot(aTarget, cas, slotFillerId);

            return new DefaultAjaxResponse(getAction(aRequest));
        }
        catch (Exception e) {
            return handleError("Unable to fill slot with existing annotation", e);
        }
    }

    @Override
    public boolean accepts(DiamRequest aRequest)
    {
        return super.accepts(aRequest) && aRequest.getContext().getAnnotatorState().isSlotArmed()
                && getVid(aRequest.getRequest()).isSet();
    }
}
