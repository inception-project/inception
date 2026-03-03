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

import static de.tudarmstadt.ukp.clarin.webanno.ui.core.users.UserTableSortKeys.CREATED;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.users.UserTableSortKeys.LAST_LOGIN;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.users.UserTableSortKeys.STATE;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.users.UserTableSortKeys.UI_NAME;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.INPUT_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.time.Duration.ofMillis;
import static java.util.stream.Collectors.toList;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.SymbolLambdaColumn;

public class UserTable
    extends Panel
{
    private static final long serialVersionUID = 3993790906387166039L;

    private static final String CID_DATA_TABLE = "dataTable";
    private static final String CID_STATE_FILTERS = "stateFilters";
    private static final String CID_NAME_FILTER = "nameFilter";
    private static final String CID_BULK_ACTION_DROPDOWN_BUTTON = "bulkActionDropdownButton";
    private static final String CID_BULK_ACTION_DROPDOWN = "bulkActionDropdown";
    private static final String CID_TOGGLE_BULK_CHANGE = "toggleBulkChange";

    private @SpringBean UserDao userService;

    private UserTableDataProvider dataProvider;
    private DataTable<UserTableRow, UserTableSortKeys> table;
    private TextField<String> nameFilter;
    private IModel<User> selectedUser;

    private LambdaAjaxLink toggleBulkChange;
    private WebMarkupContainer bulkActionDropdown;
    private WebMarkupContainer bulkActionDropdownButton;
    private boolean bulkChangeMode = false;
    private UserSelectColumn selectColumn;

    public UserTable(String aId, IModel<User> aSelectedUser, IModel<List<UserTableRow>> aModel)
    {
        super(aId, aModel);
        setOutputMarkupId(true);

        selectedUser = aSelectedUser;
        dataProvider = new UserTableDataProvider(aModel);

        var columns = new ArrayList<IColumn<UserTableRow, UserTableSortKeys>>();
        selectColumn = new UserSelectColumn(this, dataProvider);
        columns.add(selectColumn);
        columns.add(new SymbolLambdaColumn<>(new ResourceModel("UserState"), STATE,
                $ -> UserState.of($.getUser())));
        columns.add(new UsernameColumn(this, new ResourceModel("UserName"), UI_NAME));
        columns.add(new LambdaColumn<>(new ResourceModel("UserCreated"), CREATED,
                $ -> renderDate($.getUser().getCreated())));
        columns.add(new LambdaColumn<>(new ResourceModel("UserLastLogin"), LAST_LOGIN,
                $ -> renderDate($.getUser().getLastLogin())));

        table = new DataTable<>(CID_DATA_TABLE, columns, dataProvider, 25)
        {
            private static final long serialVersionUID = -251038696974390480L;

            protected Item<UserTableRow> newRowItem(String id, int index,
                    IModel<UserTableRow> model)
            {
                var item = super.newRowItem(id, index, model);

                if (selectedUser.map(u -> Objects.equals(u, model.getObject().getUser()))
                        .orElse(false).getObject()) {
                    item.add(new CssClassNameAppender("table-active"));
                }

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

        toggleBulkChange = new LambdaAjaxLink(CID_TOGGLE_BULK_CHANGE, this::actionToggleBulkChange);
        toggleBulkChange.setOutputMarkupId(true);
        toggleBulkChange.add(new CssClassNameAppender(LoadableDetachableModel
                .of(() -> bulkChangeMode ? "btn-primary active" : "btn-outline-primary")));
        queue(toggleBulkChange);

        bulkActionDropdown = new WebMarkupContainer(CID_BULK_ACTION_DROPDOWN);
        bulkActionDropdown.add(visibleWhen(() -> bulkChangeMode));
        queue(bulkActionDropdown);

        bulkActionDropdownButton = new WebMarkupContainer(CID_BULK_ACTION_DROPDOWN_BUTTON);
        bulkActionDropdownButton.add(visibleWhen(() -> bulkChangeMode));
        queue(bulkActionDropdownButton);

        queue(new LambdaAjaxLink("bulkEnable", this::actionBulkEnableUsers));
        queue(new LambdaAjaxLink("bulkDisable", this::actionBulkDisableUsers));

        queue(new UserStateFilterPanel(CID_STATE_FILTERS,
                () -> dataProvider.getFilterState().getStates()));
    }

    @SuppressWarnings("unchecked")
    public IModel<List<User>> getModel()
    {
        return (IModel<List<User>>) getDefaultModel();
    }

    private void actionToggleBulkChange(AjaxRequestTarget aTarget)
    {
        bulkChangeMode = !bulkChangeMode;
        selectColumn.setVisible(bulkChangeMode);
        dataProvider.refresh();
        aTarget.add(this);
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

    public UserTableDataProvider getDataProvider()
    {
        return dataProvider;
    }

    @OnEvent
    public void onUserStateFilterChanged(UserStateFilterChangedEvent aEvent)
    {
        aEvent.getTarget().add(this);
    }

    public DataTable<UserTableRow, UserTableSortKeys> getInnerTable()
    {
        return table;
    }

    @OnEvent
    public void onUserRowSelectionChangedEvent(UserTableRowSelectionChangedEvent aEvent)
    {
        var selected = getSelectedUsers().size();
        info("Now " + selected + " users are selected.");
        aEvent.getTarget().addChildren(getPage(), IFeedback.class);
        aEvent.getTarget().add(table);
    }

    @OnEvent
    public void onUserTableToggleSelectAllEvent(UserTableToggleSelectAllEvent aEvent)
    {
        int changed = 0;
        int selected = 0;
        boolean targetValue = aEvent.isSelectAll();

        for (var row : dataProvider) {
            if (row.isSelected() != targetValue) {
                changed++;
                row.setSelected(targetValue);
            }

            if (row.isSelected()) {
                selected++;
            }
        }

        info((changed + " users have been " + (targetValue ? "selected" : "unselected")) + ". Now "
                + selected + " users are selected.");
        aEvent.getTarget().addChildren(getPage(), IFeedback.class);
        aEvent.getTarget().add(table);
    }

    private List<User> getSelectedUsers()
    {
        return dataProvider.getModel().getObject().stream() //
                .filter(UserTableRow::isSelected) //
                .map(UserTableRow::getUser) //
                .collect(toList());
    }

    private void actionBulkEnableUsers(AjaxRequestTarget aTarget)
    {
        updateUserEnabled(aTarget, true);
    }

    private void actionBulkDisableUsers(AjaxRequestTarget aTarget)
    {
        updateUserEnabled(aTarget, false);
    }

    private void updateUserEnabled(AjaxRequestTarget aTarget, boolean aEnabled)
    {
        var updated = 0;
        for (var user : getSelectedUsers()) {
            var u = userService.get(user.getUsername());
            if (u != null) {
                u.setEnabled(aEnabled);
                userService.create(u);
                updated++;
            }
        }

        dataProvider.refresh();

        info(updated + " users have been " + (aEnabled ? "enabled" : "disabled") + ".");
        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(table);
    }
}
