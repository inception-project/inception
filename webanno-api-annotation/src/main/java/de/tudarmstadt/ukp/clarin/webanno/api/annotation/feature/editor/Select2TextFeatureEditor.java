/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.model.IModel;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;
import org.wicketstuff.select2.StringTextChoiceProvider;

import com.github.openjson.JSONException;
import com.github.openjson.JSONStringer;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;

/**
 * String feature editor using Select2.
 * 
 * <b>PROs</b>
 * <ul>
 * <li>Does nicely support paging/infinite scrolling!</li>
 * </ul>
 * 
 * <b>CONs</b>
 * <ul>
 * <li>Clicking "x" to clear the selection leaves a popup hanging around
 *     Cf. https://github.com/select2/select2/issues/3320</li>
 *       
 * <li>The input cannot be focussed and does not properly react to keyboard inputs. That
 *     Means tabbing through it doesn't work well/at all.</li>
 * </ul>
 * 
 * <b>TODOs</b>
 * <ul>
 * <li>Does not support description tooltips yet. Should be doable though.</li>
 * </ul>
 */
public class Select2TextFeatureEditor
    extends TextFeatureEditorBase
{
    private static final long serialVersionUID = 8686646370500180943L;

    public Select2TextFeatureEditor(String aId, MarkupContainer aItem,
            IModel<FeatureState> aModel)
    {
        super(aId, aItem, aModel);
    }

    @Override
    protected AbstractTextComponent createInputField()
    {
        Select2Choice<String> select = new Select2Choice<>("value");
        select.getSettings().setTags(true);
        select.getSettings().setPlaceholder("Select a value");
        select.getSettings().setAllowClear(true);
        select.getSettings().setMinimumInputLength(0);
        select.getSettings().setCloseOnSelect(true);
        select.getSettings().setTemplateResult("formatDescription");
        select.setProvider(new StringTextChoiceProvider()
        {
            private static final long serialVersionUID = 1317545703045324226L;

            @Override
            public void query(String aTerm, int aPage, Response<String> aResponse)
            {
                if (aPage == 0) {
                    aResponse.add("");
                }
                
//                // If adding own tags is allowed, the always return the current input as the first
//                // choice.
//                boolean inputAsFirstResult = aTerm != null && aPage == 0
//                        && TextFeatureEditor.this.getModelObject().feature.getTagset()
//                                .isCreateTag();
//                if (inputAsFirstResult) {
//                    aResponse.add(aTerm);
//                }
                
                List<String> matches = Select2TextFeatureEditor.this.getModelObject().tagset
                        .stream()
                        .filter(t -> isBlank(aTerm) || containsIgnoreCase(t.getName(), aTerm))
//                        // If we added the input term as the first result and by freak accident
//                        // it is even returned as a result, then skip it.
//                        .filter(t -> !(inputAsFirstResult && t.getName().equals(aTerm)))
                        .skip(aPage * 10)
                        .limit(11)
                        .map(Tag::getName)
                        .collect(Collectors.toList());
                
                aResponse.addAll(matches.subList(0, Math.min(matches.size(), 10)));
                aResponse.setHasMore(matches.size() > 10); 
            }
            
            @Override
            protected void toJson(String aChoice, JSONStringer aStringer) throws JSONException
            {
                String description = Select2TextFeatureEditor.this.getModelObject().tagset
                        .stream()
                        .filter(t -> t.getName().equals(aChoice))
                        .findFirst()
                        .map(t -> t.getDescription()).orElse("");
                
                super.toJson(aChoice, aStringer);
                
                //aStringer.key("description").value(description);
            }
        });
        return select;
    }
}
