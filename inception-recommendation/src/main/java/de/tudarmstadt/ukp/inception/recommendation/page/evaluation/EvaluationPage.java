/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.page.evaluation;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.uima.jcas.JCas;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.CollectionModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapAjaxTabbedPanel;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.app.session.SessionMetaData;
import de.tudarmstadt.ukp.inception.recommendation.api.AnnotationObjectLoader;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.conf.EvaluationConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.evaluation.IncrementalEvaluationService;
import de.tudarmstadt.ukp.inception.recommendation.util.EvaluationHelper;

@MountPath("/RecommendationEvaluationPage.html")
public class EvaluationPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -4383211607792551388L;
     
    private static final Logger logger = LoggerFactory.getLogger(EvaluationPage.class);
    
    private @SpringBean ProjectService projectService;
    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userRepository;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean RecommendationService recommendationService;
    private @SpringBean RecommenderFactoryRegistry recommendationRegistry;

    private List<String> trainingIncrementChoices = Arrays.asList(new String[] {
            "fibonacciIncrementStrategy", "equidistantIncrementStrategy"});
    private List<String> evaluationChoices = Arrays.asList(new String[] {
            "holdoutStrategy", "equidistantIncrementStrategy"});
    
    private IModel<AnnotationLayer> selectedLayer;
    private IModel<Project> selectedProject;
    private IModel<Preferences> preferences;
    
    private CollectionModel<SourceDocument> selectedDocuments;
    private CollectionModel<String> selectedClassifiers;
    private BootstrapAjaxTabbedPanel<ITab> tabbedPane;
    
    public EvaluationPage()
    {
        selectedLayer = Model.of();
        selectedProject = Model.of(
                Session.get().getMetaData(SessionMetaData.CURRENT_PROJECT)); 
        preferences = Model.of(new Preferences());
        selectedDocuments = new CollectionModel<>();
        selectedClassifiers = new CollectionModel<>();
      
        Form<Preferences> form = new Form<>("form", new CompoundPropertyModel<>(preferences));
        add(form);
        
        ListMultipleChoice<String> classifierSelection = new ListMultipleChoice<>("classifiers");
        classifierSelection.setOutputMarkupId(true);
        classifierSelection.setModel(selectedClassifiers);
        classifierSelection.setChoices(LambdaModel.of(this::listClassifiers));
        
        form.add(classifierSelection);
        
        ListChoice<AnnotationFeature> feature = new ListChoice<>("feature");
        feature.setChoices(LambdaModel.of(this::listFeatures))
               .setRequired(true)
               .setOutputMarkupId(true);
        
        form.add(feature);
        
        ListMultipleChoice<SourceDocument> documentSelection = new ListMultipleChoice<>("documents");
        documentSelection.setOutputMarkupId(true);
        documentSelection.setModel(selectedDocuments);
        documentSelection.setChoices(LambdaModel.of(this::listDocuments));
        documentSelection.setChoiceRenderer(new ChoiceRenderer<SourceDocument>("name"));
        form.add(documentSelection);

        ListChoice<AnnotationLayer> layerSelection = new ListChoice<>("layers");
        layerSelection.setOutputMarkupId(true);
        layerSelection.setModel(selectedLayer);
        layerSelection.setChoices(LambdaModel.of(this::getLayerChoices));
        layerSelection.setChoiceRenderer(new ChoiceRenderer<>("uiName"));
        layerSelection.add(new LambdaAjaxFormComponentUpdatingBehavior("change",  (t) -> {
            t.add(classifierSelection);
        }));        
        layerSelection.add(new LambdaAjaxFormComponentUpdatingBehavior("change",  (t) -> 
            t.add(form.get("feature")
        )));

        form.add(layerSelection);
        
        // TODO some Info boxes to explain the configuration options would be nice.
        
        form.add(new CheckBox("shuffleTrainingSet"));
        
        form.add(new NumberTextField<Integer>("trainingSetSizeLimit")
                .setMinimum(0));
        
        NumberTextField<Double> splitTestDataPercentage = new NumberTextField<Double>(
                "splitTestDataPercentage");
        splitTestDataPercentage.add(LambdaBehavior.onConfigure(_this -> _this
                .setVisible(form.getModelObject().evaluationStrategy.equals("holdoutStrategy"))));
        splitTestDataPercentage.setMaximum(1.);
        splitTestDataPercentage.setMinimum(0.);
        splitTestDataPercentage.setStep(0.01);
        splitTestDataPercentage.setOutputMarkupId(true);
        splitTestDataPercentage.setOutputMarkupPlaceholderTag(true);

        form.add(splitTestDataPercentage);
        
        NumberTextField<Integer> trainingIncrementSize = 
                new NumberTextField<Integer>("trainingIncrementSize");
        trainingIncrementSize.add(LambdaBehavior.onConfigure(_this -> _this
                .setVisible(form.getModelObject().evaluationStrategy.equals("equidistantIncrementStrategy"))));
        trainingIncrementSize.setMinimum(0);
        trainingIncrementSize.setOutputMarkupId(true);
        trainingIncrementSize.setOutputMarkupPlaceholderTag(true);
        form.add(trainingIncrementSize);
            
        DropDownChoice<String> trainingBatchIncrementStrategy = new DropDownChoice<String>(
                "trainingIncrementStrategy",
                new PropertyModel<String>(this.preferences, "trainingIncrementStrategy"),
                trainingIncrementChoices);
        trainingBatchIncrementStrategy
                .add(new LambdaAjaxFormComponentUpdatingBehavior("change", (t) -> {
                    t.add(trainingIncrementSize);
                }));
        form.add(trainingBatchIncrementStrategy);
        

        NumberTextField<Integer> testIncrementSize = 
                new NumberTextField<Integer>("testIncrementSize");
        testIncrementSize.add(LambdaBehavior.onConfigure(_this -> _this.setVisible(
                form.getModelObject().evaluationStrategy.equals("equidistantIncrementStrategy"))));
        testIncrementSize.setMinimum(0);
        testIncrementSize.setOutputMarkupId(true);
        testIncrementSize.setOutputMarkupPlaceholderTag(true);
        form.add(testIncrementSize);
            
        DropDownChoice<String> evaluationStrategy = new DropDownChoice<String>(
                "evaluationStrategy",
                new PropertyModel<String>(this.preferences, "evaluationStrategy"),
                evaluationChoices);
        evaluationStrategy.add(new LambdaAjaxFormComponentUpdatingBehavior("change", (t) -> {
            t.add(testIncrementSize);
        }));
        evaluationStrategy.add(new LambdaAjaxFormComponentUpdatingBehavior("change", (t) -> {
            t.add(splitTestDataPercentage);
        }));
        form.add(evaluationStrategy);
        
        
        form.add(new LambdaAjaxButton<>("startEvaluation", this::evaluate));

        tabbedPane = new BootstrapAjaxTabbedPanel<>("tabs", Collections.emptyList());
        add(tabbedPane);        
    }

    
    private List<SourceDocument> listDocuments()
    {
        if (selectedProject.getObject() == null) {
            return Collections.emptyList();
        }
        
        return documentService.listSourceDocuments(selectedProject.getObject());
    }
    
    private List<AnnotationFeature> listFeatures()
    {
        if (selectedLayer.getObject() != null) {
            List<AnnotationFeature> features = new ArrayList<>();
            
            annotationService
                .listAnnotationFeature(selectedLayer.getObject())
                .forEach(annotationFeature -> {
                    if (annotationFeature.getType() instanceof String) {
                        features.add(annotationFeature);
                    }
                });   
            return features;
            
        } else {
            return Collections.emptyList();
        }
    }
    
    private List<String> listClassifiers()
    {
        if (selectedLayer.getObject() == null) {
            return Collections.emptyList();
        }
        
        List<String> classifierList = recommendationRegistry.getAllFactories()
            .stream()
            .map(RecommendationEngineFactory::getName)
            .collect(Collectors.toList());
        if (classifierList == null) {
            return Collections.emptyList();
        }
        else {
            return classifierList;
        }
    }

    private List<AnnotationLayer> getLayerChoices()
    {
        Project p = selectedProject.getObject();
        
        if (p == null) {
            return Collections.emptyList();
        }
        
        List<AnnotationLayer> choices = new LinkedList<>();

        if (p != null && p.getId() != null) {
            List<AnnotationLayer> layers = annotationService.listAnnotationLayer(p);

            AnnotationLayer tokenLayer = annotationService.getLayer(Token.class.getName(), p);
            layers.remove(tokenLayer);

            layers.forEach(l -> {
                choices.add(l);
            });
        }

        return choices;
    }
    
    private void evaluate(AjaxRequestTarget aTarget, Form<Preferences> aForm)
    {
        User user = userRepository.getCurrentUser();

        Preferences pref = aForm.getModelObject();
        
        logger.info(pref.trainingIncrementStrategy);
        
        EvaluationConfiguration suiteConf = EvaluationHelper.getTrainingSuiteConfiguration(
                "classificationToolSelection", documentService, selectedProject.getObject(),
                pref.shuffleTrainingSet, pref.trainingSetSizeLimit, pref.splitTestDataPercentage,
                pref.useHoldout, pref.trainingIncrementStrategy, pref.trainingIncrementSize, 
                pref.testIncrementSize);
                
        List<ITab> tabs = new ArrayList<>();
        
        for (String classifierId : selectedClassifiers.getObject()) {
            if (pref.feature == null) {
                logger.error("No feature selected");
                return;
            }
            
            Recommender recommender = new Recommender("Evaluation", selectedLayer.getObject());
            recommender.setTool(classifierId);
            recommender.setFeature(pref.feature.getName());
            
            ClassificationTool<?> ct = null;
            
            if (ct == null || ct.getLoader() == null) {
                continue;
            }
            
            EvaluationHelper.customizeConfiguration(ct, "_evaluationModel.bin", documentService,
                    selectedProject.getObject());
            IncrementalEvaluationService evalService = new IncrementalEvaluationService(ct,
                    suiteConf);
            
            AnnotationObjectLoader loader = ct.getLoader();          
            List<List<AnnotationObject>> annotatedData = new LinkedList<>();
            for (SourceDocument doc : selectedDocuments.getObject()) {
                AnnotationDocument annoDoc = documentService.createOrGetAnnotationDocument(doc,
                        user);
                JCas jCas = null;
                try {
                    jCas = documentService.readAnnotationCas(annoDoc);
                }
                catch (IOException e) {
                    logger.error("Cannot read AnnotationCas.", e);
                }

                if (jCas == null) {
                    continue;
                }
                annotatedData.addAll(loader.loadAnnotationObjectsForEvaluation(jCas));
            }            
            
            EvaluationResult result = 
                    evalService.evaluateIncremental(annotatedData);

            tabs.add(new AbstractTab(Model.of(classifierId))
            {
                private static final long serialVersionUID = 6703144434578403272L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new EvaluationResultTab(panelId, Model.of(result));
                }

                @Override
                public boolean isVisible()
                {
                    return true;
                }
            });  
        }
        
        tabbedPane = (BootstrapAjaxTabbedPanel<ITab>) tabbedPane
                .replaceWith(new BootstrapAjaxTabbedPanel<>("tabs", tabs));
        aTarget.add(tabbedPane);
    }
    
    private static class Preferences
        implements Serializable
    {
        private static final long serialVersionUID = 8352300929211654244L;
        
        AnnotationFeature feature;
        boolean shuffleTrainingSet = false;
        int trainingSetSizeLimit = 0;
        double splitTestDataPercentage = 0.4;
        boolean useHoldout = false;
        String trainingIncrementStrategy = "fibonacciIncrementStrategy";
        String evaluationStrategy = "equidistantIncrementStrategy";
        int trainingIncrementSize = 1;
        int testIncrementSize = 100;
    }
}
