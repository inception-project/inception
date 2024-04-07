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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.workload.dynamic.management.DynamicWorkloadManagementPage.CSS_CLASS_STATE_TOGGLE;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.time.Duration;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxEventBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.workload.dynamic.management.support.event.AnnotatorColumnCellClickEvent;
import de.tudarmstadt.ukp.inception.workload.dynamic.management.support.event.AnnotatorColumnCellShowAnnotatorCommentEvent;
import de.tudarmstadt.ukp.inception.workload.dynamic.management.support.event.AnnotatorStateOpenContextMenuEvent;

public class AnnotationStateList
    extends Panel
{
    private static final long serialVersionUID = -8717096450102813443L;

    private @SpringBean UserDao userRepository;

    private final Duration abandonationTimeout;

    public AnnotationStateList(String aId, IModel<List<AnnotationDocument>> aModel,
            Duration aAbandonationTimeout)
    {
        super(aId, aModel);

        abandonationTimeout = aAbandonationTimeout;

        ListView<AnnotationDocument> annotationStates = new ListView<>("item", aModel)
        {
            private static final long serialVersionUID = -8178383690721509334L;

            @Override
            protected void populateItem(ListItem<AnnotationDocument> aItem)
            {
                var row = aItem.getModelObject();
                var user = userRepository.get(aItem.getModelObject().getUser());

                var state = new WebMarkupContainer("state");
                aItem.queue(state);

                var labelModel = aItem.getModel() //
                        .map(AnnotationDocumentState::symbol) //
                        .orElse(NEW.symbol());
                var stateLabel = new Label("stateSymbol");

                if (isAbandoned(row)) {
                    labelModel = labelModel
                            .map(_label -> "<i class=\"fas fa-user-clock\"></i> " + _label);
                    aItem.add(new AttributeAppender("class", "bg-warning", " "));
                }
                else {
                    aItem.add(new AttributeAppender("class", "bg-secondary", " "));
                    state.add(AjaxEventBehavior.onEvent("click", _target -> stateLabel.send(
                            stateLabel, BUBBLE,
                            new AnnotatorColumnCellClickEvent(_target, row.getDocument(), user))));
                    state.add(new AttributeAppender("class", CSS_CLASS_STATE_TOGGLE, " "));
                }

                stateLabel.setDefaultModel(labelModel);
                stateLabel.setEscapeModelStrings(false); // SAFE - WE RENDER CONTROLLED SET OF ICONS
                aItem.queue(stateLabel);

                aItem.queue(new Label("annotatorName", user.getUiName()));

                state.add(new LambdaAjaxEventBehavior("contextmenu",
                        _t -> actionShowContextMenu(_t, state, row, user)).setPreventDefault(true));

                var showComment = new LambdaAjaxLink("showComment",
                        _t -> actionShowAnnotatorComment(_t, row.getDocument(), user));
                showComment.add(visibleWhen(() -> isNotBlank(row.getAnnotatorComment())));
                aItem.queue(showComment);
            }
        };

        add(annotationStates);
    }

    private void actionShowContextMenu(AjaxRequestTarget aTarget, Component aContext,
            AnnotationDocument aAnnDoc, User aUser)
    {
        send(aContext, BUBBLE, new AnnotatorStateOpenContextMenuEvent(aTarget, aContext,
                aAnnDoc.getDocument(), aUser, aAnnDoc.getState()));
        ;
    }

    private void actionShowAnnotatorComment(AjaxRequestTarget aTarget, SourceDocument aDoc,
            User aUser)
    {
        send(this, BUBBLE, new AnnotatorColumnCellShowAnnotatorCommentEvent(aTarget, aDoc, aUser));
    }

    private boolean isAbandoned(AnnotationDocument aAnnDoc)
    {
        if (aAnnDoc.getAnnotatorState() != IN_PROGRESS) {
            return false;
        }

        Duration idleTime = aAnnDoc.getTimestamp() != null
                ? between(aAnnDoc.getTimestamp().toInstant(), now())
                : null;

        return idleTime != null && !abandonationTimeout.isZero()
                && !abandonationTimeout.isNegative() && idleTime.compareTo(abandonationTimeout) > 0;
    }
}
