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

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link DiamAutoConfig#scrollToHandler}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_ANNOTATION_HANDLER)
public class ScrollToHandler
    extends EditorAjaxRequestHandlerBase
{
    public static final String COMMAND = "scrollTo";

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
            AnnotationPageBase page = getPage();
            CAS cas = page.getEditorCas();

            IRequestParameters requestParameters = aRequest.getRequestParameters();

            if (!requestParameters.getParameterValue(PARAM_OFFSETS).isEmpty()) {
                Range offsets = getRangeFromRequest(getAnnotatorState(), requestParameters, cas);
                page.getAnnotationActionHandler().actionJump(aTarget, offsets.getBegin(),
                        offsets.getEnd());
            }
            else {
                VID vid = VID.parseOptional(
                        requestParameters.getParameterValue(PARAM_ID).toOptionalString());

                if (vid.isSet() && !vid.isSynthetic()) {
                    page.getAnnotationActionHandler().actionJump(aTarget, vid);
                }
            }

            return new DefaultAjaxResponse(getAction(aRequest));
        }
        catch (Exception e) {
            return handleError("Unable to scroll to annotation", e);
        }
    }
}
