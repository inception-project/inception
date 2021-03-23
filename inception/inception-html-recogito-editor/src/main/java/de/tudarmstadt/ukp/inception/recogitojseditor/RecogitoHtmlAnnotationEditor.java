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
package de.tudarmstadt.ukp.inception.recogitojseditor;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID.NONE_ID;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil.getUiLabelText;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;
import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.METHOD;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.uima.fit.util.JCasUtil.selectSingle;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

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
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.StringResourceStream;
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
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recogitojseditor.model.WebAnnotation;
import de.tudarmstadt.ukp.inception.recogitojseditor.model.WebAnnotationTarget;
import de.tudarmstadt.ukp.inception.recogitojseditor.model.WebAnnotationTextPositionSelector;
import de.tudarmstadt.ukp.inception.recogitojseditor.model.WebAnnotations;
import de.tudarmstadt.ukp.inception.recogitojseditor.resources.RecogitoJsCssResourceReference;
import de.tudarmstadt.ukp.inception.recogitojseditor.resources.RecogitoJsJavascriptResourceReference;

public class RecogitoHtmlAnnotationEditor
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = -3358207848681467993L;
    private static final Logger LOG = LoggerFactory.getLogger(RecogitoHtmlAnnotationEditor.class);

    private Label vis;
    private StoreAdapter storeAdapter;

    private @SpringBean PreRenderer preRenderer;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean ColoringService coloringService;

    public RecogitoHtmlAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
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

        aResponse.render(CssHeaderItem.forReference(RecogitoJsCssResourceReference.get()));
        aResponse.render(
                JavaScriptHeaderItem.forReference(RecogitoJsJavascriptResourceReference.get()));

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

    private String initAnnotatorJs(WebComponent aContainer, StoreAdapter aAdapter)
    {
        String markupId = aContainer.getMarkupId();
        String callbackUrl = aAdapter.getCallbackUrl().toString();
        StringBuilder script = new StringBuilder();
        script.append("(function() {\n");
        script.append("  var r = Recogito.init({\n");
        script.append("    content: document.getElementById('" + markupId + "'),\n");
        script.append("    mode: 'pre'\n");
        script.append("  });\n");
        script.append("\n");
        script.append("  r.on('createAnnotation', function(annotation) { \n");
        script.append("    Wicket.Ajax.ajax({\n");
        script.append("      'm' : 'POST',\n");
        script.append("      'c' : '" + markupId + "',\n");
        script.append("      'u' : '" + callbackUrl + "',\n");
        script.append("      'ep' : {\n");
        script.append("        'action': 'createAnnotation',\n");
        script.append("        'annotation': JSON.stringify(annotation)\n");
        script.append("      }});\n");
        script.append("  \n");
        script.append("  });\n");
        script.append("  r.on('selectAnnotation', function(annotation) { \n");
        script.append("    Wicket.Ajax.ajax({\n");
        script.append("      'm' : 'POST',\n");
        script.append("      'c' : '" + markupId + "',\n");
        script.append("      'u' : '" + callbackUrl + "',\n");
        script.append("      'ep' : {\n");
        script.append("        'action': 'selectAnnotation',\n");
        script.append("        'id': annotation.id\n");
        script.append("      }});\n");
        script.append("  \n");
        script.append("  });\n");
        script.append("  r.loadAnnotations('" + callbackUrl + "');\n");
        script.append("})();");
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
            if (!(getRequest().getContainerRequest() instanceof HttpServletRequest)) {
                return;
            }

            HttpServletRequest request = (HttpServletRequest) getRequest().getContainerRequest();

            LOG.debug("[" + request.getMethod() + "]");

            try {
                // Loading existing annotations
                switch (request.getMethod()) {
                case "GET":
                    read(aTarget);
                    break;
                case "POST": {
                    String action = getRequest().getRequestParameters().getParameterValue("action")
                            .toString();
                    switch (action) {
                    case "selectAnnotation":
                        select(aTarget);
                        break;
                    case "createAnnotation":
                        create(aTarget);
                        break;
                    }
                }
                }
            }
            catch (Exception e) {
                handleError("Error", e);
            }
        }

        private void read(AjaxRequestTarget aTarget)
            throws JsonParseException, JsonMappingException, IOException
        {
            CAS cas = getCasProvider().get();

            VDocument vdoc = new VDocument();
            preRenderer.render(vdoc, 0, cas.getDocumentText().length(), cas, getLayersToRender());

            WebAnnotations annotations = new WebAnnotations();

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

                    WebAnnotation anno = new WebAnnotation();
                    anno.setId(vspan.getVid().toString());
                    anno.setTarget(new ArrayList<>());
                    anno.getTarget()
                            .add(new WebAnnotationTarget(vspan.getRanges().get(0).getBegin(),
                                    vspan.getRanges().get(0).getEnd(), null));
                    annotations.add(anno);
                }
            }

            String json = JSONUtil.toInterpretableJsonString(annotations);

            StringResourceStream resource = new StringResourceStream(json, "application/ld+json");

            ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(resource);
            handler.setFileName("data.json");
            handler.setCacheDuration(Duration.ofSeconds(1));
            handler.setContentDisposition(ContentDisposition.INLINE);

            getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
        }

        private void select(AjaxRequestTarget aTarget)
            throws JsonParseException, JsonMappingException, IOException
        {
            VID paramId = VID
                    .parse(getRequest().getRequestParameters().getParameterValue("id").toString());

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

        private void create(AjaxRequestTarget aTarget)
            throws JsonParseException, JsonMappingException, IOException
        {
            WebAnnotation anno = JSONUtil.fromJsonString(WebAnnotation.class,
                    getRequest().getRequestParameters().getParameterValue("annotation").toString());

            try {
                WebAnnotationTextPositionSelector selector = anno.getTarget().stream() //
                        .filter(t -> isNotEmpty(t.getSelector()))
                        .flatMap(t -> t.getSelector().stream())
                        .filter(s -> s instanceof WebAnnotationTextPositionSelector)
                        .map(s -> (WebAnnotationTextPositionSelector) s).findFirst().get();
                int begin = selector.getStart();
                int end = selector.getEnd();

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

        private List<AnnotationLayer> getLayersToRender()
        {
            AnnotatorState state = getModelObject();
            List<AnnotationLayer> layersToRender = new ArrayList<>();
            for (AnnotationLayer layer : state.getAnnotationLayers()) {
                boolean isSegmentationLayer = layer.getName().equals(Token.class.getName())
                        || layer.getName().equals(Sentence.class.getName());
                boolean isUnsupportedLayer = layer.getType().equals(CHAIN_TYPE)
                        && CURATION == state.getMode();

                if (layer.isEnabled() && !isSegmentationLayer && !isUnsupportedLayer) {
                    layersToRender.add(layer);
                }
            }
            return layersToRender;
        }
    }
}
