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

import com.visural.wicket.component.dropdown.DropDown;
import com.visural.wicket.component.dropdown.DropDownDataSource;

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
    private DropDownDataSource annotationTypesDataSource;
    private DropDownDataSource tagsDataSource;
    DropDown<Tag> tagsDropDown;
    boolean isModify = false;
    TagSet selectedtTagSet;
    Model<Tag> tagsModel;
    Model<TagSet> tagSetsModel;
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
                Tag tag = annotationService.getTag(BratAjaxCasUtil.getType(selectedSpanType),
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

            tagsDataSource = new DropDownDataSource<Tag>()
            {
                private static final long serialVersionUID = 2234038471648260812L;

                @Override
                public String getName()
                {
                    return "tags";
                }

                @Override
                public List<Tag> getValues()
                {
                    return annotationService.listTags(selectedtTagSet);
                }

                @Override
                public String getDescriptionForValue(Tag t)
                {
                    return t.getName();
                }
            };

            add(new Label("selectedText", selectedText));

            tagsDropDown = new DropDown<Tag>("tags", tagsModel, tagsDataSource, true);
            add(tagsDropDown.setEnableFilterToggle(false).setOutputMarkupId(true));

            add(new DropDownChoice<TagSet>("tagSets", tagSetsModel, spanLayers)
            {
                private static final long serialVersionUID = -508831184292402704L;

                @Override
                protected void onSelectionChanged(TagSet aNewSelection)
                {
                    selectedtTagSet = aNewSelection;
                    tagsModel = new Model<Tag>(annotationService.listTags(selectedtTagSet).get(0));
                    updateTags(tagsModel, selectedtTagSet);
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
            })
            /*
             * .add(new AjaxFormComponentUpdatingBehavior("onchange") { private static final long
             * serialVersionUID = 1381680080441080656L;
             *
             * @Override protected void onUpdate(AjaxRequestTarget aTarget) { selectedtTagSet =
             * ((TagSet) tagSetsModel.getObject()); tagsModel = new
             * Model<Tag>(annotationService.listTags(selectedtTagSet).get(0)); updateTags(tagsModel,
             * selectedtTagSet, aTarget); } })
             */);

            add(new AjaxButton("annotate")
            {
                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> form)
                {
                    BratAjaxCasController controller = new BratAjaxCasController(repository,
                            annotationService);
                    try {

                        JCas jCas = getCas(bratAnnotatorModel);

                        OffsetsList offsetLists = (OffsetsList) jsonConverter.getObjectMapper()
                                .readValue(offsets, OffsetsList.class);

                        int start = BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                                bratAnnotatorModel.getSentenceAddress())
                                + ((Offsets) offsetLists.get(0)).getBegin();

                        int end = BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                                bratAnnotatorModel.getSentenceAddress())
                                + ((Offsets) offsetLists.get(0)).getEnd();

                        String annotationType = "";
                        Tag selectedTag = (Tag) tagsModel.getObject();
                        if (selectedTag == null) {
                            aTarget.appendJavaScript("alert('No Tag is selected!')");
                        }
                        else {
                            annotationType = BratAjaxCasUtil.getType(selectedTag);

                            controller.addSpanToCas(jCas, start, end, annotationType, null, null);
                            controller.addSpanTagSetToCas(jCas, bratAnnotatorModel.getProject(), annotationType);
                            controller.createAnnotationDocumentContent(
                                    bratAnnotatorModel.getMode(), bratAnnotatorModel.getDocument(),
                                    bratAnnotatorModel.getUser(), jCas);

                            if (bratAnnotatorModel.isScrollPage()) {
                                bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil
                                        .getSentenceBeginAddress(jCas,
                                                bratAnnotatorModel.getSentenceAddress(), start,
                                                bratAnnotatorModel.getProject(),
                                                bratAnnotatorModel.getDocument(),
                                                bratAnnotatorModel.getWindowSize()));
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

                        int start = BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                                bratAnnotatorModel.getSentenceAddress())
                                + ((Offsets) offsetLists.get(0)).getBegin();

                        AnnotationFS idFs = (AnnotationFS) jCas.getLowLevelCas().ll_getFSForRef(
                                selectedSpanId);
                        Tag selectedTag = (Tag) tagsModel.getObject();
                        String annotationType = BratAjaxCasUtil.getType(selectedTag);
                        if (annotationType.startsWith(AnnotationTypeConstant.POS_PREFIX)) {
                            aTarget.appendJavaScript("alert('POS annotations can\\'t be deleted!')");
                        }
                        else {
                            controller.deleteSpanFromCas(selectedSpanType, jCas, idFs);
                            controller.createAnnotationDocumentContent(
                                    bratAnnotatorModel.getMode(), bratAnnotatorModel.getDocument(),
                                    bratAnnotatorModel.getUser(), jCas);

                            if (bratAnnotatorModel.isScrollPage()) {
                                bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil
                                        .getSentenceBeginAddress(jCas,
                                                bratAnnotatorModel.getSentenceAddress(), start,
                                                bratAnnotatorModel.getProject(),
                                                bratAnnotatorModel.getDocument(),
                                                bratAnnotatorModel.getWindowSize()));
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
    }

    private void updateTags(Model<Tag> tagsModel, final TagSet newSelection)
    {
        tagsDropDown.remove();
        tagsDropDown = new DropDown<Tag>("tags", tagsModel, new DropDownDataSource<Tag>()
        {
            private static final long serialVersionUID = 2234038471648260812L;

            @Override
            public String getName()
            {
                return "tags";
            }

            @Override
            public List<Tag> getValues()
            {
                return new ArrayList<Tag>(annotationService.listTags(newSelection));
            }

            @Override
            public String getDescriptionForValue(Tag t)
            {
                return t.getName();
            }
        }, true);

        annotationDialogForm.add(tagsDropDown.setEnableFilterToggle(false));
        tagsDropDown.setOutputMarkupId(true);
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

        String annotationType = BratAjaxCasUtil.getAnnotationType(aType);
        String layerName = BratAjaxCasUtil.getSpanAnnotationTypeName(annotationType);

        AnnotationType layer = BratAjaxCasUtil.getAnnotationType(this.annotationService, layerName,
                "span");

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
