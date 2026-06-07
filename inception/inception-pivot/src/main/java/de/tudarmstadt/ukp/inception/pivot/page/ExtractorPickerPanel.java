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
package de.tudarmstadt.ukp.inception.pivot.page;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.pivot.report.ExtractorDecl;
import de.tudarmstadt.ukp.inception.support.lambda.AjaxPayloadCallback;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.WicketExceptionUtil;

public class ExtractorPickerPanel
    extends GenericPanel<List<ExtractorPickerPanel.LayerGroup>>
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IModel<? extends Collection<ExtractorDecl>> excludedModel;

    private AjaxPayloadCallback<ExtractorDecl> addAction;

    public ExtractorPickerPanel(String aId, IModel<List<LayerGroup>> aGroups,
            IModel<? extends Collection<ExtractorDecl>> aExcluded)
    {
        super(aId, aGroups);

        excludedModel = aExcluded;

        queue(new ListView<LayerGroup>("layerGroups", aGroups)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<LayerGroup> aItem)
            {
                var group = aItem.getModelObject();
                var excluded = excludedModel.getObject();
                var available = group.extractors().stream() //
                        .filter(e -> !excluded.contains(e)) //
                        .toList();

                aItem.setVisible(!available.isEmpty());
                aItem.add(new Label("layerName", group.label()));
                aItem.add(new ListView<ExtractorDecl>("extractors", available)
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<ExtractorDecl> aExt)
                    {
                        var extractor = aExt.getModelObject();

                        var link = new LambdaAjaxLink("addLink", t -> actionAdd(t, extractor));
                        link.add(new Label("extractorName", extractor.name()));
                        aExt.add(link);
                    }
                });
            }
        });
    }

    public ExtractorPickerPanel onAddAction(AjaxPayloadCallback<ExtractorDecl> aCallback)
    {
        addAction = aCallback;
        return this;
    }

    private void actionAdd(AjaxRequestTarget aTarget, ExtractorDecl aExtractor)
    {
        if (addAction == null) {
            return;
        }

        try {
            addAction.accept(aTarget, aExtractor);
        }
        catch (Exception e) {
            WicketExceptionUtil.handleException(LOG, getPage(), aTarget, e);
        }
    }

    public static record LayerGroup(String label, List<ExtractorDecl> extractors)
        implements Serializable
    {}
}
