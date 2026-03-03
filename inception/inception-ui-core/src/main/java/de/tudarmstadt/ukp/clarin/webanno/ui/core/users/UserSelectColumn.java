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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.users;

import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.util.stream.StreamSupport;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class UserSelectColumn
    extends LambdaColumn<UserTableRow, UserTableSortKeys>
{
    private static final long serialVersionUID = 8324173231787296215L;

    private MarkupContainer fragmentContainer;
    private boolean visible;
    private UserTableDataProvider dataProvider;

    public UserSelectColumn(MarkupContainer aFragmentContainer, UserTableDataProvider aDataProvider)
    {
        super(null, row -> row.getUser().getUsername());
        fragmentContainer = aFragmentContainer;
        dataProvider = aDataProvider;
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
    public Component getHeader(String aComponentId)
    {
        if (!visible) {
            // Simply return the empty header (because we do not set a header string) from super
            return super.getHeader(aComponentId);
        }

        Fragment frag = new Fragment(aComponentId, "select-column-header", fragmentContainer);

        var anyRowSelected = LoadableDetachableModel
                .of(() -> StreamSupport.stream(dataProvider.spliterator(), false) //
                        .allMatch(UserTableRow::isSelected));

        var checkbox = new CheckBox("allSelected", anyRowSelected);
        checkbox.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                _target -> checkbox.send(checkbox, BUBBLE,
                        new UserTableToggleSelectAllEvent(_target, anyRowSelected.getObject()))));
        frag.add(checkbox);
        frag.setVisible(visible);

        return frag;
    }

    @Override
    public void populateItem(Item<ICellPopulator<UserTableRow>> aItem, String aComponentId,
            IModel<UserTableRow> aRowModel)
    {
        var frag = new Fragment(aComponentId, "select-column", fragmentContainer, aRowModel);
        IModel<Boolean> selectedModel = CompoundPropertyModel.of(aRowModel).bind("selected");
        var checkbox = new CheckBox("selected", selectedModel);
        checkbox.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                _target -> checkbox.send(checkbox, BUBBLE,
                        new UserTableRowSelectionChangedEvent(_target, aRowModel))));
        frag.add(checkbox);
        frag.setVisible(visible);
        aItem.add(frag);
    }
}
