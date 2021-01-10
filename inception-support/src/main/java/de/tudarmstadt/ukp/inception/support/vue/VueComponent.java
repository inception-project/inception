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
package de.tudarmstadt.ukp.inception.support.vue;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

public class VueComponent
    extends Panel
{
    private static final long serialVersionUID = 2590487883771158315L;

    public VueComponent(String aId, String aVueComponentFile)
    {
        this(aId, aVueComponentFile, null);
    }

    public VueComponent(String aId, String aVueComponentFile, IModel<?> aModel)
    {
        super(aId, aModel);

        add(new VueBehavior(new PackageResourceReference(this.getClass(), aVueComponentFile)));
    }
}
