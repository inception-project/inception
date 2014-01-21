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
import java.util.Arrays;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AutomationModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.project.ProjectUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.automation.AutomationPage;

/**
 * Modal Window to configure {@link AutomationPage} MIRA templates for training and prediction
 *
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
public class MiraTemplateModalPanel
    extends Panel
{
    private static final long serialVersionUID = -2102136855109258306L;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    private final AutomationTemplateDetailForm automationTemplateForm;

    private final AutomationModel automationModel;
    private final BratAnnotatorModel bratModel;

    private class AutomationTemplateDetailForm
        extends Form<AutomationTemplateModel>
    {
        private static final long serialVersionUID = -683824912741426241L;

        public AutomationTemplateDetailForm(String id, final ModalWindow modalWindow)
        {
            super(id, new CompoundPropertyModel<AutomationTemplateModel>(
                    new AutomationTemplateModel()));

            getModelObject().capitalized = automationModel.isCapitalized();
            getModelObject().containsNumber = automationModel.isContainsNumber();
            getModelObject().prefix1 = automationModel.isPrefix1();
            getModelObject().prefix2 = automationModel.isPrefix2();
            getModelObject().prefix3 = automationModel.isPrefix3();
            getModelObject().prefix4 = automationModel.isPrefix4();
            getModelObject().prefix5 = automationModel.isPrefix5();
            getModelObject().suffix1 = automationModel.isSuffix1();
            getModelObject().suffix2 = automationModel.isSuffix2();
            getModelObject().suffix3 = automationModel.isSuffix3();
            getModelObject().suffix4 = automationModel.isSuffix4();
            getModelObject().suffix5 = automationModel.isSuffix5();

            getModelObject().ngram = automationModel.getNgram();
            getModelObject().bigram = automationModel.getBigram();

            getModelObject().predictInThisPage = automationModel.isPredictInThisPage();
            getModelObject().trainLayer = automationModel.getTrainTagSet();
            getModelObject().featureLayer = automationModel.getFeatureTagSet();

            getModelObject().predictAnnotator = automationModel.isPredictAnnotator();
            getModelObject().predictAutomator = automationModel.isPredictAutomator();

            add(new CheckBox("capitalized"));
            add(new CheckBox("containsNumber"));
            add(new CheckBox("prefix1"));
            add(new CheckBox("prefix2"));
            add(new CheckBox("prefix3"));
            add(new CheckBox("prefix4"));
            add(new CheckBox("prefix5"));
            add(new CheckBox("suffix1"));
            add(new CheckBox("suffix2"));
            add(new CheckBox("suffix3"));
            add(new CheckBox("suffix4"));
            add(new CheckBox("suffix5"));

            add(new CheckBox("predictAnnotator"));
            add(new CheckBox("predictAutomator"));

            add(new DropDownChoice<Integer>("ngram", Arrays.asList(new Integer[] { 1, 2, 3 })));

            add(new DropDownChoice<Integer>("bigram", Arrays.asList(new Integer[] { 1, 2, 3 })));

            // checkbox to limit prediction of annotation on same page or not, for Automation page

            add(new CheckBox("predictInThisPage"));

            add(new RadioChoice<TagSet>("trainLayer", new ArrayList<TagSet>(
                    bratModel.getAnnotationLayers())).setChoiceRenderer(new ChoiceRenderer<TagSet>(
                    "name", "id")));

            ArrayList<TagSet> featureLayers = new ArrayList<TagSet>(bratModel.getAnnotationLayers());
            TagSet noLayer = new TagSet();
            noLayer.setName("N feature layer");
            featureLayers.add(noLayer);

            add(new RadioChoice<TagSet>("featureLayer", featureLayers)
                    .setChoiceRenderer(new ChoiceRenderer<TagSet>("name", "id")));

            add(new AjaxSubmitLink("saveButton")
            {
                private static final long serialVersionUID = -755759008587787147L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {

                    automationModel.setBigram(getModelObject().bigram);
                    automationModel.setNgram(getModelObject().ngram);
                    automationModel.setCapitalized(getModelObject().capitalized);
                    automationModel.setContainsNumber(getModelObject().containsNumber);

                    automationModel.setPrefix1(getModelObject().prefix1);
                    automationModel.setPrefix2(getModelObject().prefix2);
                    automationModel.setPrefix3(getModelObject().prefix3);
                    automationModel.setPrefix4(getModelObject().prefix4);
                    automationModel.setPrefix5(getModelObject().prefix5);

                    automationModel.setSuffix1(getModelObject().suffix1);
                    automationModel.setSuffix2(getModelObject().suffix2);
                    automationModel.setSuffix3(getModelObject().suffix3);
                    automationModel.setSuffix4(getModelObject().suffix4);
                    automationModel.setSuffix5(getModelObject().suffix5);

                    automationModel.setTrainTagSet(getModelObject().trainLayer);
                    if (getModelObject().featureLayer != null) {
                        automationModel.setFeatureTagSet(getModelObject().featureLayer);
                    }
                    automationModel.setPredictInThisPage(getModelObject().predictInThisPage);

                    automationModel.setPredictAnnotator(getModelObject().predictAnnotator);
                    automationModel.setPredictAutomator(getModelObject().predictAutomator);

                    if (automationModel.getTrainTagSet() == null) {
                        aTarget.appendJavaScript("alert('No annotation layer is selected for MIRA tarining/prediction')");
                        return;
                    }

                    try {
                        ProjectUtil.savePreference(bratModel, automationModel, projectRepository);
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
                    AutomationTemplateDetailForm.this.detach();
                    onCancel(aTarget);
                    modalWindow.close(aTarget);
                }
            });
        }
    }

    protected void onCancel(AjaxRequestTarget aTarget)
    {
    }

    public static class AutomationTemplateModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;
        public boolean capitalized;
        public boolean containsNumber;
        public boolean prefix1;
        public boolean prefix2;
        public boolean prefix3;
        public boolean prefix4;
        public boolean prefix5;

        public boolean suffix1;
        public boolean suffix2;
        public boolean suffix3;
        public boolean suffix4;
        public boolean suffix5;

        public int ngram;
        public int bigram;

        public boolean predictInThisPage;
        public boolean useExistingModel;
        public TagSet trainLayer;
        public TagSet featureLayer;

        public boolean predictAnnotator;
        public boolean predictAutomator;
    }

    public MiraTemplateModalPanel(String aId, final ModalWindow modalWindow,
            BratAnnotatorModel aBModel, AutomationModel aAModel)
    {
        super(aId);
        this.bratModel = aBModel;
        this.automationModel = aAModel;
        automationTemplateForm = new AutomationTemplateDetailForm("automationTemplateForm",
                modalWindow);
        add(automationTemplateForm);
    }

}
