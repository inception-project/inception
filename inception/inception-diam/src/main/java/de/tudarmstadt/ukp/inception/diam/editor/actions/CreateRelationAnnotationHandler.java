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

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.ChainAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.CreateRelationAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapter;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaMenuItem;

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

    private final AnnotationSchemaService schemaService;
    private final AnnotationSchemaProperties schemaProperties;

    public CreateRelationAnnotationHandler(AnnotationSchemaService aSchemaService,
            AnnotationSchemaProperties aSchemaProperties)
    {
        schemaService = aSchemaService;
        schemaProperties = aSchemaProperties;
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
            actionArc(aBehavior, aTarget, aRequest.getRequestParameters());
            return new DefaultAjaxResponse(getAction(aRequest));
        }
        catch (Exception e) {
            return handleError("Unable to create relation annotation", e);
        }
    }

    private void actionArc(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget,
            IRequestParameters aParams)
        throws IOException, AnnotationException
    {
        var page = getPage();

        page.ensureIsEditable();

        var originSpan = VID.parse(aParams.getParameterValue(PARAM_ORIGIN_SPAN_ID).toString());
        var targetSpan = VID.parse(aParams.getParameterValue(PARAM_TARGET_SPAN_ID).toString());

        if (originSpan.isSynthetic() || targetSpan.isSynthetic()) {
            page.error("Relations cannot be created from/to synthetic annotations");
            aTarget.addChildren(page, IFeedback.class);
            return;
        }

        var cas = page.getEditorCas();
        var state = getAnnotatorState();
        var originFs = selectAnnotationByAddr(cas, originSpan.getId());
        var originLayer = schemaService.findLayer(state.getProject(), originFs);
        var originAdapter = schemaService.getAdapter(originLayer);

        if (originAdapter instanceof ChainAdapter chainAdapter) {
            createChainLink(chainAdapter, aTarget, originSpan, targetSpan);
        }
        else {
            createRelationAnnotation(aBehavior, aTarget, originLayer, originSpan, targetSpan);
        }
    }

    private void createChainLink(ChainAdapter chainAdapter, AjaxRequestTarget aTarget,
            VID aOriginSpan, VID aTargetSpan)
        throws IOException, AnnotationException
    {
        var page = getPage();
        var cas = page.getEditorCas();
        var originFs = selectAnnotationByAddr(cas, aOriginSpan.getId());
        var targetFs = selectAnnotationByAddr(cas, aTargetSpan.getId());

        if (!originFs.getType().equals(targetFs.getType())) {
            throw new IllegalPlacementException(
                    "Cannot create links between chain elements on different layers");
        }

        var state = getAnnotatorState();
        var request = new CreateRelationAnnotationRequest(state.getDocument(),
                state.getUser().getUsername(), cas, originFs, targetFs);

        var ann = chainAdapter.handle(request);
        var selection = chainAdapter.selectLink(ann);

        commitAnnotation(aTarget, page, state, selection);

    }

    private void createRelationAnnotation(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget,
            AnnotationLayer originLayer, VID aOriginSpan, VID aTargetSpan)
        throws AnnotationException, IllegalPlacementException, IOException
    {
        var candidateLayers = schemaService.getRelationLayersFor(originLayer);
        if (candidateLayers.isEmpty()) {
            throw new IllegalPlacementException(
                    "There are no relation layers that can be created between these endpoints");
        }

        if (candidateLayers.size() == 1) {
            var relationLayer = candidateLayers.get(0);
            createRelationAnnotation(aTarget, relationLayer, aOriginSpan, aTargetSpan);
        }

        var cm = aBehavior.getContextMenu();
        var items = cm.getItemList();
        items.clear();

        for (var layer : candidateLayers) {
            items.add(new LambdaMenuItem(layer.getUiName(),
                    $ -> createRelationAnnotation($, layer, aOriginSpan, aTargetSpan)));
        }

        cm.onOpen(aTarget);
    }

    private void createRelationAnnotation(AjaxRequestTarget aTarget, AnnotationLayer relationLayer,
            VID origin, VID target)
        throws AnnotationException, IOException
    {
        var state = getAnnotatorState();

        var page = getPage();
        var cas = page.getEditorCas();
        var originFs = selectAnnotationByAddr(cas, origin.getId());
        var targetFs = selectAnnotationByAddr(cas, target.getId());

        if (!schemaProperties.isCrossLayerRelationsEnabled()
                && !originFs.getType().equals(targetFs.getType())) {
            throw new IllegalPlacementException(
                    "Cannot create relation between spans on different layers");
        }

        var request = new CreateRelationAnnotationRequest(state.getDocument(),
                state.getUser().getUsername(), cas, originFs, targetFs);
        var relationAdapter = (RelationAdapter) schemaService.getAdapter(relationLayer);
        var ann = relationAdapter.handle(request);
        var selection = relationAdapter.select(VID.of(ann), ann);

        commitAnnotation(aTarget, page, state, selection);
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
