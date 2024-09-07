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

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.NEW;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CLICK_EVENT;
import static de.tudarmstadt.ukp.inception.workload.matrix.management.MatrixWorkloadManagementPage.CSS_CLASS_STATE_TOGGLE;
import static de.tudarmstadt.ukp.inception.workload.matrix.management.support.DocumentMatrixSortKey.CURATION_STATE;
import static org.apache.wicket.ajax.AjaxEventBehavior.onEvent;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.CuratorColumnCellClickEvent;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.CuratorColumnCellOpenContextMenuEvent;

public class DocumentMatrixCuratorColumn
    extends LambdaColumn<DocumentMatrixRow, DocumentMatrixSortKey>
{
    private static final long serialVersionUID = 8324173231787296215L;

    public DocumentMatrixCuratorColumn()
    {
        super(Model.of("Curation"), CURATION_STATE, row -> row.getSourceDocument());
    }

    @Override
    public void populateItem(Item<ICellPopulator<DocumentMatrixRow>> aItem, String aComponentId,
            IModel<DocumentMatrixRow> aRowModel)
    {
        @SuppressWarnings("unchecked")
        var srcDocument = (IModel<SourceDocument>) getDataModel(aRowModel);

        var row = aRowModel.getObject();

        var state = srcDocument.map(SourceDocument::getState).orElse(NEW).getObject();

        var stateLabel = new CurationStateSymbolLabel(aComponentId, state);
        stateLabel.add(new AttributeAppender("class", CSS_CLASS_STATE_TOGGLE, " "));
        stateLabel.add(onEvent(CLICK_EVENT, //
                _target -> stateLabel.send(stateLabel, BUBBLE,
                        new CuratorColumnCellClickEvent(_target, row.getSourceDocument()))));
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
                stateLabel.send(stateLabel, BUBBLE, new CuratorColumnCellOpenContextMenuEvent(
                        aTarget, stateLabel, row.getSourceDocument()));
            };
        });

        aItem.add(stateLabel);
    }
}
