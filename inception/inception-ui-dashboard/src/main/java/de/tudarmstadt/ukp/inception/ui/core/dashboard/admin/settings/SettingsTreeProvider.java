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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.settings;

import java.util.Iterator;

import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class SettingsTreeProvider
    implements ITreeProvider<SettingsNode>
{
    private static final long serialVersionUID = 1L;

    private final IModel<SettingsNode> rootModel;

    public SettingsTreeProvider(IModel<SettingsNode> aRootModel)
    {
        rootModel = aRootModel;
    }

    @Override
    public Iterator<? extends SettingsNode> getRoots()
    {
        return rootModel.getObject().getChildren().iterator();
    }

    @Override
    public boolean hasChildren(SettingsNode aNode)
    {
        return !aNode.getChildren().isEmpty();
    }

    @Override
    public Iterator<? extends SettingsNode> getChildren(SettingsNode aNode)
    {
        return aNode.getChildren().iterator();
    }

    @Override
    public IModel<SettingsNode> model(SettingsNode aNode)
    {
        return Model.of(aNode);
    }

    @Override
    public void detach()
    {
        rootModel.detach();
    }
}
