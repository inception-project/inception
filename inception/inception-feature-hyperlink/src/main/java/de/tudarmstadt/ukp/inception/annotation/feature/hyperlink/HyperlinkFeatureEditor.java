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
package de.tudarmstadt.ukp.inception.annotation.feature.hyperlink;

import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class HyperlinkFeatureEditor
        extends FeatureEditor {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(HyperlinkFeatureEditor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<HyperlinkItem> items;
    private final FormComponent<?> dummyFocusComponent;

    public HyperlinkFeatureEditor(String aId, MarkupContainer aOwner, IModel<FeatureState> aModel) {
        super(aId, aOwner, aModel);

        items = new ArrayList<>(parseValue(aModel.getObject().value));

        WebMarkupContainer itemsContainer = new WebMarkupContainer("itemsContainer");
        itemsContainer.setOutputMarkupId(true);
        add(itemsContainer);

        ListView<HyperlinkItem> listView = new ListView<HyperlinkItem>("items", Model.ofList(items)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<HyperlinkItem> item) {
                item.setModel(new CompoundPropertyModel<>(item.getModel()));

                var inputGroup = new WebMarkupContainer("inputGroup");
                item.add(inputGroup);

                var text = new TextField<String>("text");
                text.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                        HyperlinkFeatureEditor.this::updateModel));
                inputGroup.add(text);

                var url = new TextField<String>("url");
                url.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                        HyperlinkFeatureEditor.this::updateModel));
                inputGroup.add(url);

                var launchLink = new ExternalLink("launch", item.getModel().map(i -> i.url))
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isEnabledInHierarchy()
                    {
                        return true;
                    }
                };
                inputGroup.add(launchLink);

                var remove = new AjaxLink<Void>("remove") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        items.remove(item.getModelObject());
                        target.add(itemsContainer);
                        updateModel(target);
                    }
                };
                inputGroup.add(remove);
            }
        };
        listView.setReuseItems(true);
        itemsContainer.add(listView);

        add(new AjaxLink<Void>("add") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                items.add(new HyperlinkItem());
                target.add(itemsContainer);
                updateModel(target);
            }
        });

        // Dummy component to satisfy getFocusComponent abstract method.
        dummyFocusComponent = new HiddenField<String>("dummy", Model.of(""));
        add(dummyFocusComponent);
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        LOG.debug("Re-parsing hyperlink value in onConfigure from: [{}]", getModelObject().value);
        items.clear();
        items.addAll(parseValue(getModelObject().value));
    }

    private void updateModel(AjaxRequestTarget target) {
        getModelObject().value = toJson(items);
        // Trigger generic update event that parent listens to
        send(this, BUBBLE, new FeatureEditorValueChangedEvent(this, target));
    }

    @Override
    public void addFeatureUpdateBehavior() {
        // We handle updates in inner components, so no-op here
    }

    @Override
    public FormComponent<?> getFocusComponent() {
        return dummyFocusComponent;
    }

    /**
     * Parses the stored JSON value into a list of hyperlink items.
     * Expected format: [{"text": "...", "url": "..."}, ...]
     */
    private List<HyperlinkItem> parseValue(Object value) {
        var list = new ArrayList<HyperlinkItem>();
        if (!(value instanceof String) || ((String) value).isBlank()) {
            return list;
        }

        var strValue = (String) value;

        // Try pipe-delimited format first (matching HyperlinkFeatureSupport)
        if (!strValue.startsWith("[") && !strValue.startsWith("{")) {
            var entries = strValue.split(";");
            for (var entry : entries) {
                var parts = entry.split("\\|", 2);
                var item = new HyperlinkItem();
                if (parts.length == 2) {
                    item.text = parts[0].replace("\\|", "|").replace("\\;", ";");
                    item.url = parts[1].replace("\\|", "|").replace("\\;", ";");
                } else {
                    item.url = parts[0].replace("\\|", "|").replace("\\;", ";");
                }
                list.add(item);
            }
            return list;
        }

        try {
            var node = MAPPER.readTree(strValue);
            if (node.isArray()) {
                for (JsonNode itemNode : node) {
                    list.add(MAPPER.treeToValue(itemNode, HyperlinkItem.class));
                }
            } else if (node.isObject()) {
                // Single object, wrap in list
                list.add(MAPPER.treeToValue(node, HyperlinkItem.class));
            }
        } catch (Exception e) {
            // If parsing fails, treat as plain URL
            var item = new HyperlinkItem();
            item.url = strValue;
            list.add(item);
        }
        return list;
    }

    /**
     * Serializes the list of hyperlink items to JSON array format.
     * Output format: [{"text": "...", "url": "..."}, ...]
     */
    private String toJson(List<HyperlinkItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        try {
            // Always return JSON array (list of dicts)
            return MAPPER.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            LOG.error("Error serializing hyperlink items to JSON", e);
            return null;
        }
    }

    public static class HyperlinkItem implements Serializable {
        private static final long serialVersionUID = 1L;
        public String text;
        public String url;
    }
}
