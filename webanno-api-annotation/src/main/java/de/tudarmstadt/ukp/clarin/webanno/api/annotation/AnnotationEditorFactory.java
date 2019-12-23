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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation;

import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;

public interface AnnotationEditorFactory
{
    /**
     * @return get the bean name.
     */
    String getBeanName();
    
    String getDisplayName();
    
    AnnotationEditorBase create(String id, IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider);

    /**
     * Configure the state to be compatible with the editor produced by this factory. E.g. set the
     * paging strategy adequately.
     */
    void initState(AnnotatorState aModelObject);
}
