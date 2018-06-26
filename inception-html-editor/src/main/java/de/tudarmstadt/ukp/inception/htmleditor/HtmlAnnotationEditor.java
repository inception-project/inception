/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.htmleditor;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Div;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Heading;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model.Annotation;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model.Range;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.resources.AnnotatorJsCssResourceReference;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.resources.AnnotatorJsJavascriptResourceReference;

public class HtmlAnnotationEditor
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = -3358207848681467993L;
    private static final Logger LOG = LoggerFactory.getLogger(HtmlAnnotationEditor.class);

    private Label vis;
    private StoreAdapter storeAdapter;

    private @SpringBean PreRenderer preRenderer;
    private @SpringBean AnnotationSchemaService annotationService;

    public HtmlAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, JCasProvider aJCasProvider)
    {
        super(aId, aModel, aActionHandler, aJCasProvider);

        vis = new Label("vis", LambdaModel.of(this::renderHtml));
        vis.setOutputMarkupId(true);
        vis.setEscapeModelStrings(false);
        add(vis);

        storeAdapter = new StoreAdapter();
        add(storeAdapter);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(CssHeaderItem.forReference(AnnotatorJsCssResourceReference.get()));
        aResponse.render(
                JavaScriptHeaderItem.forReference(AnnotatorJsJavascriptResourceReference.get()));

        if (getModelObject().getDocument() != null) {
            initAnnotatorJs(aResponse, vis, storeAdapter);
            //render(RequestCycle.get().find(AjaxRequestTarget.class));
        }
    }

    public String renderHtml()
    {
        JCas aJCas;
        try {
            aJCas = getJCasProvider().get();
        }
        catch (IOException e) {
            handleError("Unable to load data", e);
            return "";
        }
        
        StringBuilder buf = new StringBuilder(Strings.escapeMarkup(aJCas.getDocumentText()));

        List<Node> nodes = new ArrayList<>();
        for (Div div : select(aJCas, Div.class)) {
            if (div instanceof Paragraph) {
                Node startNode = new Node();
                startNode.position = div.getBegin();
                startNode.type = "<p>";
                nodes.add(startNode);

                Node endNode = new Node();
                endNode.position = div.getEnd();
                endNode.type = "</p>";
                nodes.add(endNode);
            }
            if (div instanceof Heading) {
                Node startNode = new Node();
                startNode.position = div.getBegin();
                startNode.type = "<h1>";
                nodes.add(startNode);

                Node endNode = new Node();
                endNode.position = div.getEnd();
                endNode.type = "</h1>";
                nodes.add(endNode);
            }
        }

        // Sort backwards
        nodes.sort((a, b) -> {
            return b.position - a.position;
        });

        for (Node n : nodes) {
            buf.insert(n.position, n.type);
        }

        return buf.toString();
    }

    private void handleError(String aMessage, Throwable aCause)
    {
        LOG.error(aMessage, aCause);
        error(aMessage + ExceptionUtils.getRootCauseMessage(aCause));
        return;
    }

    private String toJson(Object result)
    {
        String json = "[]";
        try {
            json = JSONUtil.toInterpretableJsonString(result);
        }
        catch (IOException e) {
            error("Unable to produce JSON response " + ":" + ExceptionUtils.getRootCauseMessage(e));
        }
        return json;
    }

    private static void initAnnotatorJs(IHeaderResponse aResponse, WebComponent aContainer,
            StoreAdapter aAdapter)
    {
        String callbackUrl = aAdapter.getCallbackUrl().toString();
        StringBuilder script = new StringBuilder();
        script.append(
                "var ann = $('#" + aContainer.getMarkupId() + "').annotator({readOnly: true});");
        script.append("ann.annotator('addPlugin', 'Store', {");
        script.append("    prefix: null,");
        script.append("    emulateJSON: true,");
        script.append("    emulateHTTP: true,");
        script.append("    urls: {");
        script.append("        read:    '" + callbackUrl + "',");
        script.append("        create:  '" + callbackUrl + "',");
        script.append("        update:  '" + callbackUrl + "',");
        script.append("        destroy: '" + callbackUrl + "',");
        script.append("        search:  '" + callbackUrl + "'");
        script.append("    }");
        script.append("});");
        // script.append("Wicket.$('" + vis.getMarkupId() + "').annotator = ann;");
        aResponse.render(OnDomReadyHeaderItem.forScript(script.toString()));
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        aTarget.add(vis);
    }
    
    private class StoreAdapter
        extends AbstractDefaultAjaxBehavior
    {
        private static final long serialVersionUID = -7919362960963563800L;

        @Override
        protected void respond(AjaxRequestTarget aTarget)
        {
            // We always refresh the feedback panel - only doing this in the case were actually
            // something worth reporting occurs is too much of a hassel...
            aTarget.addChildren(getPage(), IFeedback.class);

            final IRequestParameters reqParams = getRequest().getRequestParameters();

            // We use "emulateHTTP" to get the method as a parameter - this makes it easier to
            // access the method without having to go to the native container request.
            String method = reqParams.getParameterValue("_method").toString();

            // We use "emulateJSON" to get the JSON payload as a parameter - again makes it
            // easier to access the payload without having to go to the native container request.
            String payload = reqParams.getParameterValue("json").toString();

            LOG.debug("[" + method + "]: " + payload);

            try {
                // Loading existing annotations
                if ("GET".equals(method)) {
                    read(aTarget);
                }

                // Update existing annotation
                if ("PUT".equals(method) && StringUtils.isNotEmpty(payload)) {
                    update(aTarget, payload);
                }

                // New annotation created
                if ("POST".equals(method) && StringUtils.isNotEmpty(payload)) {
                    create(aTarget, payload);
                }

                // Existing annotation deleted
                if ("DELETE".equals(method) && StringUtils.isNotEmpty(payload)) {
                    delete(aTarget, payload);
                }
            }
            catch (Exception e) {
                error("Error: " + e.getMessage());
                LOG.error("Error: " + e.getMessage(), e);
            }
        }

        private void create(AjaxRequestTarget aTarget, String payload)
            throws JsonParseException, JsonMappingException, IOException
        {
            Annotation anno = JSONUtil.getObjectMapper().readValue(payload,
                    Annotation.class);

            if (anno.getRanges().isEmpty()) {
                // Spurious creation event that is to be ignored.
                return;
            }

            String json = toJson(anno);
            // Since we cannot pass the JSON directly to Brat, we attach it to the HTML
            // element into which AnnotatorJS governs. In our modified annotator-full.js, we pick it
            // up from there and then pass it on to AnnotatorJS to do the rendering.
            aTarget.prependJavaScript("Wicket.$('" + vis.getMarkupId() + "').temp = " + json + ";");
        }

        private void delete(AjaxRequestTarget aTarget, String aPayload)
        {
            // TODO Auto-generated method stub
        }

        private void update(AjaxRequestTarget aTarget, String aPayload)
        {
            // TODO Auto-generated method stub
        }

        private void read(AjaxRequestTarget aTarget)
            throws JsonParseException, JsonMappingException, IOException
        {
            AnnotatorState aState = getModelObject();
            JCas jcas = getJCasProvider().get();

            VDocument vdoc = new VDocument();
            preRenderer.render(vdoc, aState, jcas, getLayersToRender());

            List<Annotation> annotations = new ArrayList<>();

            // Render visible (custom) layers
            // Map<String[], Queue<String>> colorQueues = new HashMap<>();
            for (AnnotationLayer layer : vdoc.getAnnotationLayers()) {
                // ColoringStrategy coloringStrategy = ColoringStrategy.getBestStrategy(
                // annotationService, layer, aState.getPreferences(), colorQueues);

                TypeAdapter typeAdapter = annotationService.getAdapter(layer);

                for (VSpan vspan : vdoc.spans(layer.getId())) {
                    String bratLabelText = TypeUtil.getUiLabelText(typeAdapter,
                            vspan.getFeatures());

                    Annotation anno = new Annotation();
                    anno.setId(vspan.getVid().toString());
                    anno.setText(bratLabelText);
                    // Looks like the "quote" is not really required for AnnotatorJS to render the
                    // annotation.
                    anno.setQuote("");
                    anno.setRanges(toRanges(vspan.getRanges()));
                    annotations.add(anno);
                }
            }

            String json = toJson(annotations);
            // Since we cannot pass the JSON directly to Brat, we attach it to the HTML
            // element into which AnnotatorJS governs. In our modified annotator-full.js, we pick it
            // up from there and then pass it on to AnnotatorJS to do the rendering.
            aTarget.prependJavaScript("Wicket.$('" + vis.getMarkupId() + "').temp = " + json + ";");
        }

        private List<Range> toRanges(List<VRange> aRanges)
        {
            return aRanges.stream().map(r -> new Range(r.getBegin(), r.getEnd()))
                    .collect(Collectors.toList());
        }

        private List<AnnotationLayer> getLayersToRender()
        {
            AnnotatorState state = getModelObject();
            List<AnnotationLayer> layersToRender = new ArrayList<>();
            for (AnnotationLayer layer : state.getAnnotationLayers()) {
                boolean isSegmentationLayer = layer.getName().equals(Token.class.getName())
                        || layer.getName().equals(Sentence.class.getName());
                boolean isUnsupportedLayer = layer.getType().equals(CHAIN_TYPE)
                        && (state.getMode().equals(Mode.AUTOMATION)
                                || state.getMode().equals(Mode.CORRECTION)
                                || state.getMode().equals(Mode.CURATION));

                if (layer.isEnabled() && !isSegmentationLayer && !isUnsupportedLayer) {
                    layersToRender.add(layer);
                }
            }
            return layersToRender;
        }
    }

    private static class Node
    {
        int position;
        String type;
    }
}
