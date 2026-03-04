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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;

/**
 * Shows clickable hyperlinks derived from the current annotation's hyperlink features. Only
 * features that have {@code hyperlinkEnabled=true} in their traits contribute links. The static URL
 * is read from {@code hyperlinkUrl} in the feature traits; the feature's UI name is used as the
 * link label. Because the set of visible features changes when constraints are re-evaluated (e.g.
 * selecting "chemical" makes the ChEBI feature visible, selecting "protein gene" makes the MeSH
 * feature visible), this panel must be added to the AJAX target whenever a feature value changes.
 */
public class HyperlinkLinksPanel
    extends GenericPanel<AnnotatorState>
{
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public HyperlinkLinksPanel(String aId, IModel<AnnotatorState> aModel)
    {
        super(aId, aModel);

        setOutputMarkupPlaceholderTag(true);

        var linksModel = LoadableDetachableModel.of(this::collectHyperlinks);

        var listView = new ListView<HyperlinkEntry>("links", linksModel)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<HyperlinkEntry> aItem)
            {
                var entry = aItem.getModelObject();
                var link = new ExternalLink("link", entry.url);
                link.add(new Label("label", entry.label));
                aItem.add(link);
            }
        };
        add(listView);

        add(visibleWhen(() -> !linksModel.getObject().isEmpty()));
    }

    @Override
    public boolean isEnabledInHierarchy()
    {
        return true;
    }

    private List<HyperlinkEntry> collectHyperlinks()
    {
        var result = new ArrayList<HyperlinkEntry>();
        var state = getModelObject();

        if (state == null) {
            return result;
        }

        for (var featureState : state.getFeatureStates()) {
            var url = getHyperlinkUrl(featureState);
            if (url == null || url.isBlank()) {
                continue;
            }

            var e = new HyperlinkEntry();
            e.label = featureState.feature.getUiName();
            e.url = url;
            result.add(e);
        }

        return result;
    }

    /**
     * Returns the static hyperlink URL from the feature's traits if {@code hyperlinkEnabled=true},
     * otherwise {@code null}.
     */
    private String getHyperlinkUrl(FeatureState aFeatureState)
    {
        try {
            var json = aFeatureState.feature.getTraits();
            if (json == null || json.isBlank()) {
                return null;
            }
            var node = MAPPER.readTree(json);
            if (!node.has("hyperlinkEnabled") || !node.get("hyperlinkEnabled").asBoolean(false)) {
                return null;
            }
            if (!node.has("hyperlinkUrl")) {
                return null;
            }
            return node.get("hyperlinkUrl").asText(null);
        }
        catch (Exception e) {
            LOG.debug("Error reading hyperlink traits for feature [{}]",
                    aFeatureState.feature.getName(), e);
            return null;
        }
    }

    private static class HyperlinkEntry
        implements Serializable
    {
        private static final long serialVersionUID = 1L;
        String label;
        String url;
    }
}
