/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
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
package de.tudarmstadt.ukp.inception.annotation.feature.link;

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditorEvent;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;

public abstract class LinkFeatureEvent
    extends FeatureEditorEvent
{
    private final LinkWithRoleModel linkWithRoleModel;

    public LinkFeatureEvent(FeatureEditor aEditor, AjaxRequestTarget aTarget,
            LinkWithRoleModel aLinkWithRoleModel)
    {
        super(aEditor, aTarget);
        linkWithRoleModel = aLinkWithRoleModel;
    }

    public LinkWithRoleModel getLinkWithRoleModel()
    {
        return linkWithRoleModel;
    }
}
