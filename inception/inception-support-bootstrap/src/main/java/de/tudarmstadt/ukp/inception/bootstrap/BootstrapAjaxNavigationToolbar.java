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

import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;

import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.BootstrapPagingNavigator;
import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.BootstrapPagingNavigator.Size;
import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.table.toolbars.BootstrapNavigationToolbar;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;

public class BootstrapAjaxNavigationToolbar
    extends BootstrapNavigationToolbar
{
    private static final long serialVersionUID = 239894897639967507L;

    private BootstrapAjaxPagingNavigator nav;
    private int steps = 10;

    public BootstrapAjaxNavigationToolbar(DataTable<?, ?> aTable, Size aSize)
    {
        super(aTable, aSize);
    }

    public BootstrapAjaxNavigationToolbar(DataTable<?, ?> aTable)
    {
        super(aTable);
    }

    @Override
    protected BootstrapPagingNavigator newPagingNavigator(String aNavigatorId,
            DataTable<?, ?> aTable, Size aSize)
    {
        nav = new BootstrapAjaxPagingNavigator(aNavigatorId, aTable)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public Size getSize()
            {
                return aSize;
            }
        };
        nav.add(LambdaBehavior.onConfigure(() -> nav.getPagingNavigation().setViewSize(steps)));
        return nav;
    }

    public void setViewSize(int aSteps)
    {
        steps = aSteps;
    }
}
