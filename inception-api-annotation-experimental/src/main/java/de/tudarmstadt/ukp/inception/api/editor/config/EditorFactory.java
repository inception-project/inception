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
package de.tudarmstadt.ukp.inception.api.editor.config;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorFactoryImplBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.LineOrientedPagingStrategy;
import de.tudarmstadt.ukp.inception.api.editor.AnnotationEditor;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

@Component("Experimental Editor")
public class EditorFactory extends AnnotationEditorFactoryImplBase
{
    @Override
    public String getBeanName() {
        return "experimental editor";
    }

    @Override
    public String getDisplayName()
    {
        return "experimental editor";
    }

    @Override
    public AnnotationEditorBase create(String aId, IModel<AnnotatorState> aModel, AnnotationActionHandler aActionHandler, CasProvider aCasProvider) {
        return new AnnotationEditor(aId, aModel, aActionHandler, aCasProvider);
    }

    @Override
    public void initState(AnnotatorState aModelObject)
    {
        aModelObject.setPagingStrategy(new LineOrientedPagingStrategy());
    }
}
