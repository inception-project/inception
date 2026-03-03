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
package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.inception.support.lambda.AjaxPayloadCallback;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.WicketExceptionUtil;

public class SearchHistoryPanel
    extends GenericPanel<List<SearchHistoryItem>>
{
    private static final long serialVersionUID = 4262131425061296232L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private AjaxPayloadCallback<SearchHistoryItem> selectAction;
    private AjaxPayloadCallback<ListItem<SearchHistoryItem>> togglePinAction;
    private AjaxPayloadCallback<SearchHistoryItem> deleteAction;

    public SearchHistoryPanel(String aId, IModel<List<SearchHistoryItem>> aModel)
    {
        super(aId, aModel);

        queue(new WebMarkupContainer("noHistoryNotice") //
                .add(visibleWhen(aModel.map(List::isEmpty))));
        queue(new ListView<SearchHistoryItem>("entry", aModel)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<SearchHistoryItem> aItem)
            {
                aItem.setOutputMarkupId(true);
                var item = aItem.getModelObject();
                aItem.queue(new Icon("pinState",
                        aItem.getModel().map(i -> i.pinned() ? FontAwesome5IconType.star_s
                                : FontAwesome5IconType.star_r)));
                aItem.queue(new LambdaAjaxLink("delete", _target -> actionDelete(_target, item)));
                aItem.queue(new LambdaAjaxLink("pin", _target -> actionTogglePin(_target, aItem)));
                aItem.queue(new Label("query", aItem.getModelObject().query()));
                aItem.queue(new Label("details",
                        aItem.getModel().map(SearchHistoryPanel.this::renderDetails)));
                aItem.queue(new LambdaAjaxLink("link", _target -> actionSelect(_target, item)));
            }
        });
    }

    protected String renderDetails(SearchHistoryItem aItem)
    {
        var details = new StringBuilder();
        if (aItem.groupingFeature() != null) {

        }
        if (aItem.groupingLayer() != null) {

        }

        if (aItem.limitToDocument()) {
            details.append("current document ");
        }

        return details.toString();
    }

    public SearchHistoryPanel onSelectAction(AjaxPayloadCallback<SearchHistoryItem> aCallback)
    {
        selectAction = aCallback;
        return this;
    }

    public SearchHistoryPanel onTogglePinAction(
            AjaxPayloadCallback<ListItem<SearchHistoryItem>> aCallback)
    {
        togglePinAction = aCallback;
        return this;
    }

    public SearchHistoryPanel onDeleteAction(AjaxPayloadCallback<SearchHistoryItem> aCallback)
    {
        deleteAction = aCallback;
        return this;
    }

    private void actionSelect(AjaxRequestTarget aTarget, SearchHistoryItem aItem)
    {
        if (selectAction != null) {
            try {
                selectAction.accept(aTarget, aItem);
            }
            catch (Exception e) {
                WicketExceptionUtil.handleException(LOG, getPage(), aTarget, e);
            }
        }
    }

    private void actionDelete(AjaxRequestTarget aTarget, SearchHistoryItem aItem)
    {
        if (deleteAction != null) {
            try {
                deleteAction.accept(aTarget, aItem);
            }
            catch (Exception e) {
                WicketExceptionUtil.handleException(LOG, getPage(), aTarget, e);
            }
        }
    }

    private void actionTogglePin(AjaxRequestTarget aTarget, ListItem<SearchHistoryItem> aItem)
    {
        if (togglePinAction != null) {
            try {
                togglePinAction.accept(aTarget, aItem);
            }
            catch (Exception e) {
                WicketExceptionUtil.handleException(LOG, getPage(), aTarget, e);
            }
        }
    }
}
