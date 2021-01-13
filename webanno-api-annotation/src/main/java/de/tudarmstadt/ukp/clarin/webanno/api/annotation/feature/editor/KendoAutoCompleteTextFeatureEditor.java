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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBindingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;

/**
 * String feature editor using a Kendo AutoComplete field.
 * 
 * <b>PROs</b>
 * <ul>
 * <li>Can be auto-focussed when an annotation is selected.</li>
 * <li>Description tooltips already work.</li>
 * <li>Server-side filtering</li>
 * <li>Re-focussing after safe does not work out of the box, but is covered by
 * wicket-jquery-focus-patch.js</li>
 * </ul>
 * 
 * <b>CONs</b>
 * <ul>
 * <li>Clicking into the input field does not directly open the suggestion list. Keyboard input is
 * required for the list to open.</li>
 * </ul>
 * 
 * <b>TODOs</b>
 * <ul>
 * <li>...?</li>
 * </ul>
 */
public class KendoAutoCompleteTextFeatureEditor
    extends TextFeatureEditorBase
{
    private static final long serialVersionUID = 8686646370500180943L;

    private final int maxResults;

    public KendoAutoCompleteTextFeatureEditor(String aId, MarkupContainer aItem,
            IModel<FeatureState> aModel, int aMaxResults, AnnotationActionHandler aHandler)
    {
        super(aId, aItem, aModel);

        AnnotationFeature feat = getModelObject().feature;
        StringFeatureTraits traits = readFeatureTraits(feat);

        maxResults = aMaxResults;

        add(new KeyBindingsPanel("keyBindings", () -> traits.getKeyBindings(), aModel, aHandler)
                // The key bindings are only visible when the label is also enabled, i.e. when the
                // editor is used in a "normal" context and not e.g. in the keybindings
                // configuration panel
                .add(visibleWhen(() -> getLabelComponent().isVisible())));
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
    }

    @Override
    protected AbstractTextComponent createInputField()
    {
        return new AutoCompleteTextField<ReorderableTag>("value")
        {
            private static final long serialVersionUID = 311286735004237737L;

            @Override
            public void onConfigure(JQueryBehavior behavior)
            {
                super.onConfigure(behavior);

                behavior.setOption("delay", 500);
                behavior.setOption("animation", false);
                behavior.setOption("footerTemplate",
                        Options.asString("#: instance.dataSource.total() # items found"));
                // Prevent scrolling action from closing the dropdown while the focus is on the
                // input field
                behavior.setOption("close",
                        String.join(" ", "function(e) {",
                                "  if (document.activeElement == e.sender.element[0]) {",
                                "    e.preventDefault();" + "  }", "}"));
                behavior.setOption("select", " function (e) { this.trigger('change'); }");
            }

            @Override
            protected List<ReorderableTag> getChoices(String aTerm)
            {
                List<ReorderableTag> matches = new ArrayList<>();

                // If adding own tags is allowed, the always return the current input as the
                // first choice.
                boolean inputAsFirstResult = isNotBlank(aTerm)
                        && KendoAutoCompleteTextFeatureEditor.this.getModelObject().feature
                                .getTagset().isCreateTag();
                if (inputAsFirstResult) {
                    matches.add(new ReorderableTag(aTerm, "New unsaved tag..."));
                }

                List<ReorderableTag> availableTags = new LinkedList<>(
                        KendoAutoCompleteTextFeatureEditor.this.getModelObject().tagset);

                // First add the re-ordered tags
                ListIterator<ReorderableTag> tagIterator = availableTags.listIterator();
                while (tagIterator.hasNext()) {
                    ReorderableTag t = tagIterator.next();

                    if (inputAsFirstResult && t.getName().equals(aTerm)) {
                        tagIterator.remove();
                        continue;
                    }

                    if (t.getReordered()) {
                        matches.add(t);
                        tagIterator.remove();
                    }
                }

                if (isBlank(aTerm)) {
                    availableTags.stream() //
                            .limit(Math.max(maxResults - matches.size(), 0)) //
                            .forEachOrdered(matches::add);
                    return matches;
                }

                // Now go through the remaining tags and try ranking them sensibly
                List<Pair<ReorderableTag, Integer>> scoredTags = new ArrayList<>();
                for (ReorderableTag t : availableTags) {
                    if (inputAsFirstResult && t.getName().equals(aTerm)) {
                        continue;
                    }

                    if (!containsIgnoreCase(t.getName(), aTerm)) {
                        continue;
                    }

                    if (!StringUtils.contains(t.getName(), aTerm)) {
                        scoredTags.add(Pair.of(t, 1));
                        continue;
                    }

                    if (!startsWithIgnoreCase(t.getName(), aTerm)) {
                        scoredTags.add(Pair.of(t, 2));
                        continue;
                    }

                    if (!startsWith(t.getName(), aTerm)) {
                        scoredTags.add(Pair.of(t, 3));
                        continue;
                    }

                    if (!t.getName().equals(aTerm)) {
                        scoredTags.add(Pair.of(t, 4));
                        continue;
                    }

                    scoredTags.add(Pair.of(t, 5));
                }

                Comparator<Pair<ReorderableTag, Integer>> cmp = comparing(Pair::getValue);
                cmp = cmp.reversed();
                Collections.sort(scoredTags, cmp);

                scoredTags.stream().limit(Math.max(maxResults - matches.size(), 0)) //
                        .map(Pair::getKey) //
                        .forEachOrdered(matches::add);

                return matches;
            }

            /*
             * Below is a hack which is required because all the text feature editors are expected
             * to write a plain string into the feature state. However, we cannot have an {@code
             * AutoCompleteTextField<String>} field because then we would loose easy access to the
             * tag description which we show in the tooltips. So we hack the converter to return
             * strings on the way out into the model. This is a very evil hack and we need to avoid
             * declaring generic types because we work against them!
             */
            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            public <C> IConverter<C> getConverter(Class<C> aType)
            {
                IConverter originalConverter = super.getConverter(aType);

                return new IConverter()
                {
                    private static final long serialVersionUID = -6505569244789767066L;

                    @Override
                    public Object convertToObject(String aValue, Locale aLocale)
                        throws ConversionException
                    {
                        Object value = originalConverter.convertToObject(aValue, aLocale);
                        if (value instanceof String) {
                            return value;
                        }
                        else if (value instanceof Tag) {
                            return ((Tag) value).getName();
                        }
                        else if (value instanceof ReorderableTag) {
                            return ((ReorderableTag) value).getName();
                        }
                        else {
                            return null;
                        }
                    }

                    @Override
                    public String convertToString(Object aValue, Locale aLocale)
                    {
                        return originalConverter.convertToString(aValue, aLocale);
                    }
                };
            }

            @Override
            protected IJQueryTemplate newTemplate()
            {
                return KendoChoiceDescriptionScriptReference.templateReorderable();
            }
        };
    }
}
