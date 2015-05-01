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

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getFeature;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getSentenceBeginAddress;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.isSame;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectAt;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectSentenceAt;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.setFeature;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getAdapter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
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
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import com.googlecode.wicket.kendo.ui.form.NumberTextField;
import com.googlecode.wicket.kendo.ui.form.TextField;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.command.Selection;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.VID;
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

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    private AnnotationFeatureForm annotationFeatureForm;
    private Label selectedTextLabel;
    private CheckBox forwardAnnotation;
    @SuppressWarnings("unused")
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
            if (aBModel.getSelection().getAnnotation().isNotSet()) {

            }
            else {
                // FIXME where to load from?
            }

            selectedTextLabel = new Label("selectedText", PropertyModel.of(getModelObject(),
                    "selection.text"));
            selectedTextLabel.setOutputMarkupId(true);
            add(selectedTextLabel);

            add(forwardAnnotation = new CheckBox("forwardAnnotation")
            {
                private static final long serialVersionUID = 8908304272310098353L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();

                    BratAnnotatorModel model = AnnotationFeatureForm.this.getModelObject();

                    setEnabled(model.getSelectedAnnotationLayer() != null
                            && model.getSelectedAnnotationLayer().getType()
                                    .equals(WebAnnoConst.SPAN_TYPE));

                }
            });
            forwardAnnotation.add(new AjaxFormComponentUpdatingBehavior("onchange")
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    if (getModelObject().getSelection().getAnnotation().isSet()) {
                        getModelObject().setForwardAnnotation(false);// this is editing
                        aTarget.add(forwardAnnotation);
                    }
                    else {
                        getModelObject().setForwardAnnotation(
                                getModelObject().isForwardAnnotation());
                    }
                }
            });

            forwardAnnotation.setOutputMarkupId(true);

            add(layers = new LayerSelector("selectedAnnotationLayer", annotationLayers));

            featureValues = new FeatureEditorPanel("featureValues");

            wmc = new WebMarkupContainer("wmc")
            {
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

                    setEnabled(model.getSelection().getAnnotation().isNotSet());
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
                    setVisible(model.getSelection().getAnnotation().isSet());
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
                    setVisible(model.getSelection().isRelationAnno()
                            && model.getSelection().getAnnotation().isSet());
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
            reverseButton.setOutputMarkupPlaceholderTag(true);
        }
    }

    public void actionAnnotate(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        // If there is no annotation yet, create one. During creation, the adapter
        // may notice that it would create a duplicate and return the address of
        // an existing annotation instead of a new one.
        JCas jCas = getCas(aBModel);

        actionAnnotate(aTarget, aBModel, jCas);
    }

    public void actionAnnotate(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel, JCas jCas)
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

        TypeAdapter adapter = getAdapter(annotationService, aBModel.getSelectedAnnotationLayer());

        Selection selection = aBModel.getSelection();
        if (selection.getAnnotation().isNotSet()) {
            if (selection.isRelationAnno()) {
                AnnotationFS originFs = selectByAddr(jCas, selection.getOrigin());
                AnnotationFS targetFs = selectByAddr(jCas, selection.getTarget());
                if (adapter instanceof ArcAdapter) {
                    AnnotationFS arc = ((ArcAdapter) adapter).add(originFs, targetFs, jCas,
                            aBModel, null, null);
                    selection.setAnnotation(new VID(getAddr(arc)));
                }
                else {
                    selection.setAnnotation(new VID(((ChainAdapter) adapter).addArc(jCas,
                            originFs, targetFs, null, null)));
                }
                selection.setBegin(originFs.getBegin());
            }
            else if (adapter instanceof SpanAdapter) {
                selection.setAnnotation(new VID(((SpanAdapter) adapter).add(jCas,
                        selection.getBegin(), selection.getEnd(), null, null)));
            }
            else {
                selection.setAnnotation(new VID(((ChainAdapter) adapter).addSpan(jCas,
                        selection.getBegin(), selection.getEnd(), null, null)));
            }
        }

        // Set feature values
        List<AnnotationFeature> features = new ArrayList<AnnotationFeature>();
        for (FeatureModel fm : featureModels) {
            features.add(fm.feature);

            // For string features with extensible tagsets, extend the tagset
            if (CAS.TYPE_NAME_STRING.equals(fm.feature.getType())) {
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

            adapter.updateFeature(jCas, fm.feature, aBModel.getSelection().getAnnotation()
                    .getId(), fm.value);
        }

        // Update progress information
        int sentenceNumber = getSentenceNumber(jCas, aBModel.getSelection().getBegin());
        aBModel.getDocument().setSentenceAccessed(sentenceNumber);

        // persist changes
        repository.writeCas(aBModel.getMode(), aBModel.getDocument(), aBModel.getUser(), jCas);

        if (aBModel.getPreferences().isScrollPage()) {
            autoScroll(jCas, aBModel);
        }

        if (aBModel.getSelection().isRelationAnno()) {
            aBModel.setRememberedArcLayer(aBModel.getSelectedAnnotationLayer());
            aBModel.setRememberedArcFeatures(featureModels);
        }
        else {
            aBModel.setRememberedSpanLayer(aBModel.getSelectedAnnotationLayer());
            aBModel.setRememberedSpanFeatures(featureModels);
        }

        aBModel.getSelection().setAnnotate(true);
        if (aBModel.getSelection().getAnnotation().isSet()) {
            String bratLabelText = TypeUtil.getBratLabelText(adapter,
                    selectByAddr(jCas, aBModel.getSelection().getAnnotation().getId()),
                    features);
            info(BratAnnotator.generateMessage(aBModel.getSelectedAnnotationLayer(), bratLabelText,
                    false));
        }

        onChange(aTarget, aBModel);
    }

    private void actionDelete(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
        throws IOException, UIMAException, ClassNotFoundException
    {
        JCas jCas = getCas(aBModel);
        AnnotationFS fs = selectByAddr(jCas, aBModel.getSelection().getAnnotation()
                .getId());

        // TODO We assume here that the selected annotation layer corresponds to the type of the
        // FS to be deleted. It would be more robust if we could get the layer from the FS itself.
        AnnotationLayer layer = aBModel.getSelectedAnnotationLayer();
        TypeAdapter adapter = getAdapter(annotationService, layer);

        // == DELETE ATTACHED RELATIONS ==
        // If the deleted FS is a span, we must delete all relations that
        // point to it directly or indirectly via the attachFeature.
        //
        // NOTE: It is important that this happens before UNATTACH SPANS since the attach feature
        // is no longer set after UNATTACH SPANS!
        if (adapter instanceof SpanAdapter) {
            for (AnnotationLayer relationLayer : annotationService
                    .listAttachedRelationLayers(layer)) {
                ArcAdapter relationAdapter = (ArcAdapter) getAdapter(annotationService,
                        relationLayer);
                Type relationType = CasUtil.getType(jCas.getCas(), relationLayer.getName());
                Feature sourceFeature = relationType.getFeatureByBaseName(relationAdapter
                        .getSourceFeatureName());
                Feature targetFeature = relationType.getFeatureByBaseName(relationAdapter
                        .getTargetFeatureName());

                // This code is already prepared for the day that relations can go between
                // different layers and may have different attach features for the source and
                // target layers.
                Feature relationSourceAttachFeature = null;
                Feature relationTargetAttachFeature = null;
                if (relationAdapter.getAttachFeatureName() != null) {
                    relationSourceAttachFeature = sourceFeature.getRange().getFeatureByBaseName(
                            relationAdapter.getAttachFeatureName());
                    relationTargetAttachFeature = targetFeature.getRange().getFeatureByBaseName(
                            relationAdapter.getAttachFeatureName());
                }

                List<AnnotationFS> toBeDeleted = new ArrayList<AnnotationFS>();
                for (AnnotationFS relationFS : CasUtil.select(jCas.getCas(), relationType)) {
                    // Here we get the annotations that the relation is pointing to in the UI
                    FeatureStructure sourceFS;
                    if (relationSourceAttachFeature != null) {
                        sourceFS = relationFS.getFeatureValue(sourceFeature).getFeatureValue(
                                relationSourceAttachFeature);
                    }
                    else {
                        sourceFS = relationFS.getFeatureValue(sourceFeature);
                    }

                    FeatureStructure targetFS;
                    if (relationTargetAttachFeature != null) {
                        targetFS = relationFS.getFeatureValue(targetFeature).getFeatureValue(
                                relationTargetAttachFeature);
                    }
                    else {
                        targetFS = relationFS.getFeatureValue(targetFeature);
                    }

                    if (isSame(sourceFS, fs) || isSame(targetFS, fs)) {
                        toBeDeleted.add(relationFS);
                        LOG.debug("Deleted relation [" + getAddr(relationFS) + "] from layer ["
                                + relationLayer.getName() + "]");
                    }
                }

                for (AnnotationFS attachedFs : toBeDeleted) {
                    jCas.getCas().removeFsFromIndexes(attachedFs);
                }
            }
        }

        // == DELETE ATTACHED SPANS ==
        // This case is currently not implemented because WebAnno currently does not allow to
        // create spans that attach to other spans. The only span type for which this is relevant
        // is the Token type which cannot be deleted.

        // == UNATTACH SPANS ==
        // If the deleted FS is a span that is attached to another span, the
        // attachFeature in the other span must be set to null. Typical example: POS is deleted, so
        // the pos feature of Token must be set to null. This is a quick case, because we only need
        // to look at span annotations that have the same offsets as the FS to be deleted.
        if (adapter instanceof SpanAdapter && layer.getAttachType() != null) {
            Type spanType = CasUtil.getType(jCas.getCas(), layer.getAttachType().getName());
            Feature attachFeature = spanType.getFeatureByBaseName(layer.getAttachFeature()
                    .getName());

            for (AnnotationFS attachedFs : selectAt(jCas.getCas(), spanType, fs.getBegin(),
                    fs.getEnd())) {
                if (isSame(attachedFs.getFeatureValue(attachFeature), fs)) {
                    attachedFs.setFeatureValue(attachFeature, null);
                    LOG.debug("Unattached [" + attachFeature.getShortName() + "] on annotation ["
                            + getAddr(attachedFs) + "]");
                }
            }
        }

        // == CLEAN UP LINK FEATURES ==
        // If the deleted FS is a span that is the target of a link feature, we must unset that
        // link and delete the slot if it is a multi-valued link. Here, we have to scan all
        // annotations from layers that have link features that could point to the FS
        // to be deleted: the link feature must be the type of the FS or it must be generic.
        if (adapter instanceof SpanAdapter) {
            for (AnnotationFeature linkFeature : annotationService.listAttachedLinkFeatures(layer)) {
                Type linkType = CasUtil.getType(jCas.getCas(), linkFeature.getLayer().getName());

                for (AnnotationFS linkFS : CasUtil.select(jCas.getCas(), linkType)) {
                    List<LinkWithRoleModel> links = getFeature(linkFS, linkFeature);
                    Iterator<LinkWithRoleModel> i = links.iterator();
                    boolean modified = false;
                    while (i.hasNext()) {
                        LinkWithRoleModel link = i.next();
                        if (link.targetAddr == getAddr(fs)) {
                            i.remove();
                            LOG.debug("Cleared slot [" + link.role + "] in feature ["
                                    + linkFeature.getName() + "] on annotation [" + getAddr(linkFS)
                                    + "]");
                            modified = true;
                        }
                    }
                    if (modified) {
                        setFeature(linkFS, linkFeature, links);
                    }
                }
            }
        }

        // If the deleted FS is a relation, we don't have to do anything. Nothing can point to a
        // relation.
        if (adapter instanceof ArcAdapter) {
            // Do nothing ;)
        }

        // BEGIN HACK - Issue 933
        if (adapter instanceof ChainAdapter) {
            ((ChainAdapter) adapter).setArc(false);
        }
        // END HACK - Issue 933

        // Actually delete annotation
        adapter.delete(jCas, aBModel.getSelection().getAnnotation().getId());

        // Store CAS again
        repository.writeCas(aBModel.getMode(), aBModel.getDocument(), aBModel.getUser(), jCas);

        // Update progress information
        int sentenceNumber = getSentenceNumber(jCas, aBModel.getSelection().getBegin());
        aBModel.getDocument().setSentenceAccessed(sentenceNumber);

        // Auto-scroll
        if (aBModel.getPreferences().isScrollPage()) {
            autoScroll(jCas, aBModel);
        }

        aBModel.setRememberedSpanLayer(aBModel.getSelectedAnnotationLayer());
        aBModel.getSelection().setAnnotate(false);

        info(BratAnnotator.generateMessage(aBModel.getSelectedAnnotationLayer(), null, true));

        // A hack to remember the visual DropDown display value
        aBModel.setRememberedSpanLayer(aBModel.getSelectedAnnotationLayer());
        aBModel.setRememberedSpanFeatures(featureModels);

        aBModel.getSelection().clear();

        setLayerAndFeatureModels(jCas, aBModel);

        aTarget.add(wmc);
        aTarget.add(deleteButton);
        aTarget.add(reverseButton);
        onChange(aTarget, aBModel);
    }

    private void actionReverse(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
        throws IOException, UIMAException, ClassNotFoundException, BratAnnotationException
    {
        JCas jCas;
        jCas = getCas(aBModel);

        AnnotationFS idFs = selectByAddr(jCas, aBModel.getSelection().getAnnotation().getId());

        jCas.removeFsFromIndexes(idFs);

        AnnotationFS originFs = selectByAddr(jCas, aBModel.getSelection().getOrigin());
        AnnotationFS targetFs = selectByAddr(jCas, aBModel.getSelection().getTarget());

        TypeAdapter adapter = getAdapter(annotationService, aBModel.getSelectedAnnotationLayer());
        if (adapter instanceof ArcAdapter) {
            for (FeatureModel fm : featureModels) {
                AnnotationFS arc = ((ArcAdapter) adapter).add(targetFs, originFs, jCas, aBModel,
                        fm.feature, fm.value);
                aBModel.getSelection().setAnnotation(new VID(getAddr(arc)));
            }
        }
        else {
            error("chains cannot be reversed");
            return;
        }

        // persist changes
        repository.writeCas(aBModel.getMode(), aBModel.getDocument(), aBModel.getUser(), jCas);
        int sentenceNumber = getSentenceNumber(jCas, originFs.getBegin());
        aBModel.getDocument().setSentenceAccessed(sentenceNumber);

        if (aBModel.getPreferences().isScrollPage()) {
            autoScroll(jCas, aBModel);
        }

        info("The arc has been reversed");
        aBModel.setRememberedArcLayer(aBModel.getSelectedAnnotationLayer());
        aBModel.setRememberedArcFeatures(featureModels);

        // in case the user re-reverse it
        int temp = aBModel.getSelection().getOrigin();
        aBModel.getSelection().setOrigin(aBModel.getSelection().getTarget());
        aBModel.getSelection().setTarget(temp);

        setLayerAndFeatureModels(jCas, aBModel);

        onChange(aTarget, aBModel);
    }

    protected JCas getCas(BratAnnotatorModel aBModel)
        throws UIMAException, IOException, ClassNotFoundException
    {

        if (aBModel.getMode().equals(Mode.ANNOTATION) || aBModel.getMode().equals(Mode.AUTOMATION)
                || aBModel.getMode().equals(Mode.CORRECTION)
                || aBModel.getMode().equals(Mode.CORRECTION_MERGE)) {

            return repository.readAnnotationCas(aBModel.getDocument(), aBModel.getUser());
        }
        else {
            return repository.readCurationCas(aBModel.getDocument());
        }
    }

    private void autoScroll(JCas jCas, BratAnnotatorModel aBModel)
    {
        int address = getAddr(selectSentenceAt(jCas, aBModel.getSentenceBeginOffset(),
                aBModel.getSentenceEndOffset()));
        aBModel.setSentenceAddress(getSentenceBeginAddress(jCas, address, aBModel.getSelection()
                .getBegin(), aBModel.getProject(), aBModel.getDocument(), aBModel
                .getPreferences().getWindowSize()));

        Sentence sentence = selectByAddr(jCas, Sentence.class, aBModel.getSentenceAddress());
        aBModel.setSentenceBeginOffset(sentence.getBegin());
        aBModel.setSentenceEndOffset(sentence.getEnd());
    }

    @SuppressWarnings("unchecked")
    public void setSlot(AjaxRequestTarget aTarget, JCas aJCas, final BratAnnotatorModel aBModel,
            int aAnnotationId)
    {
        // Set an armed slot
        if (!aBModel.getSelection().isRelationAnno() && aBModel.isSlotArmed()) {
            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) getFeatureModel(aBModel
                    .getArmedFeature()).value;
            LinkWithRoleModel link = links.get(aBModel.getArmedSlot());
            link.targetAddr = aAnnotationId;
            link.label = selectByAddr(aJCas, aAnnotationId).getCoveredText();
            aBModel.clearArmedSlot();
        }

        // Auto-commit if working on existing annotation
        if (annotationFeatureForm.getModelObject().getSelection().getAnnotation().isSet()) {
            try {
                actionAnnotate(aTarget, annotationFeatureForm.getModelObject(), aJCas);
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
    }

    public void setLayerAndFeatureModels(JCas aJCas, final BratAnnotatorModel aBModel)
    {
        annotationFeatureForm.setModelObject(aBModel);

        featureModels = new ArrayList<>();

        // Dragging an arc
        if (aBModel.getSelection().isRelationAnno()) {
            long layerId = TypeUtil.getLayerId(aBModel.getSelection().getOriginType());
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
            else if (spanLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
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
            if (aBModel.getSelection().getAnnotation().isSet()) {
                AnnotationFS annoFs = selectByAddr(aJCas, aBModel.getSelection().getAnnotation()
                        .getId());

                populateFeatures(aBModel, annoFs);
            }
        }
        // Rapid annotation - get saved anno layers and features
        else if (aBModel.getSelection().getAnnotation().isNotSet()
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
        else if (aBModel.getSelection().getAnnotation().isSet()) {
            AnnotationFS annoFs = selectByAddr(aJCas, aBModel.getSelection().getAnnotation()
                    .getId());
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
        else {
            setInitSpanLayers(aBModel);
        }
    }

    private static boolean isSuppressedFeature(BratAnnotatorModel aBModel,
            AnnotationFeature aFeature)
    {
        String featName = aFeature.getName();

        if (WebAnnoConst.CHAIN_TYPE.equals(aFeature.getLayer().getType())) {
            if (aBModel.getSelection().isRelationAnno()) {
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
        setInitSpanLayers(aBModel);
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
        setInitSpanFeatures(aBModel);
    }

    private void setInitSpanFeatures(BratAnnotatorModel aBModel)
    {
        List<FeatureModel> nonLinkFModels = new ArrayList<FeatureModel>();
        for (FeatureModel nonLinkF : featureModels) {
            if (nonLinkF.feature.getLinkTypeName() == null) {
                nonLinkFModels.add(nonLinkF);
            }
        }
        featureModels.removeAll(nonLinkFModels);
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(aBModel
                .getSelectedAnnotationLayer())) {
            if (!feature.isEnabled() || isSuppressedFeature(aBModel, feature)) {
                continue;
            }
            if (feature.getLinkTypeName() != null) {
                continue;
            }
            if (aBModel.getRememberedSpanFeatures().get(feature) != null) {

                featureModels.add(new FeatureModel(feature, aBModel.getRememberedSpanFeatures()
                        .get(feature)));
            }
            else if (aBModel.getRememberedArcFeatures().get(feature) != null) {

                featureModels.add(new FeatureModel(feature, aBModel.getRememberedArcFeatures().get(
                        feature)));
            }
            else if (feature.getTagset() != null) {
                featureModels.add(new FeatureModel(feature, annotationService
                        .listTags(feature.getTagset()).get(0).getName()));
            }
            else {
                featureModels.add(new FeatureModel(feature, null));
            }
        }
    }

    private void setInitSpanLayers(BratAnnotatorModel aBModel)
    {
        annotationLayers.clear();
        if (aBModel.getSelection().isRelationAnno()) {
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

    public class FeatureEditorPanel
        extends RefreshingView<FeatureModel>
    {
        private static final long serialVersionUID = -8359786805333207043L;

        public FeatureEditorPanel(String aId)
        {
            super(aId);
            setOutputMarkupId(true);
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected void populateItem(final Item<FeatureModel> item)
        {
            // Feature editors that allow multiple values may want to update themselves,
            // e.g. to add another slot.
            item.setOutputMarkupId(true);

            final FeatureModel fm = item.getModelObject();

            final FeatureEditor frag;
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
                            + fm.feature.getLinkMode() + "] on feature [" + fm.feature.getName()
                            + "]");
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported multi-value mode ["
                        + fm.feature.getMultiValueMode() + "] on feature [" + fm.feature.getName()
                        + "]");
            }
            item.add(frag);
            // whenever it is updating an annotation, it updates automatically when a component for
            // the feature lost focus - but updating is for every component edited
            // LinkFeatureEditors must be excluded because the auto-update will break the ability
            // to add slots. Adding a slot is NOT an annotation action.
            if (annotationFeatureForm.getModelObject().getSelection().getAnnotation().isSet()
                    && !(frag instanceof LinkFeatureEditor)) {
                if (frag.isDropOrchoice()) {
                    updateFeature(fm, frag, "onchange");
                }
                else {
                    updateFeature(fm, frag, "onblur");
                }
            }

            if (item.getIndex() == 0) {
                // Put focus on first feature
                frag.getFocusComponent().add(new DefaultFocusBehavior());
            }
        }

        private void updateFeature(final FeatureModel aFm, final FeatureEditor aFrag, String aEvent)
        {
            aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior(aEvent)
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    try {
                        actionAnnotate(aTarget, annotationFeatureForm.getModelObject());
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

    public static abstract class FeatureEditor
        extends Fragment
    {
        private static final long serialVersionUID = -7275181609671919722L;

        public FeatureEditor(String aId, String aMarkupId, MarkupContainer aMarkupProvider,
                IModel<?> aModel)
        {
            super(aId, aMarkupId, aMarkupProvider, aModel);
        }

        abstract public Component getFocusComponent();

        abstract public boolean isDropOrchoice();
    }

    public static class NumberFeatureEditor<T extends Number>
        extends FeatureEditor
    {
        private static final long serialVersionUID = -2426303638953208057L;
        @SuppressWarnings("rawtypes")
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

        @SuppressWarnings("rawtypes")
        @Override
        public NumberTextField getFocusComponent()
        {
            return field;
        }

        @Override
        public boolean isDropOrchoice()
        {
            return false;
        }
    };

    public static class BooleanFeatureEditor
        extends FeatureEditor
    {
        private static final long serialVersionUID = 5104979547245171152L;
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

        @Override
        public boolean isDropOrchoice()
        {
            return true;
        }
    };

    public class TextFeatureEditor
        extends FeatureEditor
    {
        private static final long serialVersionUID = 7763348613632105600L;
        @SuppressWarnings("rawtypes")
        private final AbstractTextComponent field;
        private boolean isDrop;

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
                isDrop = true;
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

        @Override
        public boolean isDropOrchoice()
        {
            return isDrop;
        }
    };

    public class LinkFeatureEditor
        extends FeatureEditor
    {
        private static final long serialVersionUID = 7469241620229001983L;

        @SuppressWarnings("rawtypes")
        private final AbstractTextComponent text;
        private boolean isDrop;

        @SuppressWarnings("unchecked")
        public LinkFeatureEditor(String aId, String aMarkupId, MarkupContainer aMarkupProvider,
                final FeatureModel aModel)
        {
            super(aId, aMarkupId, aMarkupProvider, new CompoundPropertyModel<FeatureModel>(aModel));

            add(new Label("feature", aModel.feature.getUiName()));

            add(new RefreshingView<LinkWithRoleModel>("slots",
                    Model.of((List<LinkWithRoleModel>) aModel.value))
            {
                private static final long serialVersionUID = 5475284956525780698L;

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
                    label.add(new AjaxEventBehavior("click")
                    {
                        private static final long serialVersionUID = 7633309278417475424L;

                        @Override
                        protected void onEvent(AjaxRequestTarget aTarget)
                        {
                            BratAnnotatorModel model = annotationFeatureForm.getModelObject();
                            if (model.isArmedSlot(aModel.feature, aItem.getIndex())) {
                                model.clearArmedSlot();
                            }
                            else {
                                annotationFeatureForm.getModelObject().setArmedSlot(aModel.feature,
                                        aItem.getIndex());
                            }
                            aTarget.add(wmc);
                        }
                    });
                    label.add(new AttributeAppender("style", new Model<String>()
                    {
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
                text = new ComboBox<Tag>("newRole", Model.of(""), tagset,
                        new com.googlecode.wicket.kendo.ui.renderer.ChoiceRenderer<Tag>("name"));
                add(text);
                isDrop = true;
            }
            else {
                add(text = new TextField<String>("newRole", Model.of("")));
            }

            // Add a new empty slot with the specified role
            add(new AjaxButton("add")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    if (StringUtils.isBlank((String) text.getModelObject())) {
                        error("Must set slot label before adding!");
                        aTarget.addChildren(getPage(), FeedbackPanel.class);
                    }
                    else {
                        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                                .getModelObject().value;
                        LinkWithRoleModel m = new LinkWithRoleModel();
                        m.role = (String) text.getModelObject();
                        links.add(m);

                        aTarget.add(wmc);
                    }
                }
            });

            // Add a new empty slot with the specified role
            add(new AjaxButton("del")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    BratAnnotatorModel model = annotationFeatureForm.getModelObject();
                    setEnabled(model.isSlotArmed()
                            && aModel.feature.equals(model.getArmedFeature()));
                }

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                            .getModelObject().value;

                    BratAnnotatorModel model = annotationFeatureForm.getModelObject();
                    links.remove(model.getArmedSlot());
                    model.clearArmedSlot();

                    aTarget.add(wmc);

                    // Auto-commit if working on existing annotation
                    if (annotationFeatureForm.getModelObject().getSelection()
                            .getAnnotation().isSet()) {
                        try {
                            actionAnnotate(aTarget, annotationFeatureForm.getModelObject());
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
                }
            });
        }

        public void setModelObject(FeatureModel aModel)
        {
            setDefaultModelObject(aModel);
        }

        public FeatureModel getModelObject()
        {
            return (FeatureModel) getDefaultModelObject();
        }

        @Override
        public Component getFocusComponent()
        {
            return text;
        }

        @Override
        public boolean isDropOrchoice()
        {
            return isDrop;
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
        else if (!aBModel.getSelection().isRelationAnno() && aBModel.getRememberedSpanFeatures() != null) {
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
        else if (aBModel.getSelection().isRelationAnno() && aBModel.getRememberedArcFeatures() != null) {
            // Populate from remembered values
            for (AnnotationFeature feature : annotationService.listAnnotationFeature(aBModel
                    .getSelectedAnnotationLayer())) {
                if (!feature.isEnabled() || isSuppressedFeature(aBModel, feature)) {
                    continue;
                }

                featureModels.add(new FeatureModel(feature, aBModel.getRememberedArcFeatures().get(
                        feature)));
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
            setNullValid(annotationFeatureForm.getModelObject().getSelection().isRelationAnno());
            // Only allow layer selection on new annotations
            setEnabled(annotationFeatureForm.getModelObject().getSelection()
                    .getAnnotation().isNotSet());
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
        private static final long serialVersionUID = 2027345278696308900L;
        public String role;
        public String label = "<Click to arm>";
        public int targetAddr = -1;
    }

    public static class FeatureModel
        implements Serializable
    {
        private static final long serialVersionUID = 3512979848975446735L;
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
