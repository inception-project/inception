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
package de.tudarmstadt.ukp.inception.workload.matrix.management.support;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CLICK_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CONTEXTMENU_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.workload.matrix.management.MatrixWorkloadManagementPage.CSS_CLASS_STATE_TOGGLE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxEventBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.SymbolLabel;
import de.tudarmstadt.ukp.inception.workload.matrix.management.MatrixWorkloadManagementPage;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnCellClickEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnCellOpenContextMenuEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnCellShowAnnotatorCommentEvent;

public class DocumentMatrixAnnotatorStateCell
    extends Panel
{
    private static final long serialVersionUID = 8669237603186001024L;

    private final User user;
    private final IModel<Set<String>> selectedUsers;
    private final IModel<DocumentMatrixRow> rowModel;
    private final IModel<AnnotationDocument> columnModel;

    public DocumentMatrixAnnotatorStateCell(String aId, IModel<DocumentMatrixRow> aRowModel,
            IModel<AnnotationDocument> aColumnModel, IModel<Set<String>> aSelectedUsers, User aUser)
    {
        super(aId);
        setOutputMarkupId(true);

        user = aUser;
        selectedUsers = aSelectedUsers;
        rowModel = aRowModel;
        columnModel = aColumnModel;

        var state = new WebMarkupContainer("state");

        var stateLabel = new SymbolLabel("stateSymbol",
                columnModel.map(AnnotationDocument::getState).orElse(NEW));

        state.add(new LambdaAjaxEventBehavior(CLICK_EVENT,
                _t -> actionClickCell(rowModel, stateLabel, _t)).setPreventDefault(true));
        state.add(new LambdaAjaxEventBehavior(CONTEXTMENU_EVENT,
                _t -> actionContextMenu(rowModel, columnModel, _t)).setPreventDefault(true));
        state.add(new AttributeAppender("class", CSS_CLASS_STATE_TOGGLE, " "));
        state.add(new CssClassNameAppender(rowModel.map(this::isSelected).orElse(false).getObject()
                ? MatrixWorkloadManagementPage.CSS_CLASS_SELECTED
                : ""));

        var annDoc = columnModel.getObject();
        var showComment = new LambdaAjaxLink("showComment",
                _t -> actionShowAnnotatorComment(rowModel, columnModel, _t));
        showComment
                .add(visibleWhen(() -> annDoc != null && isNotBlank(annDoc.getAnnotatorComment())));

        queue(state, stateLabel, showComment);
    }

    private boolean isSelected(DocumentMatrixRow aRow)
    {
        return selectedUsers.getObject().contains(user.getUiName()) || aRow.isSelected();
    }

    private void actionContextMenu(IModel<DocumentMatrixRow> aRowModel,
            IModel<AnnotationDocument> annDocument, AjaxRequestTarget _target)
    {
        send(this, BUBBLE,
                new AnnotatorColumnCellOpenContextMenuEvent(_target, this,
                        aRowModel.map(DocumentMatrixRow::getSourceDocument).getObject(), user,
                        annDocument.map(AnnotationDocument::getState).orElse(NEW).getObject()));
    }

    private void actionShowAnnotatorComment(IModel<DocumentMatrixRow> aRowModel,
            IModel<AnnotationDocument> annDocument, AjaxRequestTarget _target)
    {
        send(this, BUBBLE,
                new AnnotatorColumnCellShowAnnotatorCommentEvent(_target, this,
                        aRowModel.map(DocumentMatrixRow::getSourceDocument).getObject(), user,
                        annDocument.map(AnnotationDocument::getState).orElse(NEW).getObject()));
    }

    private void actionClickCell(IModel<DocumentMatrixRow> aRowModel, Label stateLabel,
            AjaxRequestTarget _target)
    {
        stateLabel.send(stateLabel, BUBBLE, new AnnotatorColumnCellClickEvent(_target,
                aRowModel.map(DocumentMatrixRow::getSourceDocument).getObject(), user));
    }
}
