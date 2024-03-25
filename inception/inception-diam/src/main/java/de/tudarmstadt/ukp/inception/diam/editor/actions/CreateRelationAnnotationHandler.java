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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DiamAutoConfig#createRelationAnnotationHandler}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_ANNOTATION_HANDLER)
public class CreateRelationAnnotationHandler
    extends EditorAjaxRequestHandlerBase
{
    public static final String COMMAND = "arcOpenDialog";

    @Override
    public String getCommand()
    {
        return COMMAND;
    }

    @Override
    public DefaultAjaxResponse handle(AjaxRequestTarget aTarget, Request aRequest)
    {
        try {
            actionArc(aTarget, aRequest.getRequestParameters());
            return new DefaultAjaxResponse(getAction(aRequest));
        }
        catch (Exception e) {
            return handleError("Unable to create relation annotation", e);
        }
    }

    private void actionArc(AjaxRequestTarget aTarget, IRequestParameters aParams)
        throws IOException, AnnotationException
    {
        var page = getPage();

        var origin = VID.parse(aParams.getParameterValue(PARAM_ORIGIN_SPAN_ID).toString());
        var target = VID.parse(aParams.getParameterValue(PARAM_TARGET_SPAN_ID).toString());

        if (origin.isSynthetic() || target.isSynthetic()) {
            page.error("Relations cannot be created from/to synthetic annotations");
            aTarget.addChildren(page, IFeedback.class);
            return;
        }

        var cas = page.getEditorCas();
        var originFs = selectAnnotationByAddr(cas, origin.getId());
        var targetFs = selectAnnotationByAddr(cas, target.getId());

        var state = page.getModelObject();
        var selection = state.getSelection();
        selection.selectArc(originFs, targetFs);

        page.getAnnotationActionHandler().actionCreateOrUpdate(aTarget, cas);
    }
}
