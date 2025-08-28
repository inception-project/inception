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

import static de.tudarmstadt.ukp.inception.diam.editor.actions.CreateSpanAnnotationHandler.getRangeFromRequest;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.inception.annotation.layer.span.api.MoveSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link DiamAutoConfig#moveSpanAnnotationHandler}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_ANNOTATION_HANDLER)
public class MoveSpanAnnotationHandler
    extends EditorAjaxRequestHandlerBase
{
    public static final String COMMAND = "moveSpan";

    private final AnnotationSchemaService annotationService;

    public MoveSpanAnnotationHandler(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

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
            var page = getPage();

            page.ensureIsEditable();

            var cas = page.getEditorCas();
            var vid = getVid(aRequest);
            var state = getAnnotatorState();
            var range = getRangeFromRequest(state, aRequest.getRequestParameters(), cas);
            moveSpan(aTarget, cas, vid, range);
            page.writeEditorCas(cas);
            return new DefaultAjaxResponse(getAction(aRequest));
        }
        catch (Exception e) {
            return handleError("Unable to move span annotation", e);
        }
    }

    private void moveSpan(AjaxRequestTarget aTarget, CAS aCas, VID aVid, Range aRange)
        throws IOException, AnnotationException
    {
        var state = getAnnotatorState();

        var annoFs = ICasUtil.selectAnnotationByAddr(aCas, aVid.getId());

        var adapter = (SpanAdapter) annotationService.findAdapter(state.getProject(), annoFs);

        adapter.handle(MoveSpanAnnotationRequest.builder() //
                .withDocument(state.getDocument(), state.getUser().getUsername(), aCas) //
                .withAnnotation(annoFs) //
                .withRange(aRange.getBegin(), aRange.getEnd()) //
                .build());

        var sel = state.getSelection();
        if (sel.isSet() && sel.getAnnotation().getId() == aVid.getId()) {
            state.getSelection().selectSpan(annoFs);
        }
    }
}
