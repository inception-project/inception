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

import static de.agilecoders.wicket.jquery.JQuery.markupId;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.jquery.Config;
import de.agilecoders.wicket.jquery.function.Function;

/**
 * @see <a href="https://github.com/l0rdn1kk0n/wicket-bootstrap/issues/924">Wicket Bootstrap Issue
 *      #924</a>
 */
public class PopoverBehavior
    extends de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverBehavior
{
    private static final long serialVersionUID = -3226509867257825927L;

    public PopoverBehavior(IModel<String> aLabel, IModel<String> aBody, PopoverConfig aConfig)
    {
        super(aLabel, aBody, aConfig);
    }

    @Override
    protected CharSequence createInitializerScript(final Component component, final Config config)
    {
        return new Function("new bootstrap.Popover", markupId(component).quoted(), config).build();
    }
}
