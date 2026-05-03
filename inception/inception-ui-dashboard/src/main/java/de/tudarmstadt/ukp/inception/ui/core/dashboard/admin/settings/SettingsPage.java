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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.settings;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.DefaultNestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.Folder;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.support.markdown.MarkdownLabel;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.AdminSettingsDashboardPageBase;

@MountPath("/admin/settings")
public class SettingsPage
    extends AdminSettingsDashboardPageBase
{
    private static final long serialVersionUID = 1L;

    // Two-step type shortener:
    // 1. PACKAGE_PREFIX strips lowercase-led, dotted package prefixes
    // (java.util.List<de.foo.Bar> -> List<Bar>).
    // 2. OUTER_CLASS_PREFIX strips outer-class chains in nested types
    // (Outer$Inner -> Inner, Outer$Inner$Deep -> Deep, Map$Entry -> Entry).
    // Both run inside generics safely because the patterns require a capitalized
    // identifier immediately after the prefix.
    private static final Pattern PACKAGE_PREFIX = Pattern.compile("(?:[a-z_][\\w]*\\.)+(?=[A-Z])");
    private static final Pattern OUTER_CLASS_PREFIX = Pattern
            .compile("(?:[A-Z]\\w*[\\.$])+(?=[A-Z])");

    private @SpringBean UserDao userService;
    private @SpringBean SpringConfigurationMetadataService metadataService;

    private final IModel<SettingsNode> rootModel;
    private final IModel<SettingsNode> selectedModel;
    private AbstractTree<SettingsNode> tree;
    private WebMarkupContainer detailContainer;
    private Label selectedPathLabel;

    public SettingsPage(final PageParameters aParameters)
    {
        super(aParameters);

        if (!userService.isCurrentUserAdmin()) {
            denyAccess();
        }

        rootModel = LoadableDetachableModel
                .of(() -> SettingsNode.buildTree(metadataService.listProperties()));
        var rootChildren = rootModel.getObject().getChildren();
        selectedModel = Model.of(rootChildren.isEmpty() ? null : rootChildren.get(0));

        tree = createTree();
        queue(tree);

        detailContainer = new WebMarkupContainer("detail");
        detailContainer.setOutputMarkupId(true);
        queue(detailContainer);

        selectedPathLabel = new Label("selectedPath",
                LoadableDetachableModel.of(() -> selectedModel.getObject() != null
                        ? selectedModel.getObject().getPath()
                        : ""));
        selectedPathLabel.setOutputMarkupId(true);
        queue(selectedPathLabel);

        var emptyState = new WebMarkupContainer("emptyState");
        emptyState.add(visibleWhen(() -> propertiesForSelection().isEmpty()));
        queue(emptyState);

        var propertiesContainer = new WebMarkupContainer("propertiesContainer");
        propertiesContainer.add(visibleWhen(() -> !propertiesForSelection().isEmpty()));
        queue(propertiesContainer);

        queue(new ListView<ConfigurationProperty>("properties",
                LoadableDetachableModel.of(this::propertiesForSelection))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ConfigurationProperty> aItem)
            {
                populateProperty(aItem);
            }
        });
    }

    private List<ConfigurationProperty> propertiesForSelection()
    {
        var selected = selectedModel.getObject();
        return selected != null ? selected.getProperties() : emptyList();
    }

    private DefaultNestedTree<SettingsNode> createTree()
    {
        return new DefaultNestedTree<SettingsNode>("tree", new SettingsTreeProvider(rootModel),
                Model.ofSet(new HashSet<>()))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected Component newContentComponent(String aId, IModel<SettingsNode> aNode)
            {
                return new Folder<SettingsNode>(aId, this, aNode)
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected IModel<String> newLabelModel(IModel<SettingsNode> aModel)
                    {
                        var node = aModel.getObject();
                        var ownCount = node.getProperties().size();
                        return Model.of(ownCount > 0 ? node.getName() + " (" + ownCount + ")"
                                : node.getName());
                    }

                    @Override
                    protected boolean isClickable()
                    {
                        return true;
                    }

                    @Override
                    protected void onClick(Optional<AjaxRequestTarget> aTarget)
                    {
                        var previous = selectedModel.getObject();
                        selectedModel.setObject(getModelObject());
                        aTarget.ifPresent(target -> {
                            if (previous != null) {
                                updateNode(previous, target);
                            }
                            updateNode(getModelObject(), target);
                            target.add(detailContainer, selectedPathLabel);
                        });
                    }

                    @Override
                    protected boolean isSelected()
                    {
                        return Objects.equals(getModelObject(), selectedModel.getObject());
                    }
                };
            }
        };
    }

    private void populateProperty(ListItem<ConfigurationProperty> aItem)
    {
        var view = aItem.getModelObject();

        if (view.isDeprecated()) {
            aItem.add(AttributeModifier.append("class", "deprecated"));
        }

        aItem.add(new Label("name", lastSegment(view.getName())));
        aItem.add(new Label("type", shortenType(view.getType())));

        var overridden = view.getEffectiveValue() != null;
        var displayedValue = overridden ? view.getEffectiveValue()
                : view.getDefaultValue() != null ? view.getDefaultValue() : "<unset>";
        var valueLabel = new Label("effectiveValue", displayedValue);
        if (!overridden) {
            // Fallback to the default value: render muted so it reads as "not explicitly set"
            valueLabel.add(AttributeModifier.append("class", "text-body-secondary"));
        }
        aItem.add(valueLabel);

        aItem.add(new Label("source",
                view.getEffectiveSource() != null ? view.getEffectiveSource() : "default"));

        // Only show the separate "default: X" row when an actual PropertySource overrode
        // the value (i.e. the source label is not "default") AND a default is known. Without
        // an override the displayed value already IS the default, so a comparison row would
        // be noise.
        aItem.add(new Label("defaultValue", view.getDefaultValue()) //
                .setVisible(view.getEffectiveSource() != null && view.getDefaultValue() != null));
        aItem.add(new MarkdownLabel("description", descriptionToMarkdown(view.getDescription()))
                .setVisible(view.getDescription() != null));
        aItem.add(new Label("deprecationNote", view.getDeprecationNote())
                .setVisible(view.getDeprecationNote() != null));
    }

    private static String shortenType(String aType)
    {
        if (aType == null) {
            return "";
        }
        var s = PACKAGE_PREFIX.matcher(aType).replaceAll("");
        return OUTER_CLASS_PREFIX.matcher(s).replaceAll("");
    }

    private static String lastSegment(String aName)
    {
        if (aName == null) {
            return "";
        }
        var dot = aName.lastIndexOf('.');
        return dot >= 0 ? aName.substring(dot + 1) : aName;
    }

    /**
     * Best-effort conversion of common Javadoc constructs in Spring configuration descriptions to
     * Markdown so they render nicely in {@link MarkdownLabel}.
     */
    private static String descriptionToMarkdown(String aText)
    {
        if (aText == null || aText.isEmpty()) {
            return aText;
        }
        var s = aText;
        // Inline Javadoc tags
        s = s.replaceAll("\\{@code\\s+([^}]+)\\}", "`$1`");
        s = s.replaceAll("\\{@link(?:plain)?\\s+([^}]+)\\}", "`$1`");
        s = s.replaceAll("\\{@literal\\s+([^}]+)\\}", "$1");
        // Block-level HTML
        s = s.replaceAll("(?i)<br\\s*/?>", "\n");
        s = s.replaceAll("(?i)<p>\\s*", "\n\n");
        s = s.replaceAll("(?i)</p>", "");
        // Lists: turn each <li>...</li> into "- ..." lines
        s = s.replaceAll("(?i)<li>\\s*", "\n- ");
        s = s.replaceAll("(?i)</li>", "");
        s = s.replaceAll("(?is)</?[uo]l>\\s*", "\n");
        // Strip any remaining tags defensively
        s = s.replaceAll("<[^>]+>", "");
        // Decode common HTML entities (decode &amp; last to avoid double-decoding)
        s = s.replace("&lt;", "<") //
                .replace("&gt;", ">") //
                .replace("&quot;", "\"") //
                .replace("&apos;", "'") //
                .replace("&amp;", "&");
        return s;
    }

    private void denyAccess()
    {
        getSession().error(format("Access to [%s] denied.", getClass().getSimpleName()));
        throw new RestartResponseException(getApplication().getHomePage());
    }
}
