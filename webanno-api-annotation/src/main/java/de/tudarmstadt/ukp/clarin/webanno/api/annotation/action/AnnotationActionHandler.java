/*
 * Copyright 2017
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
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.action;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public interface AnnotationActionHandler
{
    void actionArcAnnotation(AjaxRequestTarget aTarget, JCas jCas, VID paramId, String aOriginType,
            int aOriginSpanId, String aTargetType, int aTargetSpanId)
        throws AnnotationException, UIMAException, ClassNotFoundException, IOException;

    void actionSpanAnnotation(AjaxRequestTarget aTarget, JCas jCas, int aBegin, int aEnd,
            VID paramId)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException;

    void actionAnnotate(AjaxRequestTarget aTarget)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException;
}
