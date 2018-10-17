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
package de.tudarmstadt.ukp.inception.ui.kb.project.wizard;

import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class KnowledgeBaseCreationDialog extends ModalWindow {

    private static final long serialVersionUID = 7446798125344480445L;
    
    private IModel<Project> projectModel;

    public KnowledgeBaseCreationDialog(String aId, IModel<Project> aProjectModel) {
        super(aId, aProjectModel);
        
        setOutputMarkupPlaceholderTag(true);
        
        projectModel = aProjectModel;

        setInitialWidth(600);
        setInitialHeight(450);
        setResizable(false);
        setWidthUnit("px");
        setHeightUnit("px");
        setTitle(new StringResourceModel("kb.wizard.title", this));
        setCssClassName("w_blue w_flex");
    }
    
    @Override
    public void show(IPartialPageRequestHandler target) {
        setContent(new KnowledgeBaseCreationWizard(getContentId(), projectModel));
        super.show(target);
    }

}
