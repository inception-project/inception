/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.monitoring;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.DataGridView;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * Build dynamic columns for the user's annotation documents status {@link DataGridView}
 *
 * @author Seid Muhie Yimam
 *
 */
public class DocumentColumnMetaData
    extends AbstractColumn<List<String>>
{

    private RepositoryService projectRepositoryService;
    private static final Log LOG = LogFactory.getLog(DocumentColumnMetaData.class);

    private static final long serialVersionUID = 1L;
    private int columnNumber;
    private String color = "";

    private Project project;

    public DocumentColumnMetaData(final TableDataProvider prov, final int colNumber,
            Project aProject, RepositoryService aProjectreRepositoryService)
    {
        super(new AbstractReadOnlyModel<String>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject()
            {
                return prov.getColNames().get(colNumber);

            }
        });
        columnNumber = colNumber;
        project = aProject;
        projectRepositoryService = aProjectreRepositoryService;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<List<String>>> aCellItem,
            final String componentId, final IModel<List<String>> rowModel)
    {

        final String value = getCellValue(rowModel.getObject().get(columnNumber)).trim();
        if (value.equals("")) {
            // it is first time the monitoring page is opened or there is no document in this
            // project
        }
        else if (columnNumber == 0) {
            // it is the user column, do nothing.
        }

        else {

            SourceDocument document = projectRepositoryService.getSourceDocument(
                    value.substring(value.indexOf(":") + 1), project);
            User user = projectRepositoryService.getUser(value.substring(0, value.indexOf(":")));

            if (projectRepositoryService.existsAnnotationDocument(document, user)) {
                if (projectRepositoryService.getAnnotationDocument(document, user).getState()
                        .equals(AnnotationDocumentState.FINISHED)) {
                    color = "green";
                }
                else if (projectRepositoryService.getAnnotationDocument(document, user).getState()
                        .equals(AnnotationDocumentState.INPROGRESS)) {
                    color = "cyan";
                }
                else if (projectRepositoryService.getAnnotationDocument(document, user).getState()
                        .equals(AnnotationDocumentState.IGNOR)) {
                    color = "black";
                }
                // it is in NEW state
                else {
                    color = "red";
                }
            }
            // user didn't even start working on it
            else {
                AnnotationDocument annotationDocument = new AnnotationDocument();
                annotationDocument.setDocument(document);
                annotationDocument.setName(document.getName());
                annotationDocument.setProject(project);
                annotationDocument.setUser(user);
                annotationDocument.setState(AnnotationDocumentState.NEW);
                projectRepositoryService.createAnnotationDocument(annotationDocument);
                color = "red";
            }
        }
        // It is a username column
        if (color.equals("")) {
            aCellItem.add(new Label(componentId, value.substring(value.indexOf(":") + 1)));
        }
        else {
            aCellItem.add(new Label(componentId, "")).add(
                    new SimpleAttributeModifier("style", "color:" + color + ";background-color:"
                            + color));
        }

        aCellItem.add(new AjaxEventBehavior("onclick")
        {
            @Override
            protected void onEvent(AjaxRequestTarget aTarget)
            {
                if (columnNumber == 0) {
                    color = "";
                }
                else {

                    SourceDocument document = projectRepositoryService.getSourceDocument(
                            value.substring(value.indexOf(":") + 1), project);
                    User user = projectRepositoryService.getUser(value.substring(0,
                            value.indexOf(":")));

                    if (projectRepositoryService.existsAnnotationDocument(document, user)) {
                        if (projectRepositoryService.getAnnotationDocument(document, user)
                                .getState().equals(AnnotationDocumentState.FINISHED)) {

                            changeAnnotationDocumentState(
                                    document,
                                    user,
                                    AnnotationDocumentStateTransition.ANNOTATIONFINISHEDTOANNOTATIONINPROGRESS);
                            color = "cyan";
                        }
                        else if (projectRepositoryService.getAnnotationDocument(document, user)
                                .getState().equals(AnnotationDocumentState.INPROGRESS)) {
                            changeAnnotationDocumentState(
                                    document,
                                    user,
                                    AnnotationDocumentStateTransition.ANNOTATIONINPROGRESSTOANNOTATIONFINISHED);

                        }
                        // Change state of document to IGNORE
                        else if (projectRepositoryService.getAnnotationDocument(document, user)
                                .getState().equals(AnnotationDocumentState.NEW)) {
                            changeAnnotationDocumentState(
                                    document,
                                    user,
                                    AnnotationDocumentStateTransition.NEWTOIGNOR);
                            color = "black";
                        }
                        // change state from IGNOR to NEW
                        else {
                            changeAnnotationDocumentState(
                                    document,
                                    user,
                                    AnnotationDocumentStateTransition.IGNORTONEW);
                            color = "red";
                        }
                    }
                    // user didn't even start working on it
                    else {
                        AnnotationDocument annotationDocument = new AnnotationDocument();
                        annotationDocument.setDocument(document);
                        annotationDocument.setName(document.getName());
                        annotationDocument.setProject(project);
                        annotationDocument.setUser(user);
                        annotationDocument.setState(AnnotationDocumentStateTransition
                                .transition(AnnotationDocumentStateTransition.NEWTOANNOTATIONINPROGRESS));
                        projectRepositoryService.createAnnotationDocument(annotationDocument);

                        try {
                            changeSourceDocumentState(project, document);
                        }
                        catch (IOException e) {
                            LOG.info("Unable to change state of the document");
                        }

                        color = "red";
                    }
                }
                // It is a username column
                if (color.equals("")) {
                    // do nothing
                }
                else {
                    aCellItem.add(new SimpleAttributeModifier("style", "color:" + color
                            + ";background-color:" + color));
                }
                aTarget.add(aCellItem.setOutputMarkupId(true));
            }
        });
    }

    /**
     * Helper method to get the cell value for the user-annotation document status as
     * <b>username:documentName</b>
     *
     * @param aValue
     * @return
     */
    private String getCellValue(String aValue)
    {
        // It is the user column, return user name
        if (aValue.startsWith(MonitoringPage.DOCUMENT)) {
            return aValue.substring(aValue.indexOf(MonitoringPage.DOCUMENT));
        }
        // Initialization of the appliaction, no project selected
        else if (project.getId() == 0) {
            return "";
        }
        // It is document column, get the status from the database
        else {

            String username = aValue.substring(0, aValue.indexOf(MonitoringPage.DOCUMENT) - 1);
            String documentName = aValue.substring(aValue.indexOf(MonitoringPage.DOCUMENT)
                    + MonitoringPage.DOCUMENT.length());
            return username + ":" + documentName;
        }
    }

    private void changeSourceDocumentState(Project aProject, SourceDocument aSourceDocument)
        throws IOException
    {

        boolean allAnnotationDocumentStateFinished = true;
        for (AnnotationDocument annotationDocument : projectRepositoryService
                .listAnnotationDocument(aProject, aSourceDocument)) {
            if (!annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                allAnnotationDocumentStateFinished = false;
                break;
            }
        }
        if (allAnnotationDocumentStateFinished) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = projectRepositoryService.getUser(username);

            aSourceDocument
                    .setState(SourceDocumentStateTransition
                            .transition(SourceDocumentStateTransition.ANNOTATIONINPROGRESSTOANNOTATIONFINISHED));
            projectRepositoryService.createSourceDocument(aSourceDocument, user);
        }
        else {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = projectRepositoryService.getUser(username);

            aSourceDocument
                    .setState(SourceDocumentStateTransition
                            .transition(SourceDocumentStateTransition.ANNOTATIONFINISHEDTOANNOTATIONINPROGRESS));
            projectRepositoryService.createSourceDocument(aSourceDocument, user);
        }
    }

    private void changeAnnotationDocumentState(SourceDocument aSourceDocument, User aUser,
            AnnotationDocumentStateTransition aAnnotationDocumentStateTransition)
    {

        AnnotationDocument annotationDocument = projectRepositoryService.getAnnotationDocument(
                aSourceDocument, aUser);
        annotationDocument.setState(AnnotationDocumentStateTransition
                .transition(aAnnotationDocumentStateTransition));
        projectRepositoryService.createAnnotationDocument(annotationDocument);
        try {
            changeSourceDocumentState(project, aSourceDocument);
        }
        catch (IOException e) {
            LOG.info("Unable to change state of the document");
        }

    }
}