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
package de.tudarmstadt.ukp.inception.annotation.feature.misc;

import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.jquery.core.template.IJQueryTemplate;
import org.wicketstuff.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.support.kendo.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.inception.support.kendo.KendoStyleUtils;

public class ReorderableTagAutoCompleteField
    extends AutoCompleteTextField<ReorderableTag>
{
    private static final long serialVersionUID = 311286735004237737L;

    private final IModel<FeatureState> featureState;
    private final int maxResults;

    public ReorderableTagAutoCompleteField(String aId, IModel<FeatureState> aFeatureState,
            int aMaxResults)
    {
        super(aId);
        featureState = aFeatureState;
        maxResults = aMaxResults;
    }

    public ReorderableTagAutoCompleteField(String aId, IModel<ReorderableTag> aModel,
            IModel<FeatureState> aFeatureState, int aMaxResults)
    {
        super(aId, aModel);
        featureState = aFeatureState;
        maxResults = aMaxResults;
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
    }

    @Override
    protected List<ReorderableTag> getChoices(String aTerm)
    {
        var state = featureState.getObject();

        var ranker = new TagRanker();
        ranker.setMaxResults(maxResults);
        ranker.setTagCreationAllowed(state.getFeature().getTagset().isCreateTag());

        return ranker.rank(aTerm, state.tagset);
    }

    @Override
    public void onConfigure(JQueryBehavior aBehavior)
    {
        super.onConfigure(aBehavior);

        aBehavior.setOption("delay", 500);
        aBehavior.setOption("animation", false);
        aBehavior.setOption("footerTemplate",
                Options.asString("#: instance.dataSource.total() # items found"));
        aBehavior.setOption("open", KendoChoiceDescriptionScriptReference.applyTooltipScript());
        aBehavior.setOption("dataBound",
                KendoChoiceDescriptionScriptReference.applyTooltipScript());

        KendoStyleUtils.keepDropdownVisibleWhenScrolling(aBehavior);
        KendoStyleUtils.autoDropdownHeight(aBehavior);
        // KendoStyleUtils.autoDropdownWidth(aBehavior);

        aBehavior.setOption("select", " function (e) { this.trigger('change'); }");
    }

    /*
     * Below is a hack which is required because all the text feature editors are expected to write
     * a plain string into the feature state. However, we cannot have an {@code
     * AutoCompleteTextField<String>} field because then we would loose easy access to the tag
     * description which we show in the tooltips. So we hack the converter to return strings on the
     * way out into the model. This is a very evil hack and we need to avoid declaring generic types
     * because we work against them!
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
            public Object convertToObject(String aValue, Locale aLocale) throws ConversionException
            {
                Object value = originalConverter.convertToObject(aValue, aLocale);
                if (value instanceof String stringValue) {
                    return stringValue;
                }
                else if (value instanceof Tag tag) {
                    return tag.getName();
                }
                else if (value instanceof ReorderableTag reorderableTag) {
                    return reorderableTag.getName();
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
}
