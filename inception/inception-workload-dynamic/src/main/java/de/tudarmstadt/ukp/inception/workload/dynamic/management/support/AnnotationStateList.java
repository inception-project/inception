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
package de.tudarmstadt.ukp.inception.workload.dynamic.management.support;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.inception.workload.dynamic.management.DynamicWorkloadManagementPage.CSS_CLASS_STATE_TOGGLE;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.time.Duration;
import java.util.List;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxEventBehavior;
import de.tudarmstadt.ukp.inception.workload.dynamic.management.support.event.AnnotatorColumnCellClickEvent;
import de.tudarmstadt.ukp.inception.workload.dynamic.management.support.event.AnnotatorStateOpenContextMenuEvent;

public class AnnotationStateList
    extends Panel
{
    private static final long serialVersionUID = -8717096450102813443L;

    private @SpringBean UserDao userRepository;

    public AnnotationStateList(String aId, IModel<List<AnnotationDocument>> aModel,
            Duration aAbandonationTimeout)
    {
        super(aId, aModel);

        ListView<AnnotationDocument> annotationStates = new ListView<>("state", aModel)
        {
            private static final long serialVersionUID = -8178383690721509334L;

            @Override
            protected void populateItem(ListItem<AnnotationDocument> aItem)
            {
                AnnotationDocument row = aItem.getModelObject();
                User user = userRepository.get(aItem.getModelObject().getUser());

                IModel<String> labelModel = aItem.getModel() //
                        .map(AnnotationDocumentState::symbol) //
                        .orElse(NEW.symbol());
                Label stateLabel = new Label("stateSymbol");
                Duration idleTime = row.getTimestamp() != null
                        ? between(row.getTimestamp().toInstant(), now())
                        : null;
                if (idleTime != null && !aAbandonationTimeout.isZero()
                        && !aAbandonationTimeout.isNegative()
                        && idleTime.compareTo(aAbandonationTimeout) > 0) {
                    labelModel = labelModel
                            .map(_label -> "<i class=\"fas fa-user-clock\"></i> " + _label);
                    aItem.add(new AttributeAppender("class", "badge-warning", " "));
                }
                else {
                    aItem.add(new AttributeAppender("class", "badge-secondary", " "));
                    aItem.add(AjaxEventBehavior.onEvent("click", _target -> stateLabel.send(
                            stateLabel, BUBBLE,
                            new AnnotatorColumnCellClickEvent(_target, row.getDocument(), user))));
                    aItem.add(new AttributeAppender("class", CSS_CLASS_STATE_TOGGLE, " "));
                }
                stateLabel.setDefaultModel(labelModel);
                stateLabel.setEscapeModelStrings(false);
                aItem.add(stateLabel);

                aItem.add(new Label("annotatorName", user.getUiName()));
                aItem.add(new LambdaAjaxEventBehavior("contextmenu",
                        _target -> stateLabel.send(aItem, BUBBLE,
                                new AnnotatorStateOpenContextMenuEvent(_target, aItem,
                                        row.getDocument(), user, row.getState())))
                                                .setPreventDefault(true));
            }
        };

        add(annotationStates);
    }
}
