/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getAdapter;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import com.googlecode.wicket.jquery.ui.resource.JQueryUIResourceReference;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.OffsetsList;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.ArcOpenDialogResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.LoadConfResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.SpanOpenDialogResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.WhoamiResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAjaxResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAnnotationLogResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAnnotatorUiResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratConfigurationResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratDispatcherResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratUrlMonitorResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratUtilResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratVisualizerResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratVisualizerUiResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQueryJsonResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQuerySvgDomResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQuerySvgResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.WebfontResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.support.DefaultFocusBehavior;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Brat annotator component.
 *
 * @author Richard Eckart de Castilho
 * @author Seid Muhie Yimam
 * @author Andreas Straninger
 */
public class BratAnnotator
    extends Panel
{
    private static final Log LOG = LogFactory.getLog(BratAnnotator.class);
    private static final long serialVersionUID = -1537506294440056609L;

    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ARC_ID = "arcId";
    private static final String PARAM_ID = "id";
    private static final String PARAM_OFFSETS = "offsets";
    private static final String PARAM_SPAN_TEXT = "spanText";
    private static final String PARAM_TARGET_SPAN_ID = "targetSpanId";
    private static final String PARAM_ORIGIN_SPAN_ID = "originSpanId";
    private static final String PARAM_TARGET_TYPE = "targetType";
    private static final String PARAM_ORIGIN_TYPE = "originType";

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;
    @SpringBean(name = "documentRepository")
    private RepositoryService repository;
    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    private WebMarkupContainer vis;
    private Label selectedTextLabel;
    private DropDownChoice<AnnotationLayer> layers;
    private AnnotationFeatureForm annotationFeatureForm;
    private RefreshingView<FeatureValueModel> featureValues;
    private WebMarkupContainer wmc;
    private AjaxButton annotateButton;
    private AjaxSubmitLink deleteButton;
    private AjaxSubmitLink reverseButton;

    private AbstractAjaxBehavior controller;
    private String collection = "";
    private String selectedSpanText = "", offsets;
    private Integer originSpanId, targetSpanId;
    private String originSpanType = null, targetSpanType = null;
    private boolean isRelationAnno;
    private int beginOffset;
    private int endOffset;
    private int selectedAnnotationId = -1;
    private List<AnnotationLayer> annotationLayers = new ArrayList<AnnotationLayer>();
    private AnnotationLayer selectedAnnotationLayer;
    private Map<AnnotationFeature, String> selectedFeatureValues = new HashMap<AnnotationFeature, String>();

    private IModel<AnnotationLayer> layersModel;
    private Model<String> selectedTagModel;
    private List<IModel<FeatureValueModel>> featuresModel;
    private List<IModel<String>> featureValueModels;

    /**
     * Data models for {@link BratAnnotator}
     *
     * @param aModel
     *            the model.
     */
    public void setModel(IModel<BratAnnotatorModel> aModel)
    {
        setDefaultModel(aModel);
    }

    public void setModelObject(BratAnnotatorModel aModel)
    {
        setDefaultModelObject(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<BratAnnotatorModel> getModel()
    {
        return (IModel<BratAnnotatorModel>) getDefaultModel();
    }

    public BratAnnotatorModel getModelObject()
    {
        return (BratAnnotatorModel) getDefaultModelObject();
    }

    public BratAnnotator(String id, IModel<BratAnnotatorModel> aModel)
    {
        super(id, aModel);

        // Allow AJAX updates.
        setOutputMarkupId(true);

        // The annotator is invisible when no document has been selected. Make sure that we can
        // make it visible via AJAX once the document has been selected.
        setOutputMarkupPlaceholderTag(true);

        if (getModelObject().getDocument() != null) {
            collection = "#" + getModelObject().getProject().getName() + "/";
        }

        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);

        controller = new AbstractDefaultAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void respond(AjaxRequestTarget aTarget)
            {
                final IRequestParameters request = getRequest().getPostParameters();

                Object result = null;

                BratAjaxCasController controller = new BratAjaxCasController(repository,
                        annotationService);

                final String action = request.getParameterValue(PARAM_ACTION).toString();

                try {
                    LOG.info("AJAX-RPC CALLED: [" + action + "]");

                    if (action.equals(WhoamiResponse.COMMAND)) {
                        result = controller.whoami();
                    }
                    else if (action.equals(SpanOpenDialogResponse.COMMAND)) {
                        isRelationAnno = false;
                        JCas jCas = getCas(getModelObject());
                        if (request.getParameterValue(PARAM_ID).toString() == null) {
                            selectedAnnotationId = -1;
                            setLayerAndFeatureModels(jCas);
                        }
                        else {
                            selectedAnnotationId = request.getParameterValue(PARAM_ID).toInt();
                            setLayerAndFeatureModels(jCas);
                        }

                        offsets = request.getParameterValue(PARAM_OFFSETS).toString();
                        OffsetsList offsetLists = jsonConverter.getObjectMapper().readValue(
                                offsets, OffsetsList.class);

                        if (selectedAnnotationId == -1) {
                            Sentence sentence = BratAjaxCasUtil.selectSentenceAt(jCas,
                                    getModelObject().getSentenceBeginOffset(), getModelObject()
                                            .getSentenceEndOffset());
                            beginOffset = sentence.getBegin() + offsetLists.get(0).getBegin();
                            endOffset = sentence.getBegin()
                                    + offsetLists.get(offsetLists.size() - 1).getEnd();
                        }

                        // get the begin/end from the annotation, no need to re-calculate
                        else {
                            AnnotationFS fs = BratAjaxCasUtil.selectByAddr(jCas,
                                    selectedAnnotationId);
                            beginOffset = fs.getBegin();
                            endOffset = fs.getEnd();
                        }

                        selectedSpanText = request.getParameterValue(PARAM_SPAN_TEXT).toString();

                        if (BratAnnotatorUtility.isDocumentFinished(repository, getModelObject())) {
                            error("This document is already closed. Please ask your project "
                                    + "manager to re-open it via the Montoring page");
                        }
                        result = new SpanOpenDialogResponse();
                    }
                    else if (action.equals(ArcOpenDialogResponse.COMMAND)) {
                        JCas jCas = getCas(getModelObject());
                        isRelationAnno = true;

                        originSpanType = request.getParameterValue(PARAM_ORIGIN_TYPE).toString();
                        originSpanId = request.getParameterValue(PARAM_ORIGIN_SPAN_ID).toInteger();
                        targetSpanType = request.getParameterValue(PARAM_TARGET_TYPE).toString();
                        targetSpanId = request.getParameterValue(PARAM_TARGET_SPAN_ID).toInteger();

                        if (request.getParameterValue(PARAM_ARC_ID).toString() == null) {
                            selectedAnnotationId = -1;
                            setLayerAndFeatureModels(jCas);
                        }
                        else {
                            selectedAnnotationId = request.getParameterValue(PARAM_ARC_ID).toInt();
                            setLayerAndFeatureModels(jCas);
                        }

                        if (BratAnnotatorUtility.isDocumentFinished(repository, getModelObject())) {
                            error("This document is already closed. Please ask admin to re-open");
                        }
                        result = new ArcOpenDialogResponse();
                    }
                    else if (action.equals(LoadConfResponse.COMMAND)) {
                        result = controller.loadConf();
                    }
                    else if (action.equals(GetCollectionInformationResponse.COMMAND)) {
                        if (getModelObject().getProject() != null) {
                            result = controller.getCollectionInformation(getModelObject()
                                    .getAnnotationLayers());
                        }
                        else {
                            result = new GetCollectionInformationResponse();
                        }
                    }
                    else if (action.equals(GetDocumentResponse.COMMAND)) {
                        if (getModelObject().getProject() != null) {
                            result = controller.getDocumentResponse(getModelObject(), 0,
                                    getCas(getModelObject()), true);
                        }
                        else {
                            result = new GetDocumentResponse();
                        }
                    }

                    LOG.info("AJAX-RPC DONE: [" + action + "]");
                }
                catch (ClassNotFoundException e) {
                    error("Invalid reader: " + e.getMessage());
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCauseMessage(e));
                }
                catch (IOException e) {
                    error(e.getMessage());
                }

                // Serialize updated document to JSON
                if (result == null) {
                    LOG.warn("AJAX-RPC: Action [" + action + "] produced no result!");
                }
                else {
                    String json = toJson(result);
                    // Since we cannot pass the JSON directly to Brat, we attach it to the HTML
                    // element into which BRAT renders the SVG. In our modified ajax.js, we pick it
                    // up from there and then pass it on to BRAT to do the rendering.
                    aTarget.prependJavaScript("Wicket.$('" + vis.getMarkupId() + "').temp = "
                            + json + ";");
                }
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                listAnnotationLayers();
                aTarget.add(annotationFeatureForm);
            }
        };

        add(vis);
        add(controller);
        annotationFeatureForm = new AnnotationFeatureForm("annotationFeatureForm", getModelObject());
        annotationFeatureForm.setOutputMarkupId(true);
        add(annotationFeatureForm);
    }

    private class AnnotationFeatureForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -4104665452144589457L;

        public AnnotationFeatureForm(String id, BratAnnotatorModel bratAnnotatorModel)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            if (annotationLayers.size() == 0) {
                selectedAnnotationLayer = new AnnotationLayer();
            }
            else if (selectedAnnotationLayer == null) {
                if (bratAnnotatorModel.getRememberedSpanLayer() == null) {
                    selectedAnnotationLayer = annotationLayers.get(0);
                }
                else {
                    selectedAnnotationLayer = bratAnnotatorModel.getRememberedSpanLayer();
                }
            }
            selectedTextLabel = new Label("selectedTextLabel",
                    new LoadableDetachableModel<String>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected String load()
                        {
                            return selectedSpanText;
                        }
                    });
            selectedTextLabel.setOutputMarkupId(true);
            add(selectedTextLabel);

            layersModel = new LoadableDetachableModel<AnnotationLayer>()
            {
                private static final long serialVersionUID = -6629150412846045592L;

                @Override
                public AnnotationLayer load()
                {

                    return selectedAnnotationLayer;
                }
            };
            layers = (DropDownChoice<AnnotationLayer>) new DropDownChoice<AnnotationLayer>(
                    "layers", layersModel, annotationLayers)
            {
                private static final long serialVersionUID = -1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    // at the moment we allow one layer per relation annotation (first
                    // source/target span layers should be selected!)
                    setNullValid(!isRelationAnno);

                }
            };
            layers.setOutputMarkupId(true);
            layers.setChoiceRenderer(new ChoiceRenderer<AnnotationLayer>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getDisplayValue(AnnotationLayer aObject)
                {
                    return aObject.getUiName();
                }
            });
            layers.add(new AjaxFormComponentUpdatingBehavior("onchange")
            {

                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    featuresModel = new ArrayList<IModel<FeatureValueModel>>();
                    featureValueModels = new ArrayList<IModel<String>>();
                    selectedAnnotationLayer = layers.getModelObject();
                    for (AnnotationFeature feature : annotationService
                            .listAnnotationFeature(selectedAnnotationLayer)) {

                        if (!feature.isEnabled()) {
                            continue;
                        }
                        if (selectedAnnotationLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)
                                && feature.getName().equals(
                                        WebAnnoConst.COREFERENCE_RELATION_FEATURE)) {
                            continue;
                        }
                        IModel<FeatureValueModel> featureModel = new Model<FeatureValueModel>();

                        FeatureValueModel featureValue = new FeatureValueModel();
                        featureValue.feature = feature;
                        featureModel.setObject(featureValue);
                        featuresModel.add(featureModel);
                        IModel<String> tagModel = new LoadableDetachableModel<String>()
                        {
                            private static final long serialVersionUID = -6629150412846045592L;

                            @Override
                            public String load()
                            {

                                return "";
                            }
                        };
                        if (selectedFeatureValues.containsKey(feature)) {
                            tagModel.setObject(selectedFeatureValues.get(feature));
                        }
                        featureValueModels.add(tagModel);
                    }
                    aTarget.add(wmc);
                    aTarget.add(annotateButton);
                }
            });
            add(layers);

            featuresModel = new ArrayList<IModel<FeatureValueModel>>();
            featureValueModels = new ArrayList<IModel<String>>();

            updateFeaturesModel();

            wmc = new WebMarkupContainer("wmc");
            wmc.setOutputMarkupId(true);
            add(wmc.add(featureValues = new RefreshingView<FeatureValueModel>("featureValues")
            {
                private static final long serialVersionUID = -8359786805333207043L;

                @Override
                protected void populateItem(final Item<FeatureValueModel> item)
                {
                    FeatureValueModel featureValue = item.getModelObject();
                    AnnotationFeature feature = featureValue.feature;

                    String featureLabel = feature.getUiName();
                    if (feature.getTagset() != null) {
                        featureLabel += " (" + feature.getTagset().getName() + ")";
                    }
                    item.add(new Label("feature", featureLabel));

                    if (feature.getTagset() != null) {
                        List<Tag> tagset = new ArrayList<Tag>();
                        if (feature.getTagset() != null) {
                            tagset.addAll(annotationService.listTags(feature.getTagset()));
                        }

                        ComboBox<Tag> featureValueCombo = new ComboBox<Tag>("tag",
                                featureValueModels.get(item.getIndex()), tagset,
                                new com.googlecode.wicket.kendo.ui.renderer.ChoiceRenderer<Tag>(
                                        "name"));
                        if (item.getIndex() == 0) {
                            // Put focus on first feature
                            featureValueCombo.add(new DefaultFocusBehavior());
                        }
                        item.add(featureValueCombo);
                    }
                    else {
                        TextField<String> featureTextField = new TextField<String>("tag",
                                featureValueModels.get(item.getIndex()));
                        featureTextField.add(new AttributeAppender("class", "k-textbox"));
                        item.add(featureTextField);
                        if (item.getIndex() == 0) {
                            // Put focus on first feature
                            featureTextField.add(new DefaultFocusBehavior());
                        }
                    }
                }

                @Override
                protected Iterator<IModel<FeatureValueModel>> getItemModels()
                {
                    return featuresModel.iterator();
                }
            }));
            featureValues.setOutputMarkupId(true);
            annotateButton = new AjaxButton("annotate")
            {
                private static final long serialVersionUID = 980971048279862290L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> form)
                {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);

                    if (selectedAnnotationLayer.getId() == 0) {
                        error("There is no annotation layer selected");
                        LOG.error("There is no annotation layer selected");
                        return;
                    }
                    if (!isRelationAnno && selectedSpanText.isEmpty()) {
                        error("There is no text selected to annotate");
                        LOG.error("There is no text selected to annotate");
                        return;
                    }
                    try {
                        actionAnnotate(aTarget);

                    }
                    catch (BratAnnotationException e) {
                        error(e.getMessage());
                        LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
                    }
                    catch (Exception e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
                    }
                }

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    if (isRelationAnno) {
                        setEnabled(true);
                    }
                    else {
                        setEnabled(!selectedSpanText.equals(""));
                    }

                }

            };
            if (featuresModel.isEmpty()) {
                // Put focus on annotate button if there are no features
                annotateButton.add(new DefaultFocusBehavior());
            }
            add(annotateButton);
            setDefaultButton(annotateButton);

            add(deleteButton = new AjaxSubmitLink("delete")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(selectedAnnotationId != -1);
                }

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);

                    try {
                        actionDelete(aTarget);
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
                    }
                    catch (Exception e) {
                        error(e.getMessage());
                        LOG.error(e.getMessage(), e);
                    }
                }
            });

            add(reverseButton = new AjaxSubmitLink("reverse")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(isRelationAnno && selectedAnnotationId != -1);
                }

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    try {
                        actionReverse(aTarget);
                    }
                    catch (BratAnnotationException e) {
                        aTarget.prependJavaScript("alert('" + e.getMessage() + "')");
                        LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
                    }
                    catch (Exception e) {
                        error(e.getMessage());
                        LOG.error(e.getMessage(), e);
                    }
                }
            });

        }
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        setVisible(getModelObject() != null && getModelObject().getProject() != null);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        // Libraries
        aResponse.render(JavaScriptHeaderItem.forReference(JQueryUIResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQuerySvgResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQuerySvgDomResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQueryJsonResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(WebfontResourceReference.get()));

        // BRAT helpers
        aResponse
                .render(JavaScriptHeaderItem.forReference(BratConfigurationResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratUtilResourceReference.get()));
        aResponse
                .render(JavaScriptHeaderItem.forReference(BratAnnotationLogResourceReference.get()));

        // BRAT modules
        aResponse.render(JavaScriptHeaderItem.forReference(BratDispatcherResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratUrlMonitorResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratAjaxResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratVisualizerResourceReference.get()));
        aResponse
                .render(JavaScriptHeaderItem.forReference(BratVisualizerUiResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratAnnotatorUiResourceReference.get()));

        StringBuilder script = new StringBuilder();
        // REC 2014-10-18 - For a reason that I do not understand, the dispatcher cannot be a local
        // variable. If I put a "var" here, then communication fails with messages such as
        // "action 'openSpanDialog' returned result of action 'loadConf'" in the browsers's JS
        // console.
        script.append("(function() {");
        script.append("var dispatcher = new Dispatcher();");
        // Each visualizer talks to its own Wicket component instance
        script.append("dispatcher.ajaxUrl = '" + controller.getCallbackUrl() + "'; ");
        // We attach the JSON send back from the server to this HTML element
        // because we cannot directly pass it from Wicket to the caller in ajax.js.
        script.append("dispatcher.wicketId = '" + vis.getMarkupId() + "'; ");
        script.append("var ajax = new Ajax(dispatcher);");
        script.append("var visualizer = new Visualizer(dispatcher, '" + vis.getMarkupId() + "');");
        script.append("var visualizerUI = new VisualizerUI(dispatcher, visualizer.svg);");
        script.append("var annotatorUI = new AnnotatorUI(dispatcher, visualizer.svg);");
        script.append("var logger = new AnnotationLog(dispatcher);");
        script.append("dispatcher.post('init');");
        script.append("Wicket.$('" + vis.getMarkupId() + "').dispatcher = dispatcher;");
        script.append("})();");

        // Must be OnDomReader so that this is rendered before all other Javascript that is
        // appended to the same AJAX request which turns the annotator visible after a document
        // has been chosen.
        aResponse.render(OnDomReadyHeaderItem.forScript(script.toString()));
    }

    private String bratInitCommand()
    {
        GetCollectionInformationResponse response = new GetCollectionInformationResponse();
        response.setEntityTypes(BratAjaxCasController.buildEntityTypes(getModelObject()
                .getAnnotationLayers(), annotationService));
        String json = toJson(response);
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('collectionLoaded', [" + json
                + "]);";
    }

    public String bratRenderCommand(JCas aJCas)
    {
        LOG.info("BEGIN bratRenderCommand");
        GetDocumentResponse response = new GetDocumentResponse();
        BratAjaxCasController.render(response, getModelObject(), aJCas, annotationService);
        String json = toJson(response);
        LOG.info("END bratRenderCommand");
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('renderData', [" + json
                + "]);";
    }

    /**
     * This triggers the loading of the metadata (colors, types, etc.)
     *
     * @return the init script.
     * @see BratAjaxConfiguration#buildEntityTypes
     */
    protected String bratInitLaterCommand()
    {
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('ajax', "
                + "[{action: 'getCollectionInformation',collection: '" + getCollection()
                + "'}, 'collectionLoaded', {collection: '" + getCollection() + "',keep: true}]);";
    }

    /**
     * This one triggers the loading of the actual document data
     *
     * @return brat
     */
    protected String bratRenderLaterCommand()
    {
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('current', " + "['"
                + getCollection() + "', '1234', {}, true]);";
    }

    /**
     * Reload {@link BratAnnotator} when the Correction/Curation page is opened
     *
     * @param aResponse
     *            the response.
     */
    public void bratInitRenderLater(IHeaderResponse aResponse)
    {
        aResponse.render(OnLoadHeaderItem.forScript(bratInitLaterCommand()));
        aResponse.render(OnLoadHeaderItem.forScript(bratRenderLaterCommand()));
    }

    /**
     * Reload {@link BratAnnotator} when the Correction/Curation page is opened
     *
     * @param aTarget
     *            the AJAX target.
     */
    public void bratInitRenderLater(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(bratInitLaterCommand());
        aTarget.appendJavaScript(bratRenderLaterCommand());
    }

    /**
     * Render content in a separate request.
     *
     * @param aTarget
     *            the AJAX target.
     */
    public void bratRenderLater(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(bratRenderLaterCommand());
    }

    /**
     * Render content as part of the current request.
     *
     * @param aTarget
     *            the AJAX target.
     * @param aJCas
     *            the CAS to render.
     */
    public void bratRender(AjaxRequestTarget aTarget, JCas aJCas)
    {
        aTarget.appendJavaScript(bratRenderCommand(aJCas));
    }

    public void bratInit(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(bratInitCommand());
    }

    public String getCollection()
    {
        return collection;
    }

    public void setCollection(String collection)
    {
        this.collection = collection;
    }

    protected void onChange(AjaxRequestTarget aTarget, BratAnnotatorModel aBratAnnotatorModel)
    {

    }

    protected void onAnnotate(BratAnnotatorModel aModel, int aStart, int aEnd)
    {
        // Overriden in AutomationPage
    }

    protected void onDelete(BratAnnotatorModel aModel, int aStart, int aEnd)
    {
        // Overriden in AutomationPage
    }

    private String toJson(Object result)
    {
        StringWriter out = new StringWriter();
        JsonGenerator jsonGenerator = null;
        try {
            jsonGenerator = jsonConverter.getObjectMapper().getJsonFactory()
                    .createJsonGenerator(out);
            jsonGenerator.writeObject(result);
        }
        catch (IOException e) {
            error("Unable to produce JSON response " + ":" + ExceptionUtils.getRootCauseMessage(e));
        }
        return out.toString();
    }

    private void listAnnotationLayers()
    {
        annotationLayers.clear();
        if (isRelationAnno) {
            annotationLayers.add(selectedAnnotationLayer);
            return;
        }
        for (AnnotationLayer layer : getModelObject().getAnnotationLayers()) {
            if (layer.getType().equals(WebAnnoConst.RELATION_TYPE) || !layer.isEnabled()
                    || layer.getName().equals(Token.class.getName())) {
                continue;
            }
            List<AnnotationFeature> features = annotationService.listAnnotationFeature(layer);
            if (features.size() == 0) {
                continue;
            }
            annotationLayers.add(layer);

        }
    }

    public class FeatureValueModel
        implements Serializable
    {
        private static final long serialVersionUID = 8890434759648466456L;
        public AnnotationFeature feature;
        public String tag;
    }

    public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -4178958678920895292L;
        public AnnotationLayer layers;
        public String selectedText;

        public String feature;
        public String tag;
    }

    private void actionAnnotate(AjaxRequestTarget aTarget)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        BratAnnotatorModel bratAnnotatorModel = getModelObject();
        if (selectedAnnotationLayer == null) {
            error("No layer is selected. First select a layer");
            return;
        }
        // check type of a feature
        for (IModel<String> model : featureValueModels) {
            AnnotationFeature feature = featuresModel.get(featureValueModels.indexOf(model))
                    .getObject().feature;
            try {

                if (feature.getType().equals(CAS.TYPE_NAME_INTEGER)
                        && !((Integer) Integer.parseInt(model.getObject()) instanceof Integer)) {
                    error(model.getObject() + " is not an integer value");
                    return;
                }
                if (feature.getType().equals(CAS.TYPE_NAME_FLOAT)
                        && !((Float) Float.parseFloat(model.getObject()) instanceof Float)) {
                    error(model.getObject() + " is not a float value");
                    return;
                }
                if (feature.getType().equals(CAS.TYPE_NAME_BOOLEAN)
                        && !((Boolean) Boolean.parseBoolean(model.getObject()) instanceof Boolean)) {
                    error(model.getObject() + " is not a boolean value");
                    return;
                }
            }
            catch (Exception e) {
                error(model.getObject() + " should be of type " + feature.getType());
                return;
            }
        }

        // Verify if input is valid
        for (int i = 0; i < featureValueModels.size(); i++) {
            IModel<String> model = featureValueModels.get(i);
            AnnotationFeature feature = featuresModel.get(i).getObject().feature;
            // Check if tag is necessary, set, and correct
            if (feature.getTagset() != null && !feature.getTagset().isCreateTag()
                    && !annotationService.existsTag(model.getObject(), feature.getTagset())) {
                error("[" + model.getObject()
                        + "] is not in the tag list. Please choose form the existing tags");
                return;
            }
        }

        // If there is no annotation yet, create one. During creation, the adapter
        // may notice that it would create a duplicate and return the address of
        // an existing annotation instead of a new one.
        JCas jCas = getCas(bratAnnotatorModel);
        TypeAdapter adapter = getAdapter(selectedAnnotationLayer);

        if (selectedAnnotationId == -1) {
            if (isRelationAnno) {
                AnnotationFS originFs = selectByAddr(jCas, originSpanId);
                AnnotationFS targetFs = selectByAddr(jCas, targetSpanId);
                if (adapter instanceof ArcAdapter) {
                    selectedAnnotationId = ((ArcAdapter) adapter).add(originFs, targetFs, jCas,
                            bratAnnotatorModel, null, null);
                }
                else {
                    selectedAnnotationId = ((ChainAdapter) adapter).addArc(jCas, originFs,
                            targetFs, null, null);
                }
                beginOffset = originFs.getBegin();
            }
            else if (adapter instanceof SpanAdapter) {
                selectedAnnotationId = ((SpanAdapter) adapter).add(jCas, beginOffset, endOffset,
                        null, null);
            }
            else {
                selectedAnnotationId = ((ChainAdapter) adapter).addSpan(jCas, beginOffset,
                        endOffset, null, null);
            }
            // continue;// next time, it will update features
        }

        // Set feature values
        List<AnnotationFeature> features = new ArrayList<AnnotationFeature>();
        for (int i = 0; i < featureValueModels.size(); i++) {
            IModel<String> model = featureValueModels.get(i);
            AnnotationFeature feature = featuresModel.get(i).getObject().feature;
            features.add(feature);

            Tag selectedTag;
            if (feature.getTagset() == null) {
                selectedTag = new Tag();
                selectedTag.setName(model.getObject());
            }
            else if (feature.getTagset() != null && feature.getTagset().isCreateTag()
                    && !annotationService.existsTag(model.getObject(), feature.getTagset())) {
                selectedTag = new Tag();
                selectedTag.setName(model.getObject());
                selectedTag.setTagSet(feature.getTagset());
                if (model.getObject() != null) {
                    // Do not persist if we unset a feature value
                    annotationService.createTag(selectedTag, bratAnnotatorModel.getUser());
                }
            }
            else {
                selectedTag = annotationService.getTag(model.getObject(), feature.getTagset());
            }

            adapter.updateFeature(jCas, feature, selectedAnnotationId, selectedTag.getName());
            selectedFeatureValues.put(feature, model.getObject());
        }

        // update timestamp now
        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas, beginOffset);
        bratAnnotatorModel.getDocument().setSentenceAccessed(sentenceNumber);
        repository.updateTimeStamp(bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                bratAnnotatorModel.getMode());

        // persist changes
        repository.updateJCas(bratAnnotatorModel.getMode(), bratAnnotatorModel.getDocument(),
                bratAnnotatorModel.getUser(), jCas);

        if (bratAnnotatorModel.isScrollPage()) {
            updateSentenceAddressAndOffsets(jCas, beginOffset);
        }

        if (isRelationAnno) {
            bratAnnotatorModel.setRememberedArcLayer(selectedAnnotationLayer);
            bratAnnotatorModel.setRememberedArcFeatures(selectedFeatureValues);
        }
        else {
            bratAnnotatorModel.setRememberedSpanLayer(selectedAnnotationLayer);
            bratAnnotatorModel.setRememberedSpanFeatures(selectedFeatureValues);
        }
        bratAnnotatorModel.setAnnotate(true);
        if (selectedAnnotationId != -1) {
            String bratLabelText = TypeUtil.getBratLabelText(adapter,
                    BratAjaxCasUtil.selectByAddr(jCas, selectedAnnotationId), features);
            info(generateMessage(selectedAnnotationLayer, bratLabelText, false));
        }
        aTarget.add(annotationFeatureForm);

        setLayerAndFeatureModels(jCas);
        bratRender(aTarget, jCas);
        onChange(aTarget, getModelObject());
    }

    private void actionDelete(AjaxRequestTarget aTarget)
        throws IOException, UIMAException, ClassNotFoundException
    {
        BratAnnotatorModel bratAnnotatorModel = getModelObject();
        JCas jCas = getCas(bratAnnotatorModel);
        AnnotationFS fs = selectByAddr(jCas, selectedAnnotationId);
        TypeAdapter adapter = getAdapter(selectedAnnotationLayer);
        String attachFeatureName = adapter.getAttachFeatureName();
        String attachTypeName = adapter.getAnnotationTypeName();

        Set<TypeAdapter> typeAdapters = new HashSet<TypeAdapter>();

        for (AnnotationLayer layer : annotationService.listAnnotationLayer(bratAnnotatorModel
                .getProject())) {

            typeAdapters.add(getAdapter(layer));
        }
        // delete associated relation annotation
        for (TypeAdapter ad : typeAdapters) {
            if (adapter.getAnnotationTypeName().equals(ad.getAnnotationTypeName())) {
                continue;
            }
            String tn = ad.getAttachTypeName();
            if (tn == null) {
                continue;
            }
            if (tn.equals(attachTypeName)) {
                Sentence thisSentence = BratAjaxCasUtil.getCurrentSentence(jCas, beginOffset,
                        endOffset);
                ad.deleteBySpan(jCas, fs, thisSentence.getBegin(), thisSentence.getEnd());
                break;
            }

            String fn = ad.getAttachFeatureName();
            if (fn == null) {
                continue;
            }
            if (fn.equals(attachFeatureName)) {
                Sentence thisSentence = BratAjaxCasUtil.getCurrentSentence(jCas, beginOffset,
                        endOffset);
                ad.deleteBySpan(jCas, fs, thisSentence.getBegin(), thisSentence.getEnd());
                break;
            }
        }
        // BEGIN HACK - Issue 933
        if (adapter instanceof ChainAdapter) {
            ((ChainAdapter) adapter).setArc(false);
        }
        // END HACK - Issue 933
        adapter.delete(jCas, selectedAnnotationId);

        repository.updateJCas(bratAnnotatorModel.getMode(), bratAnnotatorModel.getDocument(),
                bratAnnotatorModel.getUser(), jCas);
        // update timestamp now
        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas, beginOffset);
        bratAnnotatorModel.getDocument().setSentenceAccessed(sentenceNumber);
        repository.updateTimeStamp(bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                bratAnnotatorModel.getMode());

        if (bratAnnotatorModel.isScrollPage()) {
            updateSentenceAddressAndOffsets(jCas, beginOffset);
        }

        bratAnnotatorModel.setRememberedSpanLayer(selectedAnnotationLayer);
        bratAnnotatorModel.setAnnotate(false);

        // store latest annotations
        for (IModel<String> model : featureValueModels) {
            AnnotationFeature feature = featuresModel.get(featureValueModels.indexOf(model))
                    .getObject().feature;
            selectedAnnotationLayer = feature.getLayer();
            selectedFeatureValues.put(feature, model.getObject());
        }

        info(generateMessage(selectedAnnotationLayer, null, true));

        // A hack to rememeber the Visural DropDown display
        // value
        bratAnnotatorModel.setRememberedSpanLayer(selectedAnnotationLayer);
        bratAnnotatorModel.setRememberedSpanFeatures(selectedFeatureValues);

        selectedSpanText = "";
        selectedAnnotationId = -1;
        aTarget.add(annotationFeatureForm);

       // setLayerAndFeatureModels(jCas);
        bratRender(aTarget, jCas);
        onChange(aTarget, getModelObject());
    }

    private void actionReverse(AjaxRequestTarget aTarget)
        throws IOException, UIMAException, ClassNotFoundException, BratAnnotationException
    {
        JCas jCas;
        jCas = getCas(getModelObject());

        AnnotationFS idFs = selectByAddr(jCas, selectedAnnotationId);

        jCas.removeFsFromIndexes(idFs);

        AnnotationFS originFs = selectByAddr(jCas, originSpanId);
        AnnotationFS targetFs = selectByAddr(jCas, targetSpanId);

        TypeAdapter adapter = getAdapter(selectedAnnotationLayer);
        if (adapter instanceof ArcAdapter) {
            for (IModel<String> model : featureValueModels) {
                AnnotationFeature feature = featuresModel.get(featureValueModels.indexOf(model))
                        .getObject().feature;
                selectedAnnotationId = ((ArcAdapter) adapter).add(targetFs, originFs, jCas,
                        getModelObject(), feature, model.getObject());
            }
        }
        else {
            error("chains cannot be reversed");
            return;
        }

        // persist changes
        repository.updateJCas(getModelObject().getMode(), getModelObject().getDocument(),
                getModelObject().getUser(), jCas);
        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas, originFs.getBegin());
        getModelObject().getDocument().setSentenceAccessed(sentenceNumber);
        repository.updateTimeStamp(getModelObject().getDocument(), getModelObject().getUser(),
                getModelObject().getMode());

        if (getModelObject().isScrollPage()) {
            int start = originFs.getBegin();
            updateSentenceAddressAndOffsets(jCas, start);
        }

        StringBuffer deletedAnnoSb = new StringBuffer();

        // store latest annotations
        for (IModel<String> model : featureValueModels) {
            deletedAnnoSb.append(model.getObject() + " ");
            AnnotationFeature feature = featuresModel.get(featureValueModels.indexOf(model))
                    .getObject().feature;
            selectedAnnotationLayer = feature.getLayer();
            selectedFeatureValues.put(feature, model.getObject());
        }

        info("The arc annotation  [" + deletedAnnoSb + "] is reversed");
        getModelObject().setRememberedArcLayer(selectedAnnotationLayer);
        getModelObject().setRememberedArcFeatures(selectedFeatureValues);

        aTarget.add(annotationFeatureForm);

        // in case the user re-reverse it
        int temp = originSpanId;
        originSpanId = targetSpanId;
        targetSpanId = temp;

        setLayerAndFeatureModels(jCas);
        bratRender(aTarget, jCas);
        onChange(aTarget, getModelObject());
    }

    private JCas getCas(BratAnnotatorModel aBratAnnotatorModel)
        throws UIMAException, IOException, ClassNotFoundException
    {

        if (aBratAnnotatorModel.getMode().equals(Mode.ANNOTATION)
                || aBratAnnotatorModel.getMode().equals(Mode.AUTOMATION)
                || aBratAnnotatorModel.getMode().equals(Mode.CORRECTION)
                || aBratAnnotatorModel.getMode().equals(Mode.CORRECTION_MERGE)) {

            return repository.readJCas(aBratAnnotatorModel.getDocument(),
                    aBratAnnotatorModel.getProject(), aBratAnnotatorModel.getUser());
        }
        else {
            return repository.getCurationDocumentContent(aBratAnnotatorModel.getDocument());
        }
    }

    private void updateSentenceAddressAndOffsets(JCas jCas, int start)
    {
        BratAnnotatorModel bratAnnotatorModel = getModelObject();
        int address = BratAjaxCasUtil.selectSentenceAt(jCas,
                bratAnnotatorModel.getSentenceBeginOffset(),
                bratAnnotatorModel.getSentenceEndOffset()).getAddress();
        bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas,
                address, start, bratAnnotatorModel.getProject(), bratAnnotatorModel.getDocument(),
                bratAnnotatorModel.getWindowSize()));

        Sentence sentence = selectByAddr(jCas, Sentence.class,
                bratAnnotatorModel.getSentenceAddress());
        bratAnnotatorModel.setSentenceBeginOffset(sentence.getBegin());
        bratAnnotatorModel.setSentenceEndOffset(sentence.getEnd());
    }

    public static String generateMessage(AnnotationLayer aLayer, String aLabel, boolean aDeleted)
    {
        String action = aDeleted ? "deleted" : "created/updated";

        String msg = "The [" + aLayer.getUiName() + "] annotation has been " + action + ".";
        if (StringUtils.isNotBlank(aLabel)) {
            msg += " Label: [" + aLabel + "]";
        }
        return msg;
    }

    private void setLayerAndFeatureModels(JCas aJCas)
    {
        featureValueModels = new ArrayList<IModel<String>>();
        featuresModel = new ArrayList<IModel<FeatureValueModel>>();

        if (isRelationAnno) {
            long layerId = TypeUtil.getLayerId(originSpanType);
            AnnotationLayer spanLayer = annotationService.getLayer(layerId);
            if (spanLayer.isBuiltIn() && spanLayer.getName().equals(POS.class.getName())) {
                selectedAnnotationLayer = annotationService.getLayer(Dependency.class.getName(),
                        getModelObject().getProject());
            }
            else if (spanLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                selectedAnnotationLayer = spanLayer;// one layer both for the span and
                // arc annotation
            }
            else {
                for (AnnotationLayer layer : annotationService.listAnnotationLayer(getModelObject()
                        .getProject())) {
                    if (layer.getAttachType() != null && layer.getAttachType().equals(spanLayer)) {
                        selectedAnnotationLayer = layer;
                        break;
                    }
                }
            }
            // populate feature value
            if (selectedAnnotationId != -1) {
                AnnotationFS annoFs = BratAjaxCasUtil.selectByAddr(aJCas, selectedAnnotationId);
                for (AnnotationFeature feature : annotationService
                        .listAnnotationFeature(selectedAnnotationLayer)) {
                    if (feature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
                        continue;
                    }
                    if (feature.isEnabled()) {
                        Feature annoFeature = annoFs.getType().getFeatureByBaseName(
                                feature.getName());
                        selectedFeatureValues.put(feature,
                                annoFs.getFeatureValueAsString(annoFeature));
                    }

                }
            }
        }
        // get saved anno layers and features - Rapid annotation
        else if (selectedAnnotationId == -1
                && getModelObject().getRememberedSpanLayer() != null
                && !getModelObject().getRememberedSpanLayer().getType()
                        .equals(WebAnnoConst.RELATION_TYPE)) {
            selectedAnnotationLayer = getModelObject().getRememberedSpanLayer();
            for (AnnotationFeature feature : annotationService
                    .listAnnotationFeature(selectedAnnotationLayer)) {
                if (feature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE)) {
                    continue;
                }
                if (feature.isEnabled()) {
                    selectedFeatureValues.put(feature, getModelObject().getRememberedSpanFeatures()
                            .get(feature));
                }

            }
        }
        else if (selectedAnnotationId != -1) {
            AnnotationFS annoFs = BratAjaxCasUtil.selectByAddr(aJCas, selectedAnnotationId);
            String type = annoFs.getType().getName();

            if (type.endsWith(ChainAdapter.CHAIN)) {
                type = type.substring(0, type.length() - ChainAdapter.CHAIN.length());
            }
            else if (type.endsWith(ChainAdapter.LINK)) {
                type = type.substring(0, type.length() - ChainAdapter.LINK.length());
            }

            selectedAnnotationLayer = annotationService.getLayer(type, getModelObject()
                    .getProject());

            // populate feature value
            for (AnnotationFeature feature : annotationService
                    .listAnnotationFeature(selectedAnnotationLayer)) {
                if (feature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE)) {
                    continue;
                }
                if (feature.isEnabled()) {
                    Feature annoFeature = annoFs.getType().getFeatureByBaseName(feature.getName());
                    selectedFeatureValues.put(feature, annoFs.getFeatureValueAsString(annoFeature));
                }

            }
        }
        else {
            selectedAnnotationLayer = new AnnotationLayer();
        }

        layersModel = new LoadableDetachableModel<AnnotationLayer>()
        {
            private static final long serialVersionUID = -6629150412846045592L;

            @Override
            public AnnotationLayer load()
            {
                return selectedAnnotationLayer;
            }
        };
        for (AnnotationFeature feature : annotationService
                .listAnnotationFeature(selectedAnnotationLayer)) {

            if (!feature.isEnabled()) {
                continue;
            }

            if (isRelationAnno && selectedAnnotationLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)
                    && feature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
                continue;
            }
            if (!isRelationAnno
                    && selectedAnnotationLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)
                    && feature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE)) {
                continue;
            }
            IModel<FeatureValueModel> featureModel = new Model<FeatureValueModel>();

            FeatureValueModel featureValue = new FeatureValueModel();
            featureValue.feature = feature;
            featureModel.setObject(featureValue);
            featuresModel.add(featureModel);
            IModel<String> tagModel = new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = -6629150412846045592L;

                @Override
                public String load()
                {
                    return "";
                }
            };
            if (selectedFeatureValues.containsKey(feature)) {
                tagModel.setObject(selectedFeatureValues.get(feature));
            }
            featureValueModels.add(tagModel);
        }
    }

    // recall saved feature values
    private void updateFeaturesModel()
    {
        if (selectedAnnotationLayer != null) {
            for (AnnotationFeature feature : annotationService
                    .listAnnotationFeature(selectedAnnotationLayer)) {
                if (!feature.isEnabled()) {
                    continue;
                }

                if (isRelationAnno
                        && selectedAnnotationLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)
                        && feature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
                    continue;
                }
                if (!isRelationAnno
                        && selectedAnnotationLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)
                        && feature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE)) {
                    continue;
                }

                IModel<FeatureValueModel> featureModel = new Model<FeatureValueModel>();

                FeatureValueModel featureValue = new FeatureValueModel();
                featureValue.feature = feature;
                featureModel.setObject(featureValue);
                featuresModel.add(featureModel);
                IModel<String> tagModel = new Model<String>();
                if (selectedAnnotationId != -1) {

                    tagModel.setObject(selectedFeatureValues.get(feature));
                }
                else if (getModelObject().getRememberedSpanFeatures() != null
                        && getModelObject().getRememberedSpanFeatures().get(feature) != null) {
                    tagModel.setObject(getModelObject().getRememberedSpanFeatures().get(feature));
                }
                featureValueModels.add(tagModel);
            }
        }
    }
}