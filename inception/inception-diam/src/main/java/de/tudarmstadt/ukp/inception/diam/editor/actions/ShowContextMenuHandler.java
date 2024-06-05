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

import static de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider.getApplicationContext;

import java.io.IOException;
import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaMenuItem;

@Order(EditorAjaxRequestHandler.PRIO_CONTEXT_MENU)
public class ShowContextMenuHandler
    extends EditorAjaxRequestHandlerBase
    implements Serializable
{
    private static final long serialVersionUID = 2566256640285857435L;

    @Override
    public String getCommand()
    {
        return ACTION_CONTEXT_MENU;
    }

    @Override
    public boolean accepts(Request aRequest)
    {
        var paramId = getVid(aRequest);
        return super.accepts(aRequest) && paramId.isSet() && !paramId.isSynthetic()
                && !paramId.isSlotSet();
    }

    @Override
    public AjaxResponse handle(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget,
            Request aRequest)
    {
        var cm = aBehavior.getContextMenu();
        if (cm == null) {
            return new DefaultAjaxResponse(getAction(aRequest));
        }

        var clientX = cm.getClientX().getAsInt();
        var clientY = cm.getClientY().getAsInt();

        // Need to fetch this here since the handler is not serializable
        var extensionRegistry = getApplicationContext()
                .getBean(AnnotationEditorExtensionRegistry.class);

        try {

            var items = cm.getItemList();
            items.clear();

            var state = getAnnotatorState();

            if (state.getSelection().isSpan()) {
                var vid = getVid(aRequest);
                items.add(new LambdaMenuItem("Link to ...",
                        $ -> actionLinkTo(aBehavior, $, vid, clientX, clientY)));
            }

            extensionRegistry.generateContextMenuItems(items);

            if (!items.isEmpty()) {
                cm.onOpen(aTarget);
            }
        }
        catch (Exception e) {
            handleError("Unable to load data", e);
        }

        return new DefaultAjaxResponse(getAction(aRequest));
    }

    private void actionLinkTo(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget, VID paramId,
            int aClientX, int aClientY)
        throws IOException, AnnotationException
    {
        var page = getPage();
        page.ensureIsEditable();

        var state = getAnnotatorState();

        if (!state.getSelection().isSpan()) {
            return;
        }

        // Need to fetch this here since the handler is not serializable
        var createRelationAnnotationHandler = getApplicationContext()
                .getBean(CreateRelationAnnotationHandler.class);

        createRelationAnnotationHandler.actionArc(aBehavior, aTarget,
                state.getSelection().getAnnotation(), paramId, aClientX, aClientY);
    }
}
