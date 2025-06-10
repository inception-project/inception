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
package de.tudarmstadt.ukp.inception.recommendation.api.recommender;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_CORRECTION_EXPLANATION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_CORRECTION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_EXPLANATION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_SUFFIX;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;

import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.rendering.model.Range;

public abstract class RecommendationEngine
{
    protected static final String BLANK_LABEL = "__NO_LABEL__";

    protected final Recommender recommender;
    protected final String layerName;
    protected final String featureName;
    protected final int maxRecommendations;

    public RecommendationEngine(Recommender aRecommender)
    {
        recommender = aRecommender;

        layerName = aRecommender.getLayer().getName();
        featureName = aRecommender.getFeature().getName();
        maxRecommendations = aRecommender.getMaxRecommendations();
    }

    public Recommender getRecommender()
    {
        return recommender;
    }

    // tag::methodDefinition[]
    /**
     * Given training data in {@code aCasses}, train a model. In order to save data between runs,
     * the {@code aContext} can be used. This method must not mutate {@code aCasses} in any way.
     * 
     * @param aContext
     *            The context of the recommender
     * @param aCasses
     *            The training data
     * @throws RecommendationException
     *             if there was a problem during training
     */
    public abstract void train(RecommenderContext aContext, List<CAS> aCasses)
        throws RecommendationException;

    /**
     * Given text in a {@link CAS}, predict target annotations. These should be written into
     * {@link CAS}. In order to restore data from e.g. previous training, the
     * {@link RecommenderContext} can be used.
     * 
     * @param aContext
     *            The context of the recommender
     * @param aCas
     *            The training data
     * @return Range in which the recommender generated predictions. No suggestions in this range
     *         should be inherited.
     * @throws RecommendationException
     *             if there was a problem during prediction
     */
    public Range predict(PredictionContext aContext, CAS aCas) throws RecommendationException
    {
        return predict(aContext, aCas, 0, aCas.getDocumentText().length());
    }

    /**
     * Given text in a {@link CAS}, predict target annotations. These should be written into
     * {@link CAS}. In order to restore data from e.g. previous training, the
     * {@link RecommenderContext} can be used.
     * <p>
     * Depending on the recommender, it may be necessary to internally extend the range in which
     * recommendations are generated so that recommendations that partially overlap the prediction
     * range may also be generated.
     * 
     * @param aContext
     *            The context of the recommender
     * @param aCas
     *            The training data
     * @param aBegin
     *            Begin of the range in which predictions should be generated.
     * @param aEnd
     *            End of the range in which predictions should be generated.
     * @return Range in which the recommender generated predictions. No suggestions in this range
     *         should be inherited.
     * @throws RecommendationException
     *             if there was a problem during prediction
     */
    public abstract Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException;

    /**
     * Evaluates the performance of a recommender by splitting the data given in {@code aCasses} in
     * training and test sets by using {@code aDataSplitter}, training on the training set and
     * measuring performance on unseen data on the training set. This method must not mutate
     * {@code aCasses} in any way.
     * 
     * @param aCasses
     *            The CASses containing target annotations
     * @param aDataSplitter
     *            The splitter which determines which annotations belong to which set
     * @return Scores available through an EvaluationResult object measuring the performance of
     *         predicting on the test set
     * @throws RecommendationException
     *             if there was a problem during evaluation
     */
    public abstract EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
        throws RecommendationException;
    // end::methodDefinition[]

    /**
     * This method should be called before attempting to call {@link #predict} to ensure that the
     * given context contains sufficient information to perform prediction. If this method returns
     * {@code false}, calling {@link #predict} might result in an exception being thrown or in
     * predictions being invalid/unusable.
     * 
     * @param aContext
     *            a recommender context
     * 
     * @return if the recommender can use the given context to make predictions. This is usually the
     *         case if the recommender has previously initialized the context with a trained model.
     *         However, some recommenders might be able to provide recommendations without a trained
     *         model, e.g. using some kind of fall back mechanism.
     */
    public abstract boolean isReadyForPrediction(RecommenderContext aContext);

