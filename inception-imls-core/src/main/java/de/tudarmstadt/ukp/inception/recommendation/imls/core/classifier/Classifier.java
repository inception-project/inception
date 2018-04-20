/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.core.classifier;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.imls.conf.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.ConfigurableComponent;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.classificationtool.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.Offset;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.TokenObject;
/**
 * This class defines the methods of a classifier. Every classifier used in a
 * {@link ClassificationTool} class has to implement the defined abstract methods.
 * 
 *
 *
 * @param <C>
 *            The classifier configuration used to configure this classifier.
 */
public abstract class Classifier<C>
    extends ConfigurableComponent<C>
{
    
    public Classifier(ClassifierConfiguration<C> conf)
    {
        super(conf);
    }

    /**
     * Set the trained model for this classifier.
     * 
     * @param m
     *            An object. Either the trained model itself or a file or an stream or something
     *            else to load the model from.
     */
    public abstract void setModel(Object m);

    /**
     * Uses the given list of sentences (List&lt;AnnotationObject&gt;) and predicts the annotation
     * label for every token (the annotation label is null, if no prediction can be made).
     * 
     * @param inputData
     *            All sentences to predict annotations for.
     * @return A list of predictions.
     */
    public <T extends TokenObject> List<AnnotationObject> predict(List<List<T>> inputData, 
            AnnotationLayer layer)
    {
        return mergeAdjacentTokensWithSameLabel(predictSentences(inputData), layer);
    }

    /**
     * Uses the given list of sentences (List&lt;AnnotationObject&gt;) and predicts the annotation
     * label for every token (the annotation label is null, if no prediction can be made).
     * 
     * @param inputData
     *            All sentences to predict annotations for.
     * @return List of sentences, each containing a list of words containing predicted annotations.
     *         These predictions are ranked from the lowest to the highest confidence.
     */
    public abstract <T extends TokenObject> List<List<List<AnnotationObject>>> predictSentences(
            List<List<T>> inputData);
    
    /**
     * Merge recommendations spanning multiple tokens with the same label, but only if the given 
     * layer does not lock to tokens. Also set AnnotationObject Ids new.
     * 
     * @param sentences
     *            All sentences to predict annotations for.
     * @param layer
     * @return A list of predictions.
     */
    public List<AnnotationObject> mergeAdjacentTokensWithSameLabel(
            List<List<List<AnnotationObject>>> sentences, AnnotationLayer layer)
    {
        if (layer.isLockToTokenOffset()) {
            List<AnnotationObject> singleTokenPredictions = new ArrayList<>();
            sentences.forEach(sentence -> sentence.forEach(singleTokenPredictions::addAll));
            return singleTokenPredictions;
        }

        List<AnnotationObject> predictionsWithMergedLabels = new ArrayList<>();
        
        int id = 0;
        
        for (List<List<AnnotationObject>> sentence: sentences) {
            int i = 0;

            // Repeat for as many predictions we have for each token
            while (i <= conf.getNumPredictions()) {

                for (int j = 0; j < sentence.size(); j++) {
                    List<AnnotationObject> word = sentence.get(j);
                    if (i >= word.size()) {
                        break;
                    }
                    AnnotationObject ao = word.get(i);

                    String coveredText = ao.getCoveredText();
                    int endCharacter = ao.getOffset().getEndCharacter();
                    int endToken = ao.getOffset().getEndToken();
                    List<Double> confidence = new ArrayList<>();
                    confidence.add(ao.getConfidence());

                    // For all neighboring predictions in sentence with same label
                    while (j < sentence.size() - 1) {
                        List<AnnotationObject> nextWord = sentence.get(j + 1);
                        if (i + 1 >= nextWord.size()) {
                            break;
                        }
                        AnnotationObject nextAo = nextWord.get(i);
                        
                        if (ao.getAnnotation() != null
                                && ao.getAnnotation().equals(nextAo.getAnnotation())) {
                            coveredText = coveredText + " " + nextAo.getCoveredText();
                            confidence.add(nextAo.getConfidence());
                            endCharacter = nextAo.getOffset().getEndCharacter();
                            endToken = nextAo.getOffset().getEndToken();
                            j++;
                        }
                        else {
                            break;
                        }
                    }

                    // prediction covering single token
                    if (ao.getCoveredText().equals(coveredText)) {
                        ao.setId(id);
                        predictionsWithMergedLabels.add(ao);
                        id++;
                    }
                    
                    // prediction covering multiple tokens
                    else {
                        double averagedConfidence = confidence.stream().mapToDouble(a -> a)
                                .average().getAsDouble();
                        AnnotationObject mergedAo = new AnnotationObject(ao.getAnnotation(),
                                ao.getDocumentURI(), ao.getDocumentName(), coveredText,
                                new Offset(ao.getOffset().getBeginCharacter(), endCharacter,
                                        ao.getOffset().getBeginToken(), endToken),
                                ao.getSentenceTokens(), id, ao.getFeature(),
                                ao.getClassifier(), averagedConfidence);
                        predictionsWithMergedLabels.add(mergedAo);
                        id++;
                    }
                }
                i++;
            }
        }
        
        return predictionsWithMergedLabels;
    }

    public abstract void setUser(User user);

    public abstract void setProject(Project project);
}
