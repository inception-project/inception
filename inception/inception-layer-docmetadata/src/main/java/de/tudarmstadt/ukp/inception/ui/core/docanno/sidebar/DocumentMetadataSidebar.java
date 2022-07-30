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
package de.tudarmstadt.ukp.inception.ui.core.docanno.sidebar;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public class DocumentMetadataSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = 2085197932148384096L;

    public DocumentMetadataSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aAnnotationPage);

        IModel<Project> project = LoadableDetachableModel.of(() -> aModel.getObject().getProject());
        IModel<SourceDocument> sourceDocument = LoadableDetachableModel
                .of(() -> aModel.getObject().getDocument());
        IModel<String> username = LoadableDetachableModel
                .of(() -> aModel.getObject().getUser().getUsername());

        add(new DocumentMetadataAnnotationSelectionPanel("annotations", project, sourceDocument,
                username, aCasProvider, aAnnotationPage, aActionHandler, getModelObject()));
    }
}
