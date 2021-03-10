/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.htmleditor;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID.NONE_ID;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil.getUiLabelText;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.METHOD;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectSingle;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
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
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.Strings;
import org.dkpro.core.api.xml.Cas2SaxEvents;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRules;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringRulesTrait;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;
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
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean ColoringService coloringService;

    public HtmlAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider)
    {
        super(aId, aModel, aActionHandler, aCasProvider);

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
            aResponse.render(OnDomReadyHeaderItem.forScript(initAnnotatorJs(vis, storeAdapter)));
        }
    }

    public String renderHtml()
    {
        CAS cas;
        try {
            cas = getCasProvider().get();
        }
        catch (IOException e) {
            handleError("Unable to load data", e);
            return "";
        }

        try {
            if (cas.select(XmlDocument.class).isEmpty()) {
                return renderLegacyHtml(cas);
            }

            return renderHtmlDocumentStructure(cas);
        }
        catch (Exception e) {
            handleError("Unable to render data", e);
            return "";
        }
    }

    private String renderHtmlDocumentStructure(CAS aCas)
        throws IOException, TransformerConfigurationException, CASException, SAXException
    {
        try (Writer out = new StringWriter()) {
            SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
            tf.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
            TransformerHandler th = tf.newTransformerHandler();
            th.getTransformer().setOutputProperty(OMIT_XML_DECLARATION, "yes");
            th.getTransformer().setOutputProperty(METHOD, "xml");
            th.getTransformer().setOutputProperty(INDENT, "no");
            th.setResult(new StreamResult(out));

            // The HtmlDocumentReader only extracts text from the body. So here we need to limit
            // rendering to the body so that the text and the annotations align properly. Also,
            // we wouldn't want to render anything outside the body anyway.
            XmlElement html = selectSingle(aCas.getJCas(), XmlDocument.class).getRoot();
            XmlElement body = html.getChildren().stream() //
                    .filter(e -> e instanceof XmlElement) //
                    .map(e -> (XmlElement) e) //
                    .filter(e -> equalsIgnoreCase("body", e.getQName())) //
                    .findFirst().orElseThrow();

            Cas2SaxEvents serializer = new Cas2SaxEvents(th);
            serializer.process(body);
            return out.toString();
        }
    }

    private String renderLegacyHtml(CAS aCas)
    {
        StringBuilder buf = new StringBuilder(Strings.escapeMarkup(aCas.getDocumentText()));

        List<Node> nodes = new ArrayList<>();
        for (AnnotationFS div : select(aCas, getType(aCas, Div.class))) {
            if (div.getType().getName().equals(Paragraph.class.getName())) {
                Node startNode = new Node();
                startNode.position = div.getBegin();
                startNode.type = "<p>";
                nodes.add(startNode);

                Node endNode = new Node();
                endNode.position = div.getEnd();
                endNode.type = "</p>";
                nodes.add(endNode);
            }
            if (div.getType().getName().equals(Heading.class.getName())) {
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

    private String initAnnotatorJs(WebComponent aContainer, StoreAdapter aAdapter)
    {
        String callbackUrl = aAdapter.getCallbackUrl().toString();
        StringBuilder script = new StringBuilder();
        script.append(
                "var ann = $('#" + aContainer.getMarkupId() + "').annotator({readOnly: false});");
        script.append("ann.annotator('addPlugin', 'Store', {");
        script.append("    prefix: null,");
        script.append("    emulateJSON: true,");
        script.append("    emulateHTTP: true,");
        script.append("    urls: {");
        script.append("        read:    '" + callbackUrl + "',");
        script.append("        create:  '" + callbackUrl + "',");
        script.append("        update:  '" + callbackUrl + "',");
        script.append("        destroy: '" + callbackUrl + "',");
        script.append("        search:  '" + callbackUrl + "',");
        script.append("        select:  '" + callbackUrl + "'");
        script.append("    }");
        script.append("});");
        return WicketUtil.wrapInTryCatch(script.toString());
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        // REC: I didn't find a good way of clearing the annotations, so we do it the hard way:
        // - re-render the entire document
        // - re-add all the annotations
        aTarget.add(vis);
        aTarget.appendJavaScript(initAnnotatorJs(vis, storeAdapter));
    }

    private void handleError(String aMessage, Exception e)
    {
        RequestCycle requestCycle = RequestCycle.get();
        requestCycle.find(AjaxRequestTarget.class)
                .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));

        if (e instanceof AnnotationException) {
            // These are common exceptions happening as part of the user interaction. We do
            // not really need to log their stack trace to the log.
            error(aMessage + ": " + e.getMessage());
            // If debug is enabled, we'll also write the error to the log just in case.
            if (LOG.isDebugEnabled()) {
                LOG.error("{}: {}", aMessage, e.getMessage(), e);
            }
            return;
        }

        LOG.error("{}", aMessage, e);
        error(aMessage);
    }

    private class StoreAdapter
        extends AbstractDefaultAjaxBehavior
    {
        private static final long serialVersionUID = -7919362960963563800L;

        @Override
        protected void respond(AjaxRequestTarget aTarget)
        {
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
                if ("PUT".equals(method) && isNotEmpty(payload)) {
                    update(aTarget, payload);
                }

                // New annotation created
                if ("POST".equals(method) && isNotEmpty(payload)) {
                    create(aTarget, payload);
                }

                // Existing annotation deleted
                if ("DELETE".equals(method) && isNotEmpty(payload)) {
                    delete(aTarget, payload);
                }

                // Existing annotation deleted
                if ("HEAD".equals(method) && isNotEmpty(payload)) {
                    select(aTarget, payload);
                }
            }
            catch (Exception e) {
                handleError("Error", e);
            }
        }

        private void select(AjaxRequestTarget aTarget, String payload)
            throws JsonParseException, JsonMappingException, IOException
        {
            Annotation anno = JSONUtil.getObjectMapper().readValue(payload, Annotation.class);

            if (anno.getRanges().isEmpty()) {
                // Spurious creation event that is to be ignored.
                return;
            }

            VID paramId = VID.parse(anno.getId());

            try {
                CAS cas = getCasProvider().get();

                if (paramId.isSynthetic()) {
                    extensionRegistry.fireAction(getActionHandler(), getModelObject(), aTarget, cas,
                            paramId, "spanOpenDialog");
                    return;
                }

                AnnotationFS fs = selectByAddr(cas, AnnotationFS.class, paramId.getId());

                AnnotatorState state = getModelObject();
                if (state.isSlotArmed()) {
                    // When filling a slot, the current selection is *NOT* changed. The
                    // Span annotation which owns the slot that is being filled remains
                    // selected!
                    getActionHandler().actionFillSlot(aTarget, cas, fs.getBegin(), fs.getEnd(),
                            paramId);
                }
                else {
                    state.getSelection().selectSpan(paramId, cas, fs.getBegin(), fs.getEnd());
                    getActionHandler().actionSelect(aTarget);
                }
            }
            catch (AnnotationException | IOException e) {
                handleError("Unable to select span annotation", e);
            }
        }

        private void create(AjaxRequestTarget aTarget, String payload)
            throws JsonParseException, JsonMappingException, IOException
        {
            Annotation anno = JSONUtil.getObjectMapper().readValue(payload, Annotation.class);

            if (anno.getRanges().isEmpty()) {
                // Spurious creation event that is to be ignored.
                return;
            }

            try {
                int begin = anno.getRanges().get(0).getStartOffset();
                int end = anno.getRanges().get(0).getEndOffset();

                if (!(begin > -1 && end > -1)) {
                    throw new AnnotationException(
                            "Unable to create span annotation: No match was found");
                }

                CAS cas = getCasProvider().get();
                AnnotatorState state = getModelObject();
                if (state.isSlotArmed()) {
                    // When filling a slot, the current selection is *NOT* changed. The
                    // Span annotation which owns the slot that is being filled remains
                    // selected!
                    getActionHandler().actionFillSlot(aTarget, cas, begin, end, NONE_ID);
                }
                else {
                    state.getSelection().selectSpan(cas, begin, end);
                    getActionHandler().actionCreateOrUpdate(aTarget, cas);
                }
            }
            catch (IOException | AnnotationException e) {
                handleError("Unable to create span annotation", e);
            }
        }

        private void delete(AjaxRequestTarget aTarget, String aPayload)
        {
            // We delete annotations via the detail sidebar, so this method is no needed.
        }

        private void update(AjaxRequestTarget aTarget, String aPayload)
        {
            // We update annotations via the detail sidebar, so this method is no needed.
        }

        private void read(AjaxRequestTarget aTarget)
            throws JsonParseException, JsonMappingException, IOException
        {
            CAS cas = getCasProvider().get();

            VDocument vdoc = new VDocument();
            preRenderer.render(vdoc, 0, cas.getDocumentText().length(), cas, getLayersToRender());

            List<Annotation> annotations = new ArrayList<>();

            AnnotatorState state = getModelObject();

            // Render visible (custom) layers
            Map<String[], Queue<String>> colorQueues = new HashMap<>();
            for (AnnotationLayer layer : state.getAllAnnotationLayers()) {
                ColoringStrategy coloringStrategy = coloringService.getStrategy(layer,
                        state.getPreferences(), colorQueues);

                // If the layer is not included in the rendering, then we skip here - but only after
                // we have obtained a coloring strategy for this layer and thus secured the layer
                // color. This ensures that the layer colors do not change depending on the number
                // of visible layers.
                if (!vdoc.getAnnotationLayers().contains(layer)) {
                    continue;
                }

                TypeAdapter typeAdapter = annotationService.getAdapter(layer);

                ColoringRules coloringRules = typeAdapter.getTraits(ColoringRulesTrait.class)
                        .map(ColoringRulesTrait::getColoringRules).orElse(null);

                for (VSpan vspan : vdoc.spans(layer.getId())) {
                    String labelText = getUiLabelText(typeAdapter, vspan);
                    String color = coloringStrategy.getColor(vspan, labelText, coloringRules);

                    Annotation anno = new Annotation();
                    anno.setId(vspan.getVid().toString());
                    anno.setText(labelText);
                    anno.setColor(color);
                    // Looks like the "quote" is not really required for AnnotatorJS to render the
                    // annotation.
                    anno.setQuote("");
                    anno.setRanges(toRanges(vspan.getRanges()));
                    annotations.add(anno);
                }
            }

            String json = toJson(annotations);
            // Since we cannot pass the JSON directly to AnnotatorJS, we attach it to the HTML
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
