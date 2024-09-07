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

import static de.tudarmstadt.ukp.inception.rendering.model.Range.rangeClippedToDocument;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.ChainAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapter;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.compact.CompactRangeList;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DiamAutoConfig#createSpanAnnotationHandler}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_ANNOTATION_HANDLER)
public class CreateSpanAnnotationHandler
    extends EditorAjaxRequestHandlerBase
{
    public static final String COMMAND = "spanOpenDialog";

    private final AnnotationSchemaService schemaService;

    public CreateSpanAnnotationHandler(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;
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
            var state = getAnnotatorState();
            var range = getRangeFromRequest(state, aRequest.getRequestParameters(), cas);

            var adapter = schemaService.getAdapter(state.getDefaultAnnotationLayer());

            var request = new CreateSpanAnnotationRequest(state.getDocument(),
                    state.getUser().getUsername(), cas, range.getBegin(), range.getEnd());

            Selection selection;
            if (adapter instanceof SpanAdapter spanAdapter) {
                var ann = spanAdapter.handle(request);
                selection = adapter.select(VID.of(ann), ann);
            }
            else if (adapter instanceof ChainAdapter chainAdapter) {
                var ann = chainAdapter.handle(request);
                selection = adapter.select(VID.of(ann), ann);
            }
            else {
                throw new AnnotationException("Cannot create span annotation on ["
                        + state.getDefaultAnnotationLayer().getUiName() + "] of type ["
                        + state.getDefaultAnnotationLayer().getType() + "]");
            }

            commitAnnotation(aTarget, page, state, selection);

            return new DefaultAjaxResponse(getAction(aRequest));
        }
        catch (Exception e) {
            return handleError("Unable to create span annotation", e);
        }
    }

    /**
     * Extract offset information from the current request. These are either offsets of an existing
     * selected annotations or offsets contained in the request for the creation of a new
     * annotation.
     */
    static Range getRangeFromRequest(AnnotatorState aState, IRequestParameters request, CAS aCas)
        throws IOException
    {
        var offsets = request.getParameterValue(PARAM_OFFSETS).toString();

        var offsetLists = JSONUtil.fromJsonString(CompactRangeList.class, offsets);

        var begin = aState.getWindowBeginOffset() + offsetLists.get(0).getBegin();
        var end = aState.getWindowBeginOffset() + offsetLists.get(offsetLists.size() - 1).getEnd();

        return rangeClippedToDocument(aCas, begin, end);
    }

    private void commitAnnotation(AjaxRequestTarget aTarget, AnnotationPageBase page,
            AnnotatorState state, Selection selection)
        throws IOException, AnnotationException
    {
        state.getSelection().set(selection);
        page.getAnnotationActionHandler().actionSelect(aTarget);
        page.getAnnotationActionHandler().writeEditorCas();
    }
}
