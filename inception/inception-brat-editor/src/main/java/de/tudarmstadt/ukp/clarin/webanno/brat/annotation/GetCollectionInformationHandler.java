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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;

import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.VisualOptions;
import de.tudarmstadt.ukp.clarin.webanno.brat.schema.BratSchemaGenerator;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandlerBase;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

class GetCollectionInformationHandler
    extends EditorAjaxRequestHandlerBase
    implements Serializable
{
    private static final long serialVersionUID = 6922527877385787431L;

    private final Component vis;
    private final BratSchemaGenerator bratSchemaGenerator;

    public GetCollectionInformationHandler(Component aVis, BratSchemaGenerator aBratSchemaGenerator)
    {
        vis = aVis;
        bratSchemaGenerator = aBratSchemaGenerator;
    }

    @Override
    public String getCommand()
    {
        return GetCollectionInformationResponse.COMMAND;
    }

    @Override
    public AjaxResponse handle(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget,
            Request aRequest)
    {
        try {
            var result = getCollectionInformation(getAnnotatorState());
            BratRequestUtils.attachResponse(aTarget, vis, result);
            return new DefaultAjaxResponse(getAction(aRequest));
        }
        catch (Exception e) {
            return handleError("Unable to load annotations", e);
        }
    }

    public GetCollectionInformationResponse getCollectionInformation(AnnotatorState aState)
    {
        var info = new GetCollectionInformationResponse();
        if (aState.getProject() != null) {
            info.setEntityTypes(bratSchemaGenerator.buildEntityTypes(aState.getProject(),
                    aState.getAnnotationLayers()));
            info.getVisualOptions().setArcBundle(
                    aState.getPreferences().isCollapseArcs() ? VisualOptions.ARC_BUNDLE_ALL
                            : VisualOptions.ARC_BUNDLE_NONE);
        }
        return info;
    }
}
