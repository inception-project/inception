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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
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
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.OffsetsList;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
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
    boolean isModify = false;
    TagSet selectedtTagSet;

    Model<TagSet> tagSetsModel;
    Model<Tag> tagsModel;
    private AnnotationDialogForm annotationDialogForm;
    private BratAnnotatorModel bratAnnotatorModel;
    String offsets;
    private String selectedText = null;
    int selectedSpanId = -1;
    String selectedSpanType;

    private class AnnotationDialogForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -4104665452144589457L;

        public AnnotationDialogForm(String id, final ModalWindow aModalWindow)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            List<TagSet> spanLayers = new ArrayList<TagSet>();

            for (TagSet tagset : bratAnnotatorModel.getAnnotationLayers()) {
                if (tagset.getType().getType().equals("span")) {
                    spanLayers.add(tagset);
                }

            }

            if (selectedSpanId != -1) {
                tagSetsModel = new Model<TagSet>(selectedtTagSet);
                Tag tag = annotationService.getTag(BratAjaxCasUtil.getLabel(selectedSpanType),
                        selectedtTagSet);

                tagsModel = new Model<Tag>(tag);
            }
            else if (bratAnnotatorModel.getRememberedSpanTagSet() != null) {
                selectedtTagSet = bratAnnotatorModel.getRememberedSpanTagSet();
                tagSetsModel = new Model<TagSet>(selectedtTagSet);
                tagsModel = new Model<Tag>(bratAnnotatorModel.getRememberedSpanTag());
            }
            else {
                selectedtTagSet = ((TagSet) spanLayers.get(0));
                tagSetsModel = new Model<TagSet>(selectedtTagSet);
                tagsModel = new Model<Tag>(null);
            }

            add(new Label("selectedText", selectedText));

            tags= new ComboBox<Tag>("tags", new Model<String>(
                    tagsModel.getObject() == null ? "" : tagsModel.getObject().getName()),
                    annotationService.listTags(selectedtTagSet), new ComboBoxRenderer<Tag>("name",
                            "name"));
            add(tags);

            add(new DropDownChoice<TagSet>("tagSets", tagSetsModel, spanLayers)
            {
                private static final long serialVersionUID = -508831184292402704L;

                @Override
                protected void onSelectionChanged(TagSet aNewSelection)
                {
                    selectedtTagSet = aNewSelection;
                    tagsModel.setObject(null);

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

            }.setChoiceRenderer(new ChoiceRenderer<TagSet>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getDisplayValue(TagSet aObject)
                {
                    return aObject.getName();
                }
            }).setOutputMarkupId(true).add(new Behavior()
            {
                private static final long serialVersionUID = -3612493911620740735L;

                @Override
                public void renderHead(Component component, IHeaderResponse response)
                {
                    super.renderHead(component, response);
                    response.renderOnLoadJavaScript("$('#" + component.getMarkupId()
                            + "').focus();Wicket.Window.unloadConfirmation = false;");
                }
            }));

            add(new AjaxButton("annotate")
            {
                private static final long serialVersionUID = 980971048279862290L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> form)
                {
                    BratAjaxCasController controller = new BratAjaxCasController(repository,
                            annotationService);
                    try {
                        JCas jCas = getCas(bratAnnotatorModel);

                        OffsetsList offsetLists = (OffsetsList) jsonConverter.getObjectMapper()
                                .readValue(offsets, OffsetsList.class);

                        Sentence sentence = BratAjaxCasUtil.getSentenceofCAS(jCas,
                                bratAnnotatorModel.getSentenceBeginOffset(),
                                bratAnnotatorModel.getSentenceEndOffset());
                        int start = sentence.getBegin() + ((Offsets) offsetLists.get(0)).getBegin();
                        int end = sentence.getBegin() + ((Offsets) offsetLists.get(0)).getEnd();

                        String annotationType = "";

                        if (tags.getModelObject() == null) {
                            aTarget.appendJavaScript("alert('No Tag is selected!')");
                        }
                        else {
                            Tag selectedTag = (Tag) annotationService.getTag(tags.getModelObject(),
                                    selectedtTagSet);
                            annotationType = BratAjaxCasUtil.getQualifiedLabel(selectedTag);

                            controller.addSpanToCas(jCas, start, end, annotationType, null, null);
                           // controller.addSpanTagSetToCas(jCas, bratAnnotatorModel.getProject(),
                             //       annotationType);
                            controller.createAnnotationDocumentContent(
                                    bratAnnotatorModel.getMode(), bratAnnotatorModel.getDocument(),
                                    bratAnnotatorModel.getUser(), jCas);

                            if (bratAnnotatorModel.isScrollPage()) {
                                updateSentenceAddressAndOffsets(jCas, start);
                            }

                            bratAnnotatorModel.setRememberedSpanTagSet(selectedtTagSet);
                            bratAnnotatorModel.setRememberedSpanTag(selectedTag);

                            // A hack to rememeber the Visural DropDown display value
                            HttpSession session = ((ServletWebRequest) RequestCycle.get()
                                    .getRequest()).getContainerRequest().getSession();
                            session.setAttribute("model", bratAnnotatorModel);
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
                    try {
                        JCas jCas = getCas(bratAnnotatorModel);

                        OffsetsList offsetLists = (OffsetsList) jsonConverter.getObjectMapper()
                                .readValue(offsets, OffsetsList.class);

                        int start = selectByAddr(jCas, Sentence.class,
                                bratAnnotatorModel.getSentenceAddress()).getBegin()
                                + ((Offsets) offsetLists.get(0)).getBegin();

                        AnnotationFS idFs = selectByAddr(jCas, selectedSpanId);
                        Tag selectedTag = (Tag) annotationService.getTag(tags.getModelObject(),
                                selectedtTagSet);
                        String annotationType = BratAjaxCasUtil.getQualifiedLabel(selectedTag);
                        if (annotationType.startsWith(AnnotationTypeConstant.POS_PREFIX)) {
                            aTarget.appendJavaScript("alert('POS annotations can\\'t be deleted!')");
                        }
                        else {
                            controller.deleteSpanFromCas(selectedSpanType, jCas, idFs);
                            controller.createAnnotationDocumentContent(
                                    bratAnnotatorModel.getMode(), bratAnnotatorModel.getDocument(),
                                    bratAnnotatorModel.getUser(), jCas);

                            if (bratAnnotatorModel.isScrollPage()) {
                                updateSentenceAddressAndOffsets(jCas, start);
                            }
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
                    // A hack to rememeber the Visural DropDown display value
                    HttpSession session = ((ServletWebRequest) RequestCycle.get().getRequest())
                            .getContainerRequest().getSession();
                    session.setAttribute("model", bratAnnotatorModel);
                    aModalWindow.close(aTarget);
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
            tags= new ComboBox<Tag>("tags", new Model<String>(
                    tagsModel.getObject() == null ? "" : tagsModel.getObject().getName()),
                    annotationService.listTags(selectedtTagSet), new ComboBoxRenderer<Tag>("name",
                            "name"));
            add(tags);
        }


    }

    private void updateSentenceAddressAndOffsets(JCas jCas, int start)
    {
        int address = BratAjaxCasUtil.getSentenceofCAS(jCas, bratAnnotatorModel.getSentenceBeginOffset(), bratAnnotatorModel.getSentenceEndOffset()).getAddress();
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

        private TagSet tagSets;
        private Tag tags;
        private String selectedText;
    }

    public SpanAnnotationModalWindowPage(ModalWindow modalWindow,
            BratAnnotatorModel aBratAnnotatorModel, String aSelectedText, String aOffsets)
    {
        this.offsets = aOffsets;

        this.selectedText = aSelectedText;

        this.bratAnnotatorModel = aBratAnnotatorModel;
        this.annotationDialogForm = new AnnotationDialogForm("annotationDialogForm", modalWindow);
        add(annotationDialogForm);
    }

    public SpanAnnotationModalWindowPage(ModalWindow modalWindow,
            BratAnnotatorModel aBratAnnotatorModel, String aSelectedText, String aOffsets,
            String aType, int aRef)
    {
        this.selectedSpanId = aRef;
        this.selectedSpanType = aType;

        String getLabelPrefix = BratAjaxCasUtil.getLabelPrefix(aType);
        String layerName = BratAjaxCasUtil.getSpanAnnotationTypeName(getLabelPrefix);

        AnnotationType layer = this.annotationService.getType(layerName, "span");

        this.selectedtTagSet = this.annotationService.getTagSet(layer,
                aBratAnnotatorModel.getProject());

        this.offsets = aOffsets;

        this.selectedText = aSelectedText;

        this.bratAnnotatorModel = aBratAnnotatorModel;
        this.annotationDialogForm = new AnnotationDialogForm("annotationDialogForm", modalWindow);
        add(annotationDialogForm);
        this.isModify = true;
    }
}
