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
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.NotEditableException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.CreateRelationAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationAdapter;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.editor.ContextMenuLookup;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.inception.support.lambda.MenuCategoryHeader;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;

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
    private static final List<String> SEGMENTATION_TYPES = asList(Sentence._TypeName,
            Token._TypeName);

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
        var originSpan = VID.parse(aParams.getParameterValue(PARAM_ORIGIN_SPAN_ID).toString());
        var targetSpan = VID.parse(aParams.getParameterValue(PARAM_TARGET_SPAN_ID).toString());

        var cm = aBehavior.getContextMenu();
        var clientX = cm.getClientX().getAsInt();
        var clientY = cm.getClientY().getAsInt();

        actionArc(aBehavior, aTarget, originSpan, targetSpan, clientX, clientY);
    }

    public void actionArc(ContextMenuLookup aBehavior, AjaxRequestTarget aTarget, VID originSpan,
            VID targetSpan, int aClientX, int aClientY)
        throws NotEditableException, IOException, AnnotationException, IllegalPlacementException
    {
        var page = getPage();
        page.ensureIsEditable();

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
            createRelationAnnotation(aBehavior, aTarget, originLayer, originSpan, targetSpan,
                    aClientX, aClientY);
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

        for (var feature : chainAdapter.listFeatures()) {
            if (feature.isRemember()) {
                var value = state.getRememberedArcFeatures().get(feature);
                chainAdapter.setFeatureValue(state.getDocument(), state.getUser().getUsername(),
                        ann, feature, value);
            }
        }

        commitAnnotation(aTarget, page, state, selection);

    }

    private void createRelationAnnotation(ContextMenuLookup aBehavior, AjaxRequestTarget aTarget,
            AnnotationLayer originLayer, VID aOriginSpan, VID aTargetSpan, int aClientX,
            int aClientY)
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
            return;
        }

        var cm = aBehavior.getContextMenu();
        var items = cm.getItemList();
        items.clear();

        items.add(new MenuCategoryHeader("Select layer..."));
        for (var layer : candidateLayers) {
            items.add(new LambdaMenuItem(layer.getUiName(), $ -> {
                // We need to fetch the bean here again because it is not serializable and
                // the AJAX callback here needs to be serializable
                var handler = ApplicationContextProvider.getApplicationContext()
                        .getBean(CreateRelationAnnotationHandler.class);
                handler.createRelationAnnotation($, layer, aOriginSpan, aTargetSpan);
            }));
        }

        cm.onOpen(aTarget, aClientX, aClientY);
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

        if (SEGMENTATION_TYPES.contains(originFs.getType().getName())
                || SEGMENTATION_TYPES.contains(targetFs.getType().getName())) {
            throw new IllegalPlacementException("Cannot create relations on segmentation units.");
        }

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

        for (var feature : relationAdapter.listFeatures()) {
            if (feature.isRemember()) {
                var value = state.getRememberedArcFeatures().get(feature);
                relationAdapter.setFeatureValue(state.getDocument(), state.getUser().getUsername(),
                        ann, feature, value);
            }
        }

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
