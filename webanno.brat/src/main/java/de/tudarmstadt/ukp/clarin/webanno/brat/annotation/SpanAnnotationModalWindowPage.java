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
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst.RELATION_TYPE;

import java.io.IOException;
import java.io.Serializable;
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
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
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
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A page that is used to display an annotation modal dialog for span annotation
 *
 * @author Seid Muhie Yimam
 *
 */
public class SpanAnnotationModalWindowPage
    extends Panel
{
    private final static Log LOG = LogFactory.getLog(SpanAnnotationModalWindowPage.class);

    private static final long serialVersionUID = -2102136855109258306L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    boolean isModify = false;
    AnnotationLayer selectedLayer;
    private Map<AnnotationFeature, String> selectedFeatureValues = new HashMap<AnnotationFeature, String>();

    Model<AnnotationLayer> layersModel;
    Model<AnnotationFeature> featuresModel;
    Model<String> selectedTagModel;

    private final AnnotationDialogForm annotationDialogForm;
    private final BratAnnotatorModel bratAnnotatorModel;
    private final int beginOffset;
    private final int endOffset;
    private String selectedText = null;
    int selectedSpanId = -1;

    List<AnnotationLayer> spanLayers = new ArrayList<AnnotationLayer>();

    List<IModel<FeatureValue>> featureModels;
    List<IModel<String>> tagModels;
    RefreshingView<FeatureValue> featureValues;
    private WebMarkupContainer wmc ;

    private class AnnotationDialogForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -4104665452144589457L;

        public AnnotationDialogForm(String id, final ModalWindow aModalWindow)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            final FeedbackPanel feedbackPanel = new FeedbackPanel("feedbackPanel");
            add(feedbackPanel);
            feedbackPanel.setOutputMarkupId(true);
            feedbackPanel.add(new AttributeModifier("class", "info"));
            feedbackPanel.add(new AttributeModifier("class", "error"));

            addSpanLayers: for (AnnotationLayer layer : bratAnnotatorModel.getAnnotationLayers()) {
                if (!layer.isEnabled() || layer.getName().equals(Token.class.getName())) {
                    continue;
                }
                List<AnnotationFeature> features = annotationService.listAnnotationFeature(layer);
                if (features.size() == 0) {
                    continue;
                }
                if (!layer.getType().equals(RELATION_TYPE)) {
                    spanLayers.add(layer);
                }
            }

            if (selectedLayer == null) {
                if (bratAnnotatorModel.getRememberedSpanLayer() == null) {
                    selectedLayer = spanLayers.get(0);
                    layersModel = new Model<AnnotationLayer>(selectedLayer);
                }
                else {
                    selectedLayer = bratAnnotatorModel.getRememberedSpanLayer();
                    layersModel = new Model<AnnotationLayer>(selectedLayer);
                }
            }

            add(new Label("selectedText", selectedText));
            @SuppressWarnings("unchecked")
			DropDownChoice<AnnotationLayer> layer = (DropDownChoice<AnnotationLayer>) new DropDownChoice<AnnotationLayer>("layers", layersModel, spanLayers)
            {
                private static final long serialVersionUID = -508831184292402704L;

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
            }).setOutputMarkupId(true);

            layer.add(new OnChangeAjaxBehavior() {

				private static final long serialVersionUID = 5179816588460867471L;

				@Override
				protected void onUpdate(AjaxRequestTarget aTarget) {
					featureModels = new ArrayList<IModel<FeatureValue>>();
                    tagModels = new ArrayList<IModel<String>>();
                    selectedLayer = layersModel.getObject();
                    for (AnnotationFeature feature : annotationService
                            .listAnnotationFeature(selectedLayer)) {

                        if (selectedLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)
                                && feature.getName().equals(
                                        WebAnnoConst.COREFERENCE_RELATION_FEATURE)) {
                            continue;
                        }
                        if (!feature.isEnabled()) {
                            continue;
                        }
                        IModel<FeatureValue> featureModel = new Model<SpanAnnotationModalWindowPage.FeatureValue>();

                        FeatureValue featureValue = new FeatureValue();
                        featureValue.feature = feature;
                        featureModel.setObject(featureValue);
                        featureModels.add(featureModel);
                        IModel<String> tagModel = new Model<String>();
                        if (feature.equals(selectedFeatureValues)) {
                            tagModel.setObject(selectedTagModel.getObject());
                        }
                        tagModels.add(tagModel);
                    }
                    aTarget.add(wmc);
				}
			});

            add (layer);

            featureModels = new ArrayList<IModel<FeatureValue>>();
            tagModels = new ArrayList<IModel<String>>();

            for (AnnotationFeature feature : annotationService.listAnnotationFeature(selectedLayer)) {
                if (!feature.isEnabled()) {
                    continue;
                }

                if (selectedLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)
                        && feature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE)) {
                    continue;
                }

                IModel<FeatureValue> featureModel = new Model<FeatureValue>();

                FeatureValue featureValue = new FeatureValue();
                featureValue.feature = feature;
                featureModel.setObject(featureValue);
                featureModels.add(featureModel);
                IModel<String> tagModel = new Model<String>();
                if (selectedSpanId != -1) {

                    tagModel.setObject(selectedFeatureValues.get(feature));
                }
                else if (bratAnnotatorModel.getRememberedSpanFeatures() != null
                        && bratAnnotatorModel.getRememberedSpanFeatures().get(feature) != null) {
                    tagModel.setObject(bratAnnotatorModel.getRememberedSpanFeatures().get(feature));
                }
                tagModels.add(tagModel);
            }

            wmc = new WebMarkupContainer("wmc");
            add(wmc.add(featureValues = new RefreshingView<FeatureValue>("featureValues")
            {
                private static final long serialVersionUID = -8359786805333207043L;

                @Override
                protected void populateItem(final Item<FeatureValue> item)
                {
                    item.add(new Label("feature",
                            item.getModelObject().feature.getTagset() == null ? item
                                    .getModelObject().feature.getUiName()
                                    : item.getModelObject().feature.getTagset().getName()));
                    List<Tag> tags = new ArrayList<Tag>();
                    if (item.getModelObject().feature.getTagset() != null
                            && item.getModelObject().feature.getTagset() != null) {
                        tags.addAll(annotationService.listTags(item.getModelObject().feature
                                .getTagset()));
                    }
                    if (tags.size() == 0) {
                        Tag tag = new Tag();
                        tag.setName(selectedText);
                        tags.add(tag);
                    }
                    item.add(new ComboBox<Tag>("tag", tagModels.get(item.getIndex()), tags,
                            new ComboBoxRenderer<Tag>("name", "name")).add(new Behavior()
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
                }

                @Override
                protected Iterator<IModel<FeatureValue>> getItemModels()
                {
                    return featureModels.iterator();
                }
            }));
            featureValues.setOutputMarkupId(true);
            wmc.setOutputMarkupId(true);
            add(new AjaxButton("annotate")
            {
                private static final long serialVersionUID = 980971048279862290L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> form)
                {
                    aTarget.add(feedbackPanel);
                    // check type of a feature

                    for (IModel<String> model : tagModels) {
                        AnnotationFeature feature = featureModels.get(tagModels.indexOf(model))
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

                    try {
                        // Verify if input is valid
                        for (IModel<String> model : tagModels) {
                            AnnotationFeature feature = featureModels.get(tagModels.indexOf(model))
                                    .getObject().feature;
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
                        TypeAdapter adapter = getAdapter(selectedLayer);

                        if (selectedSpanId == -1) {
                            if (adapter instanceof SpanAdapter) {
                                selectedSpanId = ((SpanAdapter) adapter).add(jCas, beginOffset,
                                        endOffset, null,null);
                            }
                            else {
                                selectedSpanId = ((ChainAdapter) adapter).addSpan(jCas,
                                        beginOffset, endOffset, null,null);
                            }
                          //  continue;// next time, it will update features
                        }

                        // Set feature values
                        List<AnnotationFeature> features = new ArrayList<AnnotationFeature>();
                        for (IModel<String> model : tagModels) {
                            AnnotationFeature feature = featureModels.get(tagModels.indexOf(model))
                                    .getObject().feature;
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

                            adapter.updateFeature(jCas, feature, selectedSpanId,
                                    selectedTag.getName());
                            selectedFeatureValues.put(feature, model.getObject());

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

                        bratAnnotatorModel.setRememberedSpanLayer(selectedLayer);
                        bratAnnotatorModel.setRememberedSpanFeatures(selectedFeatureValues);

                        bratAnnotatorModel.setAnnotate(true);
                        if(selectedSpanId !=-1){
	                        String bratLabelText = TypeUtil.getBratLabelText(adapter,
	                                BratAjaxCasUtil.selectByAddr(jCas, selectedSpanId), features);
	                        bratAnnotatorModel.setMessage(generateMessage(selectedLayer, bratLabelText,
                                false));
                        }
                        else{
                        	 bratAnnotatorModel.setMessage("");
                        }

                        aModalWindow.close(aTarget);
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

            });

            add(new AjaxSubmitLink("delete")
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    try {
                        JCas jCas = getCas(bratAnnotatorModel);
                        AnnotationFS fs = selectByAddr(jCas, selectedSpanId);
                        TypeAdapter adapter = getAdapter(selectedLayer);
                        String attachFeatureName = adapter.getAttachFeatureName();
                        String attachTypeName = adapter.getAnnotationTypeName();

                        Set<TypeAdapter> typeAdapters = new HashSet<TypeAdapter>();

                        for (AnnotationLayer layer : annotationService
                                .listAnnotationLayer(bratAnnotatorModel.getProject())) {

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
                                Sentence thisSentence = BratAjaxCasUtil.getCurrentSentence(jCas,
                                        beginOffset, endOffset);
                                ad.deleteBySpan(jCas, fs, thisSentence.getBegin(),
                                        thisSentence.getEnd());
                                break;
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
                        // BEGIN HACK - Issue 933
                        if (adapter instanceof ChainAdapter) {
                            ((ChainAdapter) adapter).setArc(false);
                        }
                        // END HACK - Issue 933
                        adapter.delete(jCas, selectedSpanId);

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

                        // store latest annotations
                        for (IModel<String> model : tagModels) {
                            AnnotationFeature feature = featureModels.get(tagModels.indexOf(model))
                                    .getObject().feature;
                            selectedLayer = feature.getLayer();
                            selectedFeatureValues.put(feature, model.getObject());
                        }

                        bratAnnotatorModel.setMessage(generateMessage(selectedLayer, null, true));

                        // A hack to rememeber the Visural DropDown display
                        // value
                        bratAnnotatorModel.setRememberedSpanLayer(selectedLayer);
                        bratAnnotatorModel.setRememberedSpanFeatures(selectedFeatureValues);

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

    public SpanAnnotationModalWindowPage(String aId, ModalWindow modalWindow,
            BratAnnotatorModel aBratAnnotatorModel, String aSelectedText, int aBeginOffset,
            int aEndOffset)
    {
    	super(aId);
        this.beginOffset = aBeginOffset;
        this.endOffset = aEndOffset;

        this.selectedText = aSelectedText;

        this.bratAnnotatorModel = aBratAnnotatorModel;
        this.annotationDialogForm = new AnnotationDialogForm("annotationDialogForm", modalWindow);
        add(annotationDialogForm);
    }

    public SpanAnnotationModalWindowPage(String aId, ModalWindow modalWindow,
            BratAnnotatorModel aBratAnnotatorModel, int selectedSpanId)
    {
    	super(aId);
        this.selectedSpanId = selectedSpanId;
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
        AnnotationFS annoFs = BratAjaxCasUtil.selectByAddr(jCas, selectedSpanId);
        this.selectedText = annoFs.getCoveredText();
        this.beginOffset = annoFs.getBegin();
        this.endOffset = annoFs.getEnd();

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
            if (feature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE)) {
                continue;
            }
            if (feature.isEnabled()) {
                Feature annoFeature = annoFs.getType().getFeatureByBaseName(feature.getName());
                this.selectedFeatureValues
                        .put(feature, annoFs.getFeatureValueAsString(annoFeature));
            }

        }

        this.annotationDialogForm = new AnnotationDialogForm("annotationDialogForm", modalWindow);
        add(annotationDialogForm);
        this.isModify = true;
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
}
