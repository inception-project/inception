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
package de.tudarmstadt.ukp.inception.active.learning.event;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;

public class ActiveLearningRecommendationEvent extends ApplicationEvent {

    private static final long serialVersionUID = -2741267700429534514L;
    private final SourceDocument document;
    private final AnnotationObject annotationObject;
    private final String user;
    private final AnnotationLayer layer;

    public ActiveLearningRecommendationEvent(Object source, SourceDocument document,
                                             AnnotationObject annotationObject,
                                             String user, AnnotationLayer layer) {
        super(source);
        this.document = document;
        this.annotationObject = annotationObject;
        this.user = user;
        this.layer = layer;
    }

    public SourceDocument getDocument() {
        return document;
    }

    public AnnotationObject getAnnotationObject() {
        return annotationObject;
    }

    public String getUser() {
        return user;
    }

    public AnnotationLayer getLayer() {
        return layer;
    }
}
