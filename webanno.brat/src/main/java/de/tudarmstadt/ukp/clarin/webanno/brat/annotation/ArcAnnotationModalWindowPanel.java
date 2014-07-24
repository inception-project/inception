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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.Feature;
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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import com.googlecode.wicket.jquery.ui.kendo.combobox.ComboBox;
import com.googlecode.wicket.jquery.ui.kendo.combobox.ComboBoxRenderer;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * A panel that is used to display an annotation modal dialog for arc annotation.
 *
 * @author Seid Muhie Yimam
 *
 */
public class ArcAnnotationModalWindowPanel
    extends Panel
{
    private static final long serialVersionUID = -2102136855109258306L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    // A flag to keep checking if new annotation is to be made or an existing
    // annotation is double
    // clciked.
    private boolean isModify = false;

    // currently, we have one directional chain annotation and the "reveres"
    // button not needed
    private boolean ischain = false;
    // The selected Layer
    private AnnotationLayer selectedLayer;

    private Map<AnnotationFeature, String> selectedFeatureValues = new HashMap<AnnotationFeature, String>();

    private Model<AnnotationLayer> layersModel;

    private final AnnotationDialogForm annotationDialogForm;
    private final BratAnnotatorModel bratAnnotatorModel;

    private int selectedArcId = -1;
    private int originSpanId, targetSpanId;
    private int beginOffset;

    List<AnnotationFeature> spanFeatures = new ArrayList<AnnotationFeature>();

    List<IModel<FeatureValue>> featureModels;
    List<IModel<String>> tagModels;
    RefreshingView<FeatureValue> featureValues;

    Model<String> selectedTagModel;

    private class AnnotationDialogForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -4104665452144589457L;

        public AnnotationDialogForm(String id, final ModalWindow aModalWindow)
        {
            super(id);

            final FeedbackPanel feedbackPanel = new FeedbackPanel("feedbackPanel");
            add(feedbackPanel);
            feedbackPanel.setOutputMarkupId(true);
            feedbackPanel.add(new AttributeModifier("class", "info"));
            feedbackPanel.add(new AttributeModifier("class", "error"));

            add(new DropDownChoice<AnnotationLayer>("layers", layersModel,
                    Arrays.asList(new AnnotationLayer[] { selectedLayer })).setNullValid(false)
                    .setChoiceRenderer(new ChoiceRenderer<AnnotationLayer>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public Object getDisplayValue(AnnotationLayer aObject)
                        {
                            return aObject.getUiName();
                        }
                    }).setOutputMarkupId(true));

            featureModels = new ArrayList<IModel<FeatureValue>>();
            tagModels = new ArrayList<IModel<String>>();
            for (AnnotationFeature feature : annotationService.listAnnotationFeature(selectedLayer)) {
                if (!feature.isVisible() || !feature.isEnabled()) {
                    continue;
                }
                if (selectedLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)
                        && feature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
                    continue;
                }

                IModel<FeatureValue> featureModel = new Model<FeatureValue>();

                FeatureValue featureValue = new FeatureValue();
                featureValue.feature = feature;
                featureModel.setObject(featureValue);
                featureModels.add(featureModel);
                IModel<String> tagModel = new Model<String>();
                if (selectedArcId != -1) {

                    tagModel.setObject(selectedFeatureValues.get(feature));
                }
                else if (bratAnnotatorModel.getRememberedArcFeatures().get(feature) != null) {
                    tagModel.setObject(bratAnnotatorModel.getRememberedArcFeatures().get(feature));
                }
                tagModels.add(tagModel);
            }

            add(featureValues = new RefreshingView<FeatureValue>("featureValues")
            {
                private static final long serialVersionUID = -8359786805333207043L;

                @Override
                protected void populateItem(final Item<FeatureValue> item)
                {
                    item.add(new Label("feature",
                            item.getModelObject().feature.getTagset() == null ? item
                                    .getModelObject().feature.getUiName()
                                    : item.getModelObject().feature.getTagset().getName()));

                    item.add(new ComboBox<Tag>(
                            "tag",
                            tagModels.get(item.getIndex()),
                            (item.getModelObject().feature.getTagset() != null && item
                                    .getModelObject().feature.getTagset() != null) ? annotationService
                                    .listTags(item.getModelObject().feature.getTagset())
                                    : new ArrayList<Tag>(), new ComboBoxRenderer<Tag>("name",
                                    "name")));
                }

                @Override
                protected Iterator<IModel<FeatureValue>> getItemModels()
                {
                    return featureModels.iterator();
                }
            });

            add(new AjaxButton("annotate")
            {
                private static final long serialVersionUID = 8922161039500097566L;

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    // check if at least one feature have an annotation
                    boolean existsAnnotation = false;
                    for (IModel<String> model : tagModels) {
                        if (model.getObject() != null) {
                            existsAnnotation = true;
                            break;
                        }
                    }
                    if (!existsAnnotation) {
                        aTarget.add(feedbackPanel);
                        error("No Tag is selected!");
                        return;
                    }
                    try {
                        JCas jCas = getCas(bratAnnotatorModel);
                        String tag = "";
                        for (IModel<String> model : tagModels) {
                            if (model.getObject() == null) {
                                continue;
                            }
                            AnnotationFeature feature = featureModels.get(tagModels.indexOf(model))
                                    .getObject().feature;
                            Tag selectedTag;
                            if (feature.getTagset() == null) {
                                selectedTag = new Tag();
                                selectedTag.setName(model.getObject());
                            }
                            else if (feature.getTagset() != null
                                    && feature.getTagset().isCreateTag()
                                    && !annotationService.existsTag(model.getObject(),
                                            feature.getTagset())) {
                                selectedTag = new Tag();
                                selectedTag.setName(model.getObject());
                                selectedTag.setTagSet(feature.getTagset());
                                annotationService.createTag(selectedTag,
                                        bratAnnotatorModel.getUser());
                            }
                            else if (!annotationService.existsTag(model.getObject(),
                                    feature.getTagset())) {
                                aTarget.add(feedbackPanel);
                                error(model.getObject()
                                        + " is not in the tag list. Please choose form the existing tags");
                                return;
                            }
                            else {
                                selectedTag = annotationService.getTag(model.getObject(),
                                        feature.getTagset());
                            }

                            AnnotationFS originFs = selectByAddr(jCas, originSpanId);
                            AnnotationFS targetFs = selectByAddr(jCas, targetSpanId);

                            TypeAdapter adapter = getAdapter(selectedLayer, annotationService);
                            if (selectedArcId != -1) {
                                adapter.updateFeature(jCas, feature, selectedArcId,
                                        selectedTag.getName());
                            }
                            else if (adapter instanceof ArcAdapter) {
                                ((ArcAdapter) adapter).setCrossMultipleSentence(selectedLayer
                                        .isCrossSentence());
                                ((ArcAdapter) adapter).setAllowStacking(selectedLayer
                                        .isAllowSTacking());

                                ((ArcAdapter) adapter).add(selectedTag.getName(), originFs,
                                        targetFs, jCas, bratAnnotatorModel, feature);
                            }
                            else {
                                ((ChainAdapter) adapter).add(selectedTag.getName(), jCas,
                                        originFs.getBegin(), targetFs.getEnd(), originFs, targetFs,
                                        feature);
                            }
                            if (tag.equals("")) {
                                tag = selectedTag.getName();
                            }
                            else {
                                tag = tag + "|" + selectedTag.getName();
                            }
                            selectedFeatureValues.put(feature, model.getObject());
                            beginOffset = originFs.getBegin();
                        }
                        // update timestamp now
                        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas, beginOffset);
                        bratAnnotatorModel.setRememberedArcLayer(selectedLayer);
                        bratAnnotatorModel.setRememberedArcFeatures(selectedFeatureValues);
                        bratAnnotatorModel.getDocument().setSentenceAccessed(sentenceNumber);
                        repository.updateTimeStamp(bratAnnotatorModel.getDocument(),
                                bratAnnotatorModel.getUser(), bratAnnotatorModel.getMode());

                        repository.updateJCas(bratAnnotatorModel.getMode(),
                                bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                                jCas);

                        if (bratAnnotatorModel.isScrollPage()) {
                            updateSentenceAddressAndOffsets(jCas, beginOffset);
                        }
                        bratAnnotatorModel.setMessage("The arc annotation [" + tag + "] is added");

                        aModalWindow.close(aTarget);

                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (IOException e) {
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
                    JCas jCas;
                    try {
                        jCas = getCas(bratAnnotatorModel);
                        TypeAdapter adapter = getAdapter(selectedLayer, annotationService);
                        adapter.delete(jCas, selectedArcId);
                        repository.updateJCas(bratAnnotatorModel.getMode(),
                                bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                                jCas);

                        // update timestamp now
                        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas,
                                BratAjaxCasUtil.selectByAddr(jCas, selectedArcId).getBegin());
                        bratAnnotatorModel.getDocument().setSentenceAccessed(sentenceNumber);
                        repository.updateTimeStamp(bratAnnotatorModel.getDocument(),
                                bratAnnotatorModel.getUser(), bratAnnotatorModel.getMode());

                        if (bratAnnotatorModel.isScrollPage()) {
                            AnnotationFS originFs = selectByAddr(jCas, originSpanId);
                            int start = originFs.getBegin();
                            updateSentenceAddressAndOffsets(jCas, start);
                        }

                        StringBuffer deletedAnnoSb = new StringBuffer();

                        // store latest annotations
                        for (IModel<String> model : tagModels) {
                            deletedAnnoSb.append(model.getObject()+" ");
                            AnnotationFeature feature = featureModels.get(tagModels.indexOf(model))
                                    .getObject().feature;
                            selectedLayer = feature.getLayer();
                            selectedFeatureValues.put(feature, model.getObject());
                        }

                        bratAnnotatorModel.setMessage("The arc annotation [" + deletedAnnoSb
                                + "] is deleted");

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
                    aModalWindow.close(aTarget);
                }

                @Override
                public boolean isVisible()
                {
                    return isModify;
                }
            });

            add(new AjaxSubmitLink("reverse")
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aTarget.add(feedbackPanel);
                    JCas jCas;
                    try {
                        jCas = getCas(bratAnnotatorModel);

                        AnnotationFS idFs = selectByAddr(jCas, selectedArcId);

                        jCas.removeFsFromIndexes(idFs);

                        AnnotationFS originFs = selectByAddr(jCas, originSpanId);
                        AnnotationFS targetFs = selectByAddr(jCas, targetSpanId);

                        TypeAdapter adapter = getAdapter(selectedLayer, annotationService);
                        if (adapter instanceof ArcAdapter) {
                            for (IModel<String> model : tagModels) {
                                AnnotationFeature feature = featureModels.get(
                                        tagModels.indexOf(model)).getObject().feature;
                                ((ArcAdapter) adapter).add(model.getObject(), targetFs, originFs,
                                        jCas, bratAnnotatorModel, feature);
                            }
                        }
                        else {
                            error("chains cannot be reversed");
                            return;
                        }

                        repository.updateJCas(bratAnnotatorModel.getMode(),
                                bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                                jCas);
                        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas,
                                originFs.getBegin());
                        bratAnnotatorModel.getDocument().setSentenceAccessed(sentenceNumber);
                        repository.updateTimeStamp(bratAnnotatorModel.getDocument(),
                                bratAnnotatorModel.getUser(), bratAnnotatorModel.getMode());

                        if (bratAnnotatorModel.isScrollPage()) {
                            int start = originFs.getBegin();
                            updateSentenceAddressAndOffsets(jCas, start);
                        }

                        StringBuffer deletedAnnoSb = new StringBuffer();

                        // store latest annotations
                        for (IModel<String> model : tagModels) {
                            deletedAnnoSb.append(model.getObject()+" ");
                            AnnotationFeature feature = featureModels.get(tagModels.indexOf(model))
                                    .getObject().feature;
                            selectedLayer = feature.getLayer();
                            selectedFeatureValues.put(feature, model.getObject());
                        }

                        bratAnnotatorModel.setMessage("The arc annotation  [" + deletedAnnoSb
                                + "] is reversed");

                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        aTarget.prependJavaScript("alert('" + e.getMessage() + "')");
                    }
                    aModalWindow.close(aTarget);
                }

                @Override
                public boolean isVisible()
                {
                    return isModify && !ischain;
                }
            });
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
        public String selectedText;

        public String feature;
        public String tag;
    }

    public class FeatureValue
        implements Serializable
    {
        private static final long serialVersionUID = 8890434759648466456L;
        public AnnotationFeature feature;
        public String tag;
    }

    public ArcAnnotationModalWindowPanel(String aId, final ModalWindow modalWindow,
            BratAnnotatorModel aBratAnnotatorModel, int aOriginSpanId, String aOriginSpanType,
            int aTargetSpanId, String aTargetSpanType)
    {
        super(aId);

        long layerId = Integer.parseInt(aOriginSpanType.substring(0, aOriginSpanType.indexOf("_")));

        AnnotationLayer spanLayer = annotationService.getLayer(layerId);
        if (spanLayer.isBuiltIn() && spanLayer.getName().equals(POS.class.getName())) {
            this.selectedLayer = annotationService.getLayer(Dependency.class.getName(),
                    aBratAnnotatorModel.getProject());
        }
        else if (spanLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
            this.selectedLayer = spanLayer;// one layer both for the span and
                                           // arc annotation
        }
        else {
            for (AnnotationLayer layer : annotationService.listAnnotationLayer(aBratAnnotatorModel
                    .getProject())) {
                if (layer.getAttachType() != null && layer.getAttachType().equals(spanLayer)) {
                    this.selectedLayer = layer;
                    break;
                }
            }
        }
        layersModel = new Model<AnnotationLayer>(selectedLayer);
        this.originSpanId = aOriginSpanId;
        this.targetSpanId = aTargetSpanId;

        this.bratAnnotatorModel = aBratAnnotatorModel;
        annotationDialogForm = new AnnotationDialogForm("annotationDialogForm", modalWindow);
        add(annotationDialogForm);
    }

    public ArcAnnotationModalWindowPanel(String aId, final ModalWindow modalWindow,
            BratAnnotatorModel aBratAnnotatorModel, int aOriginSpanId, int aTargetSpanId,
            int selectedArcId)
    {
        super(aId);
        this.selectedArcId = selectedArcId;
        this.bratAnnotatorModel = aBratAnnotatorModel;
        JCas jCas = null;
        try {
            jCas = getCas(bratAnnotatorModel);
        }
        catch (UIMAException e) {
            error(ExceptionUtils.getRootCause(e));
        }
        catch (IOException e) {
            error(e.getMessage());
        }
        catch (ClassNotFoundException e) {
            error(e.getMessage());
        }
        AnnotationFS annoFs = BratAjaxCasUtil.selectByAddr(jCas, selectedArcId);
        this.originSpanId = aOriginSpanId;
        this.targetSpanId = aTargetSpanId;
        String type = annoFs.getType().getName();

        if (type.endsWith(ChainAdapter.CHAIN)) {
            type = type.substring(0, type.length() - 5);
        }
        else if (type.endsWith(ChainAdapter.LINK)) {
            type = type.substring(0, type.length() - 4);
        }
        this.selectedLayer = annotationService.getLayer(type, bratAnnotatorModel.getProject());
        layersModel = new Model<AnnotationLayer>(selectedLayer);
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(selectedLayer)) {
            if (feature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
                continue;
            }
            if (feature.isEnabled() || feature.isVisible()) {
                Feature annoFeature = annoFs.getType().getFeatureByBaseName(feature.getName());
                this.selectedFeatureValues
                        .put(feature, annoFs.getFeatureValueAsString(annoFeature));
            }

        }
        annotationDialogForm = new AnnotationDialogForm("annotationDialogForm", modalWindow);
        add(annotationDialogForm);
        this.isModify = true;

        if (selectedLayer.getType().equals(WebAnnoConst.RELATION_TYPE)) {
            ischain = false;
        }

    }
}
