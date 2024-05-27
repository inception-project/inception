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
package de.tudarmstadt.ukp.inception.support.kendo;

import static java.util.Arrays.asList;

import java.util.Set;

import org.apache.wicket.ClassAttributeModifier;
import org.apache.wicket.Component;

public class KendoFixDisabledInputComponentStylingBehavior
    extends ClassAttributeModifier
{
    private static final long serialVersionUID = 1L;

    private Component owner;

    @Override
    public void bind(Component aComponent)
    {
        owner = aComponent;
    }

    @Override
    protected Set<String> update(Set<String> aClasses)
    {
        aClasses.addAll(asList("k-input", "k-input-solid", "k-input-md", "k-rounded-md"));
        if (owner.isEnabledInHierarchy()) {
            aClasses.remove("k-disabled");
        }
        else {
            aClasses.add("k-disabled");
        }

        return aClasses;
    }
}
