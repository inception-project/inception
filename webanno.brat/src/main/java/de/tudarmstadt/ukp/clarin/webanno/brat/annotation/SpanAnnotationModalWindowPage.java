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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import com.googlecode.wicket.jquery.ui.kendo.combobox.ComboBox;
import com.googlecode.wicket.jquery.ui.kendo.combobox.ComboBoxRenderer;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * A page that is used to display an annotation modal dialog for span annotation
 *
 * @author Seid Muhie Yimam
 *
 */
public class SpanAnnotationModalWindowPage
    extends WebPage
{
    private static final long serialVersionUID = -2102136855109258306L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    ComboBox<Tag> tags;
    DropDownChoice<AnnotationFeature> features;

    boolean isModify = false;
    AnnotationLayer selectedLayer;
    AnnotationFeature selectedFeature;

    Model<AnnotationLayer> layersModel;
    Model<AnnotationFeature> featuresModel;
    Model<String> tagsModel;

    private final AnnotationDialogForm annotationDialogForm;
    private final BratAnnotatorModel bratAnnotatorModel;
    private final int beginOffset;
    private final int endOffset;
    private String selectedText = null;
    int selectedSpanId = -1;
    String selectedSpanType;

    List<AnnotationLayer> spanLayers = new ArrayList<AnnotationLayer>();
    List<AnnotationFeature> spanFeatures = new ArrayList<AnnotationFeature>();

    private class AnnotationDialogForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -4104665452144589457L;

        @SuppressWarnings("unchecked")
        public AnnotationDialogForm(String id, final ModalWindow aModalWindow)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            final FeedbackPanel feedbackPanel = new FeedbackPanel("feedbackPanel");
            add(feedbackPanel);
            feedbackPanel.setOutputMarkupId(true);
            feedbackPanel.add(new AttributeModifier("class", "info"));
            feedbackPanel.add(new AttributeModifier("class", "error"));

           addSpanLayers: for (AnnotationLayer layer : bratAnnotatorModel.getAnnotationLayers()) {

                if(layer.getType().equals(WebAnnoConst.CHAIN_TYPE)){
                    for(AnnotationFeature feature: annotationService.listAnnotationFeature(layer)){
                        if(feature.getTagset() == null){
                            continue addSpanLayers;
                        }
                    }
                }

                if (!layer.getType().equals("relation")) {
                    spanLayers.add(layer);
                }

            }

            if (selectedSpanId != -1) {
                layersModel = new Model<AnnotationLayer>(selectedLayer);
                featuresModel = new Model<AnnotationFeature>(selectedFeature);

                try {
                    if (selectedFeature.getTagset() != null) {
                        tagsModel = new Model<String>(annotationService.getTag(selectedSpanType,
                                selectedFeature.getTagset()).getName());
                    }
                }
                catch (Exception e) {// It is a tag which is not in the tag
                                     // list.
                    tagsModel = new Model<String>("");
                }
            }
            else if (bratAnnotatorModel.getRememberedSpanLayer() != null
                    && bratAnnotatorModel.getRememberedSpanTag() != null
                    && conatinsLayer(bratAnnotatorModel.getAnnotationLayers(),
                            bratAnnotatorModel.getRememberedSpanLayer())) {
                selectedLayer = bratAnnotatorModel.getRememberedSpanLayer();
                if (selectedLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                    for (AnnotationFeature feature : annotationService
                            .listAnnotationFeature(selectedLayer)) {
                        if (feature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
                            selectedFeature = feature;
                            if (bratAnnotatorModel.getRememberedSpanTag().getTagSet().getFeature()
                                    .equals(feature)) {
                                tagsModel = new Model<String>(bratAnnotatorModel
                                        .getRememberedSpanTag().getName());
                            }
                            break;
                        }
                    }
                }
                else {
                    selectedFeature = bratAnnotatorModel.getRememberedSpanFeature();
                    layersModel = new Model<AnnotationLayer>(selectedLayer);
                    tagsModel = new Model<String>(bratAnnotatorModel.getRememberedSpanTag()
                            .getName());
                }
                // for lemma,stem,comment...
                if (selectedFeature.getTagset() == null) {
                    tagsModel.setObject(selectedText);
                }
            }
            else {
                selectedLayer = (spanLayers.get(0));
                layersModel = new Model<AnnotationLayer>(selectedLayer);
                if (selectedLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                    for (AnnotationFeature feature : annotationService
                            .listAnnotationFeature(selectedLayer)) {
                        if (feature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
                            selectedFeature = feature;
                            break;
                        }
                    }
                }
                else {
                    selectedFeature = annotationService.listAnnotationFeature(selectedLayer).get(0);
                }
                try {
                    if (selectedFeature.getTagset() != null) {
                        tagsModel = new Model<String>(annotationService.getTag(selectedSpanType,
                                selectedFeature.getTagset()).getName());
                    }
                    else {
                        tagsModel = new Model<String>("");
                    }
                }
                catch (Exception e) {// It is a tag which is not in the tag
                                     // list.
                    tagsModel = new Model<String>("");
                }
            }

            for (AnnotationFeature feature : annotationService.listAnnotationFeature(selectedLayer)) {
                if (feature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE)) {
                    continue;
                }
                spanFeatures.add(feature);
            }

            add(new Label("selectedText", selectedText));

            tags = new ComboBox<Tag>(
                    "tags",
                    tagsModel,
                    selectedFeature != null && selectedFeature.getTagset() != null ? annotationService
                            .listTags(selectedFeature.getTagset()) : new ArrayList<Tag>(),
                    new ComboBoxRenderer<Tag>("name", "name"));
            add(tags.setOutputMarkupId(true));

            add(new DropDownChoice<AnnotationLayer>("layers", layersModel, spanLayers)
            {
                private static final long serialVersionUID = -508831184292402704L;

                @Override
                protected void onSelectionChanged(AnnotationLayer aNewSelection)
                {
                    selectedLayer = aNewSelection;
                    spanFeatures.clear();
                    for (AnnotationFeature feature : annotationService
                            .listAnnotationFeature(aNewSelection)) {
                        spanFeatures.add(feature);
                    }
                    selectedFeature = spanFeatures.get(0);
                    features.setModelObject(selectedFeature);

                    if (selectedFeature.getTagset() == null) {
                        tagsModel.setObject(selectedText);
                    }
                    else {
                        tagsModel.setObject("");
                    }

                    updateTagsComboBox();

                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }

            }.setChoiceRenderer(new ChoiceRenderer<AnnotationLayer>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getDisplayValue(AnnotationLayer aObject)
                {
                    return aObject.getUiName();
                }
            }).setOutputMarkupId(true));

            add(features = (DropDownChoice<AnnotationFeature>) new DropDownChoice<AnnotationFeature>(
                    "features", featuresModel, spanFeatures)
            {
                private static final long serialVersionUID = -508831184292402704L;

                @Override
                protected void onSelectionChanged(AnnotationFeature aNewSelection)
                {
                    selectedFeature = aNewSelection;
                    if (aNewSelection.getTagset() == null) {
                        tagsModel.setObject(selectedText);
                    }
                    else {
                        tagsModel.setObject("");
                    }

                    updateTagsComboBox();

                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }

            }.setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getDisplayValue(AnnotationFeature aObject)
                {
                    return aObject.getTagset() == null ? aObject.getUiName() + "[Free Span]"
                            : aObject.getUiName();
                }
            }).setOutputMarkupId(true));

            add(new AjaxButton("annotate")
            {
                private static final long serialVersionUID = 980971048279862290L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> form)
                {

                    try {
                        JCas jCas = getCas(bratAnnotatorModel);

                        Tag selectedTag;
                        if (tags.getModelObject() == null) {
                            aTarget.add(feedbackPanel);
                            error("No Tag is selected!");
                            return;
                        }
                        if (selectedFeature.getTagset() == null) {
                            selectedTag = new Tag();
                            selectedTag.setName(tags.getModelObject());
                        }
                        else if (selectedFeature.getTagset() != null
                                && selectedFeature.getTagset().isCreateTag()
                                && !annotationService.existsTag(tags.getModelObject(),
                                        selectedFeature.getTagset())) {
                            selectedTag = new Tag();
                            selectedTag.setName(tags.getModelObject());
                            selectedTag.setTagSet(selectedFeature.getTagset());
                            annotationService.createTag(selectedTag, bratAnnotatorModel.getUser());
                        }
                        else if (!annotationService.existsTag(tags.getModelObject(),
                                selectedFeature.getTagset())) {
                            aTarget.add(feedbackPanel);
                            error(tags.getModelObject()
                                    + " is not in the tag list. Please choose form the existing tags");
                            return;
                        }
                        else {
                            selectedTag = annotationService.getTag(tags.getModelObject(),
                                    selectedFeature.getTagset());
                        }

                        TypeAdapter adapter = getAdapter(selectedLayer, annotationService);
                        if (adapter instanceof SpanAdapter) {
                            ((SpanAdapter) adapter).setLockToTokenOffsets(selectedLayer
                                    .isLockToTokenOffset());
                            ((SpanAdapter) adapter).setAllowStacking(selectedLayer
                                    .isAllowSTacking());
                            ((SpanAdapter) adapter).setAllowMultipleToken(selectedLayer
                                    .isMultipleTokens());
                            ((SpanAdapter) adapter).setCrossMultipleSentence(selectedLayer
                                    .isCrossSentence());
                            ((SpanAdapter) adapter).add(jCas, beginOffset, endOffset,
                                    selectedFeature, selectedTag.getName());

                        }
                        else {

                            ((ChainAdapter) adapter).add(selectedTag.getName(), jCas, beginOffset,
                                    endOffset, null, null, selectedFeature);
                        }
                        repository.updateJCas(bratAnnotatorModel.getMode(),
                                bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                                jCas);

                        // update timestamp now
                        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas, beginOffset);
                        bratAnnotatorModel.getDocument().setSentenceAccessed(sentenceNumber);
                        repository.updateTimeStamp(bratAnnotatorModel.getDocument(),
                                bratAnnotatorModel.getUser(), bratAnnotatorModel.getMode());

                        if (bratAnnotatorModel.isScrollPage()) {
                            updateSentenceAddressAndOffsets(jCas, beginOffset);
                        }

                        bratAnnotatorModel.setRememberedSpanLayer(selectedLayer);
                        bratAnnotatorModel.setRememberedSpanFeature(selectedFeature);
                        bratAnnotatorModel.setRememberedSpanTag(selectedTag);
                        bratAnnotatorModel.setAnnotate(true);
                        bratAnnotatorModel.setMessage("The span annotation ["
                                + selectedTag.getName() + "] is added");

                        // A hack to rememeber the Visural DropDown display
                        // value
                        HttpSession session = ((ServletWebRequest) RequestCycle.get().getRequest())
                                .getContainerRequest().getSession();
                        session.setAttribute("model", bratAnnotatorModel);

                        aModalWindow.close(aTarget);

                    }
                    catch (UIMAException e) {
                        aTarget.add(feedbackPanel);
                        error(ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (ClassNotFoundException e) {
                        aTarget.add(feedbackPanel);
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        aTarget.add(feedbackPanel);
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        aTarget.add(feedbackPanel);
                        error(e.getMessage());
                    }

                }

            }.add(new Behavior()
            {
                private static final long serialVersionUID = -3612493911620740735L;

                @Override
                public void renderHead(Component component, IHeaderResponse response)
                {
                    super.renderHead(component, response);
                    response.renderOnLoadJavaScript("$('#" + component.getMarkupId()
                            + "').focus();");
                }
            }));

            add(new AjaxSubmitLink("delete")
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    try {
                        JCas jCas = getCas(bratAnnotatorModel);
                        AnnotationFS fs = selectByAddr(jCas, selectedSpanId);
                        TypeAdapter adapter = getAdapter(selectedLayer, annotationService);
                        String attachFeatureName = adapter.getAttachFeatureName();

                        Set<TypeAdapter> typeAdapters = new HashSet<TypeAdapter>();

                        for (AnnotationLayer layer : annotationService
                                .listAnnotationLayer(bratAnnotatorModel.getProject())) {

                            typeAdapters.add(getAdapter(layer, annotationService));
                        }
                        // delete associated relation annotation
                        for (TypeAdapter ad : typeAdapters) {
                            if (adapter.getAnnotationTypeName().equals(ad.getAnnotationTypeName())) {
                                continue;
                            }
                            String fn = ad.getAttachFeatureName();
                            if (fn == null) {
                                continue;
                            }
                            if (fn.equals(attachFeatureName)) {
                                Sentence thisSentence = BratAjaxCasUtil.getCurrentSentence(jCas,
                                        beginOffset, endOffset);
                                ad.deleteBySpan(jCas, fs, thisSentence.getBegin(),
                                        thisSentence.getEnd());
                                break;
                            }
                        }
                        if (adapter instanceof ChainAdapter) {

                            TypeAdapter chainAdapter = new ChainAdapter(selectedLayer.getId(),
                                    selectedLayer.getName() + "Chain", selectedLayer.getName(),
                                    "first", "next");
                            ((ChainAdapter) adapter).updateCasBeforeDelete(jCas, selectedSpanId);
                            ((ChainAdapter) chainAdapter).delete(jCas, selectedSpanId);
                            ((ChainAdapter) adapter).removeInvalidChain(jCas.getCas());
                        }
                        else {
                            adapter.delete(jCas, selectedSpanId);
                        }

                        repository.updateJCas(bratAnnotatorModel.getMode(),
                                bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                                jCas);
                        // update timestamp now
                        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas, beginOffset);
                        bratAnnotatorModel.getDocument().setSentenceAccessed(sentenceNumber);
                        repository.updateTimeStamp(bratAnnotatorModel.getDocument(),
                                bratAnnotatorModel.getUser(), bratAnnotatorModel.getMode());

                        if (bratAnnotatorModel.isScrollPage()) {
                            updateSentenceAddressAndOffsets(jCas, beginOffset);
                        }

                        bratAnnotatorModel.setRememberedSpanLayer(selectedLayer);
                        bratAnnotatorModel.setAnnotate(false);
                        bratAnnotatorModel.setMessage("The span annotation [" + selectedSpanType
                                + "] is deleted");

                        // A hack to rememeber the Visural DropDown display
                        // value
                        HttpSession session = ((ServletWebRequest) RequestCycle.get().getRequest())
                                .getContainerRequest().getSession();
                        session.setAttribute("model", bratAnnotatorModel);
                        aModalWindow.close(aTarget);
                    }
                    catch (UIMAException e) {
                        aTarget.add(feedbackPanel);
                        error(ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (ClassNotFoundException e) {
                        aTarget.add(feedbackPanel);
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        aTarget.add(feedbackPanel);
                        error(e.getMessage());
                    }
                }

                @Override
                public boolean isVisible()
                {
                    return isModify;
                }
            });
        }

        private void updateTagsComboBox()
        {
            tags.remove();
            tags = new ComboBox<Tag>("tags", tagsModel,
                    selectedFeature.getTagset() == null ? new ArrayList<Tag>() : annotationService
                            .listTags(selectedFeature.getTagset()),
                    new ComboBoxRenderer<Tag>("name", "name"));
            add(tags);
        }
    }

    private void updateSentenceAddressAndOffsets(JCas jCas, int start)
    {
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
            return repository.getCurationDocumentContent(bratAnnotatorModel.getDocument());
        }
    }

    public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -4178958678920895292L;
        public AnnotationLayer layers;
        public AnnotationFeature features;
        public Tag tags;
        public String selectedText;
    }

    public SpanAnnotationModalWindowPage(ModalWindow modalWindow,
            BratAnnotatorModel aBratAnnotatorModel, String aSelectedText, int aBeginOffset,
            int aEndOffset)
    {
        this.beginOffset = aBeginOffset;
        this.endOffset = aEndOffset;

        this.selectedText = aSelectedText;

        this.bratAnnotatorModel = aBratAnnotatorModel;
        this.annotationDialogForm = new AnnotationDialogForm("annotationDialogForm", modalWindow);
        add(annotationDialogForm);
    }

    public SpanAnnotationModalWindowPage(ModalWindow modalWindow,
            BratAnnotatorModel aBratAnnotatorModel, String aSelectedText, int aBeginOffset,
            int aEndOffset, String aType, int selectedSpanId)
    {
        this.selectedSpanId = selectedSpanId;
        this.selectedSpanType = aType.substring(aType.indexOf("_") + 1);

        long layerId = Integer.parseInt(aType.substring(0, aType.indexOf("_")));

        this.selectedLayer = annotationService.getLayer(layerId);
        this.selectedFeature = annotationService.listAnnotationFeature(selectedLayer).get(0);

        this.beginOffset = aBeginOffset;
        this.endOffset = aEndOffset;

        this.selectedText = aSelectedText;

        this.bratAnnotatorModel = aBratAnnotatorModel;
        this.annotationDialogForm = new AnnotationDialogForm("annotationDialogForm", modalWindow);
        add(annotationDialogForm);
        this.isModify = true;
    }

    private boolean conatinsLayer(Set<AnnotationLayer> aLayers, AnnotationLayer aLayer)
    {
        for (AnnotationLayer layer : aLayers) {
            if (layer.getId() == aLayer.getId()) {
                return true;
            }
        }
        return false;
    }
}
