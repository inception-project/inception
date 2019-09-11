/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.export;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;

public class ExportDocumentActionBarItem
    extends Panel
{
    private static final long serialVersionUID = 4139817495914347777L;

    private @SpringBean ProjectService projectService;
    
    private final AnnotationPageBase page;
    private final ExportDocumentDialog exportDialog;

    public ExportDocumentActionBarItem(String aId, AnnotationPageBase aPage)
    {
        super(aId);
        
        setOutputMarkupPlaceholderTag(true);
        
        page = aPage;

        add(exportDialog = new ExportDocumentDialog("exportDialog", page.getModel()));
        add(new LambdaAjaxLink("showExportDialog", exportDialog::show));
    }
    
    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        AnnotatorState state = page.getModelObject();
        setVisible(state.getProject() != null
                && (projectService.isManager(state.getProject(), state.getUser())
                        || !state.getProject().isDisableExport()));
    }
}
