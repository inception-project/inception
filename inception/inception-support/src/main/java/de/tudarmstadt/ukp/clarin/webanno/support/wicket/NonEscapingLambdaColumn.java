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
package de.tudarmstadt.ukp.clarin.webanno.support.wicket;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.danekja.java.util.function.serializable.SerializableFunction;

public class NonEscapingLambdaColumn<T, S>
    extends LambdaColumn<T, S>
{
    private static final long serialVersionUID = -2103168638018286379L;

    public NonEscapingLambdaColumn(IModel<String> aDisplayModel, S aSortProperty,
            SerializableFunction<T, ?> aFunction)
    {
        super(aDisplayModel, aSortProperty, aFunction);
    }

    public NonEscapingLambdaColumn(IModel<String> aDisplayModel,
            SerializableFunction<T, ?> aFunction)
    {
        super(aDisplayModel, aFunction);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> item, String componentId, IModel<T> rowModel)
    {
        item.add(new Label(componentId, getDataModel(rowModel)).setEscapeModelStrings(false));
    }
}
