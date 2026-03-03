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
package de.tudarmstadt.ukp.inception.bootstrap;

import static org.apache.wicket.markup.html.form.AbstractChoice.LabelPosition.AFTER;

import java.util.Collection;
import java.util.List;

import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.AttributeMap;
import org.apache.wicket.util.value.IValueMap;

public class BootstrapCheckBoxMultipleChoice<T>
    extends CheckBoxMultipleChoice<T>
{
    private static final long serialVersionUID = -3652114210853457364L;

    public BootstrapCheckBoxMultipleChoice(String aId, IModel<? extends Collection<T>> aModel,
            IModel<? extends List<? extends T>> aChoices, IChoiceRenderer<? super T> aRenderer)
    {
        super(aId, aModel, aChoices, aRenderer);
    }

    public BootstrapCheckBoxMultipleChoice(String aId, IModel<? extends Collection<T>> aModel,
            IModel<? extends List<? extends T>> aChoices)
    {
        super(aId, aModel, aChoices);
    }

    public BootstrapCheckBoxMultipleChoice(String aId, IModel<? extends Collection<T>> aModel,
            List<? extends T> aChoices, IChoiceRenderer<? super T> aRenderer)
    {
        super(aId, aModel, aChoices, aRenderer);
    }

    public BootstrapCheckBoxMultipleChoice(String aId, IModel<? extends Collection<T>> aModel,
            List<? extends T> aChoices)
    {
        super(aId, aModel, aChoices);
    }

    public BootstrapCheckBoxMultipleChoice(String aId, IModel<? extends List<? extends T>> aChoices,
            IChoiceRenderer<? super T> aRenderer)
    {
        super(aId, aChoices, aRenderer);
    }

    public BootstrapCheckBoxMultipleChoice(String aId, IModel<? extends List<? extends T>> aChoices)
    {
        super(aId, aChoices);
    }

    public BootstrapCheckBoxMultipleChoice(String aId, List<? extends T> aChoices,
            IChoiceRenderer<? super T> aRenderer)
    {
        super(aId, aChoices, aRenderer);
    }

    public BootstrapCheckBoxMultipleChoice(String aId, List<? extends T> aChoices)
    {
        super(aId, aChoices);
    }

    public BootstrapCheckBoxMultipleChoice(String aId)
    {
        super(aId);
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        setPrefix("<div class=\"form-check form-switch\">");
        setSuffix("</div>");
        setLabelPosition(AFTER);
    }

    @Override
    protected IValueMap getAdditionalAttributesForLabel(int aIndex, T aChoice)
    {
        var attributes = new AttributeMap();
        attributes.put("class", "form-check-label");
        return attributes;
    }

    @Override
    protected IValueMap getAdditionalAttributes(int aIndex, T aChoice)
    {
        var attributes = new AttributeMap();
        attributes.put("class", "form-check-input");
        return attributes;
    }
}
