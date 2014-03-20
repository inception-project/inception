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
package de.tudarmstadt.ukp.clarin.webanno.webapp.dialog;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.project.ProjectUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;

/**
 * Modal Window to configure {@link BratAnnotator#setAnnotationLayers(ArrayList),
 * BratAnnotator#setWindowSize(int)...}
 *
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
public class AnnotationPreferenceModalPanel
    extends Panel
{
    private static final long serialVersionUID = -2102136855109258306L;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    private final AnnotationLayerDetailForm tagSelectionForm;

    private NumberTextField<Integer> windowSizeField;

    private final BratAnnotatorModel bModel;

    private class AnnotationLayerDetailForm
        extends Form<AnnotationLayerDetailFormModel>
    {
        private static final long serialVersionUID = -683824912741426241L;

        @SuppressWarnings({})
        public AnnotationLayerDetailForm(String id, final ModalWindow modalWindow)
        {
            super(id, new CompoundPropertyModel<AnnotationLayerDetailFormModel>(
                    new AnnotationLayerDetailFormModel()));

            // Import current settings from the annotator
            getModelObject().numberOfSentences = bModel.getWindowSize();
            getModelObject().scrollPage = bModel.isScrollPage();
            getModelObject().staticColor = bModel.isStaticColor();
            for (TagSet tagSet : bModel.getAnnotationLayers()) {
                getModelObject().annotationLayers.add(tagSet);
            }
            windowSizeField = new NumberTextField<Integer>("numberOfSentences");
            windowSizeField.setType(Integer.class);
            windowSizeField.setMinimum(1);
            add(windowSizeField);

            add(new CheckBoxMultipleChoice<TagSet>("annotationLayers")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<TagSet>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<TagSet> load()
                        {
                            // disable corefernce annotation for correction/curation pages for 0.4.0
                            // release
                            List<TagSet> tagSets = annotationService.listTagSets(bModel
                                    .getProject());
                            List<TagSet> corefTagSets = new ArrayList<TagSet>();
                            for (TagSet tagSet : tagSets) {
                                if (tagSet.getLayer().getName().equals("coreference type")
                                        || tagSet.getLayer().getName().equals("coreference")) {
                                    corefTagSets.add(tagSet);
                                }
                            }

                            if (bModel.getMode().equals(Mode.CORRECTION)
                                    || bModel.getMode().equals(Mode.CURATION)) {
                                tagSets.removeAll(corefTagSets);
                            }
                            return tagSets;
                            // return
                            // annotationService.listTagSets(bratAnnotatorModel.getProject());
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<TagSet>("name", "id"));
                }
            });


            // Add a Checkbox to enable/disable automatic page navigations while annotating
            add(new CheckBox("scrollPage"));

            add(new CheckBox("staticColor"));

            add(new Label("scrollPageLabel", "Auto-scroll document while annotating :"));

            add(new AjaxSubmitLink("saveButton")
            {
                private static final long serialVersionUID = -755759008587787147L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    bModel.setScrollPage(getModelObject().scrollPage);
                    bModel.setAnnotationLayers(getModelObject().annotationLayers);
                    bModel.setWindowSize(getModelObject().numberOfSentences);
                    bModel.setStaticColor(getModelObject().staticColor);
                    try {
                        ProjectUtil.savePreference(bModel, repository);
                    }
                    catch (FileNotFoundException e) {
                      error("Preference file not found");
                    }
                    catch (IOException e) {
                        error("Preference file not found");
                    }
                    modalWindow.close(aTarget);
                }

                @Override
                protected void onError(AjaxRequestTarget aTarget, Form<?> aForm)
                {

                }
            });

            add(new AjaxLink<Void>("cancelButton")
            {
                private static final long serialVersionUID = 7202600912406469768L;

                @Override
                public void onClick(AjaxRequestTarget aTarget)
                {
                    AnnotationLayerDetailForm.this.detach();
                    onCancel(aTarget);
                    modalWindow.close(aTarget);
                }
            });
        }
    }

    protected void onCancel(AjaxRequestTarget aTarget)
    {
    }

    public static class AnnotationLayerDetailFormModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;
        public Project project;
        public SourceDocument document;
        public int numberOfSentences;
        public boolean scrollPage;
        public boolean staticColor;
        public HashSet<TagSet> annotationLayers = new HashSet<TagSet>();
    }

    public AnnotationPreferenceModalPanel(String aId, final ModalWindow modalWindow,
            BratAnnotatorModel aBModel)
    {
        super(aId);
        this.bModel = aBModel;
        tagSelectionForm = new AnnotationLayerDetailForm("tagSelectionForm", modalWindow);
        add(tagSelectionForm);
    }

}
