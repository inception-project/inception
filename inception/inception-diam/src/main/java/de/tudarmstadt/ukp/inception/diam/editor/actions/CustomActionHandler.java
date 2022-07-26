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

import static de.tudarmstadt.ukp.inception.schema.adapter.TypeAdapter.decodeTypeName;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;
import org.apache.wicket.util.string.StringValue;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link DiamAutoConfig#customActionHandler}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_ANNOTATION_HANDLER)
public class CustomActionHandler
    extends EditorAjaxRequestHandlerBase
{
    public static final String COMMAND = "doAction";

    private AnnotationSchemaService annotationService;

    public CustomActionHandler(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public String getCommand()
    {
        return COMMAND;
    }

    @Override
    public DefaultAjaxResponse handle(AjaxRequestTarget aTarget, Request aRequest)
    {
        try {
            actionDoAction(aTarget, aRequest, getVid(aRequest));
            return new DefaultAjaxResponse(getAction(aRequest));
        }
        catch (Exception e) {
            return handleError("Unable to perform custom action", e);
        }
    }

    private void actionDoAction(AjaxRequestTarget aTarget, Request aRequest, VID paramId)
        throws AnnotationException, IOException
    {
        StringValue layerParam = aRequest.getRequestParameters().getParameterValue(PARAM_TYPE);

        if (!layerParam.isEmpty()) {
            return;
        }

        AnnotatorState state = ((AnnotationPageBase) aTarget.getPage()).getModelObject();

        long layerId = decodeTypeName(layerParam.toString());
        AnnotationLayer layer = annotationService.getLayer(state.getProject(), layerId)
                .orElseThrow(() -> new AnnotationException("Layer with ID [" + layerId
                        + "] does not exist in project [" + state.getProject().getName() + "]("
                        + state.getProject().getId() + ")"));

        if (StringUtils.isEmpty(layer.getOnClickJavascriptAction())) {
            return;
        }

        CAS cas = ((AnnotationPageBase) aTarget.getPage()).getEditorCas();
        // parse the action
        List<AnnotationFeature> features = annotationService.listSupportedFeatures(layer);
        AnnotationFS anno = ICasUtil.selectAnnotationByAddr(cas, paramId.getId());
        Map<String, Object> functionParams = parse(layer, features, state.getDocument(), anno);
        // define anonymous function, fill the body and immediately execute
        String js = String.format("(function ($PARAM){ %s })(%s)",
                WicketUtil.wrapInTryCatch(layer.getOnClickJavascriptAction()),
                JSONUtil.toJsonString(functionParams));
        aTarget.appendJavaScript(js);
    }

    /**
     * @return String with substituted variables
     */
    private static Map<String, Object> parse(AnnotationLayer aLayer,
            List<AnnotationFeature> aFeatures, SourceDocument aDocument, AnnotationFS aAnnotation)
    {
        Map<String, Object> valuesMap = new HashMap<>();

        // add some defaults
        valuesMap.put("PID", aLayer.getProject().getId());
        valuesMap.put("PNAME", aLayer.getProject().getName());
        valuesMap.put("DOCID", aDocument.getId());
        valuesMap.put("DOCNAME", aDocument.getName());
        valuesMap.put("LAYERNAME", aLayer.getName());

        // add fields from the annotation layer features and use the values from before
        aFeatures.stream().forEach(feat -> {
            if (WebAnnoCasUtil.isPrimitiveFeature(aAnnotation, feat.getName())) {
                Object val = WebAnnoCasUtil.getFeature(aAnnotation, feat.getName());
                if (val != null) {
                    valuesMap.put(feat.getUiName(), String.valueOf(val));
                }
            }
        });

        return valuesMap;
    }
}
