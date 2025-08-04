/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.sidebar.llm;

import static de.tudarmstadt.ukp.inception.recommendation.sidebar.llm.InteractiveRecommenderSidebarPrefs.KEY_INTERACTIVE_RECOMMENDER_SIDEBAR_PREFS;
import static de.tudarmstadt.ukp.inception.recommendation.tasks.PredictionTask.ReconciliationOption.KEEP_EXISTING;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Collections.emptyList;
import static wicket.contrib.input.events.EventType.click;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature_;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer_;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender_;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.tasks.PredictionTask;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import jakarta.persistence.EntityManager;
import wicket.contrib.input.events.key.KeyType;

public class InteractiveRecommenderSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -1;

    private static final String MID_FORM = "form";
    private static final String MID_TRAITS_CONTAINER = "traitsContainer";
    private static final String MID_FEATURE = "feature";
    private static final String MID_LAYER = "layer";
    private static final String MID_TRAITS = "traits";
    private static final String MID_EXECUTE = "execute";
    private static final String MID_RECOMMENDER = "recommender";
    private static final String MID_KEEP_EXISTING_SUGGESTIONS = "keepExistingSuggestions";

    private @SpringBean UserDao userService;
    private @SpringBean RecommendationService recommendationService;
    private @SpringBean AnnotationSchemaService schemaService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean SuggestionSupportRegistry suggestionSupportRegistry;
    private @SpringBean SchedulingService schedulingService;
    private @SpringBean PreferencesService preferencesService;
    private @SpringBean EntityManager entityManager;

    private WebMarkupContainer traitsContainer;

    private final IModel<Recommender> recommender;
    private final IModel<Boolean> keepExisting;

    private DropDownChoice<Recommender> recommenderChoice;

    private DropDownChoice<AnnotationFeature> featureChoice;

    private DropDownChoice<AnnotationLayer> layerChoice;

    private CompoundPropertyModel<InteractiveRecommenderSidebarPrefs> sidebarPrefs;

    public InteractiveRecommenderSidebar(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);

        sidebarPrefs = new CompoundPropertyModel<>(Model.of(loadSidebarPrefs()));

        keepExisting = new Model<>(false);
        recommender = new Model<>();
        loadLastUsedRecommender(recommender);

        var form = new Form<>(MID_FORM, CompoundPropertyModel.of(recommender));
        add(form);

        var interactiveRecommenders = listInteractiveRecommenders();
        recommenderChoice = new DropDownChoice<Recommender>(MID_RECOMMENDER);
        recommenderChoice.setModel(recommender);
        recommenderChoice.setChoiceRenderer(new ChoiceRenderer<>(Recommender_.NAME));
        recommenderChoice.setChoices(interactiveRecommenders);
        recommenderChoice.setVisible(interactiveRecommenders.size() > 0);
        recommenderChoice.setEnabled(interactiveRecommenders.size() > 1);
        recommenderChoice.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionChangeRecommender));
        form.add(recommenderChoice);

        featureChoice = new DropDownChoice<>(MID_FEATURE, this::listFeatures);
        featureChoice.setOutputMarkupPlaceholderTag(true);
        featureChoice.setChoiceRenderer(new ChoiceRenderer<>(AnnotationFeature_.UI_NAME));
        featureChoice.setRequired(true);
        featureChoice.add(LambdaBehavior.onConfigure(_this -> {
            if (featureChoice.getChoicesModel().getObject().size() == 1) {
                featureChoice.setModelObject(featureChoice.getChoicesModel().getObject().get(0));
            }
        }));
        featureChoice.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionChangeFeature));
        featureChoice.add(
                visibleWhen(() -> recommender.map(Recommender::getId).isPresent().getObject() && //
                        featureChoice.getChoicesModel().map(c -> c.size() > 1).orElse(false)
                                .getObject()));
        form.add(featureChoice);

        layerChoice = new DropDownChoice<>(MID_LAYER, this::listLayers);
        layerChoice.setOutputMarkupPlaceholderTag(true);
        layerChoice.setChoiceRenderer(new ChoiceRenderer<>(AnnotationLayer_.UI_NAME));
        layerChoice.setRequired(true);
        layerChoice.add(
                new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT, this::actionChangeLayer));
        layerChoice.add(
                visibleWhen(() -> recommender.map(Recommender::getId).isPresent().getObject() && //
                        layerChoice.getChoicesModel().map(c -> !c.isEmpty()).orElse(false)
                                .getObject()));
        form.add(layerChoice);

        form.add(traitsContainer = new WebMarkupContainer(MID_TRAITS_CONTAINER));
        traitsContainer.setOutputMarkupPlaceholderTag(true);

        form.add(new CheckBox(MID_KEEP_EXISTING_SUGGESTIONS, keepExisting).setOutputMarkupId(true));

        actionChangeRecommender(null);

        var executeButton = new LambdaAjaxButton<Recommender>(MID_EXECUTE, this::execute);
        executeButton.triggerAfterSubmit();
        executeButton
                .add(new InputBehavior(new KeyType[] { KeyType.Enter }, click).setTarget(this));
        form.add(executeButton);
    }

    private void loadLastUsedRecommender(IModel<Recommender> aRecommender)
    {
        var interactiveRecommenders = listInteractiveRecommenders();

        if (interactiveRecommenders.isEmpty()) {
            return;
        }

        var prefs = sidebarPrefs.getObject();
        var rec = interactiveRecommenders.stream() //
                .filter(r -> Objects.equals(r.getId(), prefs.getLastRecommenderUsed())) //
                .findFirst() //
                .orElse(interactiveRecommenders.get(0));

        // Since we are going to put the user's settings into the recommender, we want to have a
        // private copy for the sidebar whose changes do not persist
        if (entityManager.contains(rec)) {
            entityManager.detach(rec);
        }

        aRecommender.setObject(rec);

        loadLastUsedSettings(rec);
    }

    private void loadLastUsedSettings(Recommender aRec)
    {
        var prefs = sidebarPrefs.getObject();
        var maybeFactory = recommendationService.getRecommenderFactory(aRec);
        if (maybeFactory.isEmpty()) {
            return;
        }

        var layers = listLayers();
        if (!layers.isEmpty()) {
            var layer = layers.stream() //
                    .filter(l -> Objects.equals(l.getId(), prefs.getLastLayerUsed())) //
                    .findFirst() //
                    .orElse(layers.get(0));
            aRec.setLayer(layer);
        }
        else {
            aRec.setLayer(null);
        }

        var features = listFeatures();
        if (!features.isEmpty()) {
            var feature = features.stream() //
                    .filter(f -> Objects.equals(f.getId(), prefs.getLastFeatureUsed())) //
                    .findFirst() //
                    .orElse(features.get(0));
            aRec.setFeature(feature);
            aRec.setLayer(feature.getLayer()); // Probably redundant...
        }
        else {
            aRec.setFeature(null);
        }

        var factory = maybeFactory.get();
        var traits = (LlmRecommenderTraits) factory.readTraits(aRec);
        if (prefs.getLastPromptUsed() != null) {
            traits.setPrompt(prefs.getLastPromptUsed());
        }
        if (prefs.getLastExtractionModeUsed() != null) {
            traits.setExtractionMode(prefs.getLastExtractionModeUsed());
        }
        if (prefs.getLastPromptingModeUsed() != null) {
            traits.setPromptingMode(prefs.getLastPromptingModeUsed());
        }
        if (prefs.isLastJustificationEnabled() != null) {
            traits.setJustificationEnabled(prefs.isLastJustificationEnabled());
        }

        factory.writeTraits(aRec, traits);
    }

    private void saveLastUsedSettings(User sessionOwner, Recommender rec,
            Optional<RecommendationEngineFactory<Object>> maybeFactory)
    {
        var factory = maybeFactory.get();
        var prefs = sidebarPrefs.getObject();
        var traits = (LlmRecommenderTraits) factory.readTraits(rec);
        prefs.setLastPromptUsed(traits.getPrompt());
        prefs.setLastPromptingModeUsed(traits.getPromptingMode());
        prefs.setLastExtractionModeUsed(traits.getExtractionMode());
        prefs.setLastJustificationEnabled(traits.isJustificationEnabled());
        preferencesService.saveTraitsForUserAndProject(KEY_INTERACTIVE_RECOMMENDER_SIDEBAR_PREFS,
                sessionOwner, getModelObject().getProject(), prefs);
    }

    private InteractiveRecommenderSidebarPrefs loadSidebarPrefs()
    {
        var sessionOwner = userService.getCurrentUser();
        return preferencesService.loadTraitsForUserAndProject(
                KEY_INTERACTIVE_RECOMMENDER_SIDEBAR_PREFS, sessionOwner,
                getModelObject().getProject());
    }

    private void actionChangeFeature(AjaxRequestTarget aTarget)
    {
        var prefs = sidebarPrefs.getObject();
        prefs.setLastFeatureUsed(
                recommender.map(r -> r.getFeature()).map(f -> f.getId()).orElse(null).getObject());

        var sessionOwner = userService.getCurrentUser();
        preferencesService.saveTraitsForUserAndProject(KEY_INTERACTIVE_RECOMMENDER_SIDEBAR_PREFS,
                sessionOwner, getModelObject().getProject(), prefs);
    }

    private void actionChangeLayer(AjaxRequestTarget aTarget)
    {
        featureChoice.getChoicesModel().detach();

        if (!featureChoice.getChoicesModel().getObject().isEmpty()) {
            featureChoice.setModelObject(featureChoice.getChoicesModel().getObject().get(0));
        }
        else {
            featureChoice.setModelObject(null);
        }

        var sessionOwner = userService.getCurrentUser();
        var prefs = sidebarPrefs.getObject();
        prefs.setLastLayerUsed(
                recommender.map(r -> r.getLayer()).map(l -> l.getId()).orElse(null).getObject());
        prefs.setLastFeatureUsed(
                recommender.map(r -> r.getFeature()).map(f -> f.getId()).orElse(null).getObject());
        preferencesService.saveTraitsForUserAndProject(KEY_INTERACTIVE_RECOMMENDER_SIDEBAR_PREFS,
                sessionOwner, getModelObject().getProject(), prefs);

        if (aTarget != null) {
            aTarget.add(featureChoice, traitsContainer);
        }
    }

    private void actionChangeRecommender(AjaxRequestTarget aTarget)
    {
        var changeAccepted = false;
        var prefs = sidebarPrefs.getObject();
        if (recommender.isPresent().getObject()) {
            // Since we are going to put the user's settings into the recommender, we want to have a
            // private copy for the sidebar whose changes do not persist
            if (entityManager.contains(recommender.getObject())) {
                entityManager.detach(recommender.getObject());
            }

            var factory = recommendationService.getRecommenderFactory(recommender.getObject());
            if (factory.isPresent()) {
                prefs.setLastRecommenderUsed(
                        recommender.map(r -> r.getId()).orElse(null).getObject());
                loadLastUsedSettings(recommender.getObject());
                traitsContainer.addOrReplace( //
                        factory.get().createInteractionPanel(MID_TRAITS, recommender));
                changeAccepted = true;
            }
        }

        if (!changeAccepted) {
            prefs = new InteractiveRecommenderSidebarPrefs();
            sidebarPrefs.setObject(prefs);
            traitsContainer.addOrReplace(new EmptyPanel(MID_TRAITS));
        }

        var sessionOwner = userService.getCurrentUser();
        preferencesService.saveTraitsForUserAndProject(KEY_INTERACTIVE_RECOMMENDER_SIDEBAR_PREFS,
                sessionOwner, getModelObject().getProject(), prefs);

        if (aTarget != null) {
            aTarget.add(traitsContainer, featureChoice, layerChoice);
        }
    }

    private List<Recommender> listInteractiveRecommenders()
    {
        return recommendationService.listEnabledRecommenders(getModelObject().getProject()).stream() //
                .filter(rec -> recommendationService.getRecommenderFactory(rec)
                        .map(factory -> factory.isInteractive(rec)).orElse(false)) //
                .toList();
    }

    private List<AnnotationFeature> listFeatures()
    {
        if (!recommender.isPresent().getObject()) {
            return emptyList();
        }

        var layer = recommender.getObject().getLayer();
        if (layer == null) {
            return emptyList();
        }

        var maybeFactory = recommendationService.getRecommenderFactory(recommender.getObject());
        if (!maybeFactory.isPresent()) {
            return emptyList();
        }

        var factory = maybeFactory.get();

        return schemaService.listSupportedFeatures(layer).stream()
                .filter(feat -> feat.getType() != null) //
                .filter(featureSupportRegistry::isAccessible) //
                .filter(factory::accepts) //
                .toList();
    }

    private List<AnnotationLayer> listLayers()
    {
        if (!recommender.isPresent().getObject()) {
            return emptyList();
        }

        return listLayers(recommender.getObject());
    }

    private List<AnnotationLayer> listLayers(Recommender aRecommender)
    {
        var maybeFactory = recommendationService.getRecommenderFactory(aRecommender);
        if (!maybeFactory.isPresent()) {
            return emptyList();
        }

        var factory = maybeFactory.get();

        return schemaService.listAnnotationLayer(getModelObject().getProject()).stream() //
                .filter(layer -> !Token._TypeName.equals(layer.getName())) //
                .filter(layer -> !Sentence._TypeName.equals(layer.getName())) //
                .filter(layer -> !ChainLayerSupport.TYPE.equals(layer.getType())) //
                .filter(factory::accepts) //
                .toList();
    }

    private void execute(AjaxRequestTarget aTarget, Form<Recommender> aForm) throws Exception
    {
        var sessionOwner = userService.getCurrentUser();
        var state = getModelObject();
        var document = state.getDocument();
        var dataOwner = state.getUser().getUsername();
        var rec = aForm.getModelObject();

        var maybeFactory = recommendationService.getRecommenderFactory(rec);
        if (maybeFactory.isEmpty()) {
            return;
        }

        saveLastUsedSettings(sessionOwner, rec, maybeFactory);

        var predictionTask = PredictionTask.builder() //
                .withSessionOwner(sessionOwner) //
                .withTrigger("Interactivce recommender") //
                .withCurrentDocument(document) //
                .withDataOwner(dataOwner) //
                .withRecommender(rec); //

        if (keepExisting.getObject()) {
            predictionTask.withReconciliationOptions(KEEP_EXISTING);
        }

        schedulingService.enqueue(predictionTask.build());
    }
}
