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
package de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.support.DocumentMatrixSortKey.annotatorSortKey;
import static org.apache.wicket.ajax.AjaxEventBehavior.onEvent;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.util.Set;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
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
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.event.AnnotatorColumnCellClickEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.event.AnnotatorColumnCellOpenContextMenuEvent;

public class AnnotatorColumn
    extends LambdaColumn<DocumentMatrixRow, DocumentMatrixSortKey>
{
    private static final long serialVersionUID = 8324173231787296215L;

    private IModel<Set<String>> selectedUsers;

    public AnnotatorColumn(String aUsername, IModel<Set<String>> aSelectedUsers)
    {
        super(Model.of(aUsername), annotatorSortKey(aUsername),
                row -> row.getAnnotationDocument(aUsername));
        selectedUsers = aSelectedUsers;
    }

    @Override
    public void populateItem(Item<ICellPopulator<DocumentMatrixRow>> aItem, String aComponentId,
            IModel<DocumentMatrixRow> aRowModel)
    {
        @SuppressWarnings("unchecked")
        IModel<AnnotationDocument> annDocument = (IModel<AnnotationDocument>) getDataModel(
                aRowModel);

        DocumentMatrixRow row = aRowModel.getObject();

        AnnotationDocumentState state = annDocument.map(AnnotationDocument::getState).orElse(NEW)
                .getObject();
        Label stateLabel = new Label(aComponentId, stateSymbol(state));
        stateLabel.setEscapeModelStrings(false);
        stateLabel.add(new AttributeAppender("style", "cursor: pointer", ";"));
        stateLabel.add(onEvent("click", //
                _target -> stateLabel.send(stateLabel, BUBBLE, new AnnotatorColumnCellClickEvent(
                        _target, row.getSourceDocument(), getDisplayModel().getObject()))));
        stateLabel.add(new AjaxEventBehavior("contextmenu")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes aAttributes)
            {
                super.updateAjaxAttributes(aAttributes);
                aAttributes.setPreventDefault(true);
            };

            @Override
            protected void onEvent(AjaxRequestTarget aTarget)
            {
                stateLabel.send(stateLabel, BUBBLE,
                        new AnnotatorColumnCellOpenContextMenuEvent(aTarget, stateLabel,
                                row.getSourceDocument(), getDisplayModel().getObject(), state));
            };
        });

        aItem.add(new CssClassNameAppender(isSelected(row) ? "s" : ""));
        aItem.add(stateLabel);
    }

    private boolean isSelected(DocumentMatrixRow aRow)
    {
        return selectedUsers.getObject().contains(getDisplayModel().getObject())
                || aRow.isSelected();
    }

    private String stateSymbol(AnnotationDocumentState aDocState)
    {
        switch (aDocState) {
        case NEW:
            return "<i class=\"far fa-circle\"></i>";
        case IN_PROGRESS:
            return "<i class=\"far fa-play-circle\"></i>";
        case FINISHED:
            return "<i class=\"far fa-check-circle\"></i>";
        case IGNORE:
            return "<i class=\"fas fa-lock\"></i>";
        }

        return "";
    }
}
