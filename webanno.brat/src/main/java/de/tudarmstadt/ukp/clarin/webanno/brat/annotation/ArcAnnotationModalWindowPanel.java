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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
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
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.support.DefaultFocusBehavior;
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
    private final static Log LOG = LogFactory.getLog(ArcAnnotationModalWindowPanel.class);

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
    List<IModel<String>> featureValueModels;
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

            DropDownChoice<AnnotationLayer> layer = new DropDownChoice<AnnotationLayer>("layers",
                    layersModel, Arrays.asList(new AnnotationLayer[] { selectedLayer }))
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setEnabled(selectedArcId == -1);
                }
            };
            layer.setOutputMarkupId(true);
            layer.setNullValid(false);
            layer.setChoiceRenderer(new ChoiceRenderer<AnnotationLayer>()
                    {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getDisplayValue(AnnotationLayer aObject)
                {
                    return aObject.getUiName();
                }
            });
            add(layer);
            
            featureModels = new ArrayList<IModel<FeatureValue>>();
            featureValueModels = new ArrayList<IModel<String>>();
            for (AnnotationFeature feature : annotationService.listAnnotationFeature(selectedLayer)) {
                if (!feature.isEnabled()) {
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
                else if (bratAnnotatorModel.getRememberedArcFeatures() != null
                        && bratAnnotatorModel.getRememberedArcFeatures().get(feature) != null) {
                    tagModel.setObject(bratAnnotatorModel.getRememberedArcFeatures().get(feature));
                }
                featureValueModels.add(tagModel);
            }

            add(featureValues = new RefreshingView<FeatureValue>("featureValues")
            {
                private static final long serialVersionUID = -8359786805333207043L;

                @Override
                protected void populateItem(final Item<FeatureValue> item)
                {
                    FeatureValue featureValue = item.getModelObject();
                    AnnotationFeature feature = featureValue.feature;
                    
                    String featureLabel = feature.getUiName();
                    if (feature.getTagset() != null) {
                        featureLabel += " (" + feature.getTagset().getName()+")";
                    }
                    
                    item.add(new Label("feature", featureLabel));

                    if (feature.getTagset() != null) {
                        List<Tag> tagset = new ArrayList<Tag>();
                        if (feature.getTagset() != null) {
                            tagset.addAll(annotationService.listTags(feature.getTagset()));
                        }

                        ComboBox<Tag> featureValueCombo = new ComboBox<Tag>("tag",
                                featureValueModels.get(item.getIndex()), tagset,
                                new ComboBoxRenderer<Tag>("name", "name"));
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
                protected Iterator<IModel<FeatureValue>> getItemModels()
                {
                    return featureModels.iterator();
                }
            });

            AjaxButton annotateButton = new AjaxButton("annotate")
            {
                private static final long serialVersionUID = 8922161039500097566L;

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aTarget.add(feedbackPanel);
                    aModalWindow.close(aTarget);
                    try {
                        actionAnnotate(aTarget, aForm);
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
            };
            if (featureModels.isEmpty()) {
                // Put focus on annotate button if there are no features
                annotateButton.add(new DefaultFocusBehavior());
            }
            add(annotateButton);
            setDefaultButton(annotateButton);

            add(new AjaxSubmitLink("delete")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(isModify);
                }
                
                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aTarget.add(feedbackPanel);
                    aModalWindow.close(aTarget);
                    try {
                        actionDelete();
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

            add(new AjaxSubmitLink("reverse")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(isModify && !ischain);
                }

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aTarget.add(feedbackPanel);
                    aModalWindow.close(aTarget);
                    try {
                        actionReverse();
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

    private void actionAnnotate(AjaxRequestTarget aTarget, Form<?> aForm)
        throws IOException, BratAnnotationException, UIMAException, ClassNotFoundException
    {
        // check type of a feature
        for (IModel<String> model : featureValueModels) {
            AnnotationFeature feature = featureModels.get(featureValueModels.indexOf(model))
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
            AnnotationFeature feature = featureModels.get(i).getObject().feature;
            // Check if tag is necessary, set, and correct
            if (feature.getTagset() != null
                    && !feature.getTagset().isCreateTag()
                    && !annotationService.existsTag(model.getObject(),
                            feature.getTagset())) {
                error("["
                        + model.getObject()
                        + "] is not in the tag list. Please choose form the existing tags");
                return;
            }
        }

        // If there is no annotation yet, create one. During creation, the adapter
        // may notice that it would create a duplicate and return the address of
        // an existing annotation instead of a new one.
        JCas jCas = getCas(bratAnnotatorModel);
        AnnotationFS originFs = selectByAddr(jCas, originSpanId);
        AnnotationFS targetFs = selectByAddr(jCas, targetSpanId);

        TypeAdapter adapter = getAdapter(selectedLayer);

        if (selectedArcId == -1) {
            if (adapter instanceof ArcAdapter) {
                selectedArcId = ((ArcAdapter) adapter).add(originFs, targetFs,
                        jCas, bratAnnotatorModel, null, null);
            }
            else {
                selectedArcId = ((ChainAdapter) adapter).addArc(jCas, originFs,
                        targetFs, null, null);
            }
        }
        // Set feature values
        List<AnnotationFeature> features = new ArrayList<AnnotationFeature>();
        for (int i = 0; i < featureValueModels.size(); i++) {
            IModel<String> model = featureValueModels.get(i);
            AnnotationFeature feature = featureModels.get(i).getObject().feature;
            features.add(feature);

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
                if (model.getObject() != null) {
                    // Do not persist if we unset a feature value
                    annotationService.createTag(selectedTag,
                            bratAnnotatorModel.getUser());
                }
            }
            else {
                selectedTag = annotationService.getTag(model.getObject(),
                        feature.getTagset());
            }

            adapter.updateFeature(jCas, feature, selectedArcId,
                    selectedTag.getName());

            selectedFeatureValues.put(feature, model.getObject());
            beginOffset = originFs.getBegin();
        }

        // update timestamp now
        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas, beginOffset);

        bratAnnotatorModel.getDocument().setSentenceAccessed(sentenceNumber);
        repository.updateTimeStamp(bratAnnotatorModel.getDocument(),
                bratAnnotatorModel.getUser(), bratAnnotatorModel.getMode());

        // persist changes
        repository.updateJCas(bratAnnotatorModel.getMode(),
                bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                jCas);

        if (bratAnnotatorModel.isScrollPage()) {
            updateSentenceAddressAndOffsets(jCas, beginOffset);
        }

        if (selectedArcId != -1) {
            String bratLabelText = TypeUtil.getBratLabelText(adapter,
                    BratAjaxCasUtil.selectByAddr(jCas, selectedArcId), features);
            bratAnnotatorModel.setMessage(SpanAnnotationModalWindowPage
                    .generateMessage(selectedLayer, bratLabelText, false));
        }
        else {
            bratAnnotatorModel.setMessage("");
        }

        bratAnnotatorModel.setRememberedArcLayer(selectedLayer);
        bratAnnotatorModel.setRememberedArcFeatures(selectedFeatureValues);
    }
    
    private void actionDelete()
        throws UIMAException, ClassNotFoundException, IOException
    {
        JCas jCas;
        jCas = getCas(bratAnnotatorModel);
        TypeAdapter adapter = getAdapter(selectedLayer);
        // BEGIN HACK - Issue 933
        if (adapter instanceof ChainAdapter) {
            ((ChainAdapter) adapter).setArc(true);
        }
        // END HACK - Issue 933

        adapter.delete(jCas, selectedArcId);
        repository.updateJCas(bratAnnotatorModel.getMode(), bratAnnotatorModel.getDocument(),
                bratAnnotatorModel.getUser(), jCas);

        // update timestamp now
        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas,
                BratAjaxCasUtil.selectByAddr(jCas, selectedArcId).getBegin());
        bratAnnotatorModel.getDocument().setSentenceAccessed(sentenceNumber);
        repository.updateTimeStamp(bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                bratAnnotatorModel.getMode());

        if (bratAnnotatorModel.isScrollPage()) {
            AnnotationFS originFs = selectByAddr(jCas, originSpanId);
            int start = originFs.getBegin();
            updateSentenceAddressAndOffsets(jCas, start);
        }

        // store latest annotations
        for (IModel<String> model : featureValueModels) {
            AnnotationFeature feature = featureModels.get(featureValueModels.indexOf(model)).getObject().feature;
            selectedLayer = feature.getLayer();
            selectedFeatureValues.put(feature, model.getObject());
        }

        bratAnnotatorModel.setMessage(SpanAnnotationModalWindowPage.generateMessage(selectedLayer,
                null, true));
        bratAnnotatorModel.setRememberedArcLayer(selectedLayer);
        bratAnnotatorModel.setRememberedArcFeatures(selectedFeatureValues);
    }
    
    private void actionReverse()
        throws IOException, UIMAException, ClassNotFoundException, BratAnnotationException
    {
        JCas jCas;
        jCas = getCas(bratAnnotatorModel);

        AnnotationFS idFs = selectByAddr(jCas, selectedArcId);

        jCas.removeFsFromIndexes(idFs);

        AnnotationFS originFs = selectByAddr(jCas, originSpanId);
        AnnotationFS targetFs = selectByAddr(jCas, targetSpanId);

        TypeAdapter adapter = getAdapter(selectedLayer);
        if (adapter instanceof ArcAdapter) {
            for (IModel<String> model : featureValueModels) {
                AnnotationFeature feature = featureModels.get(
                        featureValueModels.indexOf(model)).getObject().feature;
                ((ArcAdapter) adapter).add(targetFs, originFs, jCas,
                        bratAnnotatorModel, feature, model.getObject());
            }
        }
        else {
            error("chains cannot be reversed");
            return;
        }

        // persist changes
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
        for (IModel<String> model : featureValueModels) {
            deletedAnnoSb.append(model.getObject() + " ");
            AnnotationFeature feature = featureModels.get(featureValueModels.indexOf(model))
                    .getObject().feature;
            selectedLayer = feature.getLayer();
            selectedFeatureValues.put(feature, model.getObject());
        }

        bratAnnotatorModel.setMessage("The arc annotation  [" + deletedAnnoSb
                + "] is reversed");
        bratAnnotatorModel.setRememberedArcLayer(selectedLayer);
        bratAnnotatorModel.setRememberedArcFeatures(selectedFeatureValues);
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
        long layerId = TypeUtil.getLayerId(aOriginSpanType);

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
            type = type.substring(0, type.length() - ChainAdapter.CHAIN.length());
        }
        else if (type.endsWith(ChainAdapter.LINK)) {
            type = type.substring(0, type.length() - ChainAdapter.LINK.length());
        }

        this.selectedLayer = annotationService.getLayer(type, bratAnnotatorModel.getProject());
        layersModel = new Model<AnnotationLayer>(selectedLayer);
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(selectedLayer)) {
            if (feature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
                continue;
            }
            if (feature.isEnabled()) {
                Feature annoFeature = annoFs.getType().getFeatureByBaseName(feature.getName());
                this.selectedFeatureValues
                        .put(feature, annoFs.getFeatureValueAsString(annoFeature));
            }

        }
        annotationDialogForm = new AnnotationDialogForm("annotationDialogForm", modalWindow);
        add(annotationDialogForm);
        this.isModify = true;

        if (!selectedLayer.getType().equals(WebAnnoConst.RELATION_TYPE)) {
            ischain = true;
        }

    }
}
