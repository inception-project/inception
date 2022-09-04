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

import java.util.Objects;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class UsernameColumn
    extends AbstractColumn<User, UserTableSortKeys>
{
    private static final long serialVersionUID = 8324173231787296215L;

    public static final String FID_SELECT_USER_COLUMN = "selectUserColumn";

    private MarkupContainer fragmentProvider;

    public UsernameColumn(MarkupContainer aFragmentProvider, IModel<String> aTitle,
            UserTableSortKeys aSortProperty)
    {
        super(aTitle, aSortProperty);
        fragmentProvider = aFragmentProvider;
    }

    @Override
    public void populateItem(Item<ICellPopulator<User>> aItem, String aComponentId,
            IModel<User> aRowModel)
    {
        var fragment = new Fragment(aComponentId, FID_SELECT_USER_COLUMN, fragmentProvider);
        var user = aRowModel.getObject();
        fragment.queue(new Label("uiName", aRowModel.map(User::getUiName)));
        fragment.queue(new Label("name", aRowModel.map(User::getUsername))
                .setVisible(!Objects.equals(user.getUiName(), user.getUsername())));
        aItem.add(fragment);
    }
}
