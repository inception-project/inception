/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;

/**
 * A {@link Panel} which contains a {@link Label} to display document name as concatenations of
 * {@link ExportedProject#getName()} and {@link ExportedSourceDocument#getName()}
 */
public class DocumentNamePanel
    extends Panel
{
    private static final long serialVersionUID = 3584950105138069924L;

    public DocumentNamePanel(String id, final IModel<AnnotatorState> aModel)
    {
        super(id, aModel);
        setOutputMarkupId(true);
        add(new Label("doumentName", LambdaModel.of(this::getLabel)).setOutputMarkupId(true));
    }
    
    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    private String getLabel()
    {
        StringBuilder sb = new StringBuilder();

        AnnotatorState state = getModelObject();

        if (state.getUser() != null) {
            sb.append(state.getUser().getUsername());
            sb.append(": ");
        }
        
        if (state.getProject() != null) {
            sb.append(state.getProject().getName());
        }

        sb.append("/");

        if (state.getDocument() != null) {
            sb.append(state.getDocument().getName());
        }

        if (RuntimeConfigurationType.DEVELOPMENT.equals(getApplication().getConfigurationType())) {
            sb.append(" (");
            if (state.getProject() != null) {
                sb.append(state.getProject().getId());
            }
            sb.append("/");
            if (state.getDocument() != null) {
                sb.append(state.getDocument().getId());
            }
            sb.append(")");
        }

        return sb.toString();
    }
}
