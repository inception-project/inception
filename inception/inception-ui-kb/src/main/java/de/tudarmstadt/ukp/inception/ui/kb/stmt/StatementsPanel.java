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
package de.tudarmstadt.ukp.inception.ui.kb.stmt;

import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.BootstrapRadioGroup;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.EnumRadioChoiceRenderer;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormSubmittingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.ui.kb.WriteProtectionBehavior;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxStatementGroupChangedEvent;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.model.StatementGroupBean;

public class StatementsPanel
    extends Panel
{
    private static final long serialVersionUID = -6655528906388195399L;
    private static final Logger LOG = LoggerFactory.getLogger(StatementsPanel.class);

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KnowledgeBase> kbModel;
    private IModel<? extends KBObject> instance;
    private IModel<StatementDetailPreference> detailPreference;
    private WebMarkupContainer statementGroupListWrapper;
    private IModel<Comparator<StatementGroupBean>> statementGroupComparator;

    private IModel<List<StatementGroupBean>> statementGroups;

    /**
     * {@code StatementsPanel} creator.
     * 
     * @param aId
     *            Wicket component ID
     * @param aKbModel
     *            the knowledge base
     * @param aInstance
     *            the instance to which the statements displayed in the panel belong
     * @param aDetailPreference
     *            if {@code null}, the statement detail preference can be changed in the UI; if
     *            {@code !null} the statement detail preference is fixed to the given value and
     *            can't be changed in the UI
     */
    public StatementsPanel(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<? extends KBObject> aInstance, StatementDetailPreference aDetailPreference)
    {
        super(aId, aInstance);

        setOutputMarkupPlaceholderTag(true);

        kbModel = aKbModel;
        instance = aInstance;

        // default ordering for statement groups: lexical ordering by UI label
        statementGroupComparator = LoadableDetachableModel
                .of(() -> comparing(sgb -> sgb.getProperty().getUiLabel()));

        setUpDetailPreference(aDetailPreference);

        // We must use a LambdaModel here to delay the fetching of the beans until rendering such
        // that setting the group comparator actually has an effect. If we use a static model here,
        // the default group comparator (above) will always be used.
        statementGroups = LoadableDetachableModel.of(this::getStatementGroupBeans);

        RefreshingView<StatementGroupBean> groupList = new RefreshingView<StatementGroupBean>(
                "statementGroupListView")
        {
            private static final long serialVersionUID = 5811425707843441458L;

            @Override
            protected Iterator<IModel<StatementGroupBean>> getItemModels()
            {
                return new ModelIteratorAdapter<StatementGroupBean>(statementGroups.getObject())
                {
                    @Override
                    protected IModel<StatementGroupBean> model(StatementGroupBean object)
                    {
                        return Model.of(object);
                    }
                };
            }

            @Override
            protected void populateItem(Item<StatementGroupBean> aItem)
            {
                var groupModel = new CompoundPropertyModel<>(aItem.getModel());
                StatementGroupPanel panel = new StatementGroupPanel("statementGroup", groupModel);
                aItem.add(panel);
            }
        };

        // wrap the RefreshingView in a WMC, otherwise we can't redraw it with AJAX (see
        // https://cwiki.apache.org/confluence/display/WICKET/How+to+repaint+a+ListView+via+Ajax)
        statementGroupListWrapper = new WebMarkupContainer("statementGroupListWrapper");
        statementGroupListWrapper.setOutputMarkupId(true);
        statementGroupListWrapper.add(groupList);
        add(statementGroupListWrapper);

        add(new Label("noStatementsNotice", new ResourceModel("noStatementsNotice"))
        {
            private static final long serialVersionUID = 2252854898212441711L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();

                setVisible(statementGroups.getObject().isEmpty());
            }
        });

        LambdaAjaxLink addLink = new LambdaAjaxLink("add", this::actionAdd);
        addLink.add(new Label("label", new ResourceModel("statement.add")));
        addLink.add(new WriteProtectionBehavior(kbModel));
        add(addLink);
    }

    @OnEvent
    public void actionStatementGroupChanged(AjaxStatementGroupChangedEvent event)
    {
        // event is irrelevant if it is concerned with a different knowledge base instance
        boolean isEventForThisStatementsPanel = instance.getObject()
                .equals(event.getBean().getInstance());
        if (!isEventForThisStatementsPanel) {
            return;
        }

        // if the statement group should be deleted, find and remove the matching bean from the list
        // of statement groups
        if (event.isDeleted()) {
            statementGroups.getObject().removeIf(sgb -> sgb.equals(event.getBean()));
        }
        event.getTarget().add(this);
    }

    private void setUpDetailPreference(StatementDetailPreference aDetailPreference)
    {
        StatementDetailPreference defaultPreference = StatementDetailPreference.BASIC;

        boolean isDetailPreferenceUserDefinable = aDetailPreference == null;
        detailPreference = Model
                .of(isDetailPreferenceUserDefinable ? defaultPreference : aDetailPreference);

        // the form for setting the detail preference (and its radio group) is only shown if the
        // detail preference is user-definable
        Form<StatementDetailPreference> form = new Form<StatementDetailPreference>(
                "detailPreferenceForm");
        form.add(LambdaBehavior
                .onConfigure(_this -> _this.setVisible(isDetailPreferenceUserDefinable)));
        add(form);

        // radio choice for statement detail preference
        BootstrapRadioGroup<StatementDetailPreference> choice = new BootstrapRadioGroup<>(
                "detailPreferenceChoice", Arrays.asList(StatementDetailPreference.values()));
        choice.setModel(detailPreference);
        choice.setChoiceRenderer(new EnumRadioChoiceRenderer<>(Buttons.Type.Default, this));
        choice.add(new LambdaAjaxFormSubmittingBehavior("change",
                this::actionStatementDetailPreferencesChanged));
        form.add(choice);
    }

    /**
     * Reload the statement group model if the detail preferences change.
     */
    private void actionStatementDetailPreferencesChanged(AjaxRequestTarget target)
    {
        statementGroups.setObject(getStatementGroupBeans());
        target.add(this);
    }

    /**
     * Adds an empty statement group to the statement group list.
     */
    private void actionAdd(AjaxRequestTarget target)
    {
        StatementGroupBean proto = new StatementGroupBean();
        proto.setInstance(instance.getObject().toKBHandle());
        proto.setKb(kbModel.getObject());
        proto.setProperty(new KBProperty());
        proto.setStatements(new ArrayList<>());
        proto.setDetailPreference(detailPreference.getObject());
        statementGroups.getObject().add(proto);

        target.add(this);
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setVisible(
                instance.getObject() != null && isNotEmpty(instance.getObject().getIdentifier()));
    }

    public void setStatementGroupComparator(Comparator<StatementGroupBean> aComparator)
    {
        statementGroupComparator.setObject(aComparator);
    }

    private List<StatementGroupBean> getStatementGroupBeans()
    {
        // obtain list of statements according to the detail preferences
        StatementDetailPreference prefs = detailPreference.getObject();
        List<KBStatement> statements = new ArrayList<>();
        try {
            statements = kbService.listStatements(kbModel.getObject(),
                    instance.getObject().toKBHandle(), prefs == StatementDetailPreference.ALL);
        }
        catch (QueryEvaluationException e) {
            error("Unable to list statements: " + e.getLocalizedMessage());
            LOG.error("Unable to list statements.", e);
        }

        if (prefs == StatementDetailPreference.BASIC) {
            statements.removeIf((s) -> s.isInferred());
        }

        // group statements by property
        Map<KBProperty, List<KBStatement>> groupedStatements = statements.stream()
                .collect(Collectors.groupingBy(KBStatement::getProperty));

        // for each property and associated statements, create one StatementGroupBean
        List<StatementGroupBean> beans = groupedStatements.entrySet().stream().map(entry -> {
            StatementGroupBean bean = new StatementGroupBean();
            bean.setKb(kbModel.getObject());
            bean.setInstance(instance.getObject().toKBHandle());
            bean.setProperty(entry.getKey());
            bean.setStatements(entry.getValue());
            return bean;
        }).collect(Collectors.toList());

        Collections.sort(beans, statementGroupComparator.getObject());
        return beans;
    }
}
