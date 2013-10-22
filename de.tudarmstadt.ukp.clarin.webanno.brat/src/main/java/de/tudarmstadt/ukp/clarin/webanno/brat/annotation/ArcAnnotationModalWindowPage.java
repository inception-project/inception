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

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import com.googlecode.wicket.jquery.ui.kendo.combobox.ComboBox;
import com.googlecode.wicket.jquery.ui.kendo.combobox.ComboBoxRenderer;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * A panel that is used to display an annotation modal dialog for arc annotation.
 * @author Seid Muhie Yimam
 *
 */
public class ArcAnnotationModalWindowPage
    extends Panel
{
    private static final long serialVersionUID = -2102136855109258306L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    // A flag to keep checking if new annotation is to be made or an existing annotation is double
    // clciked.
    boolean isModify = false;

    // currently, we have one directional chain annotation and the "reveres" button not needed
    boolean ischain = false;
    // The selected TagSet
    TagSet selectedtTagSet;

    // The selected Tag for the arc annotation
    TagSet selectedtTag;

    Model<Tag> tagsModel;
    Model<TagSet> tagSetsModel;

    ComboBox<Tag> tags;

    private AnnotationDialogForm annotationDialogForm;
    private BratAnnotatorModel bratAnnotatorModel;

    private String originSpanType = null;
    int selectedArcId = -1;
    int originSpanId, targetSpanId;
    String selectedArcType;

    private class AnnotationDialogForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -4104665452144589457L;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public AnnotationDialogForm(String id, final ModalWindow aModalWindow)
        {
            super(id);

            // if it is new arc annotation
            if (selectedArcId == -1) {
                // for rapid annotation, pre-fill previous annotation type again
                if (bratAnnotatorModel.getRememberedArcTagSet() != null
                        && selectedtTagSet.getName().equals(
                                bratAnnotatorModel.getRememberedArcTagSet().getName())) {
                    tagSetsModel = new Model<TagSet>(selectedtTagSet);
                    tagsModel = new Model<Tag>(bratAnnotatorModel.getRememberedArcTag());
                }
                else {
                    tagSetsModel = new Model<TagSet>(selectedtTagSet);
                    tagsModel = new Model<Tag>(null);
                }

            }
            else {
                tagSetsModel = new Model<TagSet>(selectedtTagSet);
                Tag tag = annotationService.getTag(BratAjaxCasUtil.getLabel(selectedArcType),
                        selectedtTagSet);
                tagsModel = new Model<Tag>(tag);
            }

            tags= new ComboBox<Tag>("tags", new Model<String>(
                    tagsModel.getObject() == null ? "" : tagsModel.getObject().getName()),
                    annotationService.listTags(selectedtTagSet), new ComboBoxRenderer<Tag>("name",
                            "name"));
            add(tags);


            add(new DropDownChoice<TagSet>("tagSets", tagSetsModel,
                    Arrays.asList(new TagSet[] { selectedtTagSet })).setNullValid(false)
                    .setChoiceRenderer(new ChoiceRenderer<TagSet>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public Object getDisplayValue(
                                de.tudarmstadt.ukp.clarin.webanno.model.TagSet aObject)
                        {
                            return aObject.getName();
                        }
                    }).setOutputMarkupId(true).add(new Behavior()
                    {
                        private static final long serialVersionUID = -4468214709292758331L;

                        @Override
                        public void renderHead(Component component, IHeaderResponse response)
                        {
                            super.renderHead(component, response);
                            response.renderOnLoadJavaScript("$('#" + component.getMarkupId()
                                    + "').focus();");
                        }
                    }));

            add(new AjaxSubmitLink("annotate")
            {
                private static final long serialVersionUID = 8922161039500097566L;

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    BratAjaxCasController controller = new BratAjaxCasController(repository,
                            annotationService);
                    JCas jCas;
                    try {
                        jCas = getCas(bratAnnotatorModel);

                        String annotationType = "";

                        if (tags.getModelObject() == null) {
                            aTarget.appendJavaScript("alert('No Tag is selected!')");
                        }
                        else {
                            Tag selectedTag = (Tag) annotationService.getTag(tags.getModelObject(),
                                    selectedtTagSet);
                            annotationType = BratAjaxCasUtil.getQualifiedLabel(selectedTag);

                            AnnotationFS originFs = selectByAddr(jCas, originSpanId);
                            AnnotationFS targetFs = selectByAddr(jCas, targetSpanId);

                            controller.addArcToCas(bratAnnotatorModel, annotationType, -1, -1,
                                    originFs, targetFs, jCas);
                      //      controller.addArcTagSetToCas(jCas, bratAnnotatorModel.getProject(), annotationType);

                            controller.createAnnotationDocumentContent(
                                    bratAnnotatorModel.getMode(), bratAnnotatorModel.getDocument(),
                                    bratAnnotatorModel.getUser(), jCas);

                            if (bratAnnotatorModel.isScrollPage()) {
                                int start = originFs.getBegin();
                                updateSentenceAddressAndOffsets(jCas, start);
                            }

                            // save this annotation detail for next time annotation
                            bratAnnotatorModel.setRememberedArcTagSet(selectedtTagSet);
                            bratAnnotatorModel.setRememberedArcTag(selectedTag);
                            aModalWindow.close(aTarget);
                        }
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

                }
            });

            add(new AjaxSubmitLink("delete")
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    BratAjaxCasController controller = new BratAjaxCasController(repository,
                            annotationService);
                    JCas jCas;
                    try {
                        jCas = getCas(bratAnnotatorModel);


                        controller.delteArcFromCas(selectedArcType, jCas,selectedArcId);
                        controller.createAnnotationDocumentContent(bratAnnotatorModel.getMode(),
                                bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                                jCas);

                        if (bratAnnotatorModel.isScrollPage()) {
                            AnnotationFS originFs = selectByAddr(jCas, originSpanId);
                            int start = originFs.getBegin();
                            updateSentenceAddressAndOffsets(jCas, start);
                        }

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
                    BratAjaxCasController controller = new BratAjaxCasController(repository,
                            annotationService);
                    JCas jCas;
                    try {
                        jCas = getCas(bratAnnotatorModel);

                        AnnotationFS idFs = selectByAddr(jCas, selectedArcId);

                        jCas.removeFsFromIndexes(idFs);

                        String annotationType = "";
                        Tag selectedTag = (Tag) tagsModel.getObject();

                        annotationType = BratAjaxCasUtil.getQualifiedLabel(selectedTag);

                        AnnotationFS originFs = selectByAddr(jCas, originSpanId);
                        AnnotationFS targetFs = selectByAddr(jCas, targetSpanId);

                        controller.addArcToCas(bratAnnotatorModel, annotationType, -1, -1,
                               targetFs,originFs, jCas);

                        controller.createAnnotationDocumentContent(bratAnnotatorModel.getMode(),
                                bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                                jCas);

                        if (bratAnnotatorModel.isScrollPage()) {
                            int start = originFs.getBegin();
                            updateSentenceAddressAndOffsets(jCas, start);
                        }

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
        int address = BratAjaxCasUtil.selectSentenceAt(jCas, bratAnnotatorModel.getSentenceBeginOffset(), bratAnnotatorModel.getSentenceEndOffset()).getAddress();
        bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil
                .getSentenceBeginAddress(jCas,
                        address, start,
                        bratAnnotatorModel.getProject(),
                        bratAnnotatorModel.getDocument(),
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
                || aBratAnnotatorModel.getMode().equals(Mode.CORRECTION)
                || aBratAnnotatorModel.getMode().equals(Mode.CORRECTION_MERGE)) {
            BratAjaxCasController controller = new BratAjaxCasController(repository,
                    annotationService);

            return controller.getJCas(aBratAnnotatorModel.getDocument(),
                    aBratAnnotatorModel.getProject(), aBratAnnotatorModel.getUser());
        }
        else {
            return repository.getCurationDocumentContent(bratAnnotatorModel.getDocument());
        }
    }

    static private class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private List<TagSet> tagSets;
        private List<Tag> tags;
        private String selectedText;
    }

    public ArcAnnotationModalWindowPage(String aId, final ModalWindow modalWindow,
            BratAnnotatorModel aBratAnnotatorModel, int aOriginSpanId, String aOriginSpanType,
            int aTargetSpanId, String aTargetSpanType)
    {
        super(aId);
        this.originSpanType = aOriginSpanType;

        String layerName = BratAjaxCasUtil.getArcLayerName(BratAjaxCasUtil.getLabelPrefix(originSpanType));
        AnnotationType layer = annotationService.getType(layerName, AnnotationTypeConstant.RELATION_TYPE);
        this.selectedtTagSet = annotationService.getTagSet(layer, aBratAnnotatorModel.getProject());

        this.originSpanId = aOriginSpanId;
        this.targetSpanId = aTargetSpanId;

        this.bratAnnotatorModel = aBratAnnotatorModel;
        annotationDialogForm = new AnnotationDialogForm("annotationDialogForm", modalWindow);
        add(annotationDialogForm);
    }

    public ArcAnnotationModalWindowPage(String aId, final ModalWindow modalWindow,
            BratAnnotatorModel aBratAnnotatorModel, int aOriginSpanId, String aOriginSpanType,
            int aTargetSpanId, String aTargetSpanType, int selectedArcId, String aType)
    {
        super(aId);
        this.selectedArcId = selectedArcId;
        selectedArcType = aType;

        this.originSpanType = aOriginSpanType;

        String layerName = BratAjaxCasUtil.getArcLayerName(BratAjaxCasUtil.getLabelPrefix(originSpanType));
        AnnotationType layer = annotationService.getType(layerName, AnnotationTypeConstant.RELATION_TYPE);
        this.selectedtTagSet = annotationService.getTagSet(layer, aBratAnnotatorModel.getProject());

        this.originSpanId = aOriginSpanId;
        this.targetSpanId = aTargetSpanId;

        this.bratAnnotatorModel = aBratAnnotatorModel;
        annotationDialogForm = new AnnotationDialogForm("annotationDialogForm", modalWindow);
        add(annotationDialogForm);
        this.isModify = true;
        if (layerName.equals(AnnotationTypeConstant.COREFERENCE)) {
            ischain = true;
        }
    }
}
