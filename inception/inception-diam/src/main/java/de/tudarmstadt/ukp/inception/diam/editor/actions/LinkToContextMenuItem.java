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
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;
import static java.util.Arrays.asList;

import java.io.IOException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.wicketstuff.jquery.ui.widget.menu.IMenuItem;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.menu.ContextMenuItemContext;
import de.tudarmstadt.ukp.inception.annotation.menu.ContextMenuItemExtension;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaMenuItem;

public class LinkToContextMenuItem
    implements ContextMenuItemExtension
{
    private final AnnotationSchemaService schemaService;
    private final CreateRelationAnnotationHandler createRelationAnnotationHandler;

    public LinkToContextMenuItem(AnnotationSchemaService aSchemaService,
            CreateRelationAnnotationHandler aCreateRelationAnnotationHandler)
    {
        schemaService = aSchemaService;
        createRelationAnnotationHandler = aCreateRelationAnnotationHandler;
    }

    @Override
    public boolean accepts(ContextMenuItemContext aCtx)
    {
        var state = aCtx.page().getModelObject();

        // Origin of the relation needs to be a span
        if (!state.getSelection().isSpan()) {
            return false;
        }

        // Target also needs to be a span
        try {
            var cas = aCtx.page().getEditorCas();
            var ann = selectAnnotationByAddr(cas, aCtx.vid().getId());
            if (ann == null) {
                return false;
            }

            var layer = schemaService.findLayer(state.getProject(), ann);
            if (layer == null) {
                return false;
            }

            return asList(SpanLayerSupport.TYPE, ChainLayerSupport.TYPE).contains(layer.getType());
        }
        catch (IOException e) {
            return false;
        }
    }

    @Override
    public IMenuItem createMenuItem(VID aVid, int aClientX, int aClientY)
    {
        return new LambdaMenuItem("Link to ...", $ -> {
            // Ensure that lambda is serializable
            getApplicationContext() //
                    .getBean(LinkToContextMenuItem.class) //
                    .actionLinkTo($, aVid, aClientX, aClientY);
        });
    }

    private void actionLinkTo(AjaxRequestTarget aTarget, VID paramId, int aClientX, int aClientY)
        throws IOException, AnnotationException
    {
        var page = (AnnotationPageBase) aTarget.getPage();

        var maybeContextMenuLookup = page.getContextMenuLookup();
        if (!maybeContextMenuLookup.isPresent()) {
            return;
        }

        page.ensureIsEditable();

        var state = page.getModelObject();

        if (!state.getSelection().isSpan()) {
            return;
        }

        createRelationAnnotationHandler.actionArc(maybeContextMenuLookup.get(), aTarget,
                state.getSelection().getAnnotation(), paramId, aClientX, aClientY);
    }
}
