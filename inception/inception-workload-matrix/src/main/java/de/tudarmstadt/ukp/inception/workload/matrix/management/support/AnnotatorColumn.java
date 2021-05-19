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
import static de.tudarmstadt.ukp.inception.workload.matrix.management.MatrixWorkloadManagementPage.CSS_CLASS_STATE_TOGGLE;
import static de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixSortKey.annotatorSortKey;
import static org.apache.wicket.ajax.AjaxEventBehavior.onEvent;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.util.Set;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxEventBehavior;
import de.tudarmstadt.ukp.inception.workload.matrix.management.MatrixWorkloadManagementPage;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnCellClickEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.AnnotatorColumnCellOpenContextMenuEvent;

public class AnnotatorColumn
    extends LambdaColumn<DocumentMatrixRow, DocumentMatrixSortKey>
{
    private static final long serialVersionUID = 8324173231787296215L;

    private IModel<Set<String>> selectedUsers;
    private User user;

    public AnnotatorColumn(User aUser, IModel<Set<String>> aSelectedUsers)
    {
        super(Model.of(aUser.getUiName()), annotatorSortKey(aUser.getUsername()),
                row -> row.getAnnotationDocument(aUser.getUsername()));
        user = aUser;
        selectedUsers = aSelectedUsers;
    }

    @Override
    public void populateItem(Item<ICellPopulator<DocumentMatrixRow>> aItem, String aComponentId,
            IModel<DocumentMatrixRow> aRowModel)
    {
        @SuppressWarnings("unchecked")
        IModel<AnnotationDocument> annDocument = (IModel<AnnotationDocument>) getDataModel(
                aRowModel);

        Label stateLabel = new Label(aComponentId, annDocument //
                .map(AnnotationDocumentState::symbol) //
                .orElse(NEW.symbol()));
        stateLabel.setEscapeModelStrings(false);
        stateLabel.add(new AttributeAppender("class", CSS_CLASS_STATE_TOGGLE, " "));
        stateLabel.add(onEvent("click", //
                _target -> stateLabel.send(stateLabel, BUBBLE,
                        new AnnotatorColumnCellClickEvent(_target,
                                aRowModel.map(DocumentMatrixRow::getSourceDocument).getObject(),
                                user))));
        stateLabel.add(new LambdaAjaxEventBehavior("contextmenu", _target -> stateLabel.send(
                stateLabel, BUBBLE,
                new AnnotatorColumnCellOpenContextMenuEvent(_target, stateLabel,
                        aRowModel.map(DocumentMatrixRow::getSourceDocument).getObject(), user,
                        annDocument.map(AnnotationDocument::getState).orElse(NEW).getObject())))
                                .setPreventDefault(true));

        aItem.add(new CssClassNameAppender(aRowModel.map(this::isSelected).orElse(false).getObject()
                ? MatrixWorkloadManagementPage.CSS_CLASS_SELECTED
                : ""));
        aItem.add(stateLabel);
    }

    private boolean isSelected(DocumentMatrixRow aRow)
    {
        return selectedUsers.getObject().contains(getDisplayModel().getObject())
                || aRow.isSelected();
    }
}
