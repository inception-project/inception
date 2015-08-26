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
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getSentenceBeginAddress;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.isSame;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectAt;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectSentenceAt;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.setFeature;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getAdapter;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.NoResultException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
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

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.jquery.ui.widget.tooltip.TooltipBehavior;
import com.googlecode.wicket.kendo.ui.form.NumberTextField;
import com.googlecode.wicket.kendo.ui.form.TextField;
import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
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
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.Evaluator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ValuesGenerator;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Annotation Detail Editor Panel.
 *
 */
public class AnnotationDetailEditorPanel
    extends Panel
{
    private static final long serialVersionUID = 7324241992353693848L;
    private static final Log LOG = LogFactory.getLog(AnnotationDetailEditorPanel.class);

    private final static List<String> BRUSHS = asList(WebAnnoConst.SPAN_TYPE,WebAnnoConst.RELATION_TYPE);

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    private AnnotationFeatureForm annotationFeatureForm;
    private Label selectedTextLabel;
    private CheckBox forwardAnnotation;

    private AjaxButton deleteButton;
    private AjaxButton reverseButton;

    private LayerSelector layer;
    private BrushSelector brush;
    
    private String brushModel = WebAnnoConst.SPAN_TYPE;

    private List<AnnotationLayer> annotationLayers = new ArrayList<AnnotationLayer>();

    private List<FeatureModel> featureModels;
    BratAnnotatorModel bModel;
    /**
     * Function to return tooltip using jquery
     *Docs for the JQuery tooltip widget that we configure below:
     *https://api.jqueryui.com/tooltip/
     */
    private final String functionForTooltip = "function() { return "
            + "'<div class=\"tooltip-title\">'+($(this).text() "
            + "? $(this).text() : 'no title')+'</div>"
            + "<div class=\"tooltip-content tooltip-pre\">'+($(this).attr('title') "
            + "? $(this).attr('title') : 'no description' )+'</div>' }";

    public AnnotationDetailEditorPanel(String id, IModel<BratAnnotatorModel> aModel)
    {
        super(id, aModel);
        bModel = aModel.getObject();
        annotationFeatureForm = new AnnotationFeatureForm("annotationFeatureForm",
                aModel.getObject());

        annotationFeatureForm.setOutputMarkupId(true);
        add(annotationFeatureForm);
        /*
         * relationAnnotationFeatureForm = new
         * AnnotationFeatureForm("relationAnnotationFeatureForm", aModel.getObject());
         * 
         * relationAnnotationFeatureForm.setOutputMarkupId(true);
         * add(relationAnnotationFeatureForm);
         */
    }

    private class AnnotationFeatureForm
        extends Form<BratAnnotatorModel>
    {
        private static final long serialVersionUID = 3635145598405490893L;
        private WebMarkupContainer featureEditorsContainer;

        public AnnotationFeatureForm(String id, BratAnnotatorModel aBModel)
        {
            super(id, new CompoundPropertyModel<BratAnnotatorModel>(aBModel));

            featureModels = new ArrayList<>();

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

                    setEnabled(bModel.getSelectedAnnotationLayer() != null
                            && bModel.getSelectedAnnotationLayer().getId() > 0
                            && bModel.getSelectedAnnotationLayer().getType()
                                    .equals(WebAnnoConst.SPAN_TYPE)
                            && bModel.getSelectedAnnotationLayer().isLockToTokenOffset());
                    updateForwardAnnotation(bModel);

                }
            });
            forwardAnnotation.add(new AjaxFormComponentUpdatingBehavior("onchange")
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    updateForwardAnnotation(getModelObject());
                    aTarget.add(forwardAnnotation);
                }
            });

            forwardAnnotation.setOutputMarkupId(true);

            add(deleteButton = new AjaxButton("delete")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(bModel.getSelection().getAnnotation().isSet());

                    // Avoid deleting in read-only layers
                    setEnabled(bModel.getSelectedAnnotationLayer() != null
                            && !bModel.getSelectedAnnotationLayer().isReadonly());
                }

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);

                    try {
                        actionDelete(aTarget, bModel);
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

            add(reverseButton = new AjaxButton("reverse")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(isRelation()
                            && bModel.getSelection().getAnnotation().isSet());

                    // Avoid reversing in read-only layers
                    setEnabled(bModel.getSelectedAnnotationLayer() != null
                            && !bModel.getSelectedAnnotationLayer().isReadonly());
                }

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    try {
                        actionReverse(aTarget, bModel);
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

            add(layer = new LayerSelector("selectedAnnotationLayer", annotationLayers, false));
            add(brush = new BrushSelector("brush", Model.of(brushModel), BRUSHS));

            RefreshingView<FeatureModel> featureValues = new FeatureEditorPanelContent(
                    "featureValues");

            featureEditorsContainer = new WebMarkupContainer("featureEditorsContainer")
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
            featureEditorsContainer.setOutputMarkupPlaceholderTag(true);
            featureEditorsContainer.setOutputMarkupId(true);
            featureEditorsContainer.add(featureValues);
            add(featureEditorsContainer);
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
            error("No layer is selected. First select a layer.");
            aTarget.addChildren(getPage(), FeedbackPanel.class);
            return;
        }

        if (aBModel.getSelectedAnnotationLayer().isReadonly()) {
            error("Layer is not editable.");
            aTarget.addChildren(getPage(), FeedbackPanel.class);
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
            if (isRelation()) {
                AnnotationFS originFs = selectByAddr(jCas, selection.getOrigin());
                AnnotationFS targetFs = selectByAddr(jCas, selection.getTarget());
                if (adapter instanceof ArcAdapter) {
                    AnnotationFS arc = ((ArcAdapter) adapter).add(originFs, targetFs, jCas,
                            aBModel, null, null);
                    selection.setAnnotation(new VID(getAddr(arc)));
                }
                else {
                    selection.setAnnotation(new VID(((ChainAdapter) adapter).addArc(jCas, originFs,
                            targetFs, null, null)));
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

            adapter.updateFeature(jCas, fm.feature, aBModel.getSelection().getAnnotation().getId(),
                    fm.value);
        }

        // Update progress information
        int sentenceNumber = getSentenceNumber(jCas, aBModel.getSelection().getBegin());
        aBModel.setSentenceNumber(sentenceNumber);
        aBModel.getDocument().setSentenceAccessed(sentenceNumber);

        // persist changes
        repository.writeCas(aBModel.getMode(), aBModel.getDocument(), aBModel.getUser(), jCas);

        if (aBModel.getPreferences().isScrollPage()) {
            autoScroll(jCas, aBModel);
        }

        if (isRelation()) {
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
                    selectByAddr(jCas, aBModel.getSelection().getAnnotation().getId()), features);
            info(generateMessage(aBModel.getSelectedAnnotationLayer(), bratLabelText, false));
        }

        onChange(aTarget, aBModel);
        if (aBModel.isForwardAnnotation()) {
            onAutoForward(aTarget, aBModel);
        }
        onAnnotate(aTarget, aBModel, selection.getBegin(), selection.getEnd());
    }

    private void actionDelete(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
        throws IOException, UIMAException, ClassNotFoundException, CASRuntimeException,
        BratAnnotationException
    {
        JCas jCas = getCas(aBModel);
        AnnotationFS fs = selectByAddr(jCas, aBModel.getSelection().getAnnotation().getId());

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
        aBModel.setSentenceNumber(sentenceNumber);
        aBModel.getDocument().setSentenceAccessed(sentenceNumber);

        // Auto-scroll
        if (aBModel.getPreferences().isScrollPage()) {
            autoScroll(jCas, aBModel);
        }

        aBModel.setRememberedSpanLayer(aBModel.getSelectedAnnotationLayer());
        aBModel.getSelection().setAnnotate(false);

        info(generateMessage(aBModel.getSelectedAnnotationLayer(), null, true));

        // A hack to remember the visual DropDown display value
        aBModel.setRememberedSpanLayer(aBModel.getSelectedAnnotationLayer());
        aBModel.setRememberedSpanFeatures(featureModels);

        aBModel.getSelection().clear();

        // after delete will follow annotation
        bModel.getSelection().setAnnotate(true);
        aTarget.add(annotationFeatureForm);
        // TODO aTarget.add(relationAnnotationFeatureForm.featureEditorsContainer);

        aTarget.add(deleteButton);
        aTarget.add(reverseButton);
        onChange(aTarget, aBModel);
        onDelete(aTarget, aBModel, fs);
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
        aBModel.setSentenceNumber(sentenceNumber);
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

        onChange(aTarget, aBModel);
    }

    public JCas getCas(BratAnnotatorModel aBModel)
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
                .getBegin(), aBModel.getProject(), aBModel.getDocument(), aBModel.getPreferences()
                .getWindowSize()));

        Sentence sentence = selectByAddr(jCas, Sentence.class, aBModel.getSentenceAddress());
        aBModel.setSentenceBeginOffset(sentence.getBegin());
        aBModel.setSentenceEndOffset(sentence.getEnd());

        Sentence firstSentence = selectSentenceAt(jCas, aBModel.getSentenceBeginOffset(),
                aBModel.getSentenceEndOffset());
        int lastAddressInPage = getLastSentenceAddressInDisplayWindow(jCas, getAddr(firstSentence),
                aBModel.getPreferences().getWindowSize());
        // the last sentence address in the display window
        Sentence lastSentenceInPage = (Sentence) selectByAddr(jCas, FeatureStructure.class,
                lastAddressInPage);
        aBModel.setFSN(BratAjaxCasUtil.getSentenceNumber(jCas, firstSentence.getBegin()));
        aBModel.setLSN(BratAjaxCasUtil.getSentenceNumber(jCas, lastSentenceInPage.getBegin()));
    }

    @SuppressWarnings("unchecked")
    public void setSlot(AjaxRequestTarget aTarget, JCas aJCas, final BratAnnotatorModel aBModel,
            int aAnnotationId)
    {
        // Set an armed slot
        if (!isRelation() && aBModel.isSlotArmed()) {
            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) getFeatureModel(aBModel
                    .getArmedFeature()).value;
            LinkWithRoleModel link = links.get(aBModel.getArmedSlot());
            link.targetAddr = aAnnotationId;
            link.label = selectByAddr(aJCas, aAnnotationId).getCoveredText();
            aBModel.clearArmedSlot();
        }

        // Auto-commit if working on existing annotation
        if (bModel.getSelection().getAnnotation().isSet()) {
            try {
                actionAnnotate(aTarget, bModel, aJCas);
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

    private void setLayerAndFeatureModels(AjaxRequestTarget aTarget, JCas aJCas,
            final BratAnnotatorModel aBModel)
    {
        if (aBModel.getSelection().isRelationAnno()) {
            long layerId = TypeUtil.getLayerId(aBModel.getSelection().getOriginType());
            AnnotationLayer spanLayer = annotationService.getLayer(layerId);

            // If we drag an arc between POS annotations, then the relation must be a dependency
            // relation.
            // FIXME - Actually this case should be covered by the last case - the database lookup!
            if (spanLayer.isBuiltIn() && spanLayer.getName().equals(POS.class.getName())) {
                AnnotationLayer depLayer = annotationService.getLayer(Dependency.class.getName(),
                        aBModel.getProject());
                if (aBModel.getAnnotationLayers().contains(depLayer)) {
                    aBModel.setSelectedAnnotationLayer(depLayer);
                }
                else {
                    aBModel.setSelectedAnnotationLayer(null);
                }
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
                        if (aBModel.getAnnotationLayers().contains(layer)) {
                            aBModel.setSelectedAnnotationLayer(layer);
                        }
                        else {
                            aBModel.setSelectedAnnotationLayer(null);
                        }
                        break;
                    }
                }
            }

            // populate feature value
            if (aBModel.getSelection().getAnnotation().isSet()) {
                AnnotationFS annoFs = selectByAddr(aJCas, aBModel.getSelection().getAnnotation()
                        .getId());

                populateFeatures(annoFs);
            }
            // Avoid creation of arcs on locked layers
            else if (aBModel.getSelectedAnnotationLayer() != null
                    && aBModel.getSelectedAnnotationLayer().isReadonly()) {
                aBModel.setSelectedAnnotationLayer(new AnnotationLayer());
            }
        }
    else if (aBModel.getSelection().getAnnotation().isSet()) {
            AnnotationFS annoFs = selectByAddr(aJCas, aBModel.getSelection().getAnnotation()
                    .getId());
            String type = annoFs.getType().getName();

            try {
                aBModel.setSelectedAnnotationLayer(annotationService.getLayer(type,
                        aBModel.getProject()));
            }
            catch (NoResultException e) {
                reset(aTarget);
                throw new IllegalStateException("Unknown layer [" + type + "]", e);
            }

            // Might have been reset if we didn't find the layer above. Btw. this can happen if
            // somebody imports a CAS that has subtypes of layers, e.g. DKPro Core pipelines
            // like to produce subtypes of POS for individual postags. We do not support such
            // "elevated types" in WebAnno at this time.
            if (aBModel.getSelection().getAnnotation().isSet()) {
                if (type.endsWith(ChainAdapter.CHAIN)) {
                    type = type.substring(0, type.length() - ChainAdapter.CHAIN.length());
                }
                else if (type.endsWith(ChainAdapter.LINK)) {
                    type = type.substring(0, type.length() - ChainAdapter.LINK.length());
                }

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
        }
    }

    private static boolean isSuppressedFeature(BratAnnotatorModel aBModel,
            AnnotationFeature aFeature)
    {
        String featName = aFeature.getName();

        if (WebAnnoConst.CHAIN_TYPE.equals(aFeature.getLayer().getType())) {
            return WebAnnoConst.COREFERENCE_TYPE_FEATURE.equals(featName)
                    || WebAnnoConst.COREFERENCE_RELATION_FEATURE.equals(featName);
        }

        return false;
    }

    protected void onChange(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
    {
        // Overriden in BratAnnotator
    }

    protected void onAutoForward(AjaxRequestTarget aTarget, BratAnnotatorModel aBModel)
    {
        // Overriden in BratAnnotator
    }

    public void onAnnotate(AjaxRequestTarget aTarget, BratAnnotatorModel aModel, int aStart,
            int aEnd)
    {
        // Overriden in AutomationPage
    }

    public void onDelete(AjaxRequestTarget aTarget, BratAnnotatorModel aModel, AnnotationFS aFs)
    {
        // Overriden in AutomationPage
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
        populateFeatures(null);
    }

    private void setInitSpanLayers(BratAnnotatorModel aBModel)
    {
        annotationLayers.clear();
        AnnotationLayer l = null;
        for (AnnotationLayer layer : aBModel.getAnnotationLayers()) {
            if (!layer.isEnabled() || layer.isReadonly()
                    || layer.getName().equals(Token.class.getName())) {
                continue;
            }
            if (brush.getModelObject().equals(layer.getType())) {
                annotationLayers.add(layer);
                l = layer;
            }
            // manage chain type
            else if (layer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                for (AnnotationFeature feature : annotationService.listAnnotationFeature(layer)) {
                    if (!feature.isEnabled()) {
                        continue;
                    }
                    // add it as relation
                    else if (isRelation()
                            && feature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE)) {
                        annotationLayers.add(layer);
                    }
                    // add it as span
                    else if (!isRelation()
                            && feature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
                        annotationLayers.add(layer);
                    }

                }
            }
            // chain
        }
        if (l != null) {
            bModel.setSelectedAnnotationLayer(l);
        }
    }

    public class FeatureEditorPanelContent
        extends RefreshingView<FeatureModel>
    {
        private static final long serialVersionUID = -8359786805333207043L;

        public FeatureEditorPanelContent(String aId)
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

            if (!fm.feature.getLayer().isReadonly()) {
                // whenever it is updating an annotation, it updates automatically when a component
                // for the feature lost focus - but updating is for every component edited
                // LinkFeatureEditors must be excluded because the auto-update will break the
                // ability to add slots. Adding a slot is NOT an annotation action.
              // TODO annotate every time except when position is at (0,0)
                if (!bModel.getSelection().isAnnotate()
                        && bModel.getSelection().getAnnotation().isSet()
                        && !(frag instanceof LinkFeatureEditor)) {
                    if (frag.isDropOrchoice()) {
                        addAnnotateActionBehavior(frag, "onchange");
                    }
                    else {
                        addAnnotateActionBehavior(frag, "onblur");
                    }
                }
                else if (!(frag instanceof LinkFeatureEditor)) {
                    if (frag.isDropOrchoice()) {
                        storeFeatureValue(frag, "onchange");
                    }
                    else {
                        storeFeatureValue(frag, "onblur");
                    }
                }

                /*
                 * if (item.getIndex() == 0) { // Put focus on first feature
                 * frag.getFocusComponent().add(new DefaultFocusBehavior()); }
                 */

                // Add tooltip on label
                Component labelComponent = frag.getLabelComponent();
                labelComponent.add(new AttributeAppender("style", "cursor: help", ";"));
                labelComponent.add(new DescriptionTooltipBehavior(fm.feature.getUiName(),
                        fm.feature.getDescription()));

            }
            else {
                frag.getFocusComponent().setEnabled(false);
            }
        }

        private void storeFeatureValue(final FeatureEditor aFrag, String aEvent)
        {
            aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior(aEvent)
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    aTarget.add(annotationFeatureForm);
                }
            });
        }

        private void addAnnotateActionBehavior(final FeatureEditor aFrag, String aEvent)
        {
            aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior(aEvent)
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    try {
                        if (bModel.getConstraints() != null) {
                            // Make sure we update the feature editor panel because due to
                            // constraints the contents may have to be re-rendered
                            aTarget.add(annotationFeatureForm);
                        }
                        actionAnnotate(aTarget, bModel);
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

        public Component getLabelComponent()
        {
            return get("feature");
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

            add(new Label("feature", aModel.feature.getUiName()));

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

            add(new Label("feature", aModel.feature.getUiName()));

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

            String featureLabelText = aModel.feature.getUiName();
            if (aModel.feature.getTagset() != null) {
                featureLabelText += " (" + aModel.feature.getTagset().getName() + ")";
            }
            add(new Label("feature", featureLabelText));

            if (aModel.feature.getTagset() != null) {

                List<Tag> tagset = null;
                BratAnnotatorModel model = bModel;
                // verification to check whether constraints exist for this project or NOT
                if (model.getConstraints() != null && model.getSelection().getAnnotation().isSet()) {
                    tagset = populateTagsBasedOnRules(model, aModel);
                }
                else {
                    // Earlier behavior,
                    tagset = annotationService.listTags(aModel.feature.getTagset());
                }
                field = new StyledComboBox<Tag>("value", tagset);
                
                field.setOutputMarkupId(true);

                Options options = new Options(DescriptionTooltipBehavior.makeTooltipOptions());
                options.set("content", functionForTooltip);
                field.add(new TooltipBehavior(options));
                isDrop = true;
            }
            else {
                field = new TextField<String>("value");
            }
            add(field);
        }

        /**
         * Adds and sorts tags based on Constraints rules
         */
        private List<Tag> populateTagsBasedOnRules(BratAnnotatorModel model, FeatureModel aModel)
        {
            // Add values from rules
            String restrictionFeaturePath;
            switch (aModel.feature.getLinkMode()) {
            case WITH_ROLE:
                restrictionFeaturePath = aModel.feature.getName() + "."
                        + aModel.feature.getLinkTypeRoleFeatureName();
                break;
            case NONE:
                restrictionFeaturePath = aModel.feature.getName();
                break;
            default:
                throw new IllegalArgumentException("Unsupported link mode ["
                        + aModel.feature.getLinkMode() + "] on feature ["
                        + aModel.feature.getName() + "]");
            }

            List<Tag> valuesFromTagset = annotationService.listTags(aModel.feature.getTagset());

            try {
                JCas jCas = getCas(model);

                FeatureStructure featureStructure = selectByAddr(jCas, model.getSelection()
                        .getAnnotation().getId());

                Evaluator evaluator = new ValuesGenerator();
                List<PossibleValue> possibleValues = evaluator.generatePossibleValues(
                        featureStructure, restrictionFeaturePath, model.getConstraints());

                LOG.debug("Possible values for [" + featureStructure.getType().getName() + "] ["
                        + restrictionFeaturePath + "]: " + possibleValues);

                // only adds tags which are suggested by rules and exist in tagset.
                List<Tag> tagset = compareSortAndAdd(possibleValues, valuesFromTagset);

                // add remaining tags
                addRemainingTags(tagset, valuesFromTagset);
                return tagset;
            }
            catch (IOException | ClassNotFoundException | UIMAException e) {
                error(ExceptionUtils.getRootCause(e));
                LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
            }
            return valuesFromTagset;
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

            String featureLabelText = aModel.feature.getUiName();
            if (aModel.feature.getTagset() != null) {
                featureLabelText += " (" + aModel.feature.getTagset().getName() + ")";
            }
            add(new Label("feature", featureLabelText));

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
                    final Label label;
                    if (aItem.getModelObject().targetAddr == -1
                            && bModel.isArmedSlot(aModel.feature, aItem.getIndex())) {
                        label = new Label("label", "<Select to fill>");
                    }
                    else {
                        label = new Label("label");
                    }
                    label.add(new AjaxEventBehavior("click")
                    {
                        private static final long serialVersionUID = 7633309278417475424L;

                        @Override
                        protected void onEvent(AjaxRequestTarget aTarget)
                        {
                            if (bModel.isArmedSlot(aModel.feature, aItem.getIndex())) {
                                bModel.clearArmedSlot();
                            }
                            else {
                                bModel.setArmedSlot(aModel.feature, aItem.getIndex());
                            }
                            aTarget.add(annotationFeatureForm.featureEditorsContainer);
                        }
                    });
                    label.add(new AttributeAppender("style", new Model<String>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public String getObject()
                        {
                            BratAnnotatorModel model = bModel;
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
                List<Tag> tagset = null;
                BratAnnotatorModel model = bModel;

                // verification to check whether constraints exist for this project or NOT
                if (model.getConstraints() != null && model.getSelection().getAnnotation().isSet()) {
                    tagset = addTagsBasedOnRules(model, aModel);
                }
                else {
                    // add tagsets only, earlier behavior
                    tagset = annotationService.listTags(aModel.feature.getTagset());
                }

                text = new StyledComboBox<Tag>("newRole", Model.of(""), tagset,
                        new com.googlecode.wicket.kendo.ui.renderer.ChoiceRenderer<Tag>("name"));
                add(text);
                
                Options options = new Options(DescriptionTooltipBehavior.makeTooltipOptions());
                options.set("content", functionForTooltip);
                text.add(new TooltipBehavior(options));
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
                        bModel.setArmedSlot(LinkFeatureEditor.this.getModelObject().feature,
                                links.size() - 1);

                        aTarget.add(annotationFeatureForm);
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
                    BratAnnotatorModel model = bModel;
                    setEnabled(model.isSlotArmed()
                            && aModel.feature.equals(model.getArmedFeature()));
                }

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                            .getModelObject().value;

                    BratAnnotatorModel model = bModel;
                    links.remove(model.getArmedSlot());
                    model.clearArmedSlot();

                    aTarget.add(annotationFeatureForm);

                    // Auto-commit if working on existing annotation
                    if (bModel.getSelection().getAnnotation().isSet()) {
                        try {
                            actionAnnotate(aTarget, bModel);
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

        /**
         * Adds tagset based on Constraints rules, auto-adds tags which are marked important.
         *
         * @return List containing tags which exist in tagset and also suggested by rules, followed
         *         by the remaining tags in tagset.
         */
        private List<Tag> addTagsBasedOnRules(BratAnnotatorModel model, final FeatureModel aModel)
        {
            String restrictionFeaturePath = aModel.feature.getName() + "."
                    + aModel.feature.getLinkTypeRoleFeatureName();

            List<Tag> valuesFromTagset = annotationService.listTags(aModel.feature.getTagset());

            try {
                JCas jCas = getCas(model);

                FeatureStructure featureStructure = selectByAddr(jCas, model.getSelection()
                        .getAnnotation().getId());

                Evaluator evaluator = new ValuesGenerator();

                List<PossibleValue> possibleValues = evaluator.generatePossibleValues(
                        featureStructure, restrictionFeaturePath, model.getConstraints());

                LOG.debug("Possible values for [" + featureStructure.getType().getName() + "] ["
                        + restrictionFeaturePath + "]: " + possibleValues);

                // Only adds tags which are suggested by rules and exist in tagset.
                List<Tag> tagset = compareSortAndAdd(possibleValues, valuesFromTagset);
                removeAutomaticallyAddedUnusedEntries();

                // Create entries for important tags.
                autoAddImportantTags(tagset, possibleValues);

                // Add remaining tags.
                addRemainingTags(tagset, valuesFromTagset);
                return tagset;
            }
            catch (ClassNotFoundException | UIMAException | IOException e) {
                error(ExceptionUtils.getRootCause(e));
                LOG.error(ExceptionUtils.getRootCauseMessage(e), e);
            }

            return valuesFromTagset;
        }

        private void removeAutomaticallyAddedUnusedEntries()
        {
            // Remove unused (but auto-added) tags.
            @SuppressWarnings("unchecked")
            List<LinkWithRoleModel> list = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                    .getModelObject().value;

            Iterator<LinkWithRoleModel> existingLinks = list.iterator();
            while (existingLinks.hasNext()) {
                LinkWithRoleModel link = existingLinks.next();
                if (link.autoCreated && link.targetAddr == -1) {
                    // remove it
                    existingLinks.remove();
                }
            }
        }

        private void autoAddImportantTags(List<Tag> aTagset, List<PossibleValue> possibleValues)
        {
            // Construct a quick index for tags
            Set<String> tagset = new HashSet<String>();
            for (Tag t : aTagset) {
                tagset.add(t.getName());
            }

            // Get links list and build role index
            @SuppressWarnings("unchecked")
            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) LinkFeatureEditor.this
                    .getModelObject().value;
            Set<String> roles = new HashSet<String>();
            for (LinkWithRoleModel l : links) {
                roles.add(l.role);
            }

            // Loop over values to see which of the tags are important and add them.
            for (PossibleValue value : possibleValues) {
                if (!value.isImportant() || !tagset.contains(value.getValue())) {
                    continue;
                }

                // Check if there is already a slot with the given name
                if (roles.contains(value.getValue())) {
                    continue;
                }

                // Add empty slot in UI with that name.
                LinkWithRoleModel m = new LinkWithRoleModel();
                m.role = value.getValue();
                // Marking so that can be ignored later.
                m.autoCreated = true;
                links.add(m);
                // NOT arming the slot here!
            }
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

    private void populateFeatures(FeatureStructure aFS)
    {
        featureModels = new ArrayList<>();

        if (aFS != null) {
            // Populate from feature structure
            for (AnnotationFeature feature : annotationService.listAnnotationFeature(bModel
                    .getSelectedAnnotationLayer())) {
                if (!feature.isEnabled() || isSuppressedFeature(bModel, feature)) {
                    continue;
                }

                featureModels.add(new FeatureModel(feature, (Serializable) BratAjaxCasUtil
                        .getFeature(aFS, feature)));
            }
        }
        else if (!isRelation() && bModel.getRememberedSpanFeatures() != null) {
            // Populate from remembered values
            for (AnnotationFeature feature : annotationService.listAnnotationFeature(bModel
                    .getSelectedAnnotationLayer())) {
                if (!feature.isEnabled() || isSuppressedFeature(bModel, feature)) {
                    continue;
                }

                featureModels.add(new FeatureModel(feature, bModel.getRememberedSpanFeatures()
                        .get(feature)));
            }
        }
        else if (isRelation() && bModel.getRememberedArcFeatures() != null) {
            // Populate from remembered values
            for (AnnotationFeature feature : annotationService.listAnnotationFeature(bModel
                    .getSelectedAnnotationLayer())) {
                if (!feature.isEnabled() || isSuppressedFeature(bModel, feature)) {
                    continue;
                }

                featureModels.add(new FeatureModel(feature, bModel.getRememberedArcFeatures()
                        .get(feature)));
            }
        }
    }

    public boolean isRelation(){
        return brush.getModelObject().equals(WebAnnoConst.RELATION_TYPE);
    }
    public void addRemainingTags(List<Tag> tagset, List<Tag> valuesFromTagset)
    {
        // adding the remaining part of tagset.
        for (Tag remainingTag : valuesFromTagset) {
            if (!tagset.contains(remainingTag)) {
                tagset.add(remainingTag);
            }
        }

    }

    /*
     * Compares existing tagset with possible values resulted from rule evaluation Adds only which
     * exist in tagset and is suggested by rules. The remaining values from tagset are added
     * afterwards.
     */
    private static List<Tag> compareSortAndAdd(List<PossibleValue> possibleValues,
            List<Tag> valuesFromTagset)
    {
        List<Tag> returnList = new ArrayList<Tag>();
        // // Sorting based on important flag
        // possibleValues.sort(null);
        // Comparing to check which values suggested by rules exists in existing
        // tagset and adding them first in list.
        for (PossibleValue value : possibleValues) {
            for (Tag tag : valuesFromTagset) {
                if (value.getValue().equalsIgnoreCase(tag.getName())) {
                    // HACK BEGIN
                    tag.setReordered(true);
                    // HACK END
                    returnList.add(tag);
                }
            }
        }

        return returnList;
    }

    public class LayerSelector
        extends DropDownChoice<AnnotationLayer>
    {
        private static final long serialVersionUID = 2233133653137312264L;

        public LayerSelector(String aId, List<? extends AnnotationLayer> aChoices,
                final boolean aIsRelation)
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
                    // user like to annotate with different layer - //TODO - remove the 
                    // annotation selection highlight color
                    bModel.getSelection().setAnnotate(true); 
                    bModel.getSelection().setAnnotation(VID.NONE_ID);
                    populateFeatures(null);
                    aTarget.add(annotationFeatureForm);
                    aTarget.add(forwardAnnotation);
                }
            });
        }
    }

    /**
     * A selector to enable either span or relation annotations. Based on this brush selection, the
     * user can pre-fill the annotation details before selecting a span annotation or relation arc
     * drawing
     * 
     * @author seid
     *
     */
    public class BrushSelector
        extends DropDownChoice<String>
    {
        private static final long serialVersionUID = 2233133653137312264L;

        public BrushSelector(String aId, Model<String> aModel, List<? extends String> aChoices)
        {
            super(aId, aModel, aChoices);
            setOutputMarkupId(true);

            add(new AjaxFormComponentUpdatingBehavior("onchange")
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    setInitSpanLayers(bModel);
                    populateFeatures(null);
                    aTarget.add(annotationFeatureForm);
                }
            });
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

        public static final String CLICK_HINT = "<Click to activate>";

        public String role;
        public String label = CLICK_HINT;
        public int targetAddr = -1;
        public boolean autoCreated;

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((label == null) ? 0 : label.hashCode());
            result = prime * result + ((role == null) ? 0 : role.hashCode());
            result = prime * result + targetAddr;
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            LinkWithRoleModel other = (LinkWithRoleModel) obj;
            if (label == null) {
                if (other.label != null) {
                    return false;
                }
            }
            else if (!label.equals(other.label)) {
                return false;
            }
            if (role == null) {
                if (other.role != null) {
                    return false;
                }
            }
            else if (!role.equals(other.role)) {
                return false;
            }
            if (targetAddr != other.targetAddr) {
                return false;
            }
            return true;
        }

    }

    private void updateForwardAnnotation(BratAnnotatorModel aBModel)
    {
        if (aBModel.getSelection().getAnnotation().isSet()) {
            aBModel.setForwardAnnotation(false);// this is editing

        }
        else if (aBModel.getSelectedAnnotationLayer() != null
                && !aBModel.getSelectedAnnotationLayer().isLockToTokenOffset()) {
            aBModel.setForwardAnnotation(false);// no forwarding for sub-/multitoken annotation
        }
        else {
            aBModel.setForwardAnnotation(aBModel.isForwardAnnotation());
        }
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

    public void reload(AjaxRequestTarget aTarget)
    {
        aTarget.add(annotationFeatureForm);
    }

    public void reset(AjaxRequestTarget aTarget)
    {
        bModel.getSelection().clear();
        bModel.getSelection().setBegin(0);
        bModel.getSelection().setEnd(0);
        featureModels = new ArrayList<>();
        aTarget.add(annotationFeatureForm);
    }

    public void reloadBrush(AjaxRequestTarget aTarget, String aModel)
    {
        try {
            brush.setModelObject(aModel);  
            if(!isRelation()){
                featureModels = new ArrayList<>(); 
            }
            setInitSpanLayers(bModel);
            setLayerAndFeatureModels(aTarget, getCas(bModel), bModel); 
            if(featureModels.size() ==0 ) {
                populateFeatures(null);
            }
            else if(isFeatureModelChanged(bModel.getSelectedAnnotationLayer()) ) {
                populateFeatures(null); 
            }
            aTarget.add(annotationFeatureForm);
        }
        catch (UIMAException | ClassNotFoundException | IOException e) {
          error(e.getMessage());
        }
    }
    
    /**
     * remove this model, if new annotation is to be created
     */
    public void clearArmedSlotModel()
    {
        for (FeatureModel fm : featureModels) {
            if (StringUtils.isNotBlank(fm.feature.getLinkTypeName())) {
                fm.value = new ArrayList<>();
            }
        }
    }

    private boolean isFeatureModelChanged(AnnotationLayer aLayer){

            for(FeatureModel fM: featureModels){
                if(!annotationService.listAnnotationFeature(aLayer).contains(fM.feature)){
                    return true;
            }
        }
            return false;
        
    }
    private static String generateMessage(AnnotationLayer aLayer, String aLabel, boolean aDeleted)
    {
        String action = aDeleted ? "deleted" : "created/updated";

        String msg = "The [" + aLayer.getUiName() + "] annotation has been " + action + ".";
        if (StringUtils.isNotBlank(aLabel)) {
            msg += " Label: [" + aLabel + "]";
        }
        return msg;
    }
    
    class StyledComboBox<T> extends ComboBox<T>{
        public StyledComboBox(String id, IModel<String> model, List<T> choices,
                com.googlecode.wicket.kendo.ui.renderer.ChoiceRenderer<? super T> renderer)
        {
            super(id, model, choices, renderer);            
        }

        public StyledComboBox(String string, List<T> tagset)
        {
            super(string,tagset);
        }

        private static final long serialVersionUID = 1L;

        @Override
        protected IJQueryTemplate newTemplate()
        {
            return new IJQueryTemplate()
            {
                private static final long serialVersionUID = 1L;
                /**
                 * Marks the reordered entries in bold.
                 * Same as text feature editor.
                 */
                @Override
                public String getText()
                {
                    // Some docs on how the templates work in Kendo, in case we need
                    // more fancy dropdowns
                    // http://docs.telerik.com/kendo-ui/framework/templates/overview
                    StringBuilder sb = new StringBuilder();
                    sb.append("# if (data.reordered == 'true') { #");
                    sb.append("<div title=\"#: data.description #\"><b>#: data.name #</b></div>\n");
                    sb.append("# } else { #");
                    sb.append("<div title=\"#: data.description #\">#: data.name #</div>\n");
                    sb.append("# } #");
                    return sb.toString();
                }

                @Override
                public List<String> getTextProperties()
                {
                    return Arrays.asList("name", "description", "reordered");
                }
            };
        }
    }
}