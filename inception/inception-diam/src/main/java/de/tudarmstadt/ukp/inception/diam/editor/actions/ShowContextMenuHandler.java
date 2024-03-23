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

import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;

import java.io.IOException;
import java.io.Serializable;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Request;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.inception.support.wicket.ContextMenu;

public class ShowContextMenuHandler
    extends EditorAjaxRequestHandlerBase
    implements Serializable
{
    private static final long serialVersionUID = 2566256640285857435L;

    private final AnnotationEditorExtensionRegistry extensionRegistry;
    private final ContextMenu contextMenu;
    private final IModel<AnnotatorState> model;
    private final AnnotationActionHandler actionHandler;
    private final CasProvider casProvider;

    public ShowContextMenuHandler(
            AnnotationEditorExtensionRegistry aAnnotationEditorExtensionRegistry,
            ContextMenu aContextMenu, IModel<AnnotatorState> aState,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider)
    {
        extensionRegistry = aAnnotationEditorExtensionRegistry;
        contextMenu = aContextMenu;
        model = aState;
        actionHandler = aActionHandler;
        casProvider = aCasProvider;
    }

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
    public AjaxResponse handle(AjaxRequestTarget aTarget, Request aRequest)
    {
        try {
            var items = contextMenu.getItemList();
            items.clear();

            if (model.getObject().getSelection().isSpan()) {
                var vid = getVid(aRequest);
                items.add(new LambdaMenuItem("Link to ...",
                        _target -> actionArcRightClick(_target, vid)));
            }

            extensionRegistry.generateContextMenuItems(items);

            if (!items.isEmpty()) {
                contextMenu.onOpen(aTarget);
            }
        }
        catch (Exception e) {
            handleError("Unable to load data", e);
        }

        return new DefaultAjaxResponse(getAction(aRequest));
    }

    private void actionArcRightClick(AjaxRequestTarget aTarget, VID paramId)
        throws IOException, AnnotationException
    {
        var state = model.getObject();

        if (!state.getSelection().isSpan()) {
            return;
        }

        CAS cas;
        try {
            cas = casProvider.get();
        }
        catch (Exception e) {
            handleError("Unable to load data", e);
            return;
        }

        // Currently selected span
        var originFs = selectAnnotationByAddr(cas, state.getSelection().getAnnotation().getId());

        // Target span of the relation
        var targetFs = selectAnnotationByAddr(cas, paramId.getId());

        var selection = state.getSelection();
        selection.selectArc(VID.NONE_ID, originFs, targetFs);

        // Create new annotation
        actionHandler.actionCreateOrUpdate(aTarget, cas);
    }
}
