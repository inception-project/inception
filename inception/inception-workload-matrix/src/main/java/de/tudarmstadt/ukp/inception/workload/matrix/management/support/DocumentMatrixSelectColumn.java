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

import static org.apache.wicket.event.Broadcast.BUBBLE;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.core.request.handler.IPageRequestHandler;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;

import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.workload.matrix.management.event.DocumentRowSelectionChangedEvent;

public class DocumentMatrixSelectColumn
    extends LambdaColumn<DocumentMatrixRow, DocumentMatrixSortKey>
{
    private static final long serialVersionUID = 8324173231787296215L;

    private boolean visible;

    public DocumentMatrixSelectColumn()
    {
        super(Model.of(""), row -> row.getSourceDocument().getName());
    }

    public void setVisible(boolean aVisible)
    {
        visible = aVisible;
    }

    public boolean isVisible()
    {
        return visible;
    }

    @Override
    public void populateItem(Item<ICellPopulator<DocumentMatrixRow>> aItem, String aComponentId,
            IModel<DocumentMatrixRow> aRowModel)
    {
        MarkupContainer page = IPageRequestHandler
                .getPage(RequestCycle.get().getActiveRequestHandler());
        Fragment frag = new Fragment(aComponentId, "select-column", page, aRowModel);
        IModel<Boolean> selectedModel = CompoundPropertyModel.of(aRowModel).bind("selected");
        CheckBox checkbox = new CheckBox("selected", selectedModel);
        checkbox.add(new LambdaAjaxFormComponentUpdatingBehavior("change", _target -> checkbox
                .send(checkbox, BUBBLE, new DocumentRowSelectionChangedEvent(_target, aRowModel))));
        frag.add(checkbox);
        frag.setVisible(visible);
        aItem.add(frag);
    }
}
