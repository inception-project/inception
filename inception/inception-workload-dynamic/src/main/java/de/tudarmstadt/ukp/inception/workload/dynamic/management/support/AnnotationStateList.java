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
import static org.apache.wicket.event.Broadcast.BUBBLE;

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
import de.tudarmstadt.ukp.inception.workload.dynamic.management.support.event.AnnotatorColumnCellClickEvent;

public class AnnotationStateList
    extends Panel
{
    private static final long serialVersionUID = -8717096450102813443L;

    private @SpringBean UserDao userRepository;

    public AnnotationStateList(String aId, IModel<List<AnnotationDocument>> aModel)
    {
        super(aId, aModel);

        ListView<AnnotationDocument> annotationStates = new ListView<>("state", aModel)
        {
            private static final long serialVersionUID = -8178383690721509334L;

            @Override
            protected void populateItem(ListItem<AnnotationDocument> aItem)
            {
                AnnotationDocument row = aItem.getModelObject();
                AnnotationDocumentState state = aItem.getModel().map(AnnotationDocument::getState)
                        .orElse(NEW).getObject();
                Label stateLabel = new Label("stateSymbol", stateSymbol(state));
                stateLabel.setEscapeModelStrings(false);
                aItem.add(stateLabel);
                aItem.add(new Label("annotatorName",
                        userRepository.get(aItem.getModelObject().getUser()).getUiName()));
                aItem.add(new AttributeAppender("style", "cursor: pointer", ";"));
                aItem.add(AjaxEventBehavior.onEvent("click", //
                        _target -> stateLabel.send(stateLabel, BUBBLE,
                                new AnnotatorColumnCellClickEvent(_target, row.getDocument(),
                                        row.getUser()))));
            }
        };

        add(annotationStates);
    }

    private String stateSymbol(AnnotationDocumentState aDocState)
    {
        switch (aDocState) {
        case NEW:
            return "<i class=\"far fa-circle\"></i>";
        case IN_PROGRESS:
            return "<i class=\"far fa-play-circle\"></i>";
        case FINISHED:
            return "<i class=\"far fa-check-circle\"></i>";
        case IGNORE:
            return "<i class=\"fas fa-lock\"></i>";
        }

        return "";
    }
}