    /**
     * @return which training capabilities this engine has. If training is not supported, the call
     *         to {@link #train} should be skipped and {@link #predict} should be called
     *         immediately. Note that the engine cannot expect a model to be present in the
     *         {@link RecommenderContext} if training is skipped or fails - this is meant only for
     *         engines that use pre-trained models.
     */
    public TrainingCapability getTrainingCapability()
    {
        return TrainingCapability.TRAINING_SUPPORTED;
    }

    /**
     * @return which prediction capabilities this engine has. If a recommender uses annotations and
     *         not only the text, then this method should be overwritten to return
     *         {@link PredictionCapability#PREDICTION_USES_ANNOTATIONS}
     */
    public PredictionCapability getPredictionCapability()
    {
        return PredictionCapability.PREDICTION_USES_TEXT_ONLY;
    }

    /**
     * @param aContext
     *            previous context
     * @return a new context given the previous context. This allows incrementally training
     *         recommenders to salvage information from the current context for a new iteration. By
     *         default, no information is copy and simply new context is created.
     */
    public RecommenderContext newContext(RecommenderContext aContext)
    {
        return new RecommenderContext();
    }

    /**
     * @param aCasses
     *            a list of CASes that should be examined for data points
     * @return an estimated number of data points in the data set. If the returned number is
     *         negative, no estimation could be made.
     */
    public abstract int estimateSampleCount(List<CAS> aCasses);

    public Type getPredictedType(CAS aCas)
    {
        return getType(aCas, layerName);
    }

    public Feature getPredictedFeature(CAS aCas)
    {
        return getPredictedType(aCas).getFeatureByBaseName(featureName);
    }

    public Feature getScoreFeature(CAS aCas)
    {
        return getPredictedType(aCas).getFeatureByBaseName(featureName + FEATURE_NAME_SCORE_SUFFIX);
    }

    public Feature getScoreExplanationFeature(CAS aCas)
    {
        return getPredictedType(aCas)
                .getFeatureByBaseName(featureName + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX);
    }

    public Feature getCorrectionFeature(CAS aCas)
    {
        return getPredictedType(aCas)
                .getFeatureByBaseName(featureName + FEATURE_NAME_CORRECTION_SUFFIX);
    }

    public Feature getCorrectionExplanationFeature(CAS aCas)
    {
        return getPredictedType(aCas)
                .getFeatureByBaseName(featureName + FEATURE_NAME_CORRECTION_EXPLANATION_SUFFIX);
    }

    protected Feature getModeFeature(CAS aCas)
    {
        return getPredictedType(aCas)
                .getFeatureByBaseName(featureName + FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX);
    }

    public Feature getIsPredictionFeature(CAS aCas)
    {
        var type = getPredictedType(aCas);
        var feature = type.getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);
        if (feature == null) {
            throw new IllegalArgumentException(
                    "CAS has not been prepared for prediction. Type [" + type.getName()
                            + "] does not have the feature [" + FEATURE_NAME_IS_PREDICTION + "]");
        }
        return feature;
    }

    public void exportModel(RecommenderContext aContext, OutputStream aOutput) throws IOException
    {
        throw new UnsupportedOperationException("Model export not supported");
    }

    public List<? extends TrainingInstance> generateIncrementalTrainingInstances(
            AnnotationEvent aEvent)
    {
        return emptyList();
    }

    /**
     * Store the given incremental training data into the given recommender context. The idea is
     * that the engine then picks the training data up from the context on the next training run. It
     * may then choose to use only the incremental training data during that run instead of training
     * from scratch.
     */
    public void putIncrementalTrainingData(RecommenderContext aRecommenderContext,
            List<TrainingInstance> aIncrementalTrainingData)
    {
        // Nothing do to
    }
}
