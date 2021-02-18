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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.controller.AnnotationEditorController;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;

import java.io.IOException;

public interface AnnotationEditorFactory
{
    /**
     * @return get the bean name.
     */
    String getBeanName();

    String getDisplayName();

    AnnotationEditorBase create(String id, IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider);

    default AnnotationEditorBase create(String id, AnnotationEditorController aController, String jsonUser, String jsonProject) throws IOException {
        return null;
    }

    /**
     * Configure the state to be compatible with the editor produced by this factory. E.g. set the
     * paging strategy adequately.
     */
    void initState(AnnotatorState aModelObject);
}
