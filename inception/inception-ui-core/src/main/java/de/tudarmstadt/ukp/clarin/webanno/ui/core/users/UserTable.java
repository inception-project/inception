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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.HtmlElementEvents.INPUT_EVENT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.users.UserTableSortKeys.CREATED;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.users.UserTableSortKeys.LAST_LOGIN;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.users.UserTableSortKeys.STATE;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.users.UserTableSortKeys.UI_NAME;
import static java.time.Duration.ofMillis;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.NonEscapingLambdaColumn;

public class UserTable
    extends Panel
{
    private static final long serialVersionUID = 3993790906387166039L;

    private static final String CID_DATA_TABLE = "dataTable";
    private static final String CID_STATE_FILTERS = "stateFilters";
    private static final String CID_NAME_FILTER = "nameFilter";

    private UserTableDataProvider dataProvider;
    private DataTable<User, UserTableSortKeys> table;
    private TextField<String> nameFilter;
    private IModel<User> selectedUser;

    public UserTable(String aId, IModel<User> aSelectedUser, IModel<List<User>> aModel)
    {
        super(aId, aModel);
        setOutputMarkupId(true);

        selectedUser = aSelectedUser;
        dataProvider = new UserTableDataProvider(aModel);

        var columns = new ArrayList<IColumn<User, UserTableSortKeys>>();
        columns.add(new NonEscapingLambdaColumn<>(new ResourceModel("UserState"), STATE,
                $ -> UserState.of($).symbol()));
        columns.add(new UsernameColumn(this, new ResourceModel("UserName"), UI_NAME));
        columns.add(new LambdaColumn<>(new ResourceModel("UserCreated"), CREATED,
                $ -> renderDate($.getCreated())));
        columns.add(new LambdaColumn<>(new ResourceModel("UserLastLogin"), LAST_LOGIN,
                $ -> renderDate($.getLastLogin())));

        table = new DataTable<>(CID_DATA_TABLE, columns, dataProvider, 25)
        {
            private static final long serialVersionUID = -251038696974390480L;

            protected Item<User> newRowItem(String id, int index, IModel<User> model)
            {
                var item = super.newRowItem(id, index, model);

                if (selectedUser.map(u -> Objects.equals(u, model.getObject())).orElse(false)
                        .getObject()) {
                    item.add(new CssClassNameAppender("table-active"));
                }

                item.add(AttributeModifier.replace("role", "button"));
                item.add(AjaxEventBehavior.onEvent("click",
                        _target -> actionSelectUser(_target, item, model.getObject())));

                return item;
            };
        };
        table.setOutputMarkupId(true);
        var pagingNavigator = new AjaxNavigationToolbar(table);
        table.addTopToolbar(pagingNavigator);
        table.addTopToolbar(new AjaxFallbackHeadersToolbar<>(table, dataProvider));
        queue(table);

        nameFilter = new TextField<>(CID_NAME_FILTER,
                PropertyModel.of(dataProvider.getFilterState(), "username"), String.class);
        nameFilter.setOutputMarkupPlaceholderTag(true);
        nameFilter.add(
                new LambdaAjaxFormComponentUpdatingBehavior(INPUT_EVENT, this::actionApplyFilter)
                        .withDebounce(ofMillis(200)));
        queue(nameFilter);

        queue(new UserStateFilterPanel(CID_STATE_FILTERS,
                () -> dataProvider.getFilterState().getStates()));
    }

    @SuppressWarnings("unchecked")
    public IModel<List<SourceDocument>> getModel()
    {
        return (IModel<List<SourceDocument>>) getDefaultModel();
    }

    private String renderDate(Date aDate)
    {
        if (aDate == null) {
            return "";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(aDate);
    }

    private void actionApplyFilter(AjaxRequestTarget aTarget)
    {
        aTarget.add(table);
    }

    private void actionSelectUser(AjaxRequestTarget aTarget, Component aItem, User aUser)
    {
        aItem.send(aItem, BUBBLE, new SelectUserEvent(aTarget, aUser));
    }

    public UserTableDataProvider getDataProvider()
    {
        return dataProvider;
    }

    @OnEvent
    public void onUserStateFilterChanged(UserStateFilterChangedEvent aEvent)
    {
        aEvent.getTarget().add(this);
    }

    public DataTable<User, UserTableSortKeys> getInnerTable()
    {
        return table;
    }
}
