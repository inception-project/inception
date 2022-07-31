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

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.compact.CompactRange;
import de.tudarmstadt.ukp.inception.diam.model.compact.CompactRangeList;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

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

    @Override
    public String getCommand()
    {
        return COMMAND;
    }

    @Override
    public DefaultAjaxResponse handle(AjaxRequestTarget aTarget, Request aRequest)
    {
        try {
            AnnotationPageBase page = getPage();
            CAS cas = page.getEditorCas();
            actionSpan(aTarget, aRequest.getRequestParameters(), cas);
            return new DefaultAjaxResponse(getAction(aRequest));
        }
        catch (Exception e) {
            return handleError("Unable to create span annotation", e);
        }
    }

    private void actionSpan(AjaxRequestTarget aTarget, IRequestParameters aRequestParameters,
            CAS aCas)
        throws IOException, AnnotationException
    {
        AnnotationPageBase page = (AnnotationPageBase) aTarget.getPage();

        // This is the span the user has marked in the browser in order to create a new slot-filler
        // annotation OR the span of an existing annotation which the user has selected.
        CompactRange userSelectedSpan = getOffsetsFromRequest(aTarget, aRequestParameters, aCas);

        AnnotatorState state = page.getModelObject();
        Selection selection = state.getSelection();

        if (state.isSlotArmed()) {
            // When filling a slot, the current selection is *NOT* changed. The
            // Span annotation which owns the slot that is being filled remains
            // selected!
            page.getAnnotationActionHandler().actionFillSlot(aTarget, aCas,
                    userSelectedSpan.getBegin(), userSelectedSpan.getEnd(), VID.NONE_ID);
        }
        else {
            selection.selectSpan(aCas, userSelectedSpan.getBegin(), userSelectedSpan.getEnd());

            page.getAnnotationActionHandler().actionCreateOrUpdate(aTarget, aCas);
        }
    }

    /**
     * Extract offset information from the current request. These are either offsets of an existing
     * selected annotations or offsets contained in the request for the creation of a new
     * annotation.
     */
    private CompactRange getOffsetsFromRequest(AjaxRequestTarget aTarget,
            IRequestParameters request, CAS aCas)
        throws IOException
    {
        // Create new span annotation - in this case we get the offset information from the
        // request
        String offsets = request.getParameterValue(PARAM_OFFSETS).toString();

        CompactRangeList offsetLists = JSONUtil.getObjectMapper().readValue(offsets,
                CompactRangeList.class);

        AnnotationPageBase page = (AnnotationPageBase) aTarget.getPage();
        int annotationBegin = page.getModelObject().getWindowBeginOffset()
                + offsetLists.get(0).getBegin();
        int annotationEnd = page.getModelObject().getWindowBeginOffset()
                + offsetLists.get(offsetLists.size() - 1).getEnd();
        return new CompactRange(annotationBegin, annotationEnd);
    }
}
