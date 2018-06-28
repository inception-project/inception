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
package de.tudarmstadt.ukp.inception.recommendation.imls.core.trainer.ner;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;

/**
 * Provides helper methods to transform the loaded annotated sentence into an interim stage object
 * used for the training of Named Entity Recognizer models
 * 
 *
 *
 */
public class NerDataHelper
{
    private NerDataHelper()
    {
    }

    public static List<List<AnnotationObject>> getNamedEntities(List<AnnotationObject> sentence)
    {
        List<List<AnnotationObject>> result = new LinkedList<>();
        String currentAnnotation = "";
        List<AnnotationObject> tokens = new LinkedList<>();

        for (AnnotationObject ao : sentence) {
            String annotation = ao.getLabel();

            if (currentAnnotation.isEmpty()) {
                if (annotation != null && !annotation.isEmpty()) {
                    currentAnnotation = annotation;
                    tokens.add(ao);
                }
            }
            else if (currentAnnotation.equals(annotation)) {
                tokens.add(ao);
            }
            else if (annotation != null && !annotation.isEmpty()) {
                result.add(Collections.unmodifiableList(tokens));
                tokens = new LinkedList<>();
                tokens.add(ao);
                currentAnnotation = annotation;
            }
        }

        // add the last annotation as well if not empty.
        if (!currentAnnotation.isEmpty()) {
            result.add(Collections.unmodifiableList(tokens));
        }

        return result;
    }

    public static Map<List<String>, String> getAnnotationMappingForSentence(
            List<AnnotationObject> sentence)
    {
        Map<List<String>, String> result = new HashMap<>();

        for (List<AnnotationObject> entry : getNamedEntities(sentence)) {
            List<String> tokens = new LinkedList<>();
            String annotationLabel = null;

            for (AnnotationObject ao : entry) {
                annotationLabel = ao.getLabel();
                tokens.add(ao.getCoveredText());
            }

            if (annotationLabel != null) {
                result.put(Collections.unmodifiableList(tokens), annotationLabel);
            }
        }

        return result;
    }
}
