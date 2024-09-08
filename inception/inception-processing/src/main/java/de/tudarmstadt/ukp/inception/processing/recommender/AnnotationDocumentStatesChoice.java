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
package de.tudarmstadt.ukp.inception.processing.recommender;

import static org.apache.wicket.markup.html.form.AbstractChoice.LabelPosition.AFTER;

import java.util.Collection;

import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.AttributeMap;
import org.apache.wicket.util.value.IValueMap;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;

public class AnnotationDocumentStatesChoice
    extends CheckBoxMultipleChoice<AnnotationDocumentState>
{
    private static final long serialVersionUID = 7627977162174971025L;

    public AnnotationDocumentStatesChoice(String aId)
    {
        this(aId, null);
    }

    public AnnotationDocumentStatesChoice(String aId,
            IModel<Collection<AnnotationDocumentState>> aModel)
    {
        super(aId);
        setModel(aModel);
        setPrefix("<div class=\"form-check form-switch\">");
        setSuffix("</div>");
        setLabelPosition(AFTER);
        setChoiceRenderer(new EnumChoiceRenderer<>(this));
    }

    @Override
    protected IValueMap getAdditionalAttributesForLabel(int aIndex, AnnotationDocumentState aChoice)
    {
        var attributes = new AttributeMap();
        attributes.put("class", "form-check-label");
        return attributes;
    }

    @Override
    protected IValueMap getAdditionalAttributes(int aIndex, AnnotationDocumentState aChoice)
    {
        var attributes = new AttributeMap();
        attributes.put("class", "form-check-input");
        return attributes;
    }
}
