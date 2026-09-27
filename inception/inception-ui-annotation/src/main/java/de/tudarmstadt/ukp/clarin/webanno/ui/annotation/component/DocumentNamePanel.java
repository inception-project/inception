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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

/**
 * A {@link Panel} which contains a {@link Label} to display document name as concatenations of
 * {@link ExportedProject#getName()} and {@link ExportedSourceDocument#getName()}
 */
public class DocumentNamePanel
    extends Panel
{
    private static final long serialVersionUID = 3584950105138069924L;

    private @SpringBean UserDao userService;

    /**
     * @param aEditable
     *            whether the described editor is editable; controls the read-only badge.
     */
    public DocumentNamePanel(String id, final IModel<AnnotatorState> aModel,
            IModel<Boolean> aEditable)
    {
        super(id, aModel);
        setOutputMarkupId(true);

        queue(new WebMarkupContainer("read-only").add(
                visibleWhen(() -> aModel.map(AnnotatorState::getDocument).isPresent().getObject()
                        && !aEditable.getObject())));
        queue(new Label("user", aModel.map(AnnotatorState::getUser).map(User::getUiName))
                .add(visibleWhenNot(aModel.map(AnnotatorState::getUser)
                        .map(u -> u.getUsername().equals(userService.getCurrentUsername())))));
        var projectName = aModel.map(AnnotatorState::getProject).map(Project::getName);
        queue(new WebMarkupContainer("projectBadge")
                .add(AttributeModifier.replace("title", projectName)));
        queue(new Label("project", projectName));
        queue(new Label("projectId", aModel.map(AnnotatorState::getProject).map(Project::getId))
                .add(visibleWhen(() -> DEVELOPMENT == getApplication().getConfigurationType())));

        var documentName = aModel.map(AnnotatorState::getDocument).map(SourceDocument::getName);
        queue(new WebMarkupContainer("documentBadge") //
                .add(AttributeModifier.replace("title", documentName)) //
                .add(visibleWhen(aModel.map(AnnotatorState::getDocument).isPresent())));
        queue(new Label("document", documentName));
        queue(new Label("documentId",
                aModel.map(AnnotatorState::getDocument).map(SourceDocument::getId)).add(
                        visibleWhen(() -> DEVELOPMENT == getApplication().getConfigurationType())));
    }
}
