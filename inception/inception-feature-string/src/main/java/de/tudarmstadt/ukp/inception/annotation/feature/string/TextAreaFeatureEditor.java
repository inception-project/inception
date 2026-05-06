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
package de.tudarmstadt.ukp.inception.annotation.feature.string;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.SuggestionStatePanel;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class TextAreaFeatureEditor
    extends TextFeatureEditorBase
{
    private static final long serialVersionUID = 8686646370500180943L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public TextAreaFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel)
    {
        super(aId, aItem, aModel);

        add(new SuggestionStatePanel("suggestionInfo", aModel));
    }

    @Override
    protected AbstractTextComponent<String> createInputField()
    {
        var textarea = new TextArea<String>("value");
        try {
            var traitsString = getModelObject().feature.getTraits();
            var traits = JSONUtil.fromJsonString(StringFeatureTraits.class, traitsString);
            textarea.add(new AttributeModifier("rows", traits.getCollapsedRows()));
            textarea.add(new AttributeAppender("onfocus",
                    "this.rows=" + traits.getExpandedRows() + ";"));
            textarea.add(new AttributeAppender("onblur",
                    "this.rows=" + traits.getCollapsedRows() + ";"));
        }
        catch (IOException e) {
            LOG.error("Unable to create feature editor", e);
        }
        return textarea;
    }
}
