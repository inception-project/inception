/*******************************************************************************
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getAdapter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import com.googlecode.wicket.kendo.ui.form.NumberTextField;
import com.googlecode.wicket.kendo.ui.form.TextField;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.support.DefaultFocusBehavior;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Annotation Detail Editor Panel.
 *
 * @author Seid Muhie Yimam
 */
public class AnnotationDetailEditorPanel
    extends Panel
{
    private static final long serialVersionUID = 7324241992353693848L;
    private static final Log LOG = LogFactory.getLog(AnnotationDetailEditorPanel.class);

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    private AnnotationFeatureForm annotationFeatureForm;
    private Label selectedTextLabel;
    private DropDownChoice<AnnotationLayer> layers;
    private RefreshingView<FeatureModel> featureValues;
    private WebMarkupContainer wmc;
    private AjaxButton annotateButton;
    private AjaxSubmitLink deleteButton;
    private AjaxSubmitLink reverseButton;

    private List<AnnotationLayer> annotationLayers = new ArrayList<AnnotationLayer>();

    private List<FeatureModel> featureModels;

    public AnnotationDetailEditorPanel(String id, IModel<BratAnnotatorModel> aModel)
    {
        super(id, aModel);
        annotationFeatureForm = new AnnotationFeatureForm("annotationFeatureForm",
                aModel.getObject());

        annotationFeatureForm.setOutputMarkupId(true);
        add(annotationFeatureForm);
    }

    private class AnnotationFeatureForm
        extends Form<BratAnnotatorModel>
    {
        private static final long serialVersionUID = 3635145598405490893L;

        public AnnotationFeatureForm(String id, BratAnnotatorModel aBModel)
        {
            super(id, new CompoundPropertyModel<BratAnnotatorModel>(aBModel));

            featureModels = new ArrayList<>();
            
            selectedTextLabel = new Label("selectedText");
            selectedTextLabel.setOutputMarkupId(true);
            add(selectedTextLabel);

            add(layers = new LayerSelector("selectedAnnotationLayer", annotationLayers));

            if (aBModel.getSelectedAnnotationId() == -1) {
                
            }
            else {
                // FIXME where to load from?
            }
            
            featureValues = new FeatureEditorPanel("featureValues");
            
            wmc = new WebMarkupContainer("wmc") {
                private static final long serialVersionUID = 8908304272310098353L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    
                    setVisible(!featureModels.isEmpty());
                }
            };
            // Add placeholder since wmc might start out invisible. Without the placeholder we
            // cannot make it visible in an AJAX call
            wmc.setOutputMarkupPlaceholderTag(true);
            wmc.setOutputMarkupId(true);
            wmc.add(featureValues);
            add(wmc);
            
            annotateButton = new AjaxButton("annotate")
            {
                private static final long serialVersionUID = 980971048279862290L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> form)
                {
                    BratAnnotatorModel model = AnnotationFeatureForm.this.getModelObject();

                    aTarget.addChildren(getPage(), FeedbackPanel.class);

                    if (model.getSelectedAnnotationLayer() == null) {
                        error("There is no annotation layer selected");
                        LOG.error("There is no annotation layer selected");
                        return;
                    }

                    if (!model.isRelationAnno() && model.getSelectedText().isEmpty()) {
                        error("There is no text selected to annotate");
                        LOG.error("There is no text selected to annotate");
                        return;
                    }
                    
                    try {
                        actionAnnotate(aTarget, model);
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

                    BratAnnotatorModel model = AnnotationFeatureForm.this.getModelObject();

                    if (model.isRelationAnno()) {
                        setEnabled(true);
                    }
                    else {
                        setEnabled(model.getSelectedText() != null
                                && !model.getSelectedText().equals(""));
                    }
                }
            };
            
            if (featureModels.isEmpty()) {
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
                    BratAnnotatorModel model = AnnotationFeatureForm.this.getModelObject();
                    setVisible(model.getSelectedAnnotationId() != -1);
                }

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);

                    try {
                        BratAnnotatorModel model = AnnotationFeatureForm.this.getModelObject();
                        actionDelete(aTarget, model);
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
                    BratAnnotatorModel model = AnnotationFeatureForm.this.getModelObject();
                    setVisible(model.isRelationAnno() && model.getSelectedAnnotationId() != -1);
                }

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    try {
                        BratAnnotatorModel model = AnnotationFeatureForm.this.getModelObject();
                        actionReverse(aTarget, model);
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

    private void actionAnnotate(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        if (aBModel.getSelectedAnnotationLayer() == null) {
            error("No layer is selected. First select a layer");
            return;
        }

        // Verify if input is valid according to tagset
        for (int i = 0; i < featureModels.size(); i++) {
            AnnotationFeature feature = featureModels.get(i).feature;
            if (CAS.TYPE_NAME_STRING.equals(feature.getType())) {
                String value = (String) featureModels.get(i).value;
                // Check if tag is necessary, set, and correct
                if (feature.getTagset() != null && !feature.getTagset().isCreateTag()
                        && !annotationService.existsTag(value, feature.getTagset())) {
                    error("[" + value
                            + "] is not in the tag list. Please choose form the existing tags");
                    return;
                }
            }
        }

        // If there is no annotation yet, create one. During creation, the adapter
        // may notice that it would create a duplicate and return the address of
        // an existing annotation instead of a new one.
        JCas jCas = getCas(aBModel);
        TypeAdapter adapter = getAdapter(annotationService, aBModel.getSelectedAnnotationLayer());

        if (aBModel.getSelectedAnnotationId() == -1) {
            if (aBModel.isRelationAnno()) {
                AnnotationFS originFs = selectByAddr(jCas, aBModel.getOriginSpanId());
                AnnotationFS targetFs = selectByAddr(jCas, aBModel.getTargetSpanId());
                if (adapter instanceof ArcAdapter) {
                    aBModel.setSelectedAnnotationId(((ArcAdapter) adapter).add(originFs, targetFs,
                            jCas, aBModel, null, null));
                }
                else {
                    aBModel.setSelectedAnnotationId(((ChainAdapter) adapter).addArc(jCas, originFs,
                            targetFs, null, null));
                }
                aBModel.setBeginOffset(originFs.getBegin());
            }
            else if (adapter instanceof SpanAdapter) {
                aBModel.setSelectedAnnotationId(((SpanAdapter) adapter).add(jCas,
                        aBModel.getBeginOffset(), aBModel.getEndOffset(), null, null));
            }
            else {
                aBModel.setSelectedAnnotationId(((ChainAdapter) adapter).addSpan(jCas,
                        aBModel.getBeginOffset(), aBModel.getEndOffset(), null, null));
            }
        }

        // Set feature values
        List<AnnotationFeature> features = new ArrayList<AnnotationFeature>();
        for (FeatureModel fm : featureModels) {
            features.add(fm.feature);

            // For string features with extensible tagsets, extend the tagset
            if (CAS.TYPE_NAME_STRING.equals(fm.feature.getType())) {
                @SuppressWarnings("unchecked")
                String value = (String) fm.value;

                if (fm.feature.getTagset() != null && fm.feature.getTagset().isCreateTag()
                        && !annotationService.existsTag(value, fm.feature.getTagset())) {
                    // Persist only if the feature value is actually set
                    if (value != null) {
                        Tag selectedTag = new Tag();
                        selectedTag.setName(value);
                        selectedTag.setTagSet(fm.feature.getTagset());
                        annotationService.createTag(selectedTag, aBModel.getUser());
                    }
                }
            }

            adapter.updateFeature(jCas, fm.feature, aBModel.getSelectedAnnotationId(),
                    fm.value);
        }

        // update timestamp now
        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas, aBModel.getBeginOffset());
        aBModel.getDocument().setSentenceAccessed(sentenceNumber);
        repository.updateTimeStamp(aBModel.getDocument(), aBModel.getUser(), aBModel.getMode());

        // persist changes
        repository.updateJCas(aBModel.getMode(), aBModel.getDocument(), aBModel.getUser(), jCas);

        if (aBModel.isScrollPage()) {
            autoScroll(jCas, aBModel);
        }

        if (aBModel.isRelationAnno()) {
            aBModel.setRememberedArcLayer(aBModel.getSelectedAnnotationLayer());
            aBModel.setRememberedArcFeatures(featureModels);
        }
        else {
            aBModel.setRememberedSpanLayer(aBModel.getSelectedAnnotationLayer());
            aBModel.setRememberedSpanFeatures(featureModels);
        }

        aBModel.setAnnotate(true);
        if (aBModel.getSelectedAnnotationId() != -1) {
            String bratLabelText = TypeUtil
                    .getBratLabelText(adapter,
                            BratAjaxCasUtil.selectByAddr(jCas, aBModel.getSelectedAnnotationId()),
                            features);
            info(BratAnnotator.generateMessage(aBModel.getSelectedAnnotationLayer(), bratLabelText,
                    false));
        }

        setLayerAndFeatureModels(jCas, aBModel);

        onChange(aTarget, aBModel);
    }

    private void actionDelete(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
        throws IOException, UIMAException, ClassNotFoundException
    {
        JCas jCas = getCas(aBModel);
        AnnotationFS fs = selectByAddr(jCas, aBModel.getSelectedAnnotationId());
        TypeAdapter adapter = getAdapter(annotationService, aBModel.getSelectedAnnotationLayer());
        String attachFeatureName = adapter.getAttachFeatureName();
        String attachTypeName = adapter.getAnnotationTypeName();

        Set<TypeAdapter> typeAdapters = new HashSet<TypeAdapter>();

        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aBModel.getProject())) {

            typeAdapters.add(getAdapter(annotationService, layer));
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
                        aBModel.getBeginOffset(), aBModel.getEndOffset());
                ad.deleteBySpan(jCas, fs, thisSentence.getBegin(), thisSentence.getEnd());
                break;
            }

            String fn = ad.getAttachFeatureName();
            if (fn == null) {
                continue;
            }
            if (fn.equals(attachFeatureName)) {
                Sentence thisSentence = BratAjaxCasUtil.getCurrentSentence(jCas,
                        aBModel.getBeginOffset(), aBModel.getEndOffset());
                ad.deleteBySpan(jCas, fs, thisSentence.getBegin(), thisSentence.getEnd());
                break;
            }
        }
        // BEGIN HACK - Issue 933
        if (adapter instanceof ChainAdapter) {
            ((ChainAdapter) adapter).setArc(false);
        }
        // END HACK - Issue 933
        adapter.delete(jCas, aBModel.getSelectedAnnotationId());

        repository.updateJCas(aBModel.getMode(), aBModel.getDocument(), aBModel.getUser(), jCas);
        // update timestamp now
        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas, aBModel.getBeginOffset());
        aBModel.getDocument().setSentenceAccessed(sentenceNumber);
        repository.updateTimeStamp(aBModel.getDocument(), aBModel.getUser(), aBModel.getMode());

        if (aBModel.isScrollPage()) {
            autoScroll(jCas, aBModel);
        }

        aBModel.setRememberedSpanLayer(aBModel.getSelectedAnnotationLayer());
        aBModel.setAnnotate(false);

        info(BratAnnotator.generateMessage(aBModel.getSelectedAnnotationLayer(), null, true));

        // A hack to remember the visual DropDown display value
        aBModel.setRememberedSpanLayer(aBModel.getSelectedAnnotationLayer());
        aBModel.setRememberedSpanFeatures(featureModels);

        aBModel.setSelectedText("");
        aBModel.setSelectedAnnotationId(-1);

        setLayerAndFeatureModels(jCas, aBModel);

        onChange(aTarget, aBModel);
    }

    private void actionReverse(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
        throws IOException, UIMAException, ClassNotFoundException, BratAnnotationException
    {
        JCas jCas;
        jCas = getCas(aBModel);

        AnnotationFS idFs = selectByAddr(jCas, aBModel.getSelectedAnnotationId());

        jCas.removeFsFromIndexes(idFs);

        AnnotationFS originFs = selectByAddr(jCas, aBModel.getOriginSpanId());
        AnnotationFS targetFs = selectByAddr(jCas, aBModel.getTargetSpanId());

        TypeAdapter adapter = getAdapter(annotationService, aBModel.getSelectedAnnotationLayer());
        if (adapter instanceof ArcAdapter) {
            for (FeatureModel fm : featureModels) {
                aBModel.setSelectedAnnotationId(((ArcAdapter) adapter).add(targetFs, originFs,
                        jCas, aBModel, fm.feature, fm.value));
            }
        }
        else {
            error("chains cannot be reversed");
            return;
        }

        // persist changes
        repository.updateJCas(aBModel.getMode(), aBModel.getDocument(), aBModel.getUser(), jCas);
        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas, originFs.getBegin());
        aBModel.getDocument().setSentenceAccessed(sentenceNumber);
        repository.updateTimeStamp(aBModel.getDocument(), aBModel.getUser(), aBModel.getMode());

        if (aBModel.isScrollPage()) {
            autoScroll(jCas, aBModel);
        }

        info("The arc has been reversed");
        aBModel.setRememberedArcLayer(aBModel.getSelectedAnnotationLayer());
        aBModel.setRememberedArcFeatures(featureModels);

        // in case the user re-reverse it
        int temp = aBModel.getOriginSpanId();
        aBModel.setOriginSpanId(aBModel.getTargetSpanId());
        aBModel.setTargetSpanId(temp);

        setLayerAndFeatureModels(jCas, aBModel);

        onChange(aTarget, aBModel);
    }

    private JCas getCas(BratAnnotatorModel aBModel)
        throws UIMAException, IOException, ClassNotFoundException
    {

        if (aBModel.getMode().equals(Mode.ANNOTATION) || aBModel.getMode().equals(Mode.AUTOMATION)
                || aBModel.getMode().equals(Mode.CORRECTION)
                || aBModel.getMode().equals(Mode.CORRECTION_MERGE)) {

            return repository.readJCas(aBModel.getDocument(), aBModel.getProject(),
                    aBModel.getUser());
        }
        else {
            return repository.getCurationDocumentContent(aBModel.getDocument());
        }
    }

    private void autoScroll(JCas jCas, BratAnnotatorModel aBModel)
    {
        int address = BratAjaxCasUtil.selectSentenceAt(jCas, aBModel.getSentenceBeginOffset(),
                aBModel.getSentenceEndOffset()).getAddress();
        aBModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas, address,
                aBModel.getBeginOffset(), aBModel.getProject(), aBModel.getDocument(),
                aBModel.getWindowSize()));

        Sentence sentence = selectByAddr(jCas, Sentence.class, aBModel.getSentenceAddress());
        aBModel.setSentenceBeginOffset(sentence.getBegin());
        aBModel.setSentenceEndOffset(sentence.getEnd());
    }

    public void setSlot(JCas aJCas, final BratAnnotatorModel aBModel, int aAnnotationId)
    {
        // Set an armed slot
        if (!aBModel.isRelationAnno() && aBModel.isSlotArmed()) {
            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) getFeatureModel(aBModel
                    .getArmedFeature()).value;
            LinkWithRoleModel link = links.get(aBModel.getArmedSlot());
            link.targetAddr = aAnnotationId;
            link.label = selectByAddr(aJCas, aAnnotationId).getCoveredText();
            aBModel.clearArmedSlot();
            return;
        }
    }
    
    public void setLayerAndFeatureModels(JCas aJCas, final BratAnnotatorModel aBModel)
    {
        annotationFeatureForm.setModelObject(aBModel);

        featureModels = new ArrayList<>();
        
        // Dragging an arc
        if (aBModel.isRelationAnno()) {
            long layerId = TypeUtil.getLayerId(aBModel.getOriginSpanType());
            AnnotationLayer spanLayer = annotationService.getLayer(layerId);

            // If we drag an arc between POS annotations, then the relation must be a dependency
            // relation.
            // FIXME - Actually this case should be covered by the last case - the database lookup!
            if (spanLayer.isBuiltIn() && spanLayer.getName().equals(POS.class.getName())) {
                aBModel.setSelectedAnnotationLayer(annotationService.getLayer(
                        Dependency.class.getName(), aBModel.getProject()));
            }
            // If we drag an arc in a chain layer, then the arc is of the same layer as the span
            // Chain layers consist of arcs and spans
            else 
            if (spanLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                // one layer both for the span and arc annotation
                aBModel.setSelectedAnnotationLayer(spanLayer);
            }
            // Otherwise, look up the possible relation layer(s) in the database.
            else {
                for (AnnotationLayer layer : annotationService.listAnnotationLayer(aBModel
                        .getProject())) {
                    if (layer.getAttachType() != null && layer.getAttachType().equals(spanLayer)) {
                        aBModel.setSelectedAnnotationLayer(layer);
                        break;
                    }
                }
            }
            
            // populate feature value
            if (aBModel.getSelectedAnnotationId() != -1) {
                AnnotationFS annoFs = BratAjaxCasUtil.selectByAddr(aJCas,
                        aBModel.getSelectedAnnotationId());
                
                populateFeatures(aBModel, annoFs);
            }
        }
        // Rapid annotation - get saved anno layers and features 
        else if (aBModel.getSelectedAnnotationId() == -1
                && aBModel.getRememberedSpanLayer() != null
                && !aBModel.getRememberedSpanLayer().getType().equals(WebAnnoConst.RELATION_TYPE)) {
            aBModel.setSelectedAnnotationLayer(aBModel.getRememberedSpanLayer());
            
            // populate feature value
            for (AnnotationFeature feature : annotationService.listAnnotationFeature(aBModel
                    .getSelectedAnnotationLayer())) {
                if (!feature.isEnabled() || isSuppressedFeature(aBModel, feature)) {
                    continue;
                }

                featureModels.add(new FeatureModel(feature, aBModel.getRememberedSpanFeatures()
                        .get(feature)));
            }
        }
        // Existing (span) annotation was selected
        else if (aBModel.getSelectedAnnotationId() != -1) {
            AnnotationFS annoFs = BratAjaxCasUtil.selectByAddr(aJCas,
                    aBModel.getSelectedAnnotationId());
            String type = annoFs.getType().getName();

            if (type.endsWith(ChainAdapter.CHAIN)) {
                type = type.substring(0, type.length() - ChainAdapter.CHAIN.length());
            }
            else if (type.endsWith(ChainAdapter.LINK)) {
                type = type.substring(0, type.length() - ChainAdapter.LINK.length());
            }

            aBModel.setSelectedAnnotationLayer(annotationService.getLayer(type,
                    aBModel.getProject()));

            // populate feature value
            for (AnnotationFeature feature : annotationService.listAnnotationFeature(aBModel
                    .getSelectedAnnotationLayer())) {
                if (!feature.isEnabled() || isSuppressedFeature(aBModel, feature)) {
                    continue;
                }

                featureModels.add(new FeatureModel(feature, (Serializable) BratAjaxCasUtil
                        .getFeature(annoFs, feature)));
            }
        }
        // No annotation possible
        else {
            aBModel.setSelectedAnnotationLayer(new AnnotationLayer());
        }
    }
    
    private static boolean isSuppressedFeature(BratAnnotatorModel aBModel, AnnotationFeature aFeature)
    {
        String featName = aFeature.getName();
        
        if (WebAnnoConst.CHAIN_TYPE.equals(aFeature.getLayer().getType())) {
            if (aBModel.isRelationAnno()) {
                return WebAnnoConst.COREFERENCE_TYPE_FEATURE.equals(featName);
            }
            else {
                return WebAnnoConst.COREFERENCE_RELATION_FEATURE.equals(featName);
            }
        }
        
        return false;
    }

    protected void onChange(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
    {
        // Overriden in BratAnnotator
    }

    public void setAnnotationLayers(BratAnnotatorModel aBModel)
    {
        loadSpanLayers(aBModel);
        if (annotationLayers.size() == 0) {
            aBModel.setSelectedAnnotationLayer(new AnnotationLayer());
        }
        else if (aBModel.getSelectedAnnotationLayer() == null) {
            if (aBModel.getRememberedSpanLayer() == null) {
                aBModel.setSelectedAnnotationLayer(annotationLayers.get(0));
            }
            else {
                aBModel.setSelectedAnnotationLayer(aBModel.getRememberedSpanLayer());
            }
        }
    }

    private void loadSpanLayers(BratAnnotatorModel aBModel)
    {
        annotationLayers.clear();
        if (aBModel.isRelationAnno()) {
            annotationLayers.add(aBModel.getSelectedAnnotationLayer());
            return;
        }

        for (AnnotationLayer layer : aBModel.getAnnotationLayers()) {
            if (layer.getType().equals(WebAnnoConst.RELATION_TYPE) || !layer.isEnabled()
                    || layer.getName().equals(Token.class.getName())) {
                continue;
            }
            annotationLayers.add(layer);
        }
    }

    public class FeatureEditorPanel extends RefreshingView<FeatureModel>
    {
        private static final long serialVersionUID = -8359786805333207043L;

        public FeatureEditorPanel(String aId)
        {
            super(aId);
            setOutputMarkupId(true);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void populateItem(final Item<FeatureModel> item)
        {
            // Feature editors that allow multiple values may want to update themselves,
            // e.g. to add another slot.
            item.setOutputMarkupId(true);

            FeatureModel fm = item.getModelObject();

            FeatureEditor frag;
            switch (fm.feature.getMultiValueMode()) {
            case NONE: {
                switch (fm.feature.getType()) {
                case CAS.TYPE_NAME_INTEGER: {
                    frag = new NumberFeatureEditor("editor", "numberFeatureEditor", item, fm);
                    break;
                }
                case CAS.TYPE_NAME_FLOAT: {
                    frag = new NumberFeatureEditor("editor", "numberFeatureEditor", item, fm);
                    break;
                }
                case CAS.TYPE_NAME_BOOLEAN: {
                    frag = new BooleanFeatureEditor("editor", "booleanFeatureEditor", item, fm);
                    break;
                }
                case CAS.TYPE_NAME_STRING: {
                    frag = new TextFeatureEditor("editor", "textFeatureEditor", item, fm);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported type [" + fm.feature.getType()
                            + "] on feature [" + fm.feature.getName() + "]");
                }
                break;
            }
            case ARRAY: {
                switch (fm.feature.getLinkMode()) {
                case WITH_ROLE: {
                    // If it is none of the primitive types, it must be a link feature
                    frag = new LinkFeatureEditor("editor", "linkFeatureEditor", item, fm);
                    break;

                }
                default:
                    throw new IllegalArgumentException("Unsupported link mode ["
                            + fm.feature.getLinkMode() + "] on feature [" + fm.feature.getName() + "]");
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported multi-value mode ["
                        + fm.feature.getMultiValueMode() + "] on feature [" + fm.feature.getName() + "]");
            }

            item.add(frag);

            if (item.getIndex() == 0) {
                // Put focus on first feature
                frag.getFocusComponent().add(new DefaultFocusBehavior());
            }
        }

        @Override
        protected Iterator<IModel<FeatureModel>> getItemModels()
        {
            ModelIteratorAdapter<FeatureModel> i = new ModelIteratorAdapter<FeatureModel>(
                    featureModels)
            {
                @Override
                protected IModel<FeatureModel> model(FeatureModel aObject)
                {
                    return Model.of(aObject);
                }
            };
            return i;
        }
    }

    public static abstract class FeatureEditor extends Fragment
    {
        public FeatureEditor(String aId, String aMarkupId, MarkupContainer aMarkupProvider,
                IModel<?> aModel)
        {
            super(aId, aMarkupId, aMarkupProvider, aModel);
        }

        abstract public Component getFocusComponent();
    }
    
    public static class NumberFeatureEditor<T extends Number> extends FeatureEditor
    {
        private final NumberTextField field;
        
        public NumberFeatureEditor(String aId, String aMarkupId, MarkupContainer aMarkupProvider,
                FeatureModel aModel)
        {
            super(aId, aMarkupId, aMarkupProvider, new CompoundPropertyModel<FeatureModel>(aModel));
            
            String featureLabel = aModel.feature.getUiName();
            if (aModel.feature.getTagset() != null) {
                featureLabel += " (" + aModel.feature.getTagset().getName() + ")";
            }
            add(new Label("feature", featureLabel));

            switch (aModel.feature.getType()) {
            case CAS.TYPE_NAME_INTEGER: {
                field = new NumberTextField<Integer>("value", Integer.class);
                add(field);
                break;
            }
            case CAS.TYPE_NAME_FLOAT: {
                field = new NumberTextField<Float>("value", Float.class);
                add(field);
                break;
            }
            default:
                throw new IllegalArgumentException("Type [" + aModel.feature.getType()
                        + "] cannot be rendered as a numeric input field");
            }
        }
        
        @Override
        public NumberTextField getFocusComponent()
        {
            return field;
        }
    };

    public static class BooleanFeatureEditor extends FeatureEditor
    {
        private final CheckBox field;
        
        public BooleanFeatureEditor(String aId, String aMarkupId, MarkupContainer aMarkupProvider,
                FeatureModel aModel)
        {
            super(aId, aMarkupId, aMarkupProvider, new CompoundPropertyModel<FeatureModel>(aModel));

            String featureLabel = aModel.feature.getUiName();
            if (aModel.feature.getTagset() != null) {
                featureLabel += " (" + aModel.feature.getTagset().getName() + ")";
            }
            add(new Label("feature", featureLabel));

            field = new CheckBox("value");
            add(field);
        }

        @Override
        public Component getFocusComponent()
        {
            return field;
        }
    };

    public class TextFeatureEditor
        extends FeatureEditor
    {
        private final AbstractTextComponent field;

        public TextFeatureEditor(String aId, String aMarkupId, MarkupContainer aMarkupProvider,
                FeatureModel aModel)
        {
            super(aId, aMarkupId, aMarkupProvider, new CompoundPropertyModel<FeatureModel>(aModel));

            String featureLabel = aModel.feature.getUiName();
            if (aModel.feature.getTagset() != null) {
                featureLabel += " (" + aModel.feature.getTagset().getName() + ")";
            }
            add(new Label("feature", featureLabel));

            if (aModel.feature.getTagset() != null) {
                List<Tag> tagset = annotationService.listTags(aModel.feature.getTagset());
                field = new ComboBox<Tag>("value", tagset,
                        new com.googlecode.wicket.kendo.ui.renderer.ChoiceRenderer<Tag>("name"));
            }
            else {
                field = new TextField<String>("value");
            }
            add(field);
        }

        @Override
        public Component getFocusComponent()
        {
            return field;
        }
    };

    public class LinkFeatureEditor
        extends FeatureEditor
    {
        private static final long serialVersionUID = 7469241620229001983L;
        
        private final AbstractTextComponent text;

        public LinkFeatureEditor(String aId, String aMarkupId, MarkupContainer aMarkupProvider,
                final FeatureModel aModel)
        {
            super(aId, aMarkupId, aMarkupProvider, new CompoundPropertyModel<FeatureModel>(aModel));

            add(new Label("feature", aModel.feature.getUiName()));

            add(new RefreshingView<LinkWithRoleModel>("slots", Model.of((List<LinkWithRoleModel>) aModel.value))
            {
                @Override
                protected Iterator<IModel<LinkWithRoleModel>> getItemModels()
                {
                    ModelIteratorAdapter<LinkWithRoleModel> i = new ModelIteratorAdapter<LinkWithRoleModel>(
                            (List<LinkWithRoleModel>) LinkFeatureEditor.this.getModelObject().value)
                    {
                        @Override
                        protected IModel<LinkWithRoleModel> model(LinkWithRoleModel aObject)
                        {
                            return Model.of(aObject);
                        }
                    };
                    return i;
                }

                @Override
                protected void populateItem(final Item<LinkWithRoleModel> aItem)
                {
                    aItem.setModel(new CompoundPropertyModel<LinkWithRoleModel>(aItem
                            .getModelObject()));

                    aItem.add(new Label("role"));
                    final Label label = new Label("label");
                    label.add(new AjaxEventBehavior("click") {
                        private static final long serialVersionUID = 7633309278417475424L;

                        @Override
                        protected void onEvent(AjaxRequestTarget aTarget)
                        {
                            BratAnnotatorModel model = annotationFeatureForm.getModelObject();
                            if (model.isArmedSlot(aModel.feature, aItem.getIndex())) {
                                model.clearArmedSlot();
                            }
                            else {
                                annotationFeatureForm.getModelObject().setArmedSlot(
                                        aModel.feature, aItem.getIndex());
                            }
                            aTarget.add(LinkFeatureEditor.this.getParent());
                        }
                    });
                    label.add(new AttributeAppender("style", new Model<String>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public String getObject()
                        {
                            BratAnnotatorModel model = annotationFeatureForm.getModelObject();
                            if (model.isArmedSlot(aModel.feature, aItem.getIndex())) {
                                return "; background: orange";
                            }
                            else {
                                return "";
                            }
                        }
                    }));
                    aItem.add(label);
                }
            });
            
            if (aModel.feature.getTagset() != null) {
                List<Tag> tagset = annotationService.listTags(aModel.feature.getTagset());
                 text = new ComboBox<Tag>(
                        "newRole", Model.of(""), tagset,
                        new com.googlecode.wicket.kendo.ui.renderer.ChoiceRenderer<Tag>(
                                "name"));
                add(text);
            }
            else {
                add(text = new TextField<String>("newRole", Model.of("")));                            
            }

            // Add a new empty slot with the specified role
            add(new AjaxButton("add") {
                private static final long serialVersionUID = 1L;
                
                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                            .getModelObject().value;
                    LinkWithRoleModel m = new LinkWithRoleModel();
                    m.role = (String) text.getModelObject();
                    links.add(m);
                    
                    aTarget.add(LinkFeatureEditor.this.getParent());
                }
            });                            
            
            // Add a new empty slot with the specified role
            add(new AjaxButton("del") {
                private static final long serialVersionUID = 1L;
                
                @Override
                protected void onConfigure()
                {
                    BratAnnotatorModel model = annotationFeatureForm.getModelObject();
                    setEnabled(model.isSlotArmed() && aModel.feature.equals(model.getArmedFeature()));
                }
                
                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                            .getModelObject().value;
                    
                    BratAnnotatorModel model = annotationFeatureForm.getModelObject();
                    links.remove(model.getArmedSlot());
                    model.clearArmedSlot();
                    
                    aTarget.add(LinkFeatureEditor.this.getParent());
                }
            });                            
        }
        
        public void setModelObject(FeatureModel aModel)
        {
            setDefaultModelObject(aModel);
        }
        
        @SuppressWarnings("unchecked")
        public FeatureModel getModelObject()
        {
            return (FeatureModel) getDefaultModelObject();
        }

        @Override
        public Component getFocusComponent()
        {
            return text;
        }
    };
    
    private void populateFeatures(BratAnnotatorModel aBModel, FeatureStructure aFS)
    {
        featureModels = new ArrayList<>();

        if (aFS != null) {
            // Populate from feature structure
            for (AnnotationFeature feature : annotationService.listAnnotationFeature(aBModel
                    .getSelectedAnnotationLayer())) {
                if (!feature.isEnabled() || isSuppressedFeature(aBModel, feature)) {
                    continue;
                }
        
                featureModels.add(new FeatureModel(feature, (Serializable) BratAjaxCasUtil
                        .getFeature(aFS, feature)));
            }
        }
        else if (!aBModel.isRelationAnno() && aBModel.getRememberedSpanFeatures() != null) {
            // Populate from remembered values
            for (AnnotationFeature feature : annotationService.listAnnotationFeature(aBModel
                    .getSelectedAnnotationLayer())) {
                if (!feature.isEnabled() || isSuppressedFeature(aBModel, feature)) {
                    continue;
                }

                featureModels.add(new FeatureModel(feature, aBModel.getRememberedSpanFeatures()
                        .get(feature)));
            }
        }
        else if (aBModel.isRelationAnno() && aBModel.getRememberedArcFeatures() != null) {
            // Populate from remembered values
            for (AnnotationFeature feature : annotationService.listAnnotationFeature(aBModel
                    .getSelectedAnnotationLayer())) {
                if (!feature.isEnabled() || isSuppressedFeature(aBModel, feature)) {
                    continue;
                }

                featureModels.add(new FeatureModel(feature, aBModel.getRememberedArcFeatures()
                        .get(feature)));
            }
        }
    }
    
    public class LayerSelector
        extends DropDownChoice<AnnotationLayer>
    {
        private static final long serialVersionUID = 2233133653137312264L;

        public LayerSelector(String aId, List<? extends AnnotationLayer> aChoices)
        {
            super(aId, aChoices);
            setOutputMarkupId(true);
            setChoiceRenderer(new ChoiceRenderer<AnnotationLayer>("uiName"));

            add(new AjaxFormComponentUpdatingBehavior("onchange")
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    BratAnnotatorModel model = annotationFeatureForm.getModelObject();

                    populateFeatures(model, null);
                    
                    aTarget.add(wmc);
                    aTarget.add(annotateButton);
                }
            });
        }

        @Override
        protected void onConfigure()
        {
            super.onConfigure();
            // at the moment we allow one layer per relation annotation (first
            // source/target span layer should be selected!)
            setNullValid(annotationFeatureForm.getModelObject().isRelationAnno());
            // Only allow layer selection on new annotations
            setEnabled(annotationFeatureForm.getModelObject().getSelectedAnnotationId() == -1);
        }
    }

    private FeatureModel getFeatureModel(AnnotationFeature aFeature)
    {
        for (FeatureModel f : featureModels) {
            if (f.feature.getId() == aFeature.getId()) {
                return f;
            }
        }
        return null;
    }
    
    /**
     * Represents a link with a role in the UI.
     */
    public static class LinkWithRoleModel
        implements Serializable
    {
        public String role;
        public String label = "<Click to arm>";
        public int targetAddr = -1;
    }
    
    public static class FeatureModel
        implements Serializable
    {
        public final AnnotationFeature feature;
        public Serializable value;
        
        public FeatureModel(AnnotationFeature aFeature, Serializable aValue)
        {
            feature = aFeature;
            value = aValue;
            
            // Avoid having null here because otherwise we have to handle null in zillion places!
            if (value == null && MultiValueMode.ARRAY.equals(aFeature.getMultiValueMode())) {
                value = new ArrayList<>();
            }
        }
    }
}
