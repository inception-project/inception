/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.kb;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxInstanceSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxNewInstanceEvent;

public class InstanceListPanel extends Panel {
    private static final long serialVersionUID = -2431507947235476294L;
    private static final Logger LOG = LoggerFactory.getLogger(InstanceListPanel.class);

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KnowledgeBase> kbModel;
    private IModel<KBHandle> conceptModel;
    private IModel<Boolean> showAll;

    public InstanceListPanel(String aId, IModel<KnowledgeBase> aKbModel, IModel<KBHandle> aConcept,
            IModel<KBHandle> aInstance) {
        super(aId, aInstance);

        setOutputMarkupId(true);

        kbModel = aKbModel;
        conceptModel = aConcept;
        showAll = Model.of(Boolean.FALSE);
        
        LambdaModel<List<KBHandle>> instancesModel = LambdaModel.of(this::getInstances);

        OverviewListChoice<KBHandle> overviewList = new OverviewListChoice<KBHandle>("instances") {
            private static final long serialVersionUID = -122960232588575731L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(!instancesModel.getObject().isEmpty());
            }
        };
        overviewList.setChoiceRenderer(new ChoiceRenderer<>("uiLabel"));
        overviewList.setModel(aInstance);
        overviewList.setChoices(instancesModel);
        overviewList
                .add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                    target -> send(getPage(), Broadcast.BREADTH,
                                new AjaxInstanceSelectionEvent(target, aInstance.getObject()))));
        add(overviewList);

        add(new Label("count", LambdaModel.of(() -> overviewList.getChoices().size())));

        LambdaAjaxLink addLink = new LambdaAjaxLink("add",
            target -> send(getPage(), Broadcast.BREADTH, new AjaxNewInstanceEvent(target)));
        addLink.add(new Label("label", new ResourceModel("instance.add")));
        addLink.add(new WriteProtectionBehavior(kbModel));
        add(addLink);

        add(new Label("noInstancesNotice", new ResourceModel("instance.nonedefined")) {
            private static final long serialVersionUID = 2252854898212441711L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(instancesModel.getObject().isEmpty());
            }
        });

        CheckBox showAllCheckBox = new CheckBox("showAllInstances", showAll);
        showAllCheckBox
                .add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> t.add(this)));
        add(showAllCheckBox);
    }
    
    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        setVisible(conceptModel.getObject() != null && 
                isNotEmpty(conceptModel.getObject().getIdentifier()));
    }

    private List<KBHandle> getInstances() {
        if (conceptModel.getObject() != null) {
            try {
                return kbService.listInstances(kbModel.getObject(),
                        conceptModel.getObject().getIdentifier(), showAll.getObject());
            }
            catch (QueryEvaluationException e) {
                error("Unable to list instances: " + e.getLocalizedMessage());
                LOG.error("Unable to list instances.",e);
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

}
