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

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toInterpretableJsonString;

import java.lang.invoke.MethodHandles;

import org.apache.uima.cas.impl.LowLevelException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.editor.lazydetails.LazyDetailsLookupService;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link DiamAutoConfig#lazyDetailHandler}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_RENDER_HANDLER)
public class LazyDetailsHandler
    extends EditorAjaxRequestHandlerBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String COMMAND = "normData";

    private final LazyDetailsLookupService lazyDetailsLookupService;

    public LazyDetailsHandler(LazyDetailsLookupService aLazyDetailsLookupService)
    {
        lazyDetailsLookupService = aLazyDetailsLookupService;
    }

    @Override
    public String getCommand()
    {
        return COMMAND;
    }

    @Override
    public AjaxResponse handle(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget,
            Request aRequest)
    {
        try {
            var page = (AnnotationPageBase) aTarget.getPage();

            CasProvider casProvider = () -> page.getEditorCas();

            // Parse annotation ID if present in request
            var paramId = getVid(aRequest);
            var state = page.getModelObject();
            var details = lazyDetailsLookupService.lookupLazyDetails(
                    aRequest.getRequestParameters(), paramId, casProvider, state.getDocument(),
                    state.getUser(), state.getWindowBeginOffset(), state.getWindowEndOffset());
            attachResponse(aTarget, aRequest, toInterpretableJsonString(details));

            return new DefaultAjaxResponse(COMMAND);
        }
        catch (LowLevelException e) {
            // This can happen when the lazy details are loading while switching between documents.
            // In this case, it is possible that the details for an annotation with an ID from the
            // old document are still being loaded while the backend editor state has already been
            // configured for the new document. If no suitable annotation with the given ID exists
            // in the new document, this exception will be thrown. As part of switching the
            // document, the pop-over will be destroyed and re-created anyway, so in any case, the
            // user should never see this data.
            // So, we ignore this exception.
            return new DefaultAjaxResponse(COMMAND);
        }
        catch (Exception e) {
            return handleError("Unable to load lazy details", e);
        }
    }
}
