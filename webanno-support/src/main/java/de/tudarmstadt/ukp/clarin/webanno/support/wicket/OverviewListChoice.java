/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.support.wicket;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.util.List;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.AppendingStringBuffer;

public class OverviewListChoice<T>
    extends ListChoice<T>
{
    private static final long serialVersionUID = 1L;

    public OverviewListChoice(String aId, IModel<? extends List<? extends T>> aChoices,
            IChoiceRenderer<? super T> aRenderer)
    {
        super(aId, aChoices, aRenderer);
    }

    public OverviewListChoice(String aId, IModel<? extends List<? extends T>> aChoices)
    {
        super(aId, aChoices);
    }

    public OverviewListChoice(String aId, IModel<T> aModel,
            IModel<? extends List<? extends T>> aChoices, IChoiceRenderer<? super T> aRenderer,
            int aMaxRows)
    {
        super(aId, aModel, aChoices, aRenderer, aMaxRows);
    }

    public OverviewListChoice(String aId, IModel<T> aModel,
            IModel<? extends List<? extends T>> aChoices, IChoiceRenderer<? super T> aRenderer)
    {
        super(aId, aModel, aChoices, aRenderer);
    }

    public OverviewListChoice(String aId, IModel<T> aModel,
            IModel<? extends List<? extends T>> aChoices)
    {
        super(aId, aModel, aChoices);
    }

    public OverviewListChoice(String aId, IModel<T> aModel, List<? extends T> aChoices,
            IChoiceRenderer<? super T> aRenderer, int aMaxRows)
    {
        super(aId, aModel, aChoices, aRenderer, aMaxRows);
    }

    public OverviewListChoice(String aId, IModel<T> aModel, List<? extends T> aChoices,
            IChoiceRenderer<? super T> aRenderer)
    {
        super(aId, aModel, aChoices, aRenderer);
    }

    public OverviewListChoice(String aId, IModel<T> aModel, List<? extends T> aChoices,
            int aMaxRows)
    {
        super(aId, aModel, aChoices, aMaxRows);
    }

    public OverviewListChoice(String aId, IModel<T> aModel, List<? extends T> aChoices)
    {
        super(aId, aModel, aChoices);
    }

    public OverviewListChoice(String aId, List<? extends T> aChoices,
            IChoiceRenderer<? super T> aRenderer)
    {
        super(aId, aChoices, aRenderer);
    }

    public OverviewListChoice(String aId, List<? extends T> aChoices)
    {
        super(aId, aChoices);
    }

    public OverviewListChoice(String aId)
    {
        super(aId);
    }
    
    @Override
    protected CharSequence getDefaultChoice(String aSelectedValue)
    {
        return "";
    }

    @Override
    protected void setOptionAttributes(AppendingStringBuffer aBuffer, T aChoice, int aIndex,
            String aSelected)
    {
        super.setOptionAttributes(aBuffer, aChoice, aIndex, aSelected);
        // if the choice was decorated with color, add this option to the html
        if (aChoice instanceof DecoratedObject) {
            DecoratedObject decorated = (DecoratedObject) aChoice;
            String color = defaultIfEmpty(decorated.getColor(), "black");
            aBuffer.append("style=\"color:" + color + ";\"");
        }
    }
    
    
}
