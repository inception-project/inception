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

import org.apache.commons.lang.exception.ExceptionUtils;
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
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AnnotationPreference;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
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
    private RepositoryService projectRepository;

    private final AnnotationLayerDetailForm tagSelectionForm;

    private NumberTextField<Integer> windowSizeField;

    private final BratAnnotatorModel bratAnnotatorModel;

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
            getModelObject().numberOfSentences = bratAnnotatorModel.getWindowSize();
            getModelObject().scrollPage = bratAnnotatorModel.isScrollPage();
            getModelObject().predictInThisPage = bratAnnotatorModel.isPredictInThisPage();

            for (TagSet tagSet : bratAnnotatorModel.getAnnotationLayers()) {
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
                            List<TagSet> tagSets = annotationService.listTagSets(bratAnnotatorModel
                                    .getProject());
                            List<TagSet> corefTagSets = new ArrayList<TagSet>();
                            for (TagSet tagSet : tagSets) {
                                if (tagSet.getType().getName().equals("coreference type")
                                        || tagSet.getType().getName().equals("coreference")) {
                                    corefTagSets.add(tagSet);
                                }
                            }

                            if (bratAnnotatorModel.getMode().equals(Mode.CORRECTION)
                                    || bratAnnotatorModel.getMode().equals(Mode.CURATION)) {
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

            // checkbox to limit prediction of annotation on same page or not, for Automation page

            add(new CheckBox("predictInThisPage")
            {
                @Override
                public boolean isVisible()
                {
                    return bratAnnotatorModel.getProject().getMode().equals(Mode.AUTOMATION);
                }
            });

            add(new Label("predictInThisPageLabel",
                    "Limit automatic annotation prediction on this page :"));
            // Add a Checkbox to enable/disable automatic page navigations while annotating
            add(new CheckBox("scrollPage"));

            add(new Label("scrollPageLabel", "Auto-scroll document while annotating :"));

            add(new AjaxSubmitLink("saveButton")
            {
                private static final long serialVersionUID = -755759008587787147L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    AnnotationPreference preference = new AnnotationPreference();
                    preference.setScrollPage(getModelObject().scrollPage);
                    preference.setWindowSize(getModelObject().numberOfSentences);

                    ArrayList<Long> layers = new ArrayList<Long>();

                    for (TagSet tagset : getModelObject().annotationLayers) {
                        layers.add(tagset.getId());
                    }
                    preference.setAnnotationLayers(layers);
                    String username = SecurityContextHolder.getContext().getAuthentication()
                            .getName();
                    try {
                        projectRepository.saveUserSettings(username,
                                bratAnnotatorModel.getProject(), bratAnnotatorModel.getMode(),
                                preference);
                    }
                    catch (FileNotFoundException e) {
                        error("Unable to save preferences in a property file: "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (IOException e) {
                        error("Unable to save preferences in a property file: "
                                + ExceptionUtils.getRootCauseMessage(e));
                    }

                    bratAnnotatorModel.setScrollPage(getModelObject().scrollPage);
                    bratAnnotatorModel.setPredictInThisPage(getModelObject().predictInThisPage);
                    bratAnnotatorModel.setAnnotationLayers(getModelObject().annotationLayers);
                    bratAnnotatorModel.setWindowSize(getModelObject().numberOfSentences);
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
        public boolean predictInThisPage;
        public HashSet<TagSet> annotationLayers = new HashSet<TagSet>();
    }

    public AnnotationPreferenceModalPanel(String aId, final ModalWindow modalWindow,
            BratAnnotatorModel aBratAnnotatorModel)
    {
        super(aId);
        this.bratAnnotatorModel = aBratAnnotatorModel;
        tagSelectionForm = new AnnotationLayerDetailForm("tagSelectionForm", modalWindow);
        add(tagSelectionForm);
    }

}
