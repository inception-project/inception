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
package de.tudarmstadt.ukp.inception.recommendation.api;

import static org.apache.uima.fit.pipeline.SimplePipeline.iteratePipeline;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;

/**
 * Provides functions to load annotations from JCas or a CollectionReaderDescription.
 */
public interface AnnotationObjectLoader
{

    default List<List<AnnotationObject>> loadAnnotationObjects(CollectionReaderDescription reader)
    {
        List<List<AnnotationObject>> result = new ArrayList<>();

        for (JCas jCas : iteratePipeline(reader)) {
            result.addAll(loadAnnotationObjectsForTesting(jCas));
        }

        return result;
    }

    List<List<AnnotationObject>> loadAnnotationObjectsForTesting(JCas aJCas);

    List<List<AnnotationObject>> loadAnnotationObjects(JCas aJCas);
}
