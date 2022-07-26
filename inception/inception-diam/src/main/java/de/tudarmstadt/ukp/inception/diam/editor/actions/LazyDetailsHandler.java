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

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.editor.lazydetails.LazyDetailsLookupService;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link DiamAutoConfig#lazyDetailHandler}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_RENDER_HANDLER)
public class LazyDetailsHandler
    extends EditorAjaxRequestHandlerBase
{
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
    public AjaxResponse handle(AjaxRequestTarget aTarget, Request aRequest)
    {
        try {
            AnnotationPageBase page = (AnnotationPageBase) aTarget.getPage();

            CasProvider casProvider = () -> page.getEditorCas();

            // Parse annotation ID if present in request
            final VID paramId = getVid(aRequest);

            AnnotatorState state = page.getModelObject();
            return lazyDetailsLookupService.actionLookupNormData(aRequest.getRequestParameters(),
                    paramId, casProvider, state.getDocument(), state.getUser(),
                    state.getWindowBeginOffset(), state.getWindowEndOffset());
        }
        catch (Exception e) {
            return handleError("Unable to load lazy details", e);
        }
    }
}
